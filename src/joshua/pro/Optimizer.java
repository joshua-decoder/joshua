package joshua.pro;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;
import java.lang.Class;

import joshua.zmert.*;

//this class implements the PRO tuning method
public class Optimizer 
{
	public Optimizer( long _seed, int _sentNum, Vector<String> _output, double[] _initialTotalLambda, 
					  HashMap<String,String>[] _feat_hash, HashMap<String,String>[] _stats_hash, 
					  double _finalScore, EvaluationMetric _evalMetric, int _Tau, int _Xi, double _metricDiff, 
					  double[] _normalizationOptions, String _classifierAlg, String[] _classifierParam, 
					  String _trainMode, int _numSparseParam, int _numRegParam, String _nbestFormat )
	{
		sentNum	=	_sentNum;  //total number of training sentences
		output	=	_output;  //(not used for now)
		trainMode = _trainMode;
		nbestFormat = _nbestFormat;
		
		if( trainMode.equals("1") || trainMode.equals("2") || trainMode.equals("4") )
		{
			initialLambda	=	_initialTotalLambda;  //initial weights array
			paramDim	=	initialLambda.length-1; //because in ZMERT lambda array is given length paramNum+1
			
			regParamDim = _numRegParam-1; //only for printing information in mode 2
		}
		else if( trainMode.equals("3") )
		{
			//initialLambda is only the disc feature weights
			//paramDim = disc feature weights;
			if( _numSparseParam == 0 )
			{
				System.err.println("Mode 3: sparse feature number equals to 0, exiting ...");
				System.exit(35);
			}
			
			paramDim =  _numSparseParam;
			regParamDim = _numRegParam-1;
			initialLambda = new double[paramDim+1];
			int total_len = _initialTotalLambda.length;
			
			for( int i=0; i<paramDim; i++ )
				initialLambda[paramDim-i] = _initialTotalLambda[total_len-1-i];
			
			copyLambda = _initialTotalLambda;
		}
					
		finalScore	=	_finalScore;  //(not used for now)
		feat_hash 	=	_feat_hash;  //feature hash table
		stats_hash	=	_stats_hash;  //suff. stats hash table
		
		evalMetric	=	_evalMetric;  //evaluation metric
		Tau	=	_Tau;  //param Tau in PRO
		Xi	=	_Xi;  //param Xi in PRO
		metricDiff = _metricDiff;  //threshold for sampling acceptance
		normalizationOptions	=	_normalizationOptions;  //weight normalization option
		randgen = new Random(_seed);  //random number generator
		classifierAlg = _classifierAlg;  //classification algorithm
		classifierParam = _classifierParam;  //params for the specified classifier
	}
	
	public double[] run_Optimizer()
	{
		//sampling from all candidates
		Vector<String> allSamples = process_Params();
		
		try 
		{
			//create classifier object from the given class name string
			ClassifierInterface myClassifier = (ClassifierInterface) Class.forName(classifierAlg).newInstance();
			System.out.println("Total training samples(class +1 & class -1): "+allSamples.size());
			
			//thet intitialLambda & finalLambda are all trainable parameters
			myClassifier.setClassifierParam(classifierParam);  //set classifier parameters
			finalLambda = myClassifier.runClassifier(allSamples, initialLambda, paramDim);  //run classifier
			
			if( !trainMode.equals("3") )  //for mode 3, no need to normalize sparse feature weights 
				normalizeLambda(finalLambda);
			else
				normalizeLambda_mode3(finalLambda);
			
			/*for( int i=0; i<finalLambda.length; i++ )
				System.out.print(finalLambda[i]+" ");
			System.out.println();
			System.exit(0);*/
			
			double initMetricScore = computeCorpusMetricScore(initialLambda);  //compute the initial corpus-level metric score
			double finalMetricScore = computeCorpusMetricScore(finalLambda);  //compute the final corpus-level metric score
			
			//prepare the printing info
			int numParamToPrint = 0;
			String result = "";
			
			if( trainMode.equals("1") || trainMode.equals("4") )
			{
				numParamToPrint = paramDim > 10 ? 10 : paramDim;  //how many parameters to print
				result = paramDim > 10 ? "Final lambda(first 10): {" : "Final lambda: {";
				
				for( int i=1; i<numParamToPrint; i++ )  //in ZMERT finalLambda[0] is not used
					result += finalLambda[i]+" ";
			}
			else	
			{	
				int sparseNumToPrint = 0;
				if( trainMode.equals("2") )
				{
					result = "Final lambda(regular feats + first 5 sparse feats): {";
					for (int i = 1; i <= regParamDim; ++i)
						result += finalLambda[i]+" ";
					
					result += "||| ";
					
					sparseNumToPrint = 5 < (paramDim-regParamDim) ? 5 : (paramDim-regParamDim);
					
					for(int i=1; i<=sparseNumToPrint; i++)
						result += finalLambda[regParamDim+i]+" ";
				}
				else
				{
					result = "Final lambda(first 10 sparse feats): {";
					sparseNumToPrint = 10 < paramDim ? 10 : paramDim;
					
					for(int i=1; i<sparseNumToPrint; i++)
						result += finalLambda[i]+" ";
				}
			}
			
			output.add(result+finalLambda[numParamToPrint]+"}\n"+"Initial "+evalMetric.get_metricName()
					+": "+initMetricScore + "\nFinal "+evalMetric.get_metricName()+": "+finalMetricScore);
			
			//System.out.println(output);
			
			if( trainMode.equals("3") )
			{
				//finalLambda = baseline(unchanged)+disc
				for( int i=0; i<paramDim; i++ )
					copyLambda[i+regParamDim+1] = finalLambda[i];
				
				finalLambda = copyLambda;
			}
			
			return finalLambda;
		} 
		catch (ClassNotFoundException e) 
		{
			e.printStackTrace();
			System.exit(50);
		} 
		catch (InstantiationException e) 
		{
			e.printStackTrace();
			System.exit(55);
		} 
		catch (IllegalAccessException e) 
		{
			e.printStackTrace();
			System.exit(60);
		}
		
		return null;
	}
	
