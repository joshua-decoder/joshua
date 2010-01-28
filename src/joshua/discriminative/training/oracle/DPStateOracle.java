/**
 * 
 */
package joshua.discriminative.training.oracle;

import java.util.List;

import joshua.corpus.vocab.SymbolTable;
import joshua.decoder.ff.state_maintenance.DPState;

public class DPStateOracle implements DPState {
	
	public double bestDerivationLogP;

	int[] ngramMatches; //this is not used in the signature
	
	int bestLen; //this may not be used in the signature
	List<Integer> leftLMState;
	List<Integer> rightLMState;	
	
	
	public DPStateOracle(int blen, int[] matches, List<Integer> left, List<Integer> right, double bestDerivationLogP) {
		this.bestDerivationLogP =bestDerivationLogP;
		this.bestLen = blen;
		this.ngramMatches = matches;
		this.leftLMState = left;
		this.rightLMState = right;
	}
	

	
	
	protected String getSignature(){		
		StringBuffer res = new StringBuffer();
		if(OracleExtractionOnHGV2.maitainLengthState==true){
			res.append(bestLen); 
			res.append(" ");
		}
		
		if(leftLMState!=null)//goal-item have null state
			for(int i=0; i< leftLMState.size(); i++){
				res.append(leftLMState.get(i));
				res.append(" ");
			}
		res.append("lzf ");	
		
		if(rightLMState!=null)//goal-item have null state
			for(int i=0; i< rightLMState.size(); i++){
				res.append(rightLMState.get(i));
				res.append(" ");
			}
		//if(left_lm_state==null || right_lm_state==null)System.out.println("sig is: " + res.toString());
		return res.toString();
	}
	
	protected void print(){
		StringBuffer res = new StringBuffer();
		res.append("DPstate: best_len: ");
		res.append(bestLen);
		for(int i=0; i<ngramMatches.length; i++){
			res.append("; ngram: ");
			res.append(ngramMatches[i]);
		}
		System.out.println(res.toString());
	}




	public String getSignature(boolean forceRecompute) {
		return getSignature();
	}




	public String getSignature(SymbolTable symbolTable, boolean forceRecompute) {
		return getSignature();
	}
}