package joshua.util.encoding;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Logger;

import joshua.corpus.Vocabulary;

public class FixedEncoderConfiguration {

  private static final Logger logger = Logger.getLogger(FixedEncoderConfiguration.class.getName());

  private static final Encoder DEFAULT;

  private Encoder idEncoder;
  private int[] ids;
  private Encoder[] encoderById;
  private Encoder[] encoders;
  
  static {
    DEFAULT = new BooleanEncoder();
  }

  public FixedEncoderConfiguration() {
  }
  
  public void load(String file_name) throws IOException {
    File encoding_file = new File(file_name);
    DataInputStream in_stream = 
        new DataInputStream(new BufferedInputStream(new FileInputStream(encoding_file)));
    
    idEncoder = EncoderFactory.get(in_stream.readUTF());
    idEncoder.readState(in_stream);
    boolean labeled = in_stream.readBoolean();
    
    int num_encoders = in_stream.readInt();
    encoders = new Encoder[num_encoders];
    for (int i = 0; i < num_encoders; i++) {
      String key = in_stream.readUTF();
      Encoder e = EncoderFactory.get(key);
      e.readState(in_stream);
      encoders[i] = e;
    }
    int num_features = in_stream.readInt();
    encoderById = new Encoder[num_features];
    for (int i = 0; i < num_features; i++) {
      String feature_name = in_stream.readUTF();
      int feature_id = Vocabulary.id(feature_name);
      int encoder_index = in_stream.readInt();
      if (encoder_index >= num_encoders) {
        throw new RuntimeException("Error deserializing EncoderConfig. " + "Feature "
            + feature_name + " referring to encoder " + encoder_index + " when only "
            + num_encoders + " known.");
      }
      encoderById[i] = encoders[encoder_index];
    }
    in_stream.close();
  }
}
