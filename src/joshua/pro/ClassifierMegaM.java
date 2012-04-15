package joshua.pro;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Vector;

// sparse feature representation version
public class ClassifierMegaM implements ClassifierInterface {
  @Override
  public double[] runClassifier(Vector<String> samples, double[] initialLambda, int featDim) {
    double[] lambda = new double[featDim + 1];
    // String root_dir =
    // "/media/Data/JHU/Research/MT discriminative LM training/joshua_expbleu/PRO_test/";
    // String root_dir = "/home/ycao/WS11/nist_zh_en_percep/pro_forward/pro_megam/";
    System.out.println("------- MegaM training starts ------");

    try {
      // prepare training file for MegaM
      // PrintWriter prt = new PrintWriter(new FileOutputStream(root_dir+"megam_train.data"));
      PrintWriter prt = new PrintWriter(new FileOutputStream(trainingFilePath));
      String[] feat;
      String[] featInfo;

      for (String line : samples) {
        feat = line.split("\\s+");

        if (feat[feat.length - 1].equals("1"))
          prt.print("1 ");
        else
          prt.print("0 ");

        // only for dense representation
        // for(int i=0; i<feat.length-1; i++)
        // prt.print( (i+1) + " " + feat[i]+" "); //feat id starts from 1!

        for (int i = 0; i < feat.length - 1; i++) {
          featInfo = feat[i].split(":");
          prt.print(featInfo[0] + " " + featInfo[1] + " ");
        }

        prt.println();
      }
      prt.close();

      // start running MegaM
      Runtime rt = Runtime.getRuntime();
      // String cmd = "/home/yuan/tmp_megam_command";
      // String cmd = "/home/ycao/WS11/nist_zh_en_percep/pro_forward/pro_megam/megam_command";

      Process p = rt.exec(commandFilePath);

      StreamGobbler errorGobbler = new StreamGobbler(p.getErrorStream(), 1);
      StreamGobbler outputGobbler = new StreamGobbler(p.getInputStream(), 1);

      errorGobbler.start();
      outputGobbler.start();

      int decStatus = p.waitFor();
      if (decStatus != 0) {
        System.out.println("Call to decoder returned " + decStatus + "; was expecting " + 0 + ".");
        System.exit(30);
      }

      // read the weights
      BufferedReader wt = new BufferedReader(new FileReader(weightFilePath));
      String line;
      while ((line = wt.readLine()) != null) {
        String val[] = line.split("\\s+");
        lambda[Integer.parseInt(val[0])] = Double.parseDouble(val[1]);
      }

      File file = new File(trainingFilePath);
      file.delete();
      file = new File(weightFilePath);
      file.delete();
    } catch (IOException exception) {
      exception.getStackTrace();
    } catch (InterruptedException e) {
      System.err.println("InterruptedException in MertCore.run_decoder(int): " + e.getMessage());
      System.exit(99903);;
    }

    System.out.println("------- MegaM training ends ------");

    /*
     * try { Thread.sleep(20000); } catch(InterruptedException e) { }
     */

    return lambda;
  }

  @Override
  /*
   * for MegaM classifier: param[0] = MegaM command file path param[1] = MegaM training data
   * file(generated on the fly) path param[2] = MegaM weight file(generated after training) path
   * note that the training and weight file path should be consistent with that specified in the
   * command file
   */
  public void setClassifierParam(String[] param) {
    if (param == null) {
      System.out.println("ERROR: must provide parameters for MegaM classifier!");
      System.exit(10);
    } else {
      commandFilePath = param[0];
      trainingFilePath = param[1];
      weightFilePath = param[2];
    }
  }

  String commandFilePath;
  String trainingFilePath;
  String weightFilePath;
}
