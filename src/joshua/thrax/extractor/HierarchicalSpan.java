package joshua.thrax.extractor;

import joshua.corpus.Span;

public class HierarchicalSpan {

	// TODO:
	// factor this away from here and hiero extractor
	// should only be set once.
	public static final int ARITY_LIMIT = 2;
	public static final int MAX_NT_COVERAGE = 1;
	public static final int MAX_RHS_LENGTH = 7;

	Span sourceRoot;
	Span targetRoot;


	Span sourceNonTerminals[];
	Span targetNonTerminals[];

	public int arity;
	public int sourceRhsSize;
	public int targetRhsSize;

	public int [] sourceRhs;
	public int [] targetRhs;


	public HierarchicalSpan(Span s, Span t)
	{
		this.sourceRoot = s;
		this.targetRoot = t;
		this.sourceNonTerminals = new Span[ARITY_LIMIT];
		this.targetNonTerminals = new Span[ARITY_LIMIT];

		this.sourceRhsSize = s.size();
		this.targetRhsSize = t.size();

		this.sourceRhs = new int[sourceRhsSize];
		this.targetRhs = new int[targetRhsSize];

		for (int i = 0; i < sourceRhs.length; i++) {
			this.sourceRhs[i] = 0;
		}
		for (int i = 0; i < targetRhs.length; i++) {
			this.targetRhs[i] = 0;
		}

		this.arity = 0;
	}

	private HierarchicalSpan(HierarchicalSpan hs)
	{
		this.sourceRoot = hs.sourceRoot;
		this.targetRoot = hs.targetRoot;
		this.sourceNonTerminals = new Span[ARITY_LIMIT];
		this.targetNonTerminals = new Span[ARITY_LIMIT];

		this.sourceRhsSize = hs.sourceRhsSize;
		this.targetRhsSize = hs.targetRhsSize;
		this.sourceRhs = new int[hs.sourceRhs.length];
		this.targetRhs = new int[hs.targetRhs.length];

		System.arraycopy(hs.sourceNonTerminals, 0,
		                 this.sourceNonTerminals, 0, ARITY_LIMIT);
		System.arraycopy(hs.targetNonTerminals, 0,
		                 this.targetNonTerminals, 0, ARITY_LIMIT);
		System.arraycopy(hs.sourceRhs, 0, sourceRhs, 0, hs.sourceRhs.length);
		System.arraycopy(hs.targetRhs, 0, targetRhs, 0, hs.targetRhs.length);
		this.arity = hs.arity;

	}

	public boolean consistentWith(Span s, Span t)
	{
	//	System.err.println("curr:" + this);
	//	System.err.println("s:" + s);
	//	System.err.println("t:" + t);
		if (arity >= ARITY_LIMIT) {
	//		System.err.println("arity");
			return false;
		}
		if (sourceRhsSize - s.size() - arity < MAX_NT_COVERAGE ||
		    targetRhsSize - t.size() - arity < MAX_NT_COVERAGE) {
	//		System.err.println("NT coverage");
			return false;
		}
		if (!s.strictlyContainedIn(sourceRoot)) {
	//		System.err.println("s not contained");
			return false;
		}
		if (!t.strictlyContainedIn(targetRoot)) {
	//		System.err.println("t not contained");
			return false;
		}
		int i = 0;
		while (sourceNonTerminals[i] != null) {
			if (!(s.disjointFrom(sourceNonTerminals[i]) &&
			      t.disjointFrom(targetNonTerminals[i]))) {
	//			System.err.println("not disjoint from " + i);
				return false;
			}
			i++;
		}
	//	System.err.println("OK");
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
		for (int i : s)
			ret.sourceRhs[i - ret.sourceRoot.start] = -ret.arity;
		for (int i : t)
			ret.targetRhs[i - ret.targetRoot.start] = -ret.arity;
		return ret;
	}

	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(sourceRoot);
		for (Span s : sourceNonTerminals) {
			sb.append(s);
		}
		sb.append("//");
		sb.append(targetRoot);
		for (Span t : targetNonTerminals) {
			sb.append(t);
		}
		return sb.toString();
	}

	public String ntTemplate()
	{
		StringBuilder sb = new StringBuilder();
		for (int i : sourceRhs)
			sb.append(i + " ");
		sb.append("// ");
		for (int i : targetRhs)
			sb.append(i + " ");
		return sb.toString();
	}

}
