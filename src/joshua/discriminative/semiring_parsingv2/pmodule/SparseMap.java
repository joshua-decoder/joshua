package joshua.discriminative.semiring_parsingv2.pmodule;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import joshua.discriminative.semiring_parsingv2.SignedValue;

public class SparseMap implements SparseArray<SignedValue, SparseMap>{
	
	private HashMap<Integer, SignedValue> tbl;
	
	public SparseMap(){
		this.tbl = new HashMap<Integer, SignedValue>() ;
	}
	
	public SparseMap(HashMap<Integer, SignedValue> tbl_){
		this.tbl = tbl_;
	}
	
	public void addInToArray(int id, SignedValue val) {
		this.tbl.put(id, val);
	}

	public SignedValue getValueAt(int id) {
		return this.tbl.get(id);
	}

	public Collection<Integer> getIds() {
		return  this.tbl.keySet();
	}
	
	public Collection<SignedValue> getValues() {
		return this.tbl.values();
	}
	
	/*
	public SparseArray<SignedValue> duplicate() {
		
		HashMap<Integer, SignedValue> res = new HashMap<Integer, SignedValue>();
		for(Integer id : getIds()){
			SignedValue val = getValueAt(id).duplicate();
			res.put(id, val);
		}
		return new SparseMap(res);
	}*/
	

	public void add(SparseMap b) {
		for(Map.Entry<Integer, SignedValue> feature : b.tbl.entrySet()){
			SignedValue valB = feature.getValue();
			SignedValue valA = this.tbl.get(feature.getKey());
			if(valA!=null){
				valA.add(valB);
			}else{
				this.tbl.put(feature.getKey(), SignedValue.duplicate(valB));
			}
		}			
	}

	public SparseMap duplicate() {
		HashMap<Integer, SignedValue> res = new HashMap<Integer, SignedValue>();
		for(Map.Entry<Integer, SignedValue> feature : this.tbl.entrySet()){
			SignedValue val = feature.getValue().duplicate();
			res.put(feature.getKey(), val);
		}
		return new SparseMap(res);
	}

}
