package packed;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.IntBuffer;
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

	private int[] source;
	private int[] target;
	private MappedByteBuffer features;
	private MappedByteBuffer alignments;

	private int[] featureLookup;
	private int[] alignmentLookup;
	
	private boolean have_alignments;

	public PrintRules(String dir) throws IOException {
		File source_file = new File(dir + "/slice_00000.source");
		File target_file = new File(dir + "/slice_00000.target");
		File feature_file = new File(dir + "/slice_00000.features");
		File alignment_file = new File(dir + "/slice_00000.alignments");

		have_alignments = alignment_file.exists();
		
		// Read the vocabulary.
		Vocabulary.read(dir + "/vocabulary");

		// Read the quantizer setup.
		quantization = new QuantizerConfiguration();
		quantization.read(dir + "/quantization");

		// Get the channels etc.
		FileChannel source_channel = new FileInputStream(source_file).getChannel();
		int source_size = (int) source_channel.size();
		IntBuffer source_buffer = source_channel.map(MapMode.READ_ONLY, 0,
				source_size).asIntBuffer();
		source = new int[source_size / 4];
		source_buffer.get(source);
		
		FileChannel target_channel = new FileInputStream(target_file).getChannel();
		int target_size = (int) target_channel.size();
		IntBuffer target_buffer = target_channel.map(MapMode.READ_ONLY, 0, 
				target_size).asIntBuffer();
		target = new int[target_size / 4];
		target_buffer.get(target);
		
		FileChannel feature_channel = new FileInputStream(feature_file).getChannel();
		int feature_size = (int) feature_channel.size();
		features = feature_channel.map(MapMode.READ_ONLY, 0, feature_size);
		
		if (have_alignments) {
			FileChannel alignment_channel = new FileInputStream(alignment_file).getChannel();
			int alignment_size = (int) alignment_channel.size();
			alignments = alignment_channel.map(MapMode.READ_ONLY, 0, alignment_size);
		}
		
		int num_feature_blocks = features.getInt();
		featureLookup = new int[num_feature_blocks];
		// Read away data size.
		features.getInt();
		for (int i = 0; i < num_feature_blocks; i++)
			featureLookup[i] = features.getInt();
		
		int num_alignment_blocks = alignments.getInt(); 
		alignmentLookup = new int[num_alignment_blocks];
		// Read away data size.
		alignments.getInt();
		for (int i = 0; i < num_alignment_blocks; i++)
			alignmentLookup[i] = alignments.getInt();
		
		if (num_alignment_blocks != num_feature_blocks)
			throw new RuntimeException("Number of blocks doesn't match up.");
	}

	public void traverse() {
		traverse(0, "");
	}

	private void traverse(int position, String src_side) {
		int num_children = source[position];
		int[] addresses = new int[num_children];
		int[] symbols = new int[num_children];
		int j = position + 1;
		for (int i = 0; i < num_children; i++) {
			symbols[i] = source[j++];
			addresses[i] = source[j++];
		}
		int num_rules = source[j++];
		for (int i = 0; i < num_rules; i++) {
			int lhs = source[j++];
			int tgt_address = source[j++];
			int data_address = source[j++];
			printRule(src_side, lhs, tgt_address, data_address);
		}
		for (int i = 0; i < num_children; i++) {
			traverse(addresses[i], src_side + " " + Vocabulary.word(symbols[i]));
		}
	}

	private String getTarget(int pointer) {
		StringBuilder sb = new StringBuilder();
		do {
			pointer = target[pointer];
			if (pointer != -1) {
				int symbol = target[pointer + 1];
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

		int data_position = featureLookup[block_id];
		int num_features = features.getInt(data_position);
		data_position += 4;
		for (int i = 0; i < num_features; i++) {
			int feature_id = features.getInt(data_position);
			Quantizer quantizer = quantization.get(feature_id);
			sb.append(" " + Vocabulary.word(feature_id) + "=" +
					quantizer.read(features, data_position));
			data_position += 4 + quantizer.size();
		}
		return sb.toString();
	}

	private String getAlignments(int block_id) {
		StringBuilder sb = new StringBuilder();

		int data_position = alignmentLookup[block_id];
		byte num_points = alignments.get(data_position);
		for (int i = 0; i < num_points; i++) {
			byte src = alignments.get(data_position + 1 + 2 * i);
			byte tgt = alignments.get(data_position + 2 + 2 * i);
			
			sb.append(" " + src + "-" + tgt);
		}
		return sb.toString();
	}
	
	private void printRule(String src_side, int lhs, int tgt_address,
			int data_address) {
		System.out.println(Vocabulary.word(lhs) + " |||" +
				src_side + " |||" +
				getTarget(tgt_address) + " |||" +
				getFeatures(data_address) + 
				(have_alignments ? " |||" + getAlignments(data_address) : ""));
	}

	public static void main(String args[]) throws IOException {
		PrintRules pr = new PrintRules(args[0]);
		pr.traverse();
	}
}
