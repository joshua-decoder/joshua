package joshua.discriminative.training.expbleu;

import joshua.discriminative.semiring_parsingv2.SignedValue;
import joshua.discriminative.semiring_parsingv2.bilinear_operator.BilinearOperator;
import joshua.discriminative.semiring_parsingv2.pmodule.ListPM;
import joshua.discriminative.semiring_parsingv2.pmodule.SparseMap;

public class ExpbleuBO implements BilinearOperator<NgramMatchPM, ListPM, MultiListPM> {

	public MultiListPM bilinearMulti(NgramMatchPM r, ListPM s) {
		ListPM[] product = new ListPM[5];
		for(int i = 0; i < 5; ++i){
			SparseMap vectorTimesSigned = s.getValue().duplicate();
			for(SignedValue v : vectorTimesSigned.getValues()){
				v.multi(r.getNgramMatchExp()[i]);
			}
			product[i] = new ListPM(vectorTimesSigned);
		}
		return new MultiListPM(product);
	}

}
