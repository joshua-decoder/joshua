package joshua.decoder.ff.tm.packed;

import java.util.Collection;

import joshua.decoder.ff.tm.RuleCollection;
import joshua.decoder.ff.tm.Trie;

public class PackedTrie implements Trie {
	
	private PackedTrieData data;
	private long position;

	public PackedTrie(PackedTrieData data, long position) {
		this.data = data;
		this.position = position;
	}
	
	public Trie match(int token_id) {
		int matched = 0;
		return null;
	}

	public boolean hasExtensions() {
		// TODO Auto-generated method stub
		return false;
	}

	public Collection<? extends Trie> getExtensions() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean hasRules() {
		// TODO Auto-generated method stub
		return false;
	}

	public RuleCollection getRules() {
		// TODO Auto-generated method stub
		return null;
	}

}
