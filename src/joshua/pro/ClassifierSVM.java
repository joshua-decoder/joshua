package joshua.pro;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Vector;

import joshua.util.StreamGobbler;
import joshua.util.io.LineReader;

public class ClassifierSVM implements ClassifierInterface {
  @Override
  public double[] runClassifier(Vector<String> samples, double[] initialLambda, int featDim) {
    System.out.println("------- SVM training starts ------");

    double[] lambda = new double[featDim + 1];
    for (int i = 1; i <= featDim; i++)
      lambda[i] = 0;

    // String root_dir =
    // "/media/Data/JHU/Research/MT discriminative LM training/joshua_expbleu/PRO_test/";
    // String root_dir = "/home/ycao/WS11/nist_zh_en_percep/pro_forward/pro_libsvm/";

    try {
      // prepare training file for MegaM
      PrintWriter prt = new PrintWriter(new FileOutputStream(trainingFilePath));

      for (String line : samples) {
        String[] feat = line.split("\\s+");

        if (feat[feat.length - 1].equals("1"))
          prt.print("+1 ");
        else
          prt.print("-1 ");

        for (int i = 0; i < feat.length - 1; i++)
          prt.print((i + 1) + ":" + feat[i] + " "); // feat id starts from 1!

        prt.println();
      }
      prt.close();

      // start running SVM
      Runtime rt = Runtime.getRuntime();
      // String cmd = "/home/yuan/tmp_libsvm_command";

      Process p = rt.exec(commandFilePath); // only linear kernel is used

      StreamGobbler errorGobbler = new StreamGobbler(p.getErrorStream(), 1);
      StreamGobbler outputGobbler = new StreamGobbler(p.getInputStream(), 1);

      errorGobbler.start();
      outputGobbler.start();

      int decStatus = p.waitFor();
      if (decStatus != 0) {
        System.out.println("Call to decoder returned " + decStatus + "; was expecting " + 0 + ".");
        System.exit(30);
      }

      // read the model file
      boolean sv_start = false;
      double coef;

      for (String line: new LineReader(modelFilePath)) {
        if (sv_start) // start reading support vectors and coefs
        {
          String[] val = line.split("\\s+");
          coef = Double.parseDouble(val[0]);

          // System.out.print(coef+" ");

          for (int i = 1; i < val.length; i++) // only valid for linear kernel
          // W = \sum_{i=1}^{l} y_i alpha_i phi(x_i)
          // = \sum_{i=1}^{l} coef_i x_i
          {
            String[] sv = val[i].split(":"); // feat id
            lambda[Integer.parseInt(sv[0])] += coef * Double.parseDouble(sv[1]); // index starts
                                                                                 // from 1
            // System.out.print(Integer.parseInt(sv[0])+" "+Double.parseDouble(sv[1])+" ");
          }

          // System.out.println();
        }

        if (line.equals("SV")) sv_start = true;
      }

      File file = new File(trainingFilePath);
      file.delete();
      file = new File(modelFilePath);
      file.delete();
    } catch (IOException exception) {
      exception.getStackTrace();
    } catch (InterruptedException e) {
      System.err.println("InterruptedException in MertCore.run_decoder(int): " + e.getMessage());
      System.exit(99903);;
    }

    System.out.println("------- SVM training ends ------");

    return lambda;
  }

  @Override
  /*
   * for LibSVM: param[0] = LibSVM command file path param[1] = LibSVM training data file(generated
   * on the fly) path param[2] = LibSVM model file(generated after training) path note: the training
   * file path should be consistent with the one specified in command file
   */
  public void setClassifierParam(String[] param) {
    if (param == null) {
      System.out.println("ERROR: must provide parameters for LibSVM classifier!");
      System.exit(10);
    } else {
      commandFilePath = param[0];
      trainingFilePath = param[1];
      modelFilePath = param[2];
    }
  }

  String commandFilePath;
  String trainingFilePath;
  String modelFilePath;
}
