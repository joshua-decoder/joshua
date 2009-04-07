package joshua.decoder.ff;

import java.util.ArrayList;

public class FeatureFunctionList extends ArrayList<FeatureFunction> {

	private ArrayList<PhraseModelFF> phraseModelCache;
	private int[] columnIndices;

	public FeatureFunctionList() {
		phraseModelCache = new ArrayList<PhraseModelFF>();
	}

	public void cachePhraseModelFF(PhraseModelFF ff) {
		phraseModelCache.add(ff);
	}

	/**
	 * Reassigns column indices to PhraseModelFFs in FeatureFunctionSet by order
	 * of addition, while maintaining a mapping to the original indices. Thus,
	 * rules don't have to store all features in grammar file, but only those
	 * that are actually used.
	 */
	public void collapseColumnIndices() {
		int collapsedIndex = 0;
		columnIndices = new int[phraseModelCache.size()];
		for (PhraseModelFF ff : phraseModelCache) {
			columnIndices[collapsedIndex] = ff.getColumnIndex();
			ff.setColumnIndex(collapsedIndex);
			this.add(ff);
			collapsedIndex++;
		}
		phraseModelCache = null;
	}

	/**
	 * @return array of relevant feature column indices, as specified in
	 *         configuration.
	 */
	public int[] getColumns() {
		return columnIndices;
	}

	private static final long serialVersionUID = 7637531202301211003L;
}
