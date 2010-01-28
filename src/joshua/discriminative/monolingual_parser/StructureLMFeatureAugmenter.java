package joshua.discriminative.monolingual_parser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;

import joshua.util.FileUtility;

public class StructureLMFeatureAugmenter {
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
		String newbilingualGrammarFile = args[2];
		
		
		BufferedReader bilingualReader = FileUtility.getReadFileStream(bilingualGrammarFile);
		BufferedReader monoReader = FileUtility.getReadFileStream(monolingualGrammarFile);
		BufferedWriter newBilingualReader  = FileUtility.getWriteFileStream(newbilingualGrammarFile);
		HashMap<String, Double> tbl_eng_rules = new HashMap<String, Double>();
		
		
		//read mono grammar into memory
		String nonterminalReplaceRegexp = "[\\[\\]\\,0-9]+";
		String line;
		while ((line = FileUtility.read_line_lzf(monoReader)) != null) {
			String[] fds = line.split("\\s+\\|{3}\\s+");//[x] ||| cn ||| en ||| feature-scores
			if (fds.length != 3) {
				//Support.write_log_line("rule line does not have four fds; " + line, Support.ERROR);
				System.out.println("rule line does not have four fds; " + line);
			}
			String[] scores = fds[2].split("\\s+");
			double ruleCost = new Double(scores[0]);
			fds[1]= fds[1].replaceAll(nonterminalReplaceRegexp, "");//remove [, ], and numbers
			tbl_eng_rules.put(fds[1], ruleCost);//TODO: what about LHS
		}
		monoReader.close();
		
		
		//add slm feature into bilingual grammar
		while ((line = FileUtility.read_line_lzf(bilingualReader)) != null) {
			String[] fds = line.split("\\s+\\|{3}\\s+");//[x] ||| cn ||| en ||| feature-scores
			if (fds.length != 4) {
				//Support.write_log_line("rule line does not have four fds; " + line, Support.ERROR);
				System.out.println("rule line does not have four fds; " + line);
			}
			fds[2]= fds[2].replaceAll(nonterminalReplaceRegexp, "");//remove [, ], and numbers
			Double slmCost = (Double)tbl_eng_rules.get(fds[2]);//TODO: what about LHS
			if(slmCost==null){
				System.out.println("no slm cost for rule, must be wrong");
				System.out.println(line);
				//System.exit(1);
			}
			StringBuffer newRule = new StringBuffer(); 
			newRule.append(line);
			newRule.append(" ");
			newRule.append(slmCost);
			newRule.append("\n");
			newBilingualReader.write(newRule.toString());
		}
		bilingualReader.close();
		newBilingualReader.close();
	}
}