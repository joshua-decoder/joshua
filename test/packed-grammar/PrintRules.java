import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;

import joshua.corpus.Vocabulary;
import joshua.util.quantization.Quantizer;
import joshua.util.quantization.QuantizerConfiguration;

/**
 * This program reads a packed representation and prints out some basic
 * information about it.
 * 
 * Usage: java PrintRules PACKED_GRAMMAR_DIR
 */

public class PrintRules {

	private QuantizerConfiguration quantization;

	private MappedByteBuffer source;
	private MappedByteBuffer target;
	private MappedByteBuffer data;

	private int[] lookup;

	public PrintRules(String dir) throws IOException {
		File source_file = new File(dir + "/chunk_00000.source");
		File target_file = new File(dir + "/chunk_00000.target");
		File data_file = new File(dir + "/chunk_00000.data");

		// Read the vocabulary.
		Vocabulary.read(dir + "/vocabulary");

		// Read the quantizer setup.
		quantization = new QuantizerConfiguration();
		quantization.read(dir + "/quantization");

		// Get the channels etc.
		FileChannel source_channel = new FileInputStream(source_file).getChannel();
		int source_size = (int) source_channel.size();
		FileChannel target_channel = new FileInputStream(target_file).getChannel();
		int target_size = (int) target_channel.size();
		FileChannel data_channel = new FileInputStream(data_file).getChannel();
		int data_size = (int) data_channel.size();

		source = source_channel.map(MapMode.READ_ONLY, 0,
				source_size);
		target = target_channel.map(MapMode.READ_ONLY, 0,
				target_size);
		data = data_channel.map(MapMode.READ_ONLY, 0, data_size);

		int num_blocks = data.getInt();
		lookup = new int[num_blocks];
		// Read away data size.
		data.getInt();
		for (int i = 0; i < num_blocks; i++)
			lookup[i] = data.getInt();
	}

	public void traverse() {
		traverse(0, "");
	}

	private void traverse(int position, String src_side) {
		source.position(position);
		int num_children = source.getInt();
		int[] addresses = new int[num_children];
		int[] symbols = new int[num_children];
		for (int i = 0; i < num_children; i++) {
			symbols[i] = source.getInt();
			addresses[i] = source.getInt();
		}
		int num_rules = source.getInt();
		for (int i = 0; i < num_rules; i++) {
			int lhs = source.getInt();
			int tgt_address = source.getInt();
			int data_address = source.getInt();
			printRule(src_side, lhs, tgt_address, data_address);
		}
		for (int i = 0; i < num_children; i++) {
			traverse(addresses[i], src_side + " " + Vocabulary.word(symbols[i]));
		}
	}

	private String getTarget(int pointer) {
		StringBuilder sb = new StringBuilder();
		do {
			target.position(pointer);
			pointer = target.getInt();
			if (pointer != -1) {
				int symbol = target.getInt();
				if (symbol < 0)
					sb.append(" ").append("NT" + symbol);
				else
					sb.append(" ").append(Vocabulary.word(symbol));
			}
		} while (pointer != -1);
		return sb.toString();
	}

	private String getFeatures(int block_id) {
		StringBuilder sb = new StringBuilder();

		int data_position = lookup[block_id];
		int num_features = data.getInt(data_position);
		data_position += 4;
		for (int i = 0; i < num_features; i++) {
			int feature_id = data.getInt(data_position);
			Quantizer quantizer = quantization.get(feature_id);
			sb.append(" " + Vocabulary.word(feature_id) + "=" +
					quantizer.read(data, data_position));
			data_position += 4 + quantizer.size();
		}
		return sb.toString();
	}

	private void printRule(String src_side, int lhs, int tgt_address,
			int data_address) {
		System.out.println(Vocabulary.word(lhs) + " |||" +
				src_side + " |||" +
				getTarget(tgt_address) + " |||" +
				getFeatures(data_address));
	}

	public static void main(String args[]) throws IOException {
		PrintRules pr = new PrintRules(args[0]);
		pr.traverse();
	}
}
