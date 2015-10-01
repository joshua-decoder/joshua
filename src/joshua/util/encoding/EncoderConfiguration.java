package joshua.util.encoding;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import joshua.corpus.Vocabulary;

public class EncoderConfiguration {

  public static int ID_SIZE = 4;

  private IntEncoder idEncoder;
  private int[] innerToOuter;
  private FloatEncoder[] encoderById;
  private FloatEncoder[] encoders;

  private Map<Integer, Integer> outerToInner;
  
  private boolean labeled;
  
  private int numDenseFeatures = 0;
  
  public EncoderConfiguration() {
    this.outerToInner = new HashMap<Integer, Integer>();
  }

  public int getNumDenseFeatures() {
    return numDenseFeatures;
  }
  
  public int getNumFeatures() {
    return encoders.length;
  }
  
  public void load(String file_name) throws IOException {
    File encoding_file = new File(file_name);
    BufferedInputStream buf_stream = new BufferedInputStream(new FileInputStream(encoding_file));
    DataInputStream in_stream = new DataInputStream(buf_stream);

    String id_key = in_stream.readUTF();
    idEncoder = EncoderFactory.getIntEncoder(id_key);
    idEncoder.readState(in_stream);
    ID_SIZE = idEncoder.size();
    labeled = in_stream.readBoolean();

    int num_encoders = in_stream.readInt();
    encoders = new FloatEncoder[num_encoders];
    for (int i = 0; i < num_encoders; i++) {
      String key = in_stream.readUTF();
      FloatEncoder e = EncoderFactory.getFloatEncoder(key);
      e.readState(in_stream);
      encoders[i] = e;
    }
    int num_features = in_stream.readInt();
    encoderById = new FloatEncoder[num_features];
    innerToOuter = new int[num_features];
    for (int i = 0; i < num_features; i++) {
      int outer_id;
      if (labeled) {
        String feature_name = in_stream.readUTF();
        outer_id = Vocabulary.id(feature_name);
        try {
          Integer.parseInt(feature_name);
          numDenseFeatures++;
        } catch (NumberFormatException e) {}
      } else {
        outer_id = in_stream.readInt();
      }
      int inner_id = in_stream.readInt();
      int encoder_index = in_stream.readInt();
      if (encoder_index >= num_encoders) {
        throw new RuntimeException("Error deserializing EncoderConfig. " + "Feature "
            + (labeled ? Vocabulary.word(outer_id) : outer_id) + " referring to encoder "
            + encoder_index + " when only " + num_encoders + " known.");
      }
      encoderById[inner_id] = encoders[encoder_index];
      innerToOuter[inner_id] = outer_id;
    }
    in_stream.close();
    
    outerToInner.clear();
    for (int i = 0; i < innerToOuter.length; ++i)
      outerToInner.put(innerToOuter[i], i);
  }

  public FloatEncoder encoder(int inner_id) {
    return encoderById[inner_id];
  }
  
  public int readId(ByteBuffer buffer, int pos) {
    return idEncoder.read(buffer, pos);
  }
  
  public int outerId(int inner_id) {
    return innerToOuter[inner_id];
  }
  
  public int innerId(int outer_id) {
    return outerToInner.get(outer_id);
  }
  
  public boolean isLabeled() {
    return labeled;
  }

  /**
   * For now, this just loads a configuration and prints out the number of features.
   * 
   * @param args
   */
  public static void main(String[] args) {
    String grammar_dir = null;
    try {
      grammar_dir = args[0];
    
      EncoderConfiguration encoding = new EncoderConfiguration();
      encoding.load(grammar_dir + File.separator + "encoding");
      int num_features = encoding.getNumFeatures();
      System.out.println(String.format("num_features = %d", encoding.getNumFeatures()));

      for (int feature_id = 0; feature_id < num_features; feature_id++) {
        if (Vocabulary.size() == 1) {
          System.out.println(String.format("feature: %d", feature_id));
        } else {
          String name = Vocabulary.word(encoding.outerId(feature_id));
          System.out.println(String.format("feature: %s", name));
        }
      }

    } catch (ArrayIndexOutOfBoundsException e) {
      System.err.println("Usage: EncoderConfiguration <packed_directory>");
      System.exit(1);
    } catch (IOException e) {
      System.err.println(String.format("* FATAL: can't find file %s/encoding", grammar_dir));
      System.exit(1);
    }
  }
}
