package joshua.decoder.ff.similarity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;

import joshua.corpus.Vocabulary;
import joshua.decoder.chart_parser.SourcePath;
import joshua.decoder.ff.DefaultStatefulFF;
import joshua.decoder.ff.SourceDependentFF;
import joshua.decoder.ff.state_maintenance.DPState;
import joshua.decoder.ff.state_maintenance.NgramDPState;
import joshua.decoder.ff.tm.BilingualRule;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.segment_file.Sentence;
import joshua.util.Cache;

public class EdgePhraseSimilarityFF extends DefaultStatefulFF implements SourceDependentFF {

	private static Cache<String, Double> cache = new Cache<String, Double>(1000000);

	private String host;
	private int port;

	private Socket socket;
	private PrintWriter serverAsk;
	private BufferedReader serverReply;

	private int[] source;

	private final int MAX_PHRASE_LENGTH = 3;

	public EdgePhraseSimilarityFF(int stateID, double weight, int featureID, String host, int port)
			throws NumberFormatException, UnknownHostException, IOException {
		super(stateID, weight, featureID);

		this.host = host;
		this.port = port;

		initializeConnection();
	}

	private void initializeConnection() throws NumberFormatException, UnknownHostException,
			IOException {
		socket = new Socket(host, port);
		serverAsk = new PrintWriter(socket.getOutputStream(), true);
		serverReply = new BufferedReader(new InputStreamReader(socket.getInputStream()));
	}

	@Override
	public double estimateLogP(Rule rule, int sentID) {
		return 0;
	}

	@Override
	public double estimateFutureLogP(Rule rule, DPState curDPState, int sentID) {
		return 0;
	}

	@Override
	public double transitionLogP(Rule rule, List<HGNode> antNodes, int spanStart, int spanEnd,
			SourcePath srcPath, int sentID) {
		double similarity = 0;
		int count = 0;
		if (antNodes == null || antNodes.isEmpty())
			return 0;

//		System.err.println("RULE [" + spanStart + ", " + spanEnd + "]: " + rule.toString());

		int[] target = ((BilingualRule) rule).getEnglish();
		// Find where the antecedent nodes plug into the target string.
		int[] gap_positions = new int[rule.getArity()];
		for (int t = 0; t < target.length; t++)
			if (target[t] < 0)
				gap_positions[-target[t] - 1] = t;

		for (int n = 0; n < antNodes.size(); n++) {
			HGNode node = antNodes.get(n);

			NgramDPState state = (NgramDPState) node.getDPState(getStateID());

			int anchor = gap_positions[n];
			int left_bound = Math.max(0, anchor - MAX_PHRASE_LENGTH + 1);
			int right_bound = Math.min(target.length, anchor + MAX_PHRASE_LENGTH);
			if (rule.getArity() == 2) {
				int other_anchor = gap_positions[Math.abs(n - 1)];
				if (other_anchor < anchor)
					left_bound = Math.max(left_bound, other_anchor + 1);
				else
					right_bound = Math.min(right_bound, other_anchor);
			}

//			System.err.println("CHILD: [" + node.i + ", " + node.j + "]");

			if (node.i != spanStart) {
//				System.err.print("LEFT:  ");
//				for (int w : state.getLeftLMStateWords()) System.err.print(Vocabulary.word(w) + " ");
//				System.err.println();
				
				int[] i_target = getLeftTargetPhrase(target, state.getLeftLMStateWords(),
						anchor, left_bound);
				int[] i_source = getSourcePhrase(node.i);
				
//				System.err.println(n + " src_left: " + Vocabulary.getWords(i_source));
//				System.err.println(n + " tgt_left: " + Vocabulary.getWords(i_target));
				
				similarity += getSimilarity(i_source, i_target);
				count++;
			}
			if (node.j != spanEnd) {
//				System.err.print("RIGHT: ");
//				for (int w : state.getRightLMStateWords()) System.err.print(Vocabulary.word(w) + " ");
//				System.err.println();
				
				int[] j_target = getRightTargetPhrase(target, state.getRightLMStateWords(),
						anchor, right_bound);
				int[] j_source = getSourcePhrase(node.j);

//				System.err.println(n + " src_rght: " + Vocabulary.getWords(j_source));
//				System.err.println(n + " tgt_rght: " + Vocabulary.getWords(j_target));
				
				similarity += getSimilarity(j_source, j_target);
				count++;
			}
		}
		if (count == 0)
			return 0;
		return this.getWeight() * similarity / count;
	}

	private final int[] getSourcePhrase(int anchor) {
		int idx;
		int[] phrase = new int[Math.min(anchor, MAX_PHRASE_LENGTH - 1)
				+ Math.min(source.length - anchor, MAX_PHRASE_LENGTH - 1)];
		idx = 0;
		for (int p = Math.max(0, anchor - MAX_PHRASE_LENGTH + 1); p < Math.min(source.length, anchor
				+ MAX_PHRASE_LENGTH - 1); p++)
			phrase[idx++] = source[p];
		return phrase;
	}

	private final int[] getLeftTargetPhrase(int[] target, List<Integer> state, int anchor, int bound) {
		int[] phrase = new int[anchor - bound + Math.min(state.size(), MAX_PHRASE_LENGTH)];
		int idx = 0;
		for (int p = bound; p < anchor; p++)
			phrase[idx++] = target[p];
		for (int p = 0; p < state.size(); p++)
			phrase[idx++] = state.get(p);
		return phrase;
	}

	private final int[] getRightTargetPhrase(int[] target, List<Integer> state, int anchor, int bound) {
		int[] phrase = new int[bound - anchor + Math.min(state.size(), MAX_PHRASE_LENGTH) - 1];
		int idx = 0;
		for (int p = 0; p < state.size(); p++)
			phrase[idx++] = state.get(p);
		for (int p = anchor + 1; p < bound; p++)
			phrase[idx++] = target[p];
		return phrase;
	}

	private double getSimilarity(int[] source, int[] target) {
		if (source.equals(target))
			return 1.0;
		String source_string = Vocabulary.getWords(source);
		String target_string = Vocabulary.getWords(target);

		String both;
		if (source_string.compareTo(target_string) > 0)
			both = source_string + target_string;
		else
			both = target_string + " ||| " + source_string;

		Double cached = cache.get(both);
		if (cached != null) {
//			System.err.println("SIM: " + source_string + " X " + target_string + " = " + cached);
			return cached;
		} else {
			try {
				serverAsk.println("x\t" + source_string + "\t" + target_string);
				String response = serverReply.readLine();
				double similarity = Double.parseDouble(response);
				cache.put(both, similarity);

//				System.err.println("SIM: " + source_string + " X " + target_string + " = " + similarity);

				return similarity;
			} catch (Exception e) {
				return 0;
			}
		}
	}

	@Override
	public double finalTransitionLogP(HGNode antNode, int spanStart, int spanEnd, SourcePath srcPath,
			int sentID) {
		return 0;
	}

	@Override
	public void setSource(Sentence source) {
		this.source = source.intSentence();
	}

	public EdgePhraseSimilarityFF clone() {
		try {
			return new EdgePhraseSimilarityFF(this.getStateID(), this.getWeight(), this.getFeatureID(),
					host, port);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
