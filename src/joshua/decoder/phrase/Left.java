package joshua.decoder.phrase;

public class Left implements State {

  public long[] pointers; // KENLM_MAX_ORDER - 1
  public byte length;
  public boolean full;

  public byte getLength() {
    return length;
  }

  public long identify(byte index) {
    return pointers[index];
  }

}
