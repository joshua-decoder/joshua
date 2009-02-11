/* This file is part of the Joshua Machine Translation System.
 * 
 * Joshua is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free
 * Software Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 * MA 02111-1307 USA
 */

package joshua.ZMERT;
import java.util.*;
import java.io.*;

public class ZMERT
{
  public static void main(String[] args) throws Exception
  {
    boolean external = false;

    if (args.length == 1) {
      if (args[0].equals("-h")) { MertCore.printZMERTUsage(args.length,true); System.exit(2); }
      else { external = false; }
    } else if (args.length == 3) { external = true; }
    else { MertCore.printZMERTUsage(args.length,false); System.exit(1); }

    if (!external) {
      MertCore myMert = new MertCore(args[0]);
      myMert.run_MERT(); // optimize lambda[]!!!
      myMert.finish();
    } else {
      int maxMem = Integer.parseInt(args[1]);
      String configFileName = args[2];
      String stateFileName = "ZMERT.temp.state";
      String cp = System.getProperty("java.class.path");
      boolean done = false;
      int iteration = 0;
      while (!done) {
        ++iteration;
        Runtime rt = Runtime.getRuntime();
        Process p = rt.exec("java -Xmx" + maxMem + "m -cp " + cp + " joshua.ZMERT.MertCore " + configFileName + " " + stateFileName + " " + iteration);
        BufferedReader br_i = new BufferedReader(new InputStreamReader(p.getInputStream()));
        BufferedReader br_e = new BufferedReader(new InputStreamReader(p.getErrorStream()));
        String dummy_line = null;
        while ((dummy_line = br_i.readLine()) != null) { System.out.println(dummy_line); }
        while ((dummy_line = br_e.readLine()) != null) { System.out.println(dummy_line); }
        int status = p.waitFor();

        if (status == 90) { done = true; }
        else if (status == 91) { done = false; }
        else { System.out.println("Z-MERT exiting prematurely (MertCore returned " + status + ")..."); break; }
      }
    }

    System.exit(0);

  } // main(String[] args)
}
