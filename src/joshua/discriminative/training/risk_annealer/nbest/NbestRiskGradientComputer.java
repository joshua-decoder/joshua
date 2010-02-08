package joshua.discriminative.training.risk_annealer.nbest;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import joshua.decoder.BLEU;
import joshua.decoder.NbestMinRiskReranker;
import joshua.discriminative.FileUtilityOld;
import joshua.discriminative.training.risk_annealer.GradientComputer;

/** 
* @author Zhifei Li, <zhifei.work@gmail.com>
* @version $LastChangedDate: 2008-10-20 00:12:30 -0400 $
*/

public class NbestRiskGradientComputer extends GradientComputer {
//	### in general, if the nbest itself change, all the following values need to be changed
	//size: the number of hyp in the nbest
	//ArrayList<String> l_nbest_translations;//the translation itself: each source has multiple hypothesized translations
	//ArrayList<String> l_ref;
	private List<Double> gainsWithRespectToRef =  new ArrayList<Double>();;
	
	//size:the number of hyp in the nbest * num_features
	private List<Double> featureValues=   new ArrayList<Double>();;//each hyp has multiple features
	
	//size: number of source sentences
	private List<Integer> startPoss =  new ArrayList<Integer>();//inclusive
	private List<Integer> endPoss =  new ArrayList<Integer>();//exclusive
	
	//### if the weight vector or scale changes, we need to change the following two lists 
	//size: the number of hyp in the nbest
	private List<Double> hypProbs =  new ArrayList<Double>();
	//size: the number of source sentences * num_features
	private List<Double> expectedFeatureValues= new ArrayList<Double>();//each source sentence has a vector
	
	//for tuning of scaling factor
	//ArrayList<Double> l_hyp_final_score =  new ArrayList<Double>(); // this is the linear sum, no scaling
	//ArrayList<Double> l_expected_hyp_final_score = new ArrayList<Double>();//each source sentence has an expected score
	
	private boolean useLogBleu=false;
	
	//============== google linear corpus gain
	private boolean useGoogleLinearCorpusGain = true;
	double[] linearCorpusGainThetas; //weights in the Goolge linear corpus gain function
	
	//### nums
	private int totalNumSent;
	

	private double expectedGainSum;
	private double entropySum;
	
	private String nbesFile;
	private String[] refFiles;
	
	private boolean useShortestRefLen=true;
	
	
	//## for BLEU
	private static int bleuOrder =4;
	private static boolean doNgramClip = true;
		
	/*whenever the nbesFile_ or refFile_ changes, we need to reconstruct everything*
	 */
	public NbestRiskGradientComputer(String nbesFile, String[] refFiles, boolean useShortestRefLen, int totalNumSent,  
			int numFeatures, double gainFactor, double annealingScale, double coolingTemperature, boolean computeScalingGradient, double[] linearCorpusGainThetas){
		super( numFeatures, gainFactor, annealingScale, coolingTemperature, computeScalingGradient);
		this.nbesFile = nbesFile;
		this.refFiles = refFiles;
		this.useShortestRefLen = useShortestRefLen;
		this.totalNumSent = totalNumSent;
		
		this.linearCorpusGainThetas = linearCorpusGainThetas;
		if(this.linearCorpusGainThetas!=null){
			this.useGoogleLinearCorpusGain = true;
			//System.out.println("============= use google corpuse blue ========="+ linearCorpusGainThetas[0] +"; " +linearCorpusGainThetas[1]);
		}else
			this.useGoogleLinearCorpusGain = false;
		
		preprocessCorpus(this.nbesFile, this.refFiles);
	}
	
