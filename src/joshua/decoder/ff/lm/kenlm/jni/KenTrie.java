package joshua.decoder.ff.lm.kenlm.jni;

// TODO(Joshua devs): include my state object with your LM state then update this API to pass state instead of int[].  

class KenTrie extends KenLM {
  static {
    System.loadLibrary("ken");
  }
  private final static native long create(String file_name, VocabCallback callback);
  private final static native void destroy(long pointer);
  private final static native int order(long pointer);
  private final static native int vocab(long pointer, String word);
  private final static native float prob(long pointer, int[] words);
  private final static native float probString(long pointer, int[] words, int start);

  public KenTrie(String file_name, VocabCallback callback) {
    super(create(file_name, callback));
  }

  protected int internalOrder() { return order(pointer); }

  public int vocab(String word) { return vocab(pointer, word); }

  public float prob(int[] words) { return prob(pointer, words); }

  public float probString(int[] words, int start) { return probString(pointer, words, start); }

  public void destroy() { destroy(pointer); }
}
