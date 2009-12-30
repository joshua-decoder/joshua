package joshua.decoder.ff;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class NgramStateComputeResult implements StateComputeResult<NgramDPState> {
	
	//the ngrams that are generated due to the combintation, and also in the reference
	public HashMap<String, Integer> newNgramsTbl; 
	
	public ArrayList<ArrayList<Integer>> newNgrams;
	
	//left or right equivalent lm words
	public List<Integer> leftEdgeWords; 
	public List<Integer> rightEdgeWords;
	
	//the number of new words generated at the hyperege
	public int numNewWordsAtEdge;
	
	
	public NgramStateComputeResult(
			HashMap<String, Integer> newNgramsTbl,
			List<Integer> leftEdgeWords,
			List<Integer> rightEdgeWords,
			int numNewWordsAtEdge){
		
		this.newNgramsTbl = newNgramsTbl;
		this.leftEdgeWords = leftEdgeWords;
		this.rightEdgeWords = rightEdgeWords;
		this.numNewWordsAtEdge = numNewWordsAtEdge;
	}
	
	public NgramStateComputeResult(
			ArrayList<ArrayList<Integer>> newNgrams,
			List<Integer> leftEdgeWords,
			List<Integer> rightEdgeWords,
			int numNewWordsAtEdge){
		
		this.newNgrams = newNgrams;
		this.leftEdgeWords = leftEdgeWords;
		this.rightEdgeWords = rightEdgeWords;
		this.numNewWordsAtEdge = numNewWordsAtEdge;
	}
	
	
	public void printInfo(){
		System.out.println("newNgramsTbl=" + newNgramsTbl);
		System.out.println("leftEdgeWords=" + leftEdgeWords);
		System.out.println("rightEdgeWords=" + rightEdgeWords);
		System.out.println("hypLen=" + numNewWordsAtEdge);
	}




	public NgramDPState generateDPState() {
		return new NgramDPState(this.leftEdgeWords, this.rightEdgeWords);
	}
	
	
	
 
}
