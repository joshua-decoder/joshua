package joshua.discriminative.training.expbleu.nbest;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;

import joshua.decoder.BLEU;
import joshua.discriminative.FileUtilityOld;
import joshua.discriminative.training.expbleu.ExpbleuGradientComputer;
import joshua.discriminative.training.risk_annealer.GradientComputer;
import joshua.discriminative.training.risk_annealer.hypergraph.MRConfig;
import joshua.util.Regex;

public class NbestExpbleuGradientComputer extends GradientComputer {

	private int numSentence;
	private int numFeats;
	private double[] ngramMatches = new double[5];
	private ArrayList<ArrayList<Double>> ngramMatchesGradients = new ArrayList<ArrayList<Double>>(5);
	private String[] refFiles;
	private ArrayList<ArrayList<String>> nbestlines;
	private ArrayList<ArrayList<String>> refs; 
	private double minLen;
	private double N = 1000;
	private int numRefs = 4;
	static final private Logger logger = 
		Logger.getLogger(ExpbleuGradientComputer.class.getSimpleName());

	public NbestExpbleuGradientComputer(
			String nbestFile, 
			String [] refFiles, 
			int numFeatures, 
			double gainFactor,
			double scalingFactor, 
			double temperature,
			boolean shouldComputeGradientForScalingFactor, 
			int numSentence) {
		super(numFeatures, gainFactor, scalingFactor, temperature,
				shouldComputeGradientForScalingFactor);
		this.numSentence = numSentence;
		// System.out.println("use HGRiskGradientComputer====");
		
		this.numRefs = refFiles.length;
		this.refFiles = refFiles;
		this.numFeats = numFeatures;
		for(int i = 0; i < 5; ++i){
			this.ngramMatches[i] = 0; 
			ArrayList<Double> row = new ArrayList<Double>(10);
			for(int j = 0; j < this.numFeats ; ++j){
				row.add(Double.valueOf(0));
			}
			this.ngramMatchesGradients.add(row);
		}
		this.nbestlines = new ArrayList<ArrayList<String>>(this.numSentence + 1);
		for(int i = 0; i < this.numSentence; ++i){
			ArrayList<String> nbestForOneSent = new ArrayList<String>();
			this.nbestlines.add(nbestForOneSent);
		}
		this.refs = new ArrayList<ArrayList<String>>(this.numSentence + 1);
		for(int i = 0; i < this.numSentence; ++i){
			ArrayList<String> refsForOneSent = new ArrayList<String>();
			this.refs.add(refsForOneSent);
		}
		BufferedReader nbestReader = FileUtilityOld.getReadFileStream(nbestFile, "UTF-8");
		BufferedReader [] refsReader = new BufferedReader[refFiles.length];
		for(int i = 0; i < refFiles.length; ++i){
			refsReader[i] = FileUtilityOld.getReadFileStream(refFiles[i], "UTF-8");
		}
		String line;
		int index = 0; 
		
		try {
			while((line = nbestReader.readLine()) != null){
				String [] fds = line.split("\\s+\\|{3}\\s+");
				index = Integer.valueOf(fds[0]);
//				System.out.println("Add nbest line " + index + " " + line);
				this.nbestlines.get(index).add(line);
			}
			ArrayList<Integer> minLens = new ArrayList<Integer>(this.numSentence);
			for(int i = 0; i < this.numSentence; ++i){
				minLens.add(10000);
			}
			for(int i = 0; i < refFiles.length; ++i){
				index = 0; 
				while((line = refsReader[i].readLine()) != null){
					this.refs.get(index).add(line);
					String [] wds = line.split("\\s+");
					if(wds.length < minLens.get(index)){
						minLens.set(index, wds.length);
					}
					++ index;	

				}
			}
			for(int i = 0; i < this.numSentence; ++i){
				this.minLen += minLens.get(i);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// TODO Auto-generated constructor stub
	}

	@Override
	public void printLastestStatistics() {
		// TODO Auto-generated method stub

	}

	@Override
	public void reComputeFunctionValueAndGradient(double[] theta) {
		// TODO Auto-generated method stub
		
		for(int i = 0; i < 5; ++i ){
			this.ngramMatches[i] = 0;
			for(int j = 0; j < this.numFeats; ++j){
				this.ngramMatchesGradients.get(i).set(j, 0.0);
			}
		}
		this.functionValue = 0;
		for(int j = 0; j < this.numFeats; ++j){
			this.gradientsForTheta[j] = 0;
		}
		
		double [] matches = new double[5];
		double Z = 0;
		double [] dz = new double[this.numFeats];
		for(int j = 0; j < this.numFeats; ++j){
			dz[j] = 0;
		}
		ArrayList<ArrayList<Double>> dm = new ArrayList<ArrayList<Double>>(5);
		for(int i = 0; i < 5; ++i){
			matches[i] = 0;
			ArrayList<Double> row = new ArrayList<Double>(this.numFeats);
			for(int j = 0; j < this.numFeats; ++j){
				row.add(0.0);
			}
			dm.add(row);
		}
		double closestLen = 0;
		for(int i = 0; i < this.numSentence; ++i){
			String [] sentRefs = new String[refFiles.length];
			
			this.refs.get(i).toArray(sentRefs);
			for(String nbestline : this.nbestlines.get(i)){
				String fds [] = nbestline.split("\\s+\\|{3}\\s+");
				String [] feats = fds[2].split("\\s+");
				double score = 0; 
				for(int j = 0; j < this.numFeats; ++j){
					score += Double.valueOf(feats[j]) * theta[j];
				}
				double p =  myexp(score);
				Z += p;
				int[] hypNgramMatches = BLEU.computeNgramMatches(sentRefs, fds[1]);

				for(int j = 0; j < 5; ++j){
					matches[j] += hypNgramMatches[j] * p;
					for(int k = 0; k < this.numFeats; ++k){
						dm.get(j).set(k, dm.get(j).get(k) + Double.valueOf(feats[k]) * p * hypNgramMatches[j]);
					}
				}
			
				for(int j = 0; j < this.numFeats; ++j){
					dz[j] += Double.valueOf(feats[j]) * p;
				}
				
			}
			if(Z == 0){
				continue;
			}			
			double lengthExp = matches[0]/Z;
			for(int j = 0; j < 5; ++j){
				this.ngramMatches[j] += matches[j]/Z;

						
				for(int k = 0; k < this.numFeats; ++k){
					double grad = (dm.get(j).get(k)*Z - matches[j] * dz[k])/Z/Z;
					this.ngramMatchesGradients.get(j).set(k, this.ngramMatchesGradients.get(j).get(k) + grad);
					dm.get(j).set(k, 0.0);
				}
				matches[j] = 0;
			}
			double closestLenForOneSent = 0; 
			double minDiff = 10000;
			for(int k = 0; k < numRefs; ++k){
				String[] wds = Regex.spaces.split(sentRefs[k]);
				double diff = Math.abs(wds.length - lengthExp);
				if( diff < minDiff){
					minDiff = diff;
					closestLenForOneSent = wds.length;
				}
					
			}
			closestLen += closestLenForOneSent;
			Z = 0; 
			for(int j = 0; j < this.numFeats; ++j){
				dz[j] = 0; 
			}
		}
		this.minLen = closestLen; // use closest length, instead of the minimum length to compute the length penalty; 
		finalizeFunAndGradients();
	}
	private void finalizeFunAndGradients() {
		// TODO Auto-generated method stub
		for(int i = 1; i <= 4; ++i){
			this.functionValue += 1.0/4.0 * Math.log(ngramMatches[i]);	
		}
		for(int i = 0; i < 4; ++i){
			this.functionValue -= 1.0/4.0 * Math.log(ngramMatches[0] - i * this.numSentence );
		}
		double x = 1 - this.minLen/this.ngramMatches[0];
		this.functionValue += 1/(Math.exp(N*x) + 1) * x; 
		double y;
		if(x > 0){
			y = ((1 - N * x)*myexp(-N*x) + myexp(-2*N*x))/(myexp(-N*x) + 1)/(myexp(-N*x)+1);
		}
		else{
			y = ((1 - N * x)*myexp(N*x) + 1)/(myexp(N*x) + 1)/(myexp(N*x)+1);
		}
		for(int i = 0; i < this.numFeatures ; ++i){
			for(int j = 1; j <= 4; ++j){
				this.gradientsForTheta[i] += 1.0/4.0/ngramMatches[j]*ngramMatchesGradients.get(j).get(i);
			}
			for(int j = 0; j < 4; ++j){
				this.gradientsForTheta[i] -= 1.0/4.0/(ngramMatches[0] - j*this.numSentence)*ngramMatchesGradients.get(0).get(i);
			}
			double dx = this.minLen/this.ngramMatches[0]/this.ngramMatches[0]*this.ngramMatchesGradients.get(0).get(i);
			this.gradientsForTheta[i] += y*dx;
		}
		
		this.logger.info("Function Value :" + this.functionValue);
		String diffinfo = "Derivatives :";
		for(int i = 0; i < MRConfig.printFirstN; ++i){
			diffinfo += " ";
			diffinfo += this.gradientsForTheta[i];
		}
		this.logger.info(diffinfo);
	}
	private double myexp(double x){
		if(Double.isInfinite(Math.exp(x))){
			return 0;
		}
		else{
			return Math.exp(x);
		}
	}
}
