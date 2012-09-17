package joshua.decoder.ff.similarity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import joshua.corpus.Vocabulary;
import joshua.decoder.chart_parser.SourcePath;
import joshua.decoder.ff.FeatureVector;
import joshua.decoder.ff.StatefulFF;
import joshua.decoder.ff.SourceDependentFF;
import joshua.decoder.ff.state_maintenance.StateComputer;
import joshua.decoder.ff.state_maintenance.DPState;
import joshua.decoder.ff.state_maintenance.NgramDPState;
import joshua.decoder.ff.tm.BilingualRule;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.segment_file.Sentence;
import joshua.util.Cache;

public class EdgePhraseSimilarityFF extends StatefulFF implements SourceDependentFF {

  private static Cache<String, Float> cache = new Cache<String, Float>(100000000);

  private String host;
  private int port;

  private Socket socket;
  private PrintWriter serverAsk;
  private BufferedReader serverReply;

  private int[] source;

  private final int MAX_PHRASE_LENGTH = 4;
  private final int GAP = 0;

	// This feature template defines only a single feature, so we cache the weight.
	private float weight;

  public EdgePhraseSimilarityFF(FeatureVector weights, StateComputer state, String host, int port)
      throws NumberFormatException, UnknownHostException, IOException {
    super(weights, "EdgePhraseSimilarity", state);

    this.host = host;
    this.port = port;

		this.weight = weights.get(name);

    initializeConnection();
  }

  private void initializeConnection() throws NumberFormatException, UnknownHostException,
      IOException {
    System.err.println("Opening connection.");
    socket = new Socket(host, port);
    serverAsk = new PrintWriter(socket.getOutputStream(), true);
    serverReply = new BufferedReader(new InputStreamReader(socket.getInputStream()));
  }

	public FeatureVector computeFeatures(Rule rule, List<HGNode> tailNodes, int spanStart, int spanEnd,
		SourcePath sourcePath, int sentID) {

		float value = computeCost(rule, tailNodes, spanStart, spanEnd, sourcePath, sentID);
		return new FeatureVector(name, value);
	}


  public float computeCost(Rule rule, List<HGNode> tailNodes, int spanStart, int spanEnd,
		SourcePath sourcePath, int sentID) {
    float similarity = 0.0f;
    int count = 0;
    if (tailNodes == null || tailNodes.isEmpty()) return 0;

    // System.err.println("RULE [" + spanStart + ", " + spanEnd + "]: " + rule.toString());

    int[] target = ((BilingualRule) rule).getEnglish();
    int lm_state_size = 0;
    for (HGNode node : tailNodes) {
      NgramDPState state = (NgramDPState) node.getDPState(stateComputer);
      lm_state_size += state.getLeftLMStateWords().size() + state.getRightLMStateWords().size();
    }

    ArrayList<int[]> batch = new ArrayList<int[]>();

    // Build joined target string.
    int[] join = new int[target.length + lm_state_size];

    int idx = 0, num_gaps = 1, num_anchors = 0;
    int[] anchors = new int[rule.getArity() * 2];
    int[] indices = new int[rule.getArity() * 2];
    int[] gaps = new int[rule.getArity() + 2];
    gaps[0] = 0;
    for (int t = 0; t < target.length; t++) {
      if (target[t] < 0) {
        HGNode node = tailNodes.get(-(target[t] + 1));
        if (t != 0) {
          indices[num_anchors] = node.i;
          anchors[num_anchors++] = idx;
        }
        NgramDPState state = (NgramDPState) node.getDPState(stateComputer);
        // System.err.print("LEFT:  ");
        // for (int w : state.getLeftLMStateWords()) System.err.print(Vocabulary.word(w) + " ");
        // System.err.println();
        for (int w : state.getLeftLMStateWords())
          join[idx++] = w;
        join[idx++] = GAP;
        gaps[num_gaps++] = idx;
        // System.err.print("RIGHT:  ");
        // for (int w : state.getRightLMStateWords()) System.err.print(Vocabulary.word(w) + " ");
        // System.err.println();
        for (int w : state.getRightLMStateWords())
          join[idx++] = w;
        if (t != target.length - 1) {
          indices[num_anchors] = node.j;
          anchors[num_anchors++] = idx;
        }
      } else {
        join[idx++] = target[t];
      }
    }
    gaps[gaps.length - 1] = join.length + 1;

    // int c = 0;
    // System.err.print("> ");
    // for (int k = 0; k < join.length; k++) {
    // if (c < num_anchors && anchors[c] == k) {
    // c++;
    // System.err.print("| ");
    // }
    // System.err.print(Vocabulary.word(join[k]) + " ");
    // }
    // System.err.println("<");

    int g = 0;
    for (int a = 0; a < num_anchors; a++) {
      if (a > 0 && anchors[a - 1] == anchors[a]) continue;
      if (anchors[a] > gaps[g + 1]) g++;
      int left = Math.max(gaps[g], anchors[a] - MAX_PHRASE_LENGTH + 1);
      int right = Math.min(gaps[g + 1] - 1, anchors[a] + MAX_PHRASE_LENGTH - 1);

      int[] target_phrase = new int[right - left];
      System.arraycopy(join, left, target_phrase, 0, target_phrase.length);
      int[] source_phrase = getSourcePhrase(indices[a]);

      if (source_phrase != null && target_phrase.length != 0) {
        // System.err.println("ANCHOR: " + indices[a]);
        batch.add(source_phrase);
        batch.add(target_phrase);
      }
    }
    return weight * getSimilarity(batch);
  }


