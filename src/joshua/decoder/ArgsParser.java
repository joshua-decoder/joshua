/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package joshua.decoder;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

import joshua.util.io.LineReader;

/**
 * @author orluke
 * 
 */
public class ArgsParser {

  private String configFile = null;

  /**
   * Parse the arguments passed from the command line when the JoshuaDecoder application was
   * executed from the command line.
   * 
   * @param args
   * @throws IOException 
   */
  public ArgsParser(String[] args, JoshuaConfiguration joshuaConfiguration) throws IOException {

    /*
     * Look for a verbose flag, -v.
     * 
     * Look for an argument to the "-config" flag to find the config file, if any. 
     */
    if (args.length >= 1) {
      // Search for a verbose flag
      for (int i = 0; i < args.length; i++) {
        if (args[i].equals("-v")) {
          Decoder.VERBOSE = Integer.parseInt(args[i + 1].trim());
          break;
        }
      
        if (args[i].equals("-version")) {
          LineReader reader = new LineReader(String.format("%s/VERSION", System.getenv("JOSHUA")));
          reader.readLine();
          String version = reader.readLine().split("\\s+")[2];
          System.out.println(String.format("The Joshua machine translator, version %s", version));
          System.out.println("joshua-decoder.org");
          System.exit(0);

        } else if (args[i].equals("-license")) {
          try {
            for (String line: Files.readAllLines(Paths.get(String.format("%s/../LICENSE", 
                JoshuaConfiguration.class.getProtectionDomain().getCodeSource().getLocation().getPath())), 
                Charset.defaultCharset())) {
              System.out.println(line);
            }
          } catch (IOException e) {
            System.err.println("FATAL: missing license file!");
          }
          System.exit(0);
        }
      }

      // Search for the configuration file from the end (so as to take the last one)
      for (int i = args.length-1; i >= 0; i--) {
        if (args[i].equals("-c") || args[i].equals("-config")) {

          setConfigFile(args[i + 1].trim());
          try {
            Decoder.LOG(1, "Parameters read from configuration file:");
            joshuaConfiguration.readConfigFile(getConfigFile());
          } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }

          break;
        }
      }

      // Now process all the command-line args
      Decoder.LOG(1, "Parameters overridden from the command line:");
      joshuaConfiguration.processCommandLineOptions(args);
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
}