	public double computeCorpusMetricScore(double[] finalLambda)
	{
		int suffStatsCount = evalMetric.get_suffStatsCount();
		double modelScore;
		double maxModelScore;
		Set<String> candSet;
		String candStr;
		String[] feat_str;
		String[] tmpStatsVal = new String[suffStatsCount];
		int[] corpusStatsVal = new int[suffStatsCount];
		for( int i=0; i<suffStatsCount; i++ )
			corpusStatsVal[i] = 0;
		
		for( int i=0; i<sentNum; i++ )
		{
			candSet = feat_hash[i].keySet();
			
			//find out the 1-best candidate for each sentence
			//this depends on the training mode
			maxModelScore = -99999999999.0;
			for(Iterator it=candSet.iterator(); it.hasNext(); )
			{
				modelScore = 0.0;
				candStr = it.next().toString();
						
				feat_str = feat_hash[i].get(candStr).split("\\s+");
				
				if( nbestFormat.equals("dense") )
				{
					for(int f=0; f<feat_str.length; f++)
						modelScore += Double.parseDouble(feat_str[f])*finalLambda[f+1];
				}
				else
				{
					String[] feat_info;
					
					if( !trainMode.equals("3") )
					{
						for( int f=0; f<feat_str.length; f++ )
						{
							feat_info = feat_str[f].split(":");
							modelScore += Double.parseDouble(feat_info[1])*finalLambda[Integer.parseInt(feat_info[0])];
						}
					}
					else
					{
						int new_feat_id = 0;
						
						for( int f=0; f<feat_str.length; f++ )
						{
							feat_info = feat_str[f].split(":");
							
							//System.out.println(feat_info[0]+" "+Double.parseDouble(feat_info[1]));
							
							new_feat_id = Integer.parseInt(feat_info[0])-regParamDim; //for mode 3, re-index the sparse feature
																					  //to make it start from 1, so as to match
																					  //the lambda vector
							
							//only care about sparse features
							if( new_feat_id>=1 )
								modelScore += Double.parseDouble(feat_info[1])*finalLambda[new_feat_id];
						}
					}
				}
				
				if( maxModelScore < modelScore )
				{
					maxModelScore = modelScore;
					tmpStatsVal = stats_hash[i].get(candStr).split("\\s+");  //save the suff stats
				}
			}
			
			for( int j=0; j<suffStatsCount; j++ )
				corpusStatsVal[j] += Integer.parseInt(tmpStatsVal[j]);  //accumulate corpus-leve suff stats
		} //for( int i=0; i<sentNum; i++ )
		
		return evalMetric.score(corpusStatsVal);
	}
	
	public Vector<String> process_Params()
	{
		Vector<String> allSamples = new Vector<String>();  //to save all sampled pairs
		
		//sampling
		Vector<String> sampleVec = new Vector<String>();  //use String to make sparse representation easy
		for( int i=0; i<sentNum; i++ )
		{
			sampleVec = Sampler(i);
			allSamples.addAll(sampleVec);
		}
		
		return allSamples;
	}
	
