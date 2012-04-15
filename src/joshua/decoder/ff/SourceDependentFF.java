package joshua.decoder.ff;

import joshua.decoder.segment_file.Sentence;

public interface SourceDependentFF extends Cloneable {

  public void setSource(Sentence sentence);

  public FeatureFunction clone();

}
