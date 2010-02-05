package joshua.decoder.hypergraph;

import java.util.List;
import java.util.Stack;

//example: (ROOT ([S] ([X] ([X] scientists completed ([X] for ([X] ([X] chromosome) related to ([X] early ([X] OOV))))) sequencing)))

public class StringToTreeConverter {
	
	static private final String beginSymbol = "(b"; 
	static private final String nodeSymbol ="node";
	
	HyperGraph convert(String inputStr){

		HyperGraph tree = null;
		
		Stack<String> stack = new Stack<String>();
		for(int i=0; i<inputStr.length(); i++){
			char curChar = inputStr.charAt(i);
			
			if(curChar ==')' && inputStr.charAt(i-1)!=' '){//end of a rule
				StringBuffer ruleString = new StringBuffer();
				List<HGNode> antNodes = null;
				
				while(stack.empty()==false){
					String cur = stack.pop();					
					if(cur.equals(beginSymbol)){//stop
						//setup a node
						//HGNode(int i, int j, int lhs, HashMap<Integer,DPState> dpStates, HyperEdge initHyperedge, double estTotalLogP)
						//public HyperEdge(Rule rule, double bestDerivationLogP, Double transitionLogP, List<HGNode> antNodes, SourcePath srcPath)
						//	public BilingualRule(int lhs, int[] sourceRhs, int[] targetRhs, float[] featureScores, int arity, int owner, float latticeCost, int ruleID)
						
						
						stack.add(nodeSymbol);//TODO: should be lHS+id
						break;
					}else if(cur.equals(nodeSymbol)){
						
					}else{
						ruleString.append(cur);
					}
				}
			}else if(curChar =='(' && inputStr.charAt(i+1) != ' '){//begin of a rule
				stack.add(beginSymbol);
			}else{
				stack.add(""+curChar);
			}
		}
		
		
		
		return tree;
	}

}
