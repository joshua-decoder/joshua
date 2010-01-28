package joshua.discriminative.monolingual_parser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
 
import joshua.util.FileUtility;


public class BilingualToMonolingualGrammar {
	public static void main(String[] args) 	throws IOException{	
		/*String bilingualGrammarFile = "C:\\data_disk\\java_work_space\\sf_trunk\\example\\example.hiero.tm.gz";
		String monolingualGrammarFile = "C:\\data_disk\\java_work_space\\sf_trunk\\example\\example.hiero.mono.tm";
		*/
		if(args.length<3){
			System.out.println("Wrong number of parameters, it must have at least two parameters: java StructuredLMEM f_joshua_config f_train");
			System.exit(1);
		}
		String bilingualGrammarFile = args[0];
		String monolingualGrammarFile = args[1];
		ArrayList<Double> featureWeights = new ArrayList<Double>();
		for(int i=2; i<args.length; i++){
			featureWeights.add(new Double(args[i]));
		}
		int numFeatures = featureWeights.size();
		
		BufferedReader t_reader = FileUtility.getReadFileStream(bilingualGrammarFile);
		BufferedWriter t_writer  = FileUtility.getWriteFileStream(monolingualGrammarFile);
		HashMap<String, Boolean> tbl_unique_rules = new HashMap<String, Boolean>();
		String line;
		while ((line = FileUtility.read_line_lzf(t_reader)) != null) {
			String[] fds = line.split("\\s+\\|{3}\\s+");//[x] ||| cn ||| en ||| feature-scores
			if (fds.length != 4) {
				System.out.println("rule line does not have four fds; " + line);
			}
			StringBuffer rule = new StringBuffer();
			rule.append(fds[0]);
			rule.append(" ||| ");

			//skip fds[1]
			
			/** we do not care about the order index of the non-terminal in the rule
			 * */
			rule.append(fds[2]);
			
			if(tbl_unique_rules.containsKey(rule.toString())){
				System.out.println("duplicate rule: " + rule.toString());
			}else{
				tbl_unique_rules.put(rule.toString(),true);
				//rule.append(" ||| 0");
				//rule.append(fds[3]);
				rule.append(" ||| ");
				String[] scores = fds[3].split("\\s+");
				double combinedScore = 0;
				if(scores.length!=numFeatures){
					System.out.println("number of features in rule is not " + numFeatures);
					System.exit(1);
				}
				for(int j=0; j<scores.length; j++)
					combinedScore += (new Double(scores[j]))*featureWeights.get(j);
				rule.append(combinedScore);	
				
				rule.append("\n");
				t_writer.write(rule.toString());
				//System.out.println("rule: " + rule.toString());
			}
		}
		t_reader.close();
		t_writer.close();
	}
}
