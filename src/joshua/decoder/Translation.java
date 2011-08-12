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

package joshua.decoder;

import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.segment_file.Sentence;
import joshua.decoder.hypergraph.HyperGraph;
import joshua.decoder.hypergraph.KBestExtractor;

import joshua.util.Regex;

import java.io.StringWriter;
import java.io.BufferedWriter;
import java.io.IOException;

import java.util.List;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class Translation {
    private int          id = -1;
    private Sentence     source;
    private String       translation;
    private List<Double> modelScores = null;
    private double       score;
    private HyperGraph   hypergraph;
    private List<FeatureFunction> featureFunctions;

    public Translation(Sentence source, HyperGraph hypergraph, List<FeatureFunction> featureFunctions) {
        this.source = source;
        this.hypergraph = hypergraph;
        this.featureFunctions = featureFunctions;
    }

    public HyperGraph hypergraph() {
        return this.hypergraph;
    }

    public Sentence getSourceSentence() {
        return this.source;
    }

    public int id() {
        return source.id();
    }

    public String translation() {

        if (this.hypergraph == null) {
            return getSourceSentence().sentence();

        } else {
            KBestExtractor kBestExtractor = new KBestExtractor(JoshuaDecoder.symbolTable,
                JoshuaConfiguration.use_unique_nbest,
                JoshuaConfiguration.use_tree_nbest,
                JoshuaConfiguration.include_align_index,
                JoshuaConfiguration.add_combined_cost,
                false, true);

            StringWriter sw = new StringWriter();
            BufferedWriter out = new BufferedWriter(sw);

            try {
                kBestExtractor.lazyKBestExtractOnHG(this.hypergraph, this.featureFunctions, 1, id(), out);
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return sw.toString();
        }
    }

    public void print() {
        if (hypergraph != null) {
            KBestExtractor kBestExtractor = new KBestExtractor(JoshuaDecoder.symbolTable,
                JoshuaConfiguration.use_unique_nbest,
                JoshuaConfiguration.use_tree_nbest,
                JoshuaConfiguration.include_align_index,
                JoshuaConfiguration.add_combined_cost,
                false, true);

            try {
                kBestExtractor.lazyKBestExtractOnHG(hypergraph, 
                    this.featureFunctions, JoshuaConfiguration.topN, id(), null);
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else {
            
            System.out.print(id() + " ||| " + getSourceSentence().sentence() + " ||| ");
			
            for (FeatureFunction ff: featureFunctions)
                System.out.print(" 0");

            System.out.println(" ||| 0.0\n");

        }
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(id());
        sb.append(" ||| ");
        sb.append(translation());
        sb.append(" ||| ");
        for (double score : modelScores) {
            sb.append(score);
            sb.append(" ");
        }
        sb.append("||| ");
        sb.append(score);

        return sb.toString();
    }

}
