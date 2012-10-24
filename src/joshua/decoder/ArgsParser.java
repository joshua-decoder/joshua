/**
 * 
 */
package joshua.decoder;

import java.io.IOException;

/**
 * @author orluke
 *
 */
public class ArgsParser {

  private String configFile = null;
  private String testFile = "-";
  private String nbestFile = "-";
  private String oracleFile = null;

  /**
   * Parse the arguments passed from the command line when the JoshuaDecoder application was
   * executed from the command line.
   * 
   * @param args
   */
  public ArgsParser(String[] args) {

    // if (args.length < 1) {
    // System.out.println("Usage: java " + JoshuaDecoder.class.getName()
    // + " -c configFile [other args]");
    // System.exit(1);
    // }

    // Step-0: Process the configuration file. We accept two use
    // cases. (1) For backwards compatility, Joshua can be called
    // with as "Joshua configFile [testFile [outputFile
    // [oracleFile]]]". (2) Command-line options can be used, in
    // which case we look for an argument to the "-config" flag.
    // We can distinguish these two cases by looking at the first
    // argument; if it starts with a hyphen, the new format has
    // been invoked.

    if (args.length >= 1) {
      if (args[0].startsWith("-")) {

        // Search for the configuration file
        for (int i = 0; i < args.length; i++) {
          if (args[i].equals("-c") || args[i].equals("-config")) {

            setConfigFile(args[i + 1].trim());
            try {
              JoshuaConfiguration.readConfigFile(getConfigFile());
            } catch (IOException e) {
              // TODO Auto-generated catch block
              e.printStackTrace();
            }

            break;
          }
        }

        // now process all the command-line args
        JoshuaConfiguration.processCommandLineOptions(args);

        setOracleFile(JoshuaConfiguration.oracleFile);

      } else {

        setConfigFile(args[0].trim());

        try {
          JoshuaConfiguration.readConfigFile(getConfigFile());
        } catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }

        if (args.length >= 2) setTestFile(args[1].trim());
        if (args.length >= 3) setNbestFile(args[2].trim());
        if (args.length == 4) setOracleFile(args[3].trim());
      }
    }

  }

  /**
   * @return the configFile
   */
  public String getConfigFile() {
    return configFile;
  }

  /**
   * @param configFile the configFile to set
   */
  public void setConfigFile(String configFile) {
    this.configFile = configFile;
  }

  /**
   * @return the testFile
   */
  public String getTestFile() {
    return testFile;
  }

  /**
   * @param testFile the testFile to set
   */
  public void setTestFile(String testFile) {
    this.testFile = testFile;
  }

  /**
   * @return the nbestFile
   */
  public String getNbestFile() {
    return nbestFile;
  }

  /**
   * @param nbestFile the nbestFile to set
   */
  public void setNbestFile(String nbestFile) {
    this.nbestFile = nbestFile;
  }

  /**
   * @return the oracleFile
   */
  public String getOracleFile() {
    return oracleFile;
  }

  /**
   * @param oracleFile the oracleFile to set
   */
  public void setOracleFile(String oracleFile) {
    this.oracleFile = oracleFile;
  }
}
