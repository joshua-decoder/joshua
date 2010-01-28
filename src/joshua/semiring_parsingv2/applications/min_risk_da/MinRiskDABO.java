package joshua.semiring_parsingv2.applications.min_risk_da;

import joshua.semiring_parsingv2.SignedValue;
import joshua.semiring_parsingv2.bilinear_operator.BilinearOperator;
import joshua.semiring_parsingv2.pmodule.ListPM;
import joshua.semiring_parsingv2.pmodule.SparseMap;

public class MinRiskDABO  implements BilinearOperator<RiskAndEntropyPM, ListPM, ListPM>{

	public ListPM bilinearMulti(RiskAndEntropyPM r, ListPM s) {
		
		//== get SparseArray
		SparseMap res = s.getValue().duplicate();
		for(SignedValue signedVal : res.getValues())
			signedVal.multi(r.getValue());		
		
		
		return new ListPM(res);
	}
	
	
	public ListPM bilinearMulti(SignedValue r, ListPM s) {
		
		//== get SparseArray
		SparseMap res = s.getValue().duplicate();
		for(SignedValue signedVal : res.getValues())
			signedVal.multi(r);		
		
		return new ListPM(res);
	}
	

	
}
