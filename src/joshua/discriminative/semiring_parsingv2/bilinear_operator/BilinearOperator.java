package joshua.discriminative.semiring_parsingv2.bilinear_operator;

import joshua.discriminative.semiring_parsingv2.pmodule.PModule;

public interface BilinearOperator<R extends PModule, S extends PModule, T extends PModule> {

	public T bilinearMulti(R r, S s);

}
