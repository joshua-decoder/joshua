package joshua.decoder.chart_parser;

public interface Prunable<O> extends Comparable<O> {
	
	boolean isDead();
	
	void setDead();
	
	double getPruneLogP();
	
	void setPruneLogP(double logP);
}