	private Vector<String> Sampler( int sentId )
	{
		int candCount = stats_hash[sentId].size();
		Vector<String> sampleVec = new Vector<String>();
		HashMap<String, Double> candScore = new HashMap<String, Double>();  //metric(e.g BLEU) score of all candidates
		
		//extract all candidates to a string array to save time in computing BLEU score
		String[] cands = new String[candCount];
		Set<String> candSet = stats_hash[sentId].keySet();
		HashMap<Integer, String> candMap = new HashMap<Integer, String>();
		
		int candId = 0;
		for(Iterator it=candSet.iterator(); it.hasNext(); )
		{
			cands[candId] = it.next().toString();
			candMap.put(candId, cands[candId]); //map an integer to each candidate
			candId++;
		}
		candScore = compute_Score(sentId, cands);  //compute BLEU for each candidate
		
		//start to sample
		double scoreDiff;
		double probAccept;
		boolean accept;
		HashMap<String, Double> acceptedPair = new HashMap<String, Double>();
		
		if( Tau < candCount*(candCount-1) ) //otherwise no need to sample
		{
			int j1, j2;
			for( int i=0; i<Tau; i++ )
			{
				//here the case in which the same pair is sampled more than once is allowed
				//otherwise if Tau is almost the same as candCount^2, it might take a lot of time to find
				//Tau distinct pairs
				j1 = randgen.nextInt(candCount);
				j2 = randgen.nextInt(candCount);
				while(j1==j2)
					j2 = randgen.nextInt(candCount);
				
				//accept or not?
				scoreDiff = Math.abs(candScore.get(candMap.get(j1)) - candScore.get(candMap.get(j2)));
				probAccept = Alpha(scoreDiff);
				
				accept = randgen.nextDouble() <= probAccept ? true : false;  
				 
				if(accept)
					acceptedPair.put(j1+" "+j2, scoreDiff);
			}
		}
		else
		{	
			for( int i=0; i<candCount; i++ )
			{
				for ( int j=0; j<candCount; j++ )
				{
					if(j!=i)
					{	
						//accept or not?
						scoreDiff = Math.abs( candScore.get(candMap.get(i)) - candScore.get(candMap.get(j)) );
						probAccept = Alpha(scoreDiff);
						
						accept = randgen.nextDouble() <= probAccept ? true : false;
						 
						if(accept)
							acceptedPair.put(i+" "+j, scoreDiff);
					}
				}
			}
		}
		
		//System.out.println("Tau="+Tau+"\nAll possible pair number: "+candCount*(candCount-1));
		//System.out.println("Number of accepted pairs after random selection: "+acceptedNum);
		
		//sort sampled pairs according to "scoreDiff"
		ValueComparator comp = new ValueComparator(acceptedPair);
		TreeMap<String, Double> acceptedPairSort = new TreeMap<String, Double>(comp);
		acceptedPairSort.putAll(acceptedPair);
		
		int topCount = 0;
		int label;
		String[] pair_str;
		String[] feat_str_j1, feat_str_j2;
		String j1Cand, j2Cand;
		String featDiff, neg_featDiff;
		HashSet<String> added = new HashSet<String>();  //to avoid symmetric duplicate
		
		for( String key :  acceptedPairSort.keySet() )
		{
			if( topCount == Xi )
				break;
			
			pair_str = key.split("\\s+");
			//System.out.println(pair_str[0]+" "+pair_str[1]+" "+acceptedPair.get(key));
			
			if( ! added.contains(key) )
			{
				j1Cand = candMap.get(Integer.parseInt(pair_str[0]));
				j2Cand = candMap.get(Integer.parseInt(pair_str[1]));
				
				if( evalMetric.getToBeMinimized() )  //if smaller metric score is better(like TER)
					label = (candScore.get(j1Cand) - candScore.get(j2Cand)) < 0 ? 1 : -1;
				else  //like BLEU
					label = (candScore.get(j1Cand) - candScore.get(j2Cand)) > 0 ? 1 : -1;	
				
				feat_str_j1 = feat_hash[sentId].get(j1Cand).split("\\s+");
				feat_str_j2 = feat_hash[sentId].get(j2Cand).split("\\s+");
	
				featDiff = "";
				neg_featDiff = "";
				
				if( nbestFormat.equals("dense") )
				{
					for( int i=0; i<feat_str_j1.length; i++ )
					{
						featDiff += (i+1)+":"+( Double.parseDouble(feat_str_j1[i]) - Double.parseDouble(feat_str_j2[i]) ) + " ";
						neg_featDiff += (i+1)+":"+( Double.parseDouble(feat_str_j2[i]) - Double.parseDouble(feat_str_j1[i]) ) + " ";
					}
				}
				else
				{
					HashMap<String, String> firing_feat = new HashMap<String, String>();
					String[] feat_info;
					
					for( int i=0; i<feat_str_j1.length; i++ )
					{
						feat_info = feat_str_j1[i].split(":");
						firing_feat.put(feat_info[0], feat_info[1]);
					}
					
					if( !trainMode.equals("3") )
					{
						for( int i=0; i<feat_str_j2.length; i++ )
						{
							feat_info = feat_str_j2[i].split(":");
							
							if( firing_feat.containsKey(feat_info[0]) )
							{
								featDiff += feat_info[0]+":"+( Double.parseDouble(firing_feat.get(feat_info[0])) - 
										Double.parseDouble(feat_info[1]) )  + " ";
								neg_featDiff += feat_info[0]+":"+( Double.parseDouble(feat_info[1]) - 
										Double.parseDouble(firing_feat.get(feat_info[0])) )  + " ";
							}
						}
					}
					else
					{
						int new_feat_id = 0;
						
						for( int i=0; i<feat_str_j2.length; i++ )
						{
							feat_info = feat_str_j2[i].split(":");
							new_feat_id = Integer.parseInt(feat_info[0])-regParamDim; //for mode 3, re-index the sparse feature
																					  //to make it start from 1, so as to match
																					  //the lambda vector
							
							//only cares about sparse features
							if( firing_feat.containsKey(feat_info[0]) && new_feat_id>=1 )
							{
								featDiff += new_feat_id+":"+( Double.parseDouble(firing_feat.get(feat_info[0])) - 
										Double.parseDouble(feat_info[1]) )  + " ";
								neg_featDiff += new_feat_id+":"+( Double.parseDouble(feat_info[1]) - 
										Double.parseDouble(firing_feat.get(feat_info[0])) )  + " ";
							}
						}
					}
				}
					
				featDiff += label;
				neg_featDiff += -label;
				
				//System.out.println(featDiff);
				
				//System.out.println(featDiff + " | " + candScore.get(j1Cand) + " " + candScore.get(j2Cand));
				//System.out.println(neg_featDiff + " | " + acceptedPair.get(key));
				
				sampleVec.add(featDiff);
				sampleVec.add(neg_featDiff);
				
				//both (j1,j2) and (j2,j1) have been added to training set
				added.add(key);
				added.add(pair_str[1]+" "+pair_str[0]);
				
				topCount++;
			}
		}
		
		//System.out.println("Selected top "+topCount+ "pairs for training");
		
		return sampleVec;
	}
	