  @Override
  public float computeFinalCost(HGNode antNode, int i, int j, SourcePath sourcePath, int sentID) {
    return 0.0f;
  }

	public float estimateFutureCost(Rule rule, DPState currentState, int sentID) {
		return 0.0f;
	}

	/**
	 * From SourceDependentFF interface.
	 */
  @Override
  public void setSource(Sentence source) {
    this.source = source.intSentence();
  }

  public EdgePhraseSimilarityFF clone() {
    try {
      return new EdgePhraseSimilarityFF(this.weights, this.stateComputer, host, port);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

	@Override
	public float estimateCost(Rule rule, int sentID) {
		return 0.0f;
	}

  private final int[] getSourcePhrase(int anchor) {
    int idx;
    int length =
        Math.min(anchor, MAX_PHRASE_LENGTH - 1)
            + Math.min(source.length - anchor, MAX_PHRASE_LENGTH - 1);
    if (length <= 0) return null;
    int[] phrase = new int[length];
    idx = 0;
    for (int p = Math.max(0, anchor - MAX_PHRASE_LENGTH + 1); p < Math.min(source.length, anchor
        + MAX_PHRASE_LENGTH - 1); p++)
      phrase[idx++] = source[p];
    return phrase;
  }

  private float getSimilarity(List<int[]> batch) {
    float similarity = 0.0f;
    int count = 0;
    StringBuilder query = new StringBuilder();
    List<String> to_cache = new ArrayList<String>();
    query.append("xb");
    for (int i = 0; i < batch.size(); i += 2) {
      int[] source = batch.get(i);
      int[] target = batch.get(i + 1);

      if (source.equals(target)) {
        similarity += 1;
        count++;
      } else {
        String source_string = Vocabulary.getWords(source);
        String target_string = Vocabulary.getWords(target);

        String both;
        if (source_string.compareTo(target_string) > 0)
          both = source_string + " ||| " + target_string;
        else
          both = target_string + " ||| " + source_string;

        Float cached = cache.get(both);
        if (cached != null) {
          // System.err.println("SIM: " + source_string + " X " + target_string + " = " + cached);
          similarity += cached;
          count++;
        } else {
          query.append("\t").append(source_string);
          query.append("\t").append(target_string);
          to_cache.add(both);
        }
      }
    }
    if (!to_cache.isEmpty()) {
      try {
        serverAsk.println(query.toString());
        String response = serverReply.readLine();
        String[] scores = response.split("\\s+");
        for (int i = 0; i < scores.length; i++) {
          Float score = Float.parseFloat(scores[i]);
          cache.put(to_cache.get(i), score);
          similarity += score;
          count++;
        }
      } catch (Exception e) {
        return 0;
      }
    }
    return (count == 0 ? 0 : similarity / count);
  }

}