	/*use the latest weights, annealing_scale, and cooling_temperature
	 * the objective here is to maximize the expected gain
	 **/
	public void reComputeFunctionValueAndGradient(double[] weights) {
		double[] weights2 = weights; 
		if(shouldComputeGradientForScalingFactor){//first weight is for scaling parameter
			//==sanity check
			if(weights.length!=numFeatures+1){System.out.println("number of weights is not right"); System.exit(1);}
			
			scalingFactor = weights[0];//!!!!!!!!!! big bug: old code does not have this!!!!!!!!!!!!!!!
			//System.out.println("scaling is " + annealing_scale + "; weight is " + weights[0]);
			
			weights2 = new double[numFeatures];
			for(int i=0; i<numFeatures; i++)
				weights2[i] = weights[i+1];
		}
		
		//==reset values
		for(int i=0; i<numFeatures; i++)
			gradientsForTheta[i] = 0;
		if(shouldComputeGradientForScalingFactor)
			gradientForScalingFactor = 0;
		functionValue = 0;
		
		//### preprocessing and inference on the nbests
		redoCorpusInference(weights2, scalingFactor);
		
		//### compute gradient
		computeCorpusGradient(weights2, gradientsForTheta, temperature, scalingFactor);
		
		//### compute function value
		computeCorpusFuncVal(temperature);
		
		//printLastestStatistics();
		//System.exit(1);
	}
	
	public void printLastestStatistics(){
	 	System.out.println("Func value=" + getLatestFunctionValue() + "="  + getLatestExpectedGain()+"*"+gainFactor + "+" + getLatestEntropy()+"*"+temperature);
	 	System.out.println("AVG Expected_gain=" + (getLatestExpectedGain())/totalNumSent+ "%; avg entropy=" + getLatestEntropy()/totalNumSent);
	}
	
	
	private double getLatestEntropy(){
		if(Double.isNaN(entropySum)){System.out.println("func_val isNaN"); System.exit(1);} 
		return entropySum;
	}
	
	private double getLatestExpectedGain(){
		if(Double.isNaN(expectedGainSum)){System.out.println("func_val isNaN"); System.exit(1);} 
		return expectedGainSum;
	}
	
	
	
//######preprocess:	create: l_feature_value, l_gain_withrespectto_ref, l_start_pos, l_end_pos
	//do not need to store nbest and reference themselves
	private void preprocessCorpus(String nbestFile, String[] refFiles){
		System.out.println("preprocess nbest " + nbestFile + " and ref files " + refFiles);
		//### process nbest file
		BufferedReader nbestReader = FileUtilityOld.getReadFileStream(nbestFile,"UTF-8");
		BufferedReader[] refReaders = new BufferedReader[refFiles.length];
		for(int i=0; i<refFiles.length; i++)
			refReaders[i] = FileUtilityOld.getReadFileStream(refFiles[i],"UTF-8");
		
		String line=null;
		int oldSentId=-1;
		List<String> nbest = new ArrayList<String>();
		while((line=FileUtilityOld.readLineLzf(nbestReader))!=null){
			String[] fds = line.split("\\s+\\|{3}\\s+");
			int newSentID = new Integer(fds[0]);
			if(oldSentId!=-1 && oldSentId!=newSentID){
				String[] refs = new String[refReaders.length];
				for(int i=0; i<refReaders.length; i++)
				    refs[i]=FileUtilityOld.readLineLzf(refReaders[i]);
				preprocessSentNbest(nbest, oldSentId, refs);			
				nbest.clear();
			}
			oldSentId = newSentID;
			nbest.add(line);
		}
		//last source sentence
		String[] refs = new String[refReaders.length];
		for(int i=0; i<refReaders.length; i++)
		    refs[i]=FileUtilityOld.readLineLzf(refReaders[i]);
		preprocessSentNbest(nbest, oldSentId, refs);			
		nbest.clear();
		
		FileUtilityOld.closeReadFile(nbestReader);
		for(int i=0; i<refReaders.length; i++)
			FileUtilityOld.closeReadFile(refReaders[i]);
		
		System.out.println("after proprecessing");
		//System.out.println("l_start_pos size " + l_start_pos.toString());
		//System.out.println("l_end_pos size " + l_end_pos.toString());
		System.out.println("featureValues size " + featureValues.size());
		System.out.println("gainsWithRespectToRef size " + gainsWithRespectToRef.size());
		//System.exit(1);
	}
	
	
	
	
	