	private double Alpha(double x)
	{
		return x < metricDiff ? 0 : 1;  //default implementation of the paper's method
								        //other functions possible
	}
	
	//compute *sentence-level* metric score
	private HashMap<String, Double> compute_Score( int sentId, String[] cands )
	{
		HashMap<String, Double> candScore = new HashMap<String, Double>();
		String statString;
		String[] statVal_str;
		int[] statVal = new int[evalMetric.get_suffStatsCount()];
		
		//for all candidates
		for( int i=0; i<cands.length; i++ )
		{
			statString = stats_hash[sentId].get(cands[i]);
			statVal_str = statString.split("\\s+");
			
			for( int j=0; j<evalMetric.get_suffStatsCount(); j++ )
				statVal[j] = Integer.parseInt(statVal_str[j]);
			
			candScore.put(cands[i], evalMetric.score(statVal));
		}
		
		return candScore;
	}
	
	//from ZMERT
	private void normalizeLambda(double[] origLambda)
	{
		  // private String[] normalizationOptions;
	      // How should a lambda[] vector be normalized (before decoding)?
	      //   nO[0] = 0: no normalization
	      //   nO[0] = 1: scale so that parameter nO[2] has absolute value nO[1]
	      //   nO[0] = 2: scale so that the maximum absolute value is nO[1]
	      //   nO[0] = 3: scale so that the minimum absolute value is nO[1]
	      //   nO[0] = 4: scale so that the L-nO[1] norm equals nO[2]

		int normalizationMethod = (int)normalizationOptions[0];
	    double scalingFactor = 1.0;
	    if (normalizationMethod == 0) 
	    {
	    	scalingFactor = 1.0;
	    } 
	    else if (normalizationMethod == 1) 
	    {
	    	int c = (int)normalizationOptions[2];
	    	scalingFactor = normalizationOptions[1]/Math.abs(origLambda[c]);
	    } 
	    else if (normalizationMethod == 2) 
	    {
	    	double maxAbsVal = -1;
	    	int maxAbsVal_c = 0;
	    	for (int c = 1; c <= paramDim; ++c) 
	    	{
	    		if (Math.abs(origLambda[c]) > maxAbsVal) 
	    		{
	    			maxAbsVal = Math.abs(origLambda[c]);
	    			maxAbsVal_c = c;
	    		}
	    	}
	    	scalingFactor = normalizationOptions[1]/Math.abs(origLambda[maxAbsVal_c]);

	    } 
	    else if (normalizationMethod == 3) 
	    {
	    	double minAbsVal = PosInf;
	    	int minAbsVal_c = 0;
	    	
	    	for (int c = 1; c <= paramDim; ++c) 
	    	{
	    		if (Math.abs(origLambda[c]) < minAbsVal) 
	    		{
	    			minAbsVal = Math.abs(origLambda[c]);
	    			minAbsVal_c = c;
	    		}
	    	}
	    	scalingFactor = normalizationOptions[1]/Math.abs(origLambda[minAbsVal_c]);

	    } 
	    else if (normalizationMethod == 4) 
	    {
	    	double pow = normalizationOptions[1];
	    	double norm = L_norm(origLambda,pow);
	    	scalingFactor = normalizationOptions[2]/norm;
	    }

	    for (int c = 1; c <= paramDim; ++c) 
	    {
	    	origLambda[c] *= scalingFactor;
	    }
	}
	
