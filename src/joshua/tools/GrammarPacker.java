package joshua.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class GrammarPacker {

	public GrammarPacker(Properties config, List<File> grammar_file_names) {
		// initialize vocabulary
		
		// add features to vocabulary
		
		
	}
	
	public void pack() {
		
	}

	public static void main(String[] args) throws IOException {
		if (args.length >= 3) {
			Properties config = new Properties();
			
			FileInputStream config_stream = new FileInputStream(args[0]);
			config.load(config_stream);
			config_stream.close();
			
			List<File> grammar_files = new ArrayList<File>();
			for (int i = 2; i < args.length; i++) {
				File grammar_file = new File(args[i]);
				if (grammar_file.exists() && grammar_file.isFile()) {
					grammar_files.add(grammar_file);
				} else {
					System.err.println("Trouble finding file: " + args[i]);
					System.exit(0);
				}
			}
			GrammarPacker packer = new GrammarPacker(config, grammar_files);
			packer.pack();
			
		} else {
			System.err.println("Expecting at least two arguments: ");
			System.err.println("\tjoshua.tools.GrammarPacker config_file output_file grammar_file ...");
		}
	}

}
