package joshua.ui.hypergraph_visualizer;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Scanner;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JScrollPane;

import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.JoshuaDecoder;
import joshua.decoder.hypergraph.HyperGraph;

public class Browser {
	private static ArrayList<String> sourceSentences;
	private static ArrayList<String> referenceTranslations;
	
	static JList sentenceList;
	static String sourceFile;
	static JoshuaDecoder decoder;
	
	public static final String USAGE = String.format("USAGE: %s <config> <source> <reference>", Browser.class.getName());

	public static final int DEFAULT_HEIGHT = 480;
	public static final int DEFAULT_WIDTH = 640;
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length < 3) {
			System.err.println(USAGE);
			System.exit(1);
		}
		decoder = new JoshuaDecoder(args[0]);
		JFrame mainFrame = new JFrame("Joshua Decoder / Hypergraph Visualizer");
		mainFrame.setLayout(new BorderLayout());
		sentenceList = new JList(new DefaultListModel());
		mainFrame.getContentPane().add(new JScrollPane(sentenceList), BorderLayout.CENTER);
		JButton decodeButton = new JButton("Decode");
		mainFrame.getContentPane().add(decodeButton, BorderLayout.SOUTH);
		try {
			sourceFile = args[1];
			sourceSentences = populateListFromFile(args[1]);
			referenceTranslations = populateListFromFile(args[2]);
		}
		catch (IOException e) {
			System.err.println(e.getMessage());
			System.exit(1);
		}
		DefaultListModel sentenceListModel = (DefaultListModel) sentenceList.getModel();
		for (String ref : referenceTranslations) {
			sentenceListModel.addElement(ref);
		}
		mainFrame.setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
		mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		mainFrame.setVisible(true);
		
		decodeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				for (int i : sentenceList.getSelectedIndices()) {
					decoder.visualizeHyperGraphForSentence(sourceSentences.get(i));
				}
			}
		});
		return;
	}

	private static ArrayList<String> populateListFromFile(String filename) throws IOException
	{
		ArrayList<String> retValue = new ArrayList<String>();
		Scanner fileScanner = new Scanner(new File(filename));
		while (fileScanner.hasNextLine()) {
			retValue.add(fileScanner.nextLine());
		}
		return retValue;
	}
}
