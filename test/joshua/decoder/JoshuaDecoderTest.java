package joshua.decoder;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

import org.testng.Assert;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

/**
 * Performs regression tests to verify that the decoder produces expected output
 * on known data sets.
 * 
 * @author Lane Schwartz
 */
public class JoshuaDecoderTest {

  @Parameters({ "configFile", "sourceInput", "referenceOutput" })
  @Test
  public void regressionTest(String configFile, String sourceInput, String referenceOutput)
      throws IOException {

    File referenceFile = new File(referenceOutput);
    File output = File.createTempFile("output", null);// ,
                                                      // referenceFile.getParentFile());

    String[] args = { configFile, sourceInput, output.getAbsoluteFile().toString() };
    JoshuaDecoder.main(args);

    Scanner resultScanner = new Scanner(output);
    Scanner refScanner = new Scanner(referenceFile);

    while (resultScanner.hasNextLine() && refScanner.hasNextLine()) {

      String resultLine = resultScanner.nextLine();
      String refLine = refScanner.nextLine();

      String[] resultParts = resultLine.split(" \\|\\|\\| ");
      String[] refParts = refLine.split(" \\|\\|\\| ");

      Assert.assertEquals(resultParts.length, 4);
      Assert.assertEquals(refParts.length, 4);

      Assert.assertEquals(Integer.parseInt(resultParts[0]), Integer.parseInt(refParts[0]));
      Assert.assertEquals(resultParts[1], refParts[1]);

      String[] resultFeatures = resultParts[2].split(" ");
      String[] refFeatures = refParts[2].split(" ");

      Assert.assertEquals(resultFeatures.length, 5);
      Assert.assertEquals(refFeatures.length, 5);

      float acceptableDelta = 0.001f;
      for (int i = 0; i < refFeatures.length; i++) {
        Assert.assertEquals(Float.valueOf(resultFeatures[i]), Float.valueOf(refFeatures[i]),
            acceptableDelta);
      }
    }
    
    resultScanner.close();
    refScanner.close();
  }

}
