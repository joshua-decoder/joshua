package joshua.decoder.ff.morph;

/*
 * Format of training file:
 * 
 * source_word target_word feature:value feature:value feature:value ...
 * 
 * Invocation:
 * 
 * java -cp /Users/post/code/joshua/lib/mallet-2.0.7.jar:/Users/post/code/joshua/lib/trove4j-2.0.2.jar:$JOSHUA/class joshua.decoder.ff.morph.InflectionPredictor /path/to/training/data 
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import cc.mallet.classify.*;
import cc.mallet.pipe.*;
import cc.mallet.pipe.iterator.CsvIterator;
import cc.mallet.types.InstanceList;
import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.chart_parser.SourcePath;
import joshua.decoder.ff.FeatureVector;
import joshua.decoder.ff.StatelessFF;
import joshua.decoder.ff.state_maintenance.DPState;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.segment_file.Sentence;

public class InflectionPredictor extends StatelessFF {

  private Classifier classifier = null;
  
  public InflectionPredictor(final FeatureVector weights, String[] args, JoshuaConfiguration config) {
    super(weights, "InfectionPredictor", args, config);

    if (parsedArgs.containsKey("model")) {
      String modelFile = parsedArgs.get("model");
      if (! new File(modelFile).exists()) {
        if (parsedArgs.getOrDefault("training-data", null) != null) {
          try {
            classifier = train(parsedArgs.get("training-data"));
          } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
        } else {
          System.err.println("* FATAL: no model and no training data.");
          System.exit(1);
        }
      } else {
        // TODO: load the model
      }
    }
  }
  
  public Classifier train(String dataFile) throws FileNotFoundException {
    ArrayList<Pipe> pipeList = new ArrayList<Pipe>();

    // I don't know if this is needed
    pipeList.add(new Target2Label());
    // Convert SVM-light format to sparse feature vector
    pipeList.add(new SvmLight2FeatureVectorAndLabel());
    // Validation
//    pipeList.add(new PrintInputAndTarget());
    
    // name: english word
    // data: features (FeatureVector)
    // target: foreign inflection
    // source: null

    // Remove the first field (Mallet's "name" field), leave the rest for SVM-light conversion
    InstanceList instances = new InstanceList(new SerialPipes(pipeList));
    instances.addThruPipe(new CsvIterator(new FileReader(dataFile),
        "(\\w+)\\s+(.*)",
        2, -1, 1));
          
    ClassifierTrainer trainer = new MaxEntTrainer();
    Classifier classifier = trainer.train(instances);
    
    return classifier;
  }
    
  @Override
  public DPState compute(Rule rule, List<HGNode> tailNodes, int i, int j, SourcePath sourcePath,
      Sentence sentence, Accumulator acc) {
    
    return null;
  }
  
  public static void main(String[] args) throws FileNotFoundException {
    InflectionPredictor ip = new InflectionPredictor(null, args, null);
    
    String dataFile = "/Users/post/Desktop/amazon16/model";
    if (args.length > 0)
      dataFile = args[0];
    
    ip.train(dataFile);
  }

}
