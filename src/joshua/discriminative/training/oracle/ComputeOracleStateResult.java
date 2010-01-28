package joshua.discriminative.training.oracle;

import java.util.HashMap;
import java.util.List;

public class ComputeOracleStateResult{
	
	//the ngrams that are generated due to the combintation, and also in the reference
	public HashMap<String, Integer> newNgramsTbl; 
	
	//left or right equivalent lm words
	public List<Integer> leftEdgeWords; 
	public List<Integer> rightEdgeWords;
	
	//the number of new words generated at the hyperege
	public int numNewWordsAtEdge; 

	
	public ComputeOracleStateResult(HashMap<String, Integer> newNgramsTbl_,
			List<Integer> leftEdgeWords_,
			List<Integer> rightEdgeWords_,
			int numNewWordsAtEdge_){
		
		newNgramsTbl = newNgramsTbl_;
		leftEdgeWords = leftEdgeWords_;
		rightEdgeWords = rightEdgeWords_;
		numNewWordsAtEdge = numNewWordsAtEdge_;
	}
	
	public void printInfo(){
		System.out.println("newNgramsTbl=" + newNgramsTbl);
		System.out.println("leftEdgeWords=" + leftEdgeWords);
		System.out.println("rightEdgeWords=" + rightEdgeWords);
		System.out.println("hypLen=" + numNewWordsAtEdge);
	}
}
