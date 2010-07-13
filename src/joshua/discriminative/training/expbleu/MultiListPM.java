package joshua.discriminative.training.expbleu;

import joshua.discriminative.semiring_parsingv2.pmodule.ListPM;
import joshua.discriminative.semiring_parsingv2.pmodule.PModule;
import joshua.discriminative.semiring_parsingv2.semiring.LogSemiring;

public class MultiListPM implements PModule<LogSemiring, MultiListPM> {
	ListPM[] listPMs;
	
	public MultiListPM(){
		listPMs = new ListPM[5];
		for(int i = 0; i < 5; ++i){
			listPMs[i] = new ListPM();
		}
	}
	
	public MultiListPM(ListPM[] listPMs){
		this.listPMs = new ListPM[5];
		for(int i = 0; i < 5; ++i){
			this.listPMs[i] = listPMs[i];
		}
	}
	public void add(MultiListPM b) {
		for(int i = 0; i < 5; ++i){
			this.listPMs[i].add(b.listPMs[i]);
		}
	}

	public MultiListPM duplicate() {
		ListPM[] copied = new ListPM[5];
		for(int i = 0; i < 5; ++i){
			copied[i] = this.listPMs[i].duplicate();
		}
		return new MultiListPM(copied);
	}

	public void multiSemiring(LogSemiring p) {
		for(int i = 0; i < 5; ++i){
			this.listPMs[i].multiSemiring(p);
		}
	}

	public void printInfor() {
		for(int i = 0; i < 5; ++i){
			this.listPMs[i].printInfor();
		}
	}

	public void setToZero() {
		for(int i = 0; i < 5; ++i){
			this.listPMs[i].setToZero();
		}
	}
	
	public ListPM[] getListPMs(){
		return listPMs;
	}

}
