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

public class NbestExpbleuGradientComputer extends GradientComputer {

	private int numSentence;
	private int numFeats;
	private int[] ngramMatches;
	private ArrayList<ArrayList<Double>> ngramMatchesGradients;
	private String[] refFiles;
	private ArrayList<ArrayList<String>> nbestlines;
	private ArrayList<ArrayList<String>> refs; 
	private double avgRefLen;
	private double N = 10000;
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
				index = Integer.getInteger(fds[0]);
				System.out.println("Add nbest line " + index + " " + line);
				this.nbestlines.get(index).add(line);
			}
			index = 1; 
			for(int i = 0; i < refFiles.length; ++i){
				while((line = refsReader[i].readLine()) != null){
					this.refs.get(index).add(line);
					++ index;
					String [] wds = line.split("\\s+");
					this.avgRefLen += wds.length;
				}
			}
			this.avgRefLen = this.avgRefLen/refFiles.length;
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
				row.set(j, 0.0);
			}
			dm.add(row);
		}
		for(int i = 1; i <= this.numSentence; ++i){
			String [] sentRefs = new String[refFiles.length];
			
			this.refs.get(i).toArray(sentRefs);
			for(String nbestline : this.nbestlines.get(i)){
				String fds [] = nbestline.split("\\s+\\|{3}\\s+");
				double p =  Math.exp(Double.valueOf(fds[3]));
				Z += p;
				int[] hypNgramMatches = BLEU.computeNgramMatches(sentRefs, fds[1]);
				String [] feats = fds[2].split("\\s+");
				for(int j = 0; j < 4; ++j){
					matches[j] += hypNgramMatches[j] * p;
					for(int k = 0; k < this.numFeats; ++k){
						System.out.println("feature " + j + " " + feats[j]);
						dm.get(j).set(k, dm.get(j).get(k) + Double.valueOf(feats[k]) * p * hypNgramMatches[j]);
					}
				}
				String [] wds = fds[1].split("\\s+");
				matches[4] += wds.length * p;
				for(int j = 0; j < this.numFeats; ++j){
					
					dm.get(4).set(j, dm.get(4).get(j) + wds.length * Double.valueOf(feats[j]) * p);
				}
				for(int j = 0; j < this.numFeats; ++j){
					dz[j] += Double.valueOf(feats[j]) * p;
				}
				
			}
			for(int j = 0; j < 5; ++j){
				this.ngramMatches[j] += matches[j]/Z;
				for(int k = 0; k < this.numFeats; ++k){
					double grad = (dm.get(j).get(k)*Z - matches[j] * dz[k])/Z/Z;
					this.ngramMatchesGradients.get(j).set(k, this.ngramMatchesGradients.get(j).get(k) + grad);
					dm.get(j).set(k, 0.0);
				}
				matches[j] = 0;
			}
			Z = 0; 
			for(int j = 0; j < this.numFeats; ++j){
				dz[j] = 0; 
			}
		}
		finalizeFunAndGradients();
	}
	private void finalizeFunAndGradients() {
		// TODO Auto-generated method stub
		for(int i = 0; i < 4; ++i){
			this.functionValue += 1.0/4.0 * Math.log(ngramMatches[i]);				
		}
		for(int i = 0; i < 4; ++i){
			this.functionValue -= 1.0/4.0 * Math.log(ngramMatches[4] - i * this.numSentence );
		}
		double x = 1 - this.avgRefLen/this.ngramMatches[4];
		this.functionValue += 1/(Math.exp(N*x) + 1) * x; 
		double y = ((1 - N * x)*Math.exp(N*x) + 1)/(Math.exp(N*x) + 1)/(Math.exp(N*x)+1);
		for(int i = 0; i < this.numFeatures ; ++i){
			for(int j = 0; j < 4; ++j){
				this.gradientsForTheta[i] += 1.0/4.0/ngramMatches[j]*ngramMatchesGradients.get(j).get(i);
			}
			for(int j = 0; j < 4; ++j){
				this.gradientsForTheta[i] -= 1.0/4.0/(ngramMatches[4] - j*this.numSentence)*ngramMatchesGradients.get(4).get(i);
			}
			double dx = - this.avgRefLen/this.ngramMatches[4]/this.ngramMatches[4]*this.ngramMatchesGradients.get(4).get(i);
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
}