	//from ZMERT
	private double L_norm(double[] A, double pow)
	{
	    // calculates the L-pow norm of A[]
	    // NOTE: this calculation ignores A[0]
		double sum = 0.0;
	    for (int i = 1; i < A.length; ++i) 
	      sum += Math.pow(Math.abs(A[i]),pow);
	    	
	    return Math.pow(sum,1/pow);
	}
	
	//simply limit the sparse feature weight range to [-1,1]
	private void normalizeLambda_mode3(double[] origLambda)
	{
		double max = NegInf;
		double min = PosInf;
		double scaling = 0.0;
		
		for( int i=1; i<origLambda.length; i++ )
		{
			if( origLambda[i] > max )
				max = origLambda[i];
			if( origLambda[i] < min )
				min = origLambda[i];
		}
		
		if( Math.abs(max)>1e-30 || Math.abs(min)>1e-30 ) //not all weights are zero
		{
			if( max>0 && min>0 )
				scaling = max;
			else if( max>0 && min<0 )
			{
				if( Math.abs(max) > Math.abs(min) )
					scaling = max;
				else
					scaling = Math.abs(min);
			}
			else if( max<=0 )
				scaling = Math.abs(min);
			
			for( int i=1; i<origLambda.length; i++ )
				origLambda[i] /= scaling;
		}
	}
	
	private EvaluationMetric evalMetric;
	private Vector<String> output;
	private double[] initialLambda; //the Lambdas should correspond to those trainable features
	private double[] finalLambda; //the Lambdas should correspond to those trainable features
	private double[] copyLambda; //only used in mode 3
	private double finalScore;
	private double[] normalizationOptions;
	private HashMap<String, String>[] feat_hash;
	private HashMap<String, String>[] stats_hash;
	private Random randgen;
	private int paramDim; //this should be the dimension of parameters that are TRAINABLE
						  //mode 1: paramDim = regular feat num
						  //mode 2: paramDim = regular feat num + disc feat num
						  //mode 3: paramDim = disc feat num
						  //mode 4: paramDim = regular feat num + 1
	private int regParamDim; //sparse feat dim - only used for mode 3
	private int sentNum;
	private String trainMode; //training mode
	private int Tau; //size of sampled candidate set(say 5000)
	private int Xi; //choose top Xi candidates from sampled set(say 50)
	private double metricDiff; //metric difference threshold(to select the qualified candidates)
	private String classifierAlg; //optimization algorithm
	private String nbestFormat;
	private String[] classifierParam;
	
	private final static double NegInf = (-1.0 / 0.0);
	private final static double PosInf = (+1.0 / 0.0);
}

class ValueComparator implements Comparator
{
	Map base;
	public ValueComparator(Map base) 
	{
		this.base = base;
	}

	public int compare(Object a, Object b) 
	{
		if((Double)base.get(a) <= (Double)base.get(b))
			return 1;
		else if((Double)base.get(a) == (Double)base.get(b))
			return 0;
		else 
			return -1;
  }	
}
