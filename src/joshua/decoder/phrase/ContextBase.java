package joshua.decoder.phrase;

import joshua.decoder.Decoder;
import joshua.decoder.JoshuaConfiguration;

public class ContextBase {
  private JoshuaConfiguration config;
  
  public ContextBase(JoshuaConfiguration config) {
    this.config = config;
  }
  
  public int PopLimit() {
    return config.pop_limit;
  }
  
  public float LMWeight() {
    return Decoder.weights.get("lm_0");
  }
  
  public JoshuaConfiguration GetConfig() {
    return config;
  }
}
