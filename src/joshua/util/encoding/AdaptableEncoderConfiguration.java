package joshua.util.encoding;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import joshua.corpus.Vocabulary;

public class AdaptableEncoderConfiguration {

  private static final Logger logger = Logger.getLogger(AdaptableEncoderConfiguration.class.getName());

  private static final Encoder DEFAULT;

  private ArrayList<Encoder> encoders;
  private Map<Integer, Integer> encoderByFeatureId;

  private Map<Integer, Integer> featureIdMap;
  
  // Is the feature setup labeled.
  private boolean labeled;
  
  // Is the encoder configuration open for new features (that are not assumed boolean)?
  private boolean open;

  static {
    DEFAULT = new BooleanEncoder();
  }

  public AdaptableEncoderConfiguration() {
    this(false);
  }

  public AdaptableEncoderConfiguration(boolean open) {
    this.open = open;
    this.encoders = new ArrayList<Encoder>();
    this.encoderByFeatureId = new HashMap<Integer, Integer>();
    this.featureIdMap = new HashMap<Integer, Integer>();
  }

  private int add(String encoder_key) {
    Encoder q = EncoderFactory.get(encoder_key);
    encoders.add(q);
    return encoders.size() - 1;
  }

  public void add(String encoder_key, List<Integer> feature_ids) {
    int index = add(encoder_key);
    for (int feature_id : feature_ids)
      encoderByFeatureId.put(feature_id, index);
  }

  public void add(String encoder_key, int feature_id) {
    int index = add(encoder_key);
    encoderByFeatureId.put(feature_id, index);
  }

  public void initialize() {
    for (Encoder q : encoders)
      q.initialize();
  }

  public void finalize() {
    for (Encoder q : encoders)
      q.finalize();
  }

  public final Encoder get(int feature_id) {
    Integer index = encoderByFeatureId.get(feature_id);
    if (index != null)
      return encoders.get(index);
    else if (open) {
      this.add("8bit", feature_id);
      Encoder q = encoders.get(encoderByFeatureId.get(feature_id));
      q.initialize();
      return q;
    } else
      return DEFAULT;
  }

  // Inspects the collected histograms, inferring actual type of feature. Then replaces the 
  // temporary 8-bit encoder with the most compact applicable type.
  public void inferTypes(boolean labeled) {
    for (int i = 0; i < encoders.size(); ++i) {
      if ("8bit".equals(encoders.get(i).getKey())) {
        EightBitQuantizer q = (EightBitQuantizer) encoders.get(i);
        if (q.isBoolean())
          encoders.set(i, new BooleanEncoder());
        else if (q.isByte())
          encoders.set(i, new ByteEncoder());
        else if (q.is8Bit())
          continue;
        else if (q.isChar())
          encoders.set(i, new CharEncoder());
        else if (q.isShort())
          encoders.set(i, new ShortEncoder());
        else if (q.isInt())
          encoders.set(i, new IntEncoder());
        else
          encoders.set(i, new FloatEncoder());
      }
    }
    for (int id : encoderByFeatureId.keySet())
      logger.info("Type inferred: " + (labeled ? Vocabulary.word(id) : "Feature " + id) + " is "
          + encoders.get(encoderByFeatureId.get(id)).getKey());
  }
  
  public void buildFeatureMap() {
    int[] known_features = new int[encoderByFeatureId.keySet().size()];
    int i = 0;
    for (int f : encoderByFeatureId.keySet())
      known_features[i++] = f;
    Arrays.sort(known_features);
    
    featureIdMap.clear();
    for (i = 0; i < known_features.length; ++i)
      featureIdMap.put(known_features[i], i);
  }
  
  public int getRank(int feature_id) {
    return featureIdMap.get(feature_id);
  }
  
  public Encoder getIdEncoder() {
    int num_features = featureIdMap.size(); 
    if (num_features <= Byte.MAX_VALUE)
      return new ByteEncoder();
    else if (num_features <= Character.MAX_VALUE)
      return new CharEncoder();
    else
      return new IntEncoder();
  }
  
  public void read(String file_name) throws IOException {
    encoders.clear();
    encoderByFeatureId.clear();
    featureIdMap.clear();

    File encoder_file = new File(file_name);
    DataInputStream in_stream =
        new DataInputStream(new BufferedInputStream(new FileInputStream(encoder_file)));

    Encoder encoder = EncoderFactory.get(in_stream.readUTF());
    encoder.readState(in_stream);
    labeled = in_stream.readBoolean();
    
    int num_encoders = in_stream.readInt();
    encoders.ensureCapacity(num_encoders);
    for (int i = 0; i < num_encoders; i++) {
      String key = in_stream.readUTF();
      Encoder e = EncoderFactory.get(key);
      e.readState(in_stream);
      encoders.add(e);
    }
    
    int num_mappings = in_stream.readInt();
    for (int i = 0; i < num_mappings; i++) {
      int feature_id = (labeled ? Vocabulary.id(in_stream.readUTF()) : in_stream.readInt());
      int feature_rank = in_stream.readInt();
      int quantizer_index = in_stream.readInt();
      if (quantizer_index >= num_encoders) {
        throw new RuntimeException("Error deserializing encoder config. Feature "
            + (labeled ? Vocabulary.word(feature_id) : feature_id) + " referring to encoder "
            + quantizer_index + " when only " + num_encoders + " known.");
      }
      this.encoderByFeatureId.put(feature_id, quantizer_index);
      this.featureIdMap.put(feature_id, feature_rank);
    }
    in_stream.close();
  }

  public void write(String file_name) throws IOException {
    File out_file = new File(file_name);
    DataOutputStream out_stream =
        new DataOutputStream(new BufferedOutputStream(new FileOutputStream(out_file)));
    
    out_stream.writeUTF(getIdEncoder().getKey());
    getIdEncoder().writeState(out_stream);
    out_stream.writeBoolean(labeled);
    
    out_stream.writeInt(encoders.size());
    for (int index = 0; index < encoders.size(); index++)
      encoders.get(index).writeState(out_stream);
    
    buildFeatureMap();
    out_stream.writeInt(encoderByFeatureId.size());
    for (int feature_id : encoderByFeatureId.keySet()) {
      if (labeled) 
        out_stream.writeUTF(Vocabulary.word(feature_id));
      else 
        out_stream.writeInt(feature_id);
      out_stream.writeInt(featureIdMap.get(feature_id));
      out_stream.writeInt(encoderByFeatureId.get(feature_id));
    }
    out_stream.close();
  }

  public boolean isLabeled() {
    return labeled;
  }

  public void setLabeled(boolean labeled) {
    this.labeled = labeled;
  }
}
