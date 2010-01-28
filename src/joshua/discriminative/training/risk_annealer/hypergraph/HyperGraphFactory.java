package joshua.discriminative.training.risk_annealer.hypergraph;

import java.io.BufferedReader;
import java.util.logging.Logger;

import joshua.corpus.vocab.SymbolTable;
import joshua.decoder.hypergraph.DiskHyperGraph;
import joshua.decoder.hypergraph.HyperGraph;
import joshua.discriminative.FileUtilityOld;

/**provide HG and reference
 * */

public class HyperGraphFactory {
	
	 private SymbolTable symbolTbl;
	 private int ngramStateID;
	    
	 private DiskHyperGraph diskHG = null;
	 private String diskHGFilePrefix;

	 private String[] referenceFiles; 
	 private BufferedReader[] refFileReaders;
	
	 boolean readReferences = true;
	 
	 /** Logger for this class. */
	private static final Logger logger = 
		Logger.getLogger(HyperGraphFactory.class.getName());
	 

	 public HyperGraphFactory(String diskHGFilePrefix, String[] referenceFiles,  int ngramStateID, SymbolTable symbolTbl, boolean readReferences){

		 this.diskHGFilePrefix = diskHGFilePrefix;      
		 this.ngramStateID = ngramStateID; 
		 this.symbolTbl = symbolTbl;     
		 this.referenceFiles = referenceFiles;
		 this.readReferences = readReferences;
	 }
	 
	 
	 public void startLoop(){
       	initDiskReading();       
	 }

	 public void endLoop(){		 
		 finalizeDiskReading();
	 }
	 

	 public HGAndReferences nextHG(){	    
	    return  new HGAndReferences(readOneHGFromDisk(), readReferencesFromDisk() );	    
	 }
	 
	 
	 private String[] readReferencesFromDisk(){
		if(this.readReferences){
			String[] referenceSentences = new String[refFileReaders.length];
			for(int i=0; i<refFileReaders.length; i++)
				referenceSentences[i]= FileUtilityOld.readLineLzf(refFileReaders[i]);
			
			return  referenceSentences;
		}else
			return null;
		
	 }
	
	 
	 private void initDiskReading(){
		logger.info("initialize reading hypergraphss..............");
		 
		diskHG = new DiskHyperGraph(symbolTbl, ngramStateID, true, null); //have model costs stored
        diskHG.initRead(diskHGFilePrefix+".hg.items", diskHGFilePrefix+".hg.rules", null);
        
        //=== references files, they are needed only when we want annote the hypergraph with risk   
        if(this.readReferences){
	        refFileReaders = new BufferedReader[referenceFiles.length];
			for(int i=0; i<referenceFiles.length; i++)
				refFileReaders[i] = FileUtilityOld.getReadFileStream(referenceFiles[i],"UTF-8");
		}
	 }
	 
	 private void finalizeDiskReading(){
		 logger.info("finalize reading hypergraphss..............");
		 diskHG.closeReaders();
		 
	     //=== references files
	     if(this.readReferences){
			 for(int i=0; i<referenceFiles.length; i++){
				FileUtilityOld.closeReadFile(refFileReaders[i]);
			 }
		 }
	 }
	 
	 private HyperGraph readOneHGFromDisk(){
		 		
		//=== disk hypergraph
		return  diskHG.readHyperGraph();
	 }
	 
	 

}
