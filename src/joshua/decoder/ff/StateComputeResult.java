package joshua.decoder.ff;

public interface StateComputeResult<D extends DPState> {
	D generateDPState();
	public void printInfo();
}