	private void preprocessSentNbest(List<String> nbest, int sentID, String[] refs){
		//### add start and end pos
		int start_pos = gainsWithRespectToRef.size();//inclusive
		int end_pos = start_pos + nbest.size();//exclusive
		startPoss.add(start_pos);
		endPoss.add(end_pos);
		
		//### compute gain for each hyp corresponding to ref; and add feature values
		for(String hyp : nbest){
			String[] fds = hyp.split("\\s+\\|{3}\\s+");			
			//gain
			double gain=0;
			if(useGoogleLinearCorpusGain){
				int hypLength = fds[1].split("\\s+").length;
				HashMap<String, Integer> refereceNgramTable = BLEU.constructMaxRefCountTable(refs, bleuOrder);
				HashMap<String, Integer> hypNgramTable = BLEU.constructNgramTable(fds[1], bleuOrder);
				gain = BLEU.computeLinearCorpusGain(linearCorpusGainThetas, hypLength, hypNgramTable,  refereceNgramTable); 
			}else{
				gain = BLEU.computeSentenceBleu(refs, fds[1], doNgramClip, bleuOrder, useShortestRefLen);
			}
			
			if(useLogBleu){
				if(gain==0)
					gainsWithRespectToRef.add(0.0);//log0=0
				else
					gainsWithRespectToRef.add(Math.log(gain));
			}else
				gainsWithRespectToRef.add(gain);
			//System.out.println("Gain is: " +gain + "||| " + ref + "||| " +fds[1]);
			hypProbs.add(0.0);//add fake probe
			
			//feat values
			String[] logFeatProb = fds[2].split("\\s+");
			for(int i=0; i< logFeatProb.length; i++){
				featureValues.add(new Double(logFeatProb[i]));
			}			
		}
		 
		//add fake feature expectations
		for(int i=0; i< numFeatures; i++){
			expectedFeatureValues.add(0.0);
		}
		
		//if(sent_id==1) System.exit(1);
	}
		
	
//=================Inference: based on current weight vector, scaling_factor
//	change l_hyp_probability and l_expected_feature_value, optional: l_hyp_final_score and l_expected_feature_value
	private void redoCorpusInference(double[] weights, double scaling_factor){
		for(int i=0; i<totalNumSent; i++){
			redoSentInference(i, weights, scaling_factor);
		}
		//System.exit(1);
	}	
	
	private void redoSentInference(int sent_id, double[] weights, double scaling_factor){
		int start_pos = startPoss.get(sent_id);
		int end_pos = endPoss.get(sent_id);
		List<Double> nbestLogProbs = hypProbs.subList(start_pos, end_pos);
		
		//### first reset nbest_logprobs to the new final score, this reflects the change of weight vector
		for(int i=0; i< nbestLogProbs.size(); i++){
			double final_score = 0;;
			for(int j=0; j<numFeatures; j++){
				double hyp_feat_val = getFeatVal(start_pos, i, j);
				final_score += hyp_feat_val*weights[j];
			}
			if(Double.isNaN(final_score)){
				System.out.println("final_score is NaN, must be wrong; " + final_score);
				for(int t=0; t<weights.length;t++)
					System.out.println("weight: "+ weights[t]);
				System.exit(1);
			}
			nbestLogProbs.set(i, final_score);
		}
		
		//### change the probability distribution
		NbestMinRiskReranker.computeNormalizedProbs(nbestLogProbs, scaling_factor);//this will automatically change l_hyp_probability
		
		//### re-compute the expectation of feature values		
		double[] expectedValues = new double[numFeatures];
		for(int i=0; i< nbestLogProbs.size(); i++){
			double prob = nbestLogProbs.get(i);
			for(int j=0; j<numFeatures; j++){
				double hypFeatVal = getFeatVal(start_pos, i, j);
				expectedValues[j] += hypFeatVal*prob;
			}
		}
		
		//set the expected feature values
		List<Double> expecedFeatScores = getSentExpectedFeatureScoreList(sent_id);
		double t_expected_sum=0;
		for(int j=0; j<numFeatures; j++){
			expecedFeatScores.set(j, expectedValues[j]);
			t_expected_sum +=  expectedValues[j]*weights[j];
		}
		//System.out.println("sub list size is " + l_expeced_feat_scores);
		
	}
//=================Inference: END
	
	
//=================compute Gradient
	private void computeCorpusGradient(double[] weights, double[] gradients, double temperature, double scale){	
		for(int i=0; i<totalNumSent; i++){
			accumulateSentGradient(i, temperature, weights, gradients, scale);
		}
	}
	
