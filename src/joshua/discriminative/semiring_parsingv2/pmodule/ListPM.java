package joshua.discriminative.semiring_parsingv2.pmodule;


import joshua.discriminative.semiring_parsingv2.SignedValue;
import joshua.discriminative.semiring_parsingv2.semiring.LogSemiring;

/** P is in a SemiringLog
 * */
public class ListPM implements PModule<LogSemiring, ListPM>{
	
	private SparseMap value;


	public ListPM(SparseMap v_){
		this.value = v_;
	}
	
	public ListPM duplicate() {
		SparseMap v = this.value.duplicate();
		return new ListPM(v);
	}

	public void multiSemiring(LogSemiring p) {
		for(SignedValue signedVal : this.value.getValues())
			signedVal.multiLogNumber( p.getLogValue() );		
	}

	/*public void add(ListPM b) {
		for(Integer id : b.value.getIds()){
			SignedValue valB = b.value.getValueAt(id);
			SignedValue valA = this.value.getValueAt(id);
			if(valA!=null){
				valA.add(valB);
			}else{
				this.value.addInToArray(id, SignedValue.duplicate(valB) );
			}
		}	
	}*/

	
	public void add(ListPM b) {
		this.value.add(b.value);
	}
	
	public void printInfor() {
		for(SignedValue val : this.value.getValues()){
			val.printInfor();
		}
	}

	public void setToZero() {
		for(SignedValue val : this.value.getValues()){
			val.setToZero();
		}	
	}
	
	public SparseMap getValue(){
		return this.value;
	}

	 
}
