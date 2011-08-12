
package joshua.decoder.segment_file;

public class LatticeInput extends Sentence {

    public LatticeInput(String input, int id) {
        super(input, id);
    }

    // looks_like_lattice    = sentence.sentence().startsWith("(((");

    // public Pattern pattern() {
    //     System.err.println("* WARNING: I don't know how to prune grammars to lattices");
    //     // TODO: suffix array needs to accept lattices!
    //     return null;
    // }

    // public Lattice lattice() {
    //     return Lattice.createFromString(sentence.sentence(), this.symbolTable);
    // }
}
