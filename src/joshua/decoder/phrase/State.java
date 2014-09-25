package joshua.decoder.phrase;

public interface State {
  byte getLength();

  long identify(byte index);
}