	//accumulate sentence gradient into gradients
	private void accumulateSentGradient(int sentID, double temperature,  double[] weights, double[] gradients, double scale){
		int start_pos = startPoss.get(sentID);
		int end_pos = endPoss.get(sentID);
		List<Double> nbestProbs = hypProbs.subList(start_pos, end_pos);
		List<Double> gainWithRespecitToRef = gainsWithRespectToRef.subList(start_pos, end_pos);
		List<Double> expectedFeatureValues = getSentExpectedFeatureScoreList(sentID);		
		
		double expectedHypFinalScore = 0;
		for(int j=0; j<numFeatures; j++){
			expectedHypFinalScore += expectedFeatureValues.get(j)*weights[j];
		}	
		
		for(int i=0; i< nbestProbs.size(); i++){
			double hypFinalScore = 0;			
			double prob = nbestProbs.get(i);			
			double gain = gainWithRespecitToRef.get(i)*gainFactor;
			double entropyFactor;
			if(prob==0)
				entropyFactor = -temperature*(0+1);//+TH(P); log(0)=0 as otherwise not well-defined 
			else
				entropyFactor = -temperature*(Math.log(prob)+1);//+TH(P)
			
			double anotherSentGradientForScaling = 0;	//another way to compute the gradient for scaling factor		
			for(int j=0; j<numFeatures; j++){
				double hypFeatVal = getFeatVal(start_pos, i, j);
				hypFinalScore += hypFeatVal*weights[j];
				double common = scale*prob*(hypFeatVal-expectedFeatureValues.get(j));
				double sentGradient = common * (gain+entropyFactor);
				gradients[j] += sentGradient;
				anotherSentGradientForScaling += sentGradient*weights[j];
			}
			
			anotherSentGradientForScaling /= scale;
			
			//compute gradient for the scaling factor			
			if(shouldComputeGradientForScalingFactor){
				double common = prob*(hypFinalScore-expectedHypFinalScore);	
				double sentGradientForScaling = common * (gain+entropyFactor);
				gradientForScalingFactor += sentGradientForScaling;
				
				//another way to compute the gradient for scaling factor
				//====== sanity check
				if(Math.abs(sentGradientForScaling-anotherSentGradientForScaling)>1e-2){
					System.out.println("gradientForScalingFactor is not equal; " + sentGradientForScaling + "!=" + anotherSentGradientForScaling + "; scale=" + scale);
					System.exit(1);
				}				
			}			
		}
	}
	
	private void computeCorpusFuncVal(double temperature){
		functionValue = 0;
		expectedGainSum = 0;
		entropySum = 0;
			
		for(int i=0; i<totalNumSent; i++){
			computeSentFuncVal(i, temperature);
		}
		//return func_val;
	}
	
