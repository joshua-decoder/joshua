package joshua.discriminative.semiring_parsingv2.bilinear_operator;


import joshua.discriminative.semiring_parsingv2.SignedValue;
import joshua.discriminative.semiring_parsingv2.pmodule.ScalarPM;



public class ScalarBO implements BilinearOperator<ScalarPM, ScalarPM, ScalarPM>{

	public ScalarPM bilinearMulti(ScalarPM r, ScalarPM s) {
		SignedValue res = SignedValue.multi(r.getValue(), s.getValue() );
		return new ScalarPM(res);
	}

}
