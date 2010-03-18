package joshua.thrax.extractor;

import joshua.corpus.Span;

public class HierarchicalSpan {

	public static final int ARITY_LIMIT = 2;
	public static final int MAX_NT_COVERAGE = 1;
	public static final int MAX_RHS_LENGTH = 7;

	Span sourceRoot;
	Span targetRoot;

/*
	Span sourceX1;
	Span targetX1;

	Span sourceX2;
	Span targetX2;
*/
	Span sourceNonTerminals[];
	Span targetNonTerminals[];

	public int arity;
	public int sourceRhsSize;
	public int targetRhsSize;

	public int [] targetNonTerminalOrder;

	public HierarchicalSpan(Span s, Span t)
	{
		this.sourceRoot = s;
		this.targetRoot = t;
/*
		this.sourceX1 = null;
		this.targetX1 = null;
		this.sourceX2 = null;
		this.targetX2 = null;
*/
		this.sourceNonTerminals = new Span[ARITY_LIMIT];
		this.targetNonTerminals = new Span[ARITY_LIMIT];
		this.targetNonTerminalOrder = new int[ARITY_LIMIT];

		this.sourceRhsSize = s.size();
		this.targetRhsSize = t.size();

		this.arity = 0;
	}

	private HierarchicalSpan(HierarchicalSpan hs)
	{
		this.sourceRoot = hs.sourceRoot;
		this.targetRoot = hs.targetRoot;
/*		this.sourceX1 = hs.sourceX1;
		this.targetX1 = hs.targetX1;
		this.sourceX2 = hs.sourceX2;
		this.targetX2 = hs.targetX2;
*/
		this.sourceNonTerminals = new Span[ARITY_LIMIT];
		this.targetNonTerminals = new Span[ARITY_LIMIT];
		this.targetNonTerminalOrder = new int[ARITY_LIMIT];

		this.sourceRhsSize = hs.sourceRhsSize;
		this.targetRhsSize = hs.targetRhsSize;

		System.arraycopy(hs.sourceNonTerminals, 0,
		                 this.sourceNonTerminals, 0, ARITY_LIMIT);
		System.arraycopy(hs.targetNonTerminals, 0,
		                 this.targetNonTerminals, 0, ARITY_LIMIT);
		System.arraycopy(hs.targetNonTerminalOrder, 0,
		                 this.targetNonTerminalOrder, 0, ARITY_LIMIT);
		this.arity = hs.arity;
	}

	public boolean consistentWith(Span s, Span t)
	{
		if (arity >= ARITY_LIMIT) {
			return false;
		}
		if (sourceRhsSize - s.size() + 1 < MAX_NT_COVERAGE ||
		    targetRhsSize - t.size() + 1 < MAX_NT_COVERAGE) {
			return false;
		}
		if (!s.strictlyContainedIn(sourceRoot)) {
			return false;
		}
		if (!t.strictlyContainedIn(targetRoot)) {
			return false;
		}
		int i = 0;
		while (sourceNonTerminals[i] != null) {
			if (!(s.disjointFrom(sourceNonTerminals[i]) &&
			      t.disjointFrom(targetNonTerminals[i]))) {
				return false;
			}
			i++;
		}
		return true;
	}

	public HierarchicalSpan add(Span s, Span t)
	{
		HierarchicalSpan ret = new HierarchicalSpan(this);

		ret.sourceNonTerminals[ret.arity] = s;
		ret.targetNonTerminals[ret.arity] = t;

		ret.sourceRhsSize -= (s.size() - 1);
		ret.targetRhsSize -= (t.size() - 1);
		ret.arity++;
		/*
		if (sourceX1 == null) {
			ret.sourceX1 = s;
			ret.targetX1 = t;
			return ret;
		}
		else {
			ret.sourceX2 = s;
			ret.targetX2 = t;
			return ret;
		}
		*/
		return ret;
	}

	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(sourceRoot);
		for (Span s : sourceNonTerminals) {
			sb.append(s);
		}
		return sb.toString();
	}

}
