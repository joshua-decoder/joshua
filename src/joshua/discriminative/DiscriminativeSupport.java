package joshua.discriminative;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;




public class DiscriminativeSupport {
	
	static public void increaseCount(HashMap<String, Double> tbl, String feat, double increment){
		Double oldCount = tbl.get(feat);
		if(oldCount!=null)
			tbl.put(feat, oldCount + increment);
		else
			tbl.put(feat, increment);
	}

	
	
	
	public static void loadRegularModel(String f_avg_model, HashMap<String, Double> tbl_model, boolean negateFeatureWeight, String baselineFeatName){
		BufferedReader t_reader = FileUtilityOld.getReadFileStream(f_avg_model,"UTF-8");
		tbl_model.clear();		
		String line;
		while((line=FileUtilityOld.readLineLzf(t_reader))!=null){
			String[] fds = line.split("\\s+\\|{3}\\s+");
			StringBuffer feat_key = new StringBuffer();
			for(int i=0; i<fds.length-1; i++){
				feat_key.append(fds[i]);
				if(i<fds.length-2) feat_key.append(" ||| ");
			}
			double val = new Double(fds[fds.length-1]);
			if(negateFeatureWeight && feat_key.toString().compareTo(baselineFeatName)!=0){//negate all the feature weights except the baseline feature
					val = -val; 
			}
			tbl_model.put(feat_key.toString(), val);
			//System.out.println("key: " + feat_key.toString() + "; val: " + val);
		}
		FileUtilityOld.closeReadFile(t_reader);
	}
	

	public static void loadAvgPercetronModel(String f_avg_model, HashMap<String, Double[]> avgModelTbl, boolean negateFeatureWeight,  String baselineFeatName){
		BufferedReader t_reader = FileUtilityOld.getReadFileStream(f_avg_model,"UTF-8");
		avgModelTbl.clear();		
		String line;
		while((line=FileUtilityOld.readLineLzf(t_reader))!=null){
			String[] fds = line.split("\\s+\\|{3}\\s+");
			StringBuffer feat_key = new StringBuffer();
			for(int i=0; i<fds.length-1; i++){
				feat_key.append(fds[i]);
				if(i<fds.length-2) feat_key.append(" ||| ");
			}
			String vals = fds[fds.length-1];
			String[] wrds = vals.split("\\s+");
			Double[] wrds_val = new Double[wrds.length];
			for(int i=0; i<wrds.length; i++){
				if(i==0 && negateFeatureWeight && feat_key.toString().compareTo(baselineFeatName)!=0){//negate all the feature weights except the baseline feature
					wrds_val[i] = - new Double(wrds[i]);
				}else
					wrds_val[i] = new Double(wrds[i]);
			}
			avgModelTbl.put(feat_key.toString(), wrds_val);
		}
		FileUtilityOld.closeReadFile(t_reader);
	}
	

	static public ArrayList readFileList(String file){
		ArrayList<String> res = new ArrayList<String>();
		BufferedReader t_reader = FileUtilityOld.getReadFileStream(file,"UTF-8");
		String line;
		while((line=FileUtilityOld.readLineLzf(t_reader))!=null){
			res.add(line);
		}
		FileUtilityOld.closeReadFile(t_reader);
		return res;
	}
	
	//read the sent ids into the hashtable
	public static HashMap<Integer, Boolean> setupDataSelTbl(String fDataSel){
		if(fDataSel==null)
			return null;
		HashMap<Integer, Boolean> res = new HashMap<Integer, Boolean>();
		BufferedReader t_reader_data_sel = FileUtilityOld.getReadFileStream(fDataSel,"UTF-8");
		String sentID;
		while((sentID=FileUtilityOld.readLineLzf(t_reader_data_sel))!=null){
			res.put(new Integer(sentID), true);
		}
		FileUtilityOld.closeReadFile(t_reader_data_sel);
		return res;
	}
	
//	read the feat set into the hashtable
	public static void loadFeatureSet(String featureSetFile, HashMap<String, Double> featSet){
		featSet.clear();
		BufferedReader t_reader = FileUtilityOld.getReadFileStream(featureSetFile,"UTF-8");
		String feat;
		while((feat=FileUtilityOld.readLineLzf(t_reader))!=null){
			featSet.put(feat, 1.0);
		}
		FileUtilityOld.closeReadFile(t_reader);		
	}
	
	
	public static void scaleMapEntries(HashMap<?, Double> map, double scale){
		for(Map.Entry<?, Double> entry : map.entrySet()){
		    entry.setValue(entry.getValue()*scale);
		}
	}

 
	
	
	//speed issue: assume tbl_feats is smaller than model
	static public double computeLinearCombination(HashMap featTbl, HashMap  model, boolean isValueAVector){
		double res = 0;
		for(Iterator it = featTbl.keySet().iterator(); it.hasNext();){//TODO use entryset to speed up
			String feat_key = (String) it.next();
			double feat_count = (Double)featTbl.get(feat_key);
			Double weight = null;
			if(isValueAVector){
				if(model.containsKey(feat_key)) weight = ((Double[])model.get(feat_key))[0];
			}else
				weight =(Double)model.get(feat_key);
			if(weight!=null) res += weight*feat_count;
		}	
		return res;
	}
}
