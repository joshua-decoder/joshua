package joshua.decoder.phrase;

public class Right implements State {

  public int[] words; // c++ typedef uint WordIndex, KENLM_MAX_ORDER - 1
  public float[] backoff; // KENLM_MAX_ORDER - 1
  public byte length;

  public byte getLength() {
    return length;
  }

  public long identify(byte index) {
    return words[index];
  }

  @Override
  public boolean equals(Object obj) {
    if (! (obj instanceof Right))
      return false;
    
    Right other = (Right)obj;
    if (getLength() != other.getLength())
      return false;
    if (words.length != other.words.length)
      return false;
    for (int i = 0; i < words.length; i++)
      if (words[i] != other.words[i])
        return false;

    return true;
  }
  
  @Override
  public int hashCode() {
    return java.util.Arrays.hashCode(words) * length;
  }
}
