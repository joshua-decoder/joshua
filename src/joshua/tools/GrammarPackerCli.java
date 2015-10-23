package joshua.tools;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.StringArrayOptionHandler;

public class GrammarPackerCli {
  
  private static final Logger log = Logger.getLogger(GrammarPackerCli.class.getName());

  // Input grammars to be packed (with a joint vocabulary)
  @Option(name = "--grammars", aliases = {"-g", "-i"}, handler = StringArrayOptionHandler.class, required = true, usage = "list of grammars to pack (jointly, i.e. they share the same vocabulary)")
  private List<String> grammars = new ArrayList<>();
  
  // Output grammars
  @Option(name = "--outputs", aliases = {"-p", "-o"}, handler = StringArrayOptionHandler.class, required = true, usage = "output directories of packed grammars.")
  private List<String> outputs = new ArrayList<>();
  
  // Output grammars
  @Option(name = "--alignments", aliases = {"-a", "--fa"}, handler = StringArrayOptionHandler.class, required = false, usage = "alignment files")
  private List<String> alignments_filenames = new ArrayList<>();
  
  // Config filename
  @Option(name = "--config_file", aliases = {"-c"}, required = false, usage = "(optional) packing configuration file")
  private String config_filename;
  
  @Option(name = "--dump_files", aliases = {"-d"}, handler = StringArrayOptionHandler.class, usage = "(optional) dump feature stats to file")
  private List<String> featuredump_filenames = new ArrayList<>();
  
  @Option(name = "--ga", usage = "whether alignments are present in the grammar")
  private boolean grammar_alignments = false;
  
  @Option(name = "--slice_size", aliases = {"-s"}, required = false, usage = "approximate slice size in # of rules (default=1000000)")
  private int slice_size = 1000000;
  
  
  private void run() throws IOException {

    final List<String> missingFilenames = new ArrayList<>(grammars.size());
    for (final String g : grammars) {
      if (!new File(g).exists()) {
        missingFilenames.add(g);
      }
    }
    if (!missingFilenames.isEmpty()) {
      throw new IOException("Input grammar files not found: " + missingFilenames.toString());
    }
    
    if (config_filename != null && !new File(config_filename).exists()) {
      throw new IOException("Config file not found: " + config_filename);
    }

    if (!outputs.isEmpty()) {
      if (outputs.size() != grammars.size()) {
        throw new IOException("Must provide an output directory for each grammar");
      }
      final List<String> existingOutputs = new ArrayList<>(outputs.size());
      for (final String o : outputs) {
        if (new File(o).exists()) {
          existingOutputs.add(o);
        }
      }
      if (!existingOutputs.isEmpty()) {
        throw new IOException("These output directories already exist (will not overwrite): " + existingOutputs.toString());
      }
    }
    if (outputs.isEmpty()) {
      for (final String g : grammars) {
        outputs.add(g + ".packed");
      }
    }
    
    if (!alignments_filenames.isEmpty()) {
      final List<String> missingAlignmentFiles = new ArrayList<>(alignments_filenames.size());
      for (final String a : alignments_filenames) {
        if (!new File(a).exists()) {
          missingAlignmentFiles.add(a);
        }
      }
      if (!missingAlignmentFiles.isEmpty()) {
        throw new IOException("Alignment files not found: " + missingAlignmentFiles.toString());
      }
    }

    // create Packer instances for each grammar
    final List<GrammarPacker> packers = new ArrayList<>(grammars.size());
    for (int i = 0; i < grammars.size(); i++) {
      log.info("Starting GrammarPacker for " + grammars.get(i));
      final String alignment_filename = alignments_filenames.isEmpty() ? null : alignments_filenames.get(i);
      final String featuredump_filename = featuredump_filenames.isEmpty() ? null : featuredump_filenames.get(i);
      final GrammarPacker packer = new GrammarPacker(
          grammars.get(i),
          config_filename,
          outputs.get(i),
          alignment_filename,
          featuredump_filename,
          grammar_alignments,
          slice_size);
      packers.add(packer);
    }
    
    // run all packers in sequence, accumulating vocabulary items
    for (final GrammarPacker packer : packers) {
      log.info("Starting GrammarPacker for " + packer.getGrammar());
      packer.pack();
      log.info("PackedGrammar located at " + packer.getOutputDirectory());
    }
    
    // for each packed grammar, overwrite the internally serialized vocabulary with the current global one.
    for (final GrammarPacker packer : packers) {
      log.info("Writing final common Vocabulary to " + packer.getOutputDirectory());
      packer.writeVocabulary();
    }
  }

  public static void main(String[] args) throws IOException {
    final GrammarPackerCli cli = new GrammarPackerCli();
    final CmdLineParser parser = new CmdLineParser(cli);

    try {
      parser.parseArgument(args);
      cli.run();
    } catch (CmdLineException e) {
      log.info(e.toString());
      parser.printUsage(System.err);
      System.exit(1);
    }
  }

}
