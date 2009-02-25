package joshua.corpus;

public abstract class AbstractSymbolTable implements SymbolTable {

    final public int[] addTerminals(String[] strings){
		int[] res =new int[strings.length];
		for(int t=0; t<strings.length; t++)
			res[t]=addTerminal(strings[t]);
		return res;
	}	
    
	final public int getTargetNonterminalIndex(int id) {
		if (! isNonterminal(id)) {
			return -1;
		} else {
			// TODO: get rid of this expensive interim object
			String symbol = getWord(id);
			
			return getEngNonTerminalIndex(symbol);
		}
	}
	
	final protected int getEngNonTerminalIndex(String wrd) {
		// Assumes the last character is a digit
		// and extracts it, starting from one.
		// Assumes the whole prefix is the
		// nonterminal-ID portion of the string
		return Integer.parseInt( wrd.substring(wrd.length() - 2,	wrd.length() - 1) ) - 1;
	}

}
