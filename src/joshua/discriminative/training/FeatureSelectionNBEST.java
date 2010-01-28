package joshua.discriminative.training;

import java.io.BufferedReader;
import java.util.HashMap;
import java.util.List;

import joshua.discriminative.DiscriminativeSupport;
import joshua.discriminative.FileUtilityOld;




public class FeatureSelectionNBEST {
	/*for 919 sent, time_on_reading: 148797
	time_on_orc_extract: 580286*/
	public static void main(String[] args) {	
		//long start_time = System.currentTimeMillis();
		if(args.length!=2){
			System.out.println("wrong command, correct command should be: java FeatureSelection_NBEST f_l_test_nbest f_feature_set_out");
			System.out.println("num of args is "+ args.length);
			for(int i=0; i <args.length; i++) System.out.println("arg is: " + args[i]);
			System.exit(0);		
		}
		String f_l_test_nbest= args[0].trim();
		String f_feature_set_out= args[1].trim();//output file	
		
		List<String> testNbestFiles = DiscriminativeSupport.readFileList(f_l_test_nbest);
		
		//#### extract feat tbl
		HashMap tbl_feats = new HashMap();		
		tbl_feats.put(HGDiscriminativeLearner.baselineFeatName, 1);
		for(int fid=0; fid < testNbestFiles.size(); fid++){
			System.out.println("#######Process file id " + fid);
			BufferedReader t_reader_nbest = FileUtilityOld.getReadFileStream((String)testNbestFiles.get(fid),"UTF-8");
			String line=null;			
			while((line=FileUtilityOld.readLineLzf(t_reader_nbest))!=null){										
				NBESTDiscriminativeLearner.featureExtraction(line, tbl_feats, null, 1, false);
			}						
			FileUtilityOld.closeReadFile(t_reader_nbest);
		}

		//#### write hashtable
		FileUtilityOld.printHashTbl(tbl_feats, f_feature_set_out, true, false);
	}	
}


