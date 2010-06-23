package joshua.discriminative.training.expbleu;

import joshua.discriminative.semiring_parsingv2.pmodule.ListPM;
import joshua.discriminative.semiring_parsingv2.pmodule.PModule;
import joshua.discriminative.semiring_parsingv2.semiring.LogSemiring;

public class MultiListPM implements PModule<LogSemiring, MultiListPM> {
	ListPM[] listPMs;
	
	public MultiListPM(){
		listPMs = new ListPM[4];
		for(int i = 0; i < 4; ++i){
			listPMs[i] = new ListPM();
		}
	}
	
	public MultiListPM(ListPM[] listPMs){
		this.listPMs = new ListPM[4];
		for(int i = 0; i < 4; ++i){
			this.listPMs[i] = listPMs[i];
		}
	}
	@Override
	public void add(MultiListPM b) {
		// TODO Auto-generated method stub
		for(int i = 0; i < 4; ++i){
			this.listPMs[i].add(b.listPMs[i]);
		}
	}

	@Override
	public MultiListPM duplicate() {
		// TODO Auto-generated method stub
		ListPM[] copied = new ListPM[4];
		for(int i = 0; i < 4; ++i){
			copied[i] = this.listPMs[i].duplicate();
		}
		return new MultiListPM(copied);
	}

	@Override
	public void multiSemiring(LogSemiring p) {
		// TODO Auto-generated method stub
		for(int i = 0; i < 4; ++i){
			this.listPMs[i].multiSemiring(p);
		}
	}

	@Override
	public void printInfor() {
		// TODO Auto-generated method stub
		for(int i = 0; i < 4; ++i){
			this.listPMs[i].printInfor();
		}
	}

	@Override
	public void setToZero() {
		// TODO Auto-generated method stub
		for(int i = 0; i < 4; ++i){
			this.listPMs[i].setToZero();
		}
	}
	
	public ListPM[] getListPMs(){
		return listPMs;
	}

}