	private void computeSentFuncVal(int sentID, double temperature){
		int start_pos = startPoss.get(sentID);
		int end_pos = endPoss.get(sentID);
		List<Double> nbestGains = gainsWithRespectToRef.subList(start_pos, end_pos);
		List<Double> nbestProbs = hypProbs.subList(start_pos, end_pos);
		double expectedGain = computeExpectedGain(nbestGains, nbestProbs);
		
		double entropy = computeEntropy(nbestProbs);//compute it always, though may not be used in the objective
		
		expectedGainSum += expectedGain;
		entropySum += entropy;
		functionValue +=  expectedGain*gainFactor+entropy*temperature;//maximize function
	}
//=================compute Gradient: END	

	
	
//######Utility function
//	natural base
	static public double computeEntropy( List<Double> nbestProbs){
		double entropy =0;
		double tSum=0;
		for(double prob : nbestProbs){
			if(prob!=0)//log0 is not well defined
				entropy -= prob*Math.log(prob);//natural base
			//if(Double.isNaN(entropy)){System.out.println("entropy becomes NaN, must be wrong; prob is " + prob ); System.exit(1);}
			tSum+=prob;
		}
//		sanity check
		if(Math.abs(tSum-1.0)>1e-4){System.out.println("probabilities not sum to one, must be wrong");	System.exit(1);}
		if(Double.isNaN(entropy)){System.out.println("entropy is NaN, must be wrong"); System.exit(1);}
		if(entropy<0 || entropy > Math.log(nbestProbs.size()+ 1e-2)){System.out.println("entropy is negative or above upper bound, must be wrong; " + entropy); System.exit(1);}
		//System.out.println("entropy is: " + entropy);
		return entropy;
	}
	
	
	//in the real domain, and use natural base
	//KL(P||Q)
	static public double computeKLDivergence( List<Double> P, List<Double> Q){
		double divergence =0;
	
		if(P.size()!=Q.size()){
			System.out.println("the size of the event space of two distributions is not the same");
			System.exit(1);
		}
		
		double pSum=0;
		double qSum=0;
		for(int i=0; i< P.size(); i++){
			double p = P.get(i);
			double q = Q.get(i);
			double logRatio=0;
			if(q==0 && p !=0 ){
				System.out.println("q is zero, but p is not, not well defined");
				System.exit(1);
			}else if(p==0 || q==0){
				logRatio = 0;
			}else{//both p and q non-zero
				logRatio = Math.log(p/q);
			}
			
			divergence += p * logRatio;
			pSum += p;
			qSum += q;
		}
//		sanity check
		if(divergence < 0 ){
			System.out.println("divergence is negative, must be wrong");
			System.exit(1);
		}
		if(Math.abs(pSum-1.0)>1e-4){
			System.out.println("P is not sum to one, must be wrong");
			System.exit(1);
		}
		if(Math.abs(qSum-1.0)>1e-4){
			System.out.println("Q is not sum to one, must be wrong");
			System.exit(1);
		}
		return divergence;
	}
	
	
	
	//Gain(e) = \sum_{e'} G(e, e')P(e')
	//cur_hyp: e
	//true_hyp: e'
	private double computeExpectedGain( List<Double> nbestGains, List<Double> nbestProbs){
		//### get normalization constant, remember features, remember the combined linear score
		double expectedGain = 0;
		for(int i=0; i<nbestGains.size(); i++){
			double gain = nbestGains.get(i);
			double trueProb = nbestProbs.get(i);
			expectedGain += trueProb*gain;
		}
		//System.out.println("Expected gain is " + expected_gain);		
//		sanity check
		if(Double.isNaN(expectedGain)){
			System.out.println("expected_gain isNaN, must be wrong");
			System.exit(1);
		}
		if(useGoogleLinearCorpusGain==false){
			if(useLogBleu){
				if(expectedGain>1e-2){
					System.out.println("Warning: expected_gain is not smaller than zero when using logBLEU, must be wrong: " + expectedGain);
					System.exit(1);
				}
			}else{
				if(expectedGain<-(1e-2) || expectedGain > 1+1e-2){
					System.out.println("Warning: expected_gain is not within [0,1], must be wrong: " + expectedGain);
					System.exit(1);
				}
			}
		}
		return expectedGain;
	} 
	
	private List<Double> getSentExpectedFeatureScoreList(int sentID){
		return expectedFeatureValues.subList(sentID*numFeatures, (sentID+1)*numFeatures);
	}

	
	private  double getFeatVal(int startPos, int hypID, int featID){
		return featureValues.get((startPos+hypID)*numFeatures+featID);
	}
	
	
	
//######Utility function: END
	
}
