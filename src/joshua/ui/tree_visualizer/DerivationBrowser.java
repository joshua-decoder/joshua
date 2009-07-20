package joshua.ui.tree_visualizer;

import javax.swing.*;
import javax.swing.event.*;

import java.io.InputStream;
import java.io.FileInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;
import java.util.LinkedList;
import java.util.zip.GZIPInputStream;

import java.awt.*;
import java.awt.event.*;

import edu.uci.ics.jung.algorithms.layout.StaticLayout;

/**
 * A stand-alone application for viewing derivation trees. Given files holding the source-side
 * sentences and n-best output from the Joshua decoder, the user can browse through visualizations
 * showing derivation trees for each sentence.
 * 
 * @author jonny
 *
 */
public class DerivationBrowser {

	/**
	 * A frame that displays the list of source-side sentences and the list of target-side
	 * sentences.
	 */
	private static JFrame chooserFrame;

	/**
	 * A list that will display all the sentences in the current source-side file.
	 */
	private static JList sourceList;
	
	/**
	 * A list that will display all the candidate translations for the currently-selected
	 * source sentence.
	 */
	private static JList targetList;

	/**
	 * A file containing source-side sentences.
	 */
	private static File sourceFile;
	
	/**
	 * A file containing candidate translations for all source sentences, annotated with
	 *  source-side alignments.
	 */
	private static File targetFile;

	/**
	 * Each member of this array is a linked list holding the candidate translations for one
	 * particular sentence.
	 */
	private static LinkedList<String> [] nBestLists;

	/**
	 * A JFileChooser to allow the user to choose the source or target file.
	 */
	private static JFileChooser fileChooser;

	/**
	 * The active frame is the frame that displays a derivation tree for the currently selected
	 * candidate translation.
	 */
	private static ActiveFrame activeFrame;

	
	public static final int DEFAULT_WIDTH = 640;
	public static final int DEFAULT_HEIGHT = 480;

	/**
	 * The main method.
	 * 
	 * @param argv command-line parameters. Can specify the source or target file.
	 */
	public static void main(String [] argv)
	{
		initializeJComponents();
		if (argv.length > 0) {
			sourceFile = new File(argv[0]);
			populateSourceList();
		}
		if (argv.length > 1) {
			targetFile = new File(argv[1]);
			setNBestLists();
			populateTargetList();
		}
		activeFrame.drawGraph();
		chooserFrame.setVisible(true);
		return;
	}

	/**
	 * Initializes the various components in the chooser frame. The two JLists are laid out,
	 * and a menu bar is attached.
	 */
	private static void initializeJComponents()
	{
		// JFrame init
		chooserFrame = new JFrame("Joshua Derivation Tree Browser");
		chooserFrame.setSize(640, 480);
		chooserFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		chooserFrame.setJMenuBar(createJMenuBar());
		chooserFrame.setLayout(new GridLayout(2,1));

		sourceList = new JList(new DefaultListModel());
		sourceList.setFixedCellWidth(200);
		sourceList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		targetList = new JList(new DefaultListModel());
		targetList.setFixedCellWidth(200);
		targetList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		chooserFrame.getContentPane().add(new JScrollPane(sourceList));
		chooserFrame.getContentPane().add(new JScrollPane(targetList));

		SentenceListListener sll = new SentenceListListener(sourceList, targetList);		
		sourceList.getSelectionModel().addListSelectionListener(sll);
		targetList.getSelectionModel().addListSelectionListener(sll);

		// fileChooser
		fileChooser = new JFileChooser();

		activeFrame = new ActiveFrame();
		return;
	}

	/**
	 * Creates the menu bar for the chooser frame. We put this in a seperate method so that
	 * it can be easily found and changed if need be.
	 * 
	 * @return the menu bar for the chooser frame
	 */
	private static JMenuBar createJMenuBar()
	{
		JMenuBar mb = new JMenuBar();
		JMenu openMenu = new JMenu("Control");
		JMenuItem creat = new JMenuItem("New tree viewer window");
		JMenuItem src = new JMenuItem("Open source file ...");
		JMenuItem tgt = new JMenuItem("Open n-best derivations file ...");
		JMenuItem quit = new JMenuItem("Quit");

		FileChoiceListener fcl = new FileChoiceListener(src, tgt);
		src.addActionListener(fcl);
		tgt.addActionListener(fcl);

		creat.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				activeFrame.disableNavigationButtons();
				activeFrame = new ActiveFrame();
				return;
			}
		});
		quit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				System.exit(0);
			}
		});
		openMenu.add(creat);
		openMenu.add(src);
		openMenu.add(tgt);
		openMenu.add(quit);
		mb.add(openMenu);
		return mb;
	}

	/**
	 * Displays the source-side sentences in their list. This method is called whenever the source-
	 * side file is changed.
	 */
	private static void populateSourceList()
	{
		if (sourceFile == null)
			return;
		try {
			DefaultListModel model = (DefaultListModel) sourceList.getModel();
			InputStream inp;
			if (sourceFile.getName().endsWith("gz")) 
				inp = new GZIPInputStream(new FileInputStream(sourceFile));
			else
				inp = new FileInputStream(sourceFile);
			Scanner scanner = new Scanner(inp, "UTF-8");
			model.removeAllElements();
			while (scanner.hasNextLine())
				model.addElement(scanner.nextLine());

		}
		catch (FileNotFoundException e) {

		}
		catch (IOException e) {
		}
	}

	/**
	 * Displays the candidate translations for the current source sentence in their list. This method
	 * is called whenever the source list's selected value is changed.
	 */
	private static void populateTargetList()
	{
		DefaultListModel model = (DefaultListModel) targetList.getModel();
		model.removeAllElements();
		if (sourceList.getSelectedValue() == null) {
			return;
		}
		for (String s : nBestLists[sourceList.getSelectedIndex()]) {
			model.addElement(new Derivation(s));
		}
		return;
	}

	/**
	 * Reads in the currently set target file and organizes it into seperate n-best lists. Each
	 * n-best list corresponds to a different source sentence.
	 */
	private static void setNBestLists()
	{
		if (targetFile == null) {
			System.err.println("setNBestLists: target file null");
			return;
		}

		nBestLists = new LinkedList[sourceList.getModel().getSize()];
		for (int i = 0; i < nBestLists.length; i++)
			nBestLists[i] = new LinkedList<String>();

		try {
			InputStream inp;
			if (targetFile.getName().endsWith("gz")) 
				inp = new GZIPInputStream(new FileInputStream(targetFile));
			else
				inp = new FileInputStream(targetFile);
			Scanner scanner = new Scanner(inp, "UTF-8");
			int src;
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				String [] tokens = line.split(DerivationTree.DELIMITER);
				try {
					src = Integer.parseInt(tokens[0].trim());
				//	System.err.println("Derivation for source sentence " + src);
					nBestLists[src].add(line);
				}
				catch (NumberFormatException e) {
					System.err.println("Caught NumberFormatException in setNBestLists");
					// fall through
				}
			}
		}
		catch (FileNotFoundException e) {

		}
		catch (IOException e) {
		}
		return;
	}

	/**
	 * ActiveFrame is a Frame that's specially designed to display a derivation tree. An important
	 * property of an ActiveFrame is that it can be "fixed", meaning that its displayed derivation
	 * tree can no longer be changed.
	 * 
	 * The DerivationBrowser will have at most one un-fixed frame at any one time. Other, fixed
	 * frames can be maintained so that derivation trees can be compared.
	 * 
	 * @author Jonathan Weese
	 *
	 */
	public static class ActiveFrame extends JFrame {
		/**
		 * The serial version UID. I don't know what it's for, but eclipse is convinced it's
		 * necessary.
		 */
		private static final long serialVersionUID = 6482495170692955314L;
		
		/**
		 * A button that should be pressed to view the next candidate translation in the list.
		 */
		JButton nextDerivation;
		/**
		 * A button that should be pressed to view the previous candidate translation in the list.
		 */
		JButton previousDerivation;
		/**
		 * A button to move to the next source-side sentence in the file.
		 */
		JButton nextSource;
		/**
		 * A button to move to the previous source-side sentence in the file.
		 */
		JButton previousSource;

		/**
		 * A panel that holds the buttons, as well as labels to show which derivation is currently
		 * being displayed.
		 */
		private JPanel controlPanel;
		/**
		 * A panel used to display the derivation tree itself.
		 */
		private JPanel viewPanel;

		/**
		 * This label displays the text of the source sentence.
		 */
		private JLabel source;
		/**
		 * This label displays the text of the candidate translation.
		 */
		private JLabel derivation;

		/**
		 * This component displays the derivation tree's JUNG graph.
		 */
		private DerivationViewer dv;

		/**
		 * The default constructor.
		 */
		public ActiveFrame()
		{
			super("Joshua Derivation Tree");
			setLayout(new BorderLayout());
			setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
			controlPanel = new JPanel(new BorderLayout());

			source = new JLabel("source: none");
			derivation = new JLabel("derivation: none");

			initializeButtons();
			layoutControl();

			viewPanel = new JPanel(new BorderLayout());
			dv = null;

			getContentPane().add(viewPanel, BorderLayout.CENTER);
			getContentPane().add(controlPanel, BorderLayout.SOUTH);
			drawGraph();
			setVisible(true);
		}

		/**
		 * Lays out the control buttons of this frame.
		 */
		private void layoutControl()
		{
			JPanel ctlLeft = new JPanel(new GridLayout(2, 1));
			JPanel ctlCenter = new JPanel(new GridLayout(2, 1));
			JPanel ctlRight = new JPanel(new GridLayout(2, 1));

			controlPanel.add(ctlLeft, BorderLayout.WEST);
			controlPanel.add(ctlCenter, BorderLayout.CENTER);
			controlPanel.add(ctlRight, BorderLayout.EAST);

			ctlLeft.add(previousDerivation);
			ctlLeft.add(previousSource);
			ctlCenter.add(derivation);
			ctlCenter.add(source);
			ctlRight.add(nextDerivation);
			ctlRight.add(nextSource);
			return;
		}

		/**
		 * Initializes the control buttons of this frame.
		 */
		private void initializeButtons()
		{

			nextDerivation = new JButton(">");
			previousDerivation = new JButton("<");
			nextSource = new JButton(">");
			previousSource = new JButton("<");

			nextDerivation.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e)
				{
					int x = targetList.getSelectedIndex();
					targetList.setSelectedIndex(x + 1);
					return;
				}
			});
			previousDerivation.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e)
				{
					int x = targetList.getSelectedIndex();
					targetList.setSelectedIndex(x - 1);
					return;
				}
			});
			nextSource.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e)
				{
					int x = sourceList.getSelectedIndex();
					sourceList.setSelectedIndex(x + 1);
					return;
				}
			});
			previousSource.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e)
				{
					int x = sourceList.getSelectedIndex();
					sourceList.setSelectedIndex(x - 1);
					return;
				}
			});
			return;
		}

		/**
		 * Displays the derivation tree for the current candidate translation. The current candidate
		 * translation is whichever translation is currently highlighted in the Derivation Browser's
		 * chooser frame.
		 */
		public void drawGraph()
		{
			viewPanel.removeAll();
			String src = (String) sourceList.getSelectedValue();
			Derivation tgtDer = (Derivation) targetList.getSelectedValue();

			if ((src == null) || (tgtDer == null)) {
				return;
			}
			source.setText(src);
			derivation.setText(tgtDer.toString());
			String tgt = tgtDer.complete();
			DerivationTree tree = new DerivationTree(tgt.split(DerivationTree.DELIMITER)[1], src);
			if (dv == null) {
				dv = new DerivationViewer(tree, viewPanel.getSize(), Color.red, DerivationViewer.AnchorType.ANCHOR_LEFTMOST_LEAF);
			}
			else {
				dv.setGraphLayout(new StaticLayout<Node,DerivationTreeEdge>(tree, new DerivationTreeTransformer(tree, dv.getSize(), true)));
				tree.addCorrespondences();
			}
			viewPanel.add(dv, BorderLayout.CENTER);
			dv.revalidate();
			repaint();
			getContentPane().repaint();
			return;
		}

		/**
		 * Makes this frame unmodifiable, so that the tree it displays cannot be changed. In fact,
		 * all that happens is the title is update and the navigation buttons are disabled. This
		 * method is intended to prevent the user from modifying the frame, not to prevent other code
		 * from modifying it.
		 */
		public void disableNavigationButtons()
		{
			setTitle(getTitle() + " (fixed)");
			nextDerivation.setEnabled(false);
			previousDerivation.setEnabled(false);
			nextSource.setEnabled(false);
			previousSource.setEnabled(false);
			return;
		}

	}

	/**
	 * An action listener that updates the source and target sentence lists when a new file is chosen
	 * in the main frame of the application.
	 * 
	 * @author Jonathan Weese
	 *
	 */
	public static class FileChoiceListener implements ActionListener {
		/**
		 * The button in the main frame's "Control" menu that's labelled "Open source file".
		 */
		private JMenuItem source;

		/**
		 * The default constructor.
		 * 
		 * @param s the button in the main frame's "Control" menu for opening a source file.
		 * @param t the button in the main frame's "Control" menu for opening a target file.
		 */
		public FileChoiceListener(JMenuItem s, JMenuItem t)
		{
			super();
			source = s;
		}

		/**
		 * 
		 */
		public void actionPerformed(ActionEvent e)
		{
			int ret = fileChooser.showOpenDialog(chooserFrame);
			if (ret == JFileChooser.APPROVE_OPTION) {
				File chosen = fileChooser.getSelectedFile();
				JMenuItem origin = (JMenuItem) e.getSource();
				if (origin.equals(source)) {
					sourceFile = chosen;
					populateSourceList();
				}
				else {
					targetFile = chosen;
					setNBestLists();
					populateTargetList();
				}
			}
			return;
		}
	}

	public static class SentenceListListener implements ListSelectionListener {
		private JList source;

		public SentenceListListener(JList s, JList t)
		{
			source = s;
		}

		public void valueChanged(ListSelectionEvent e)
		{
			if (e.getSource().equals(source.getSelectionModel())) {
				populateTargetList();
				targetList.setSelectedIndex(0);
			}
			int src = sourceList.getSelectedIndex();
			int tgt = targetList.getSelectedIndex();
			if (src < 1)
				activeFrame.previousSource.setEnabled(false);
			else
				activeFrame.previousSource.setEnabled(true);
			if (src == sourceList.getModel().getSize() - 1)
				activeFrame.nextSource.setEnabled(false);
			else
				activeFrame.nextSource.setEnabled(true);
			if (tgt < 1)
				activeFrame.previousDerivation.setEnabled(false);
			else
				activeFrame.previousDerivation.setEnabled(true);
			if (tgt == targetList.getModel().getSize() - 1)
				activeFrame.nextDerivation.setEnabled(false);
			else
				activeFrame.nextDerivation.setEnabled(true);
			activeFrame.drawGraph();
			return;
		}
	}

	/**
	 * This class represents a candidate translation. We want a distinct class for this because we
	 * are keeping the candidate translations in a JList, and the JList by default displays items
	 * as the results of their toString() method. Hence we want a toString method that will extract
	 * the terminal symbols from the annotated derivation.
	 * 
	 * @author Jonathan Weese
	 *
	 */
	public static class Derivation {
		/**
		 * The raw output from the joshua decoder.
		 */
		private String complete;
		/**
		 * The terminal symbols of the derivation; a "human-readable" translation.
		 */
		private String terminals;

		/**
		 * This constructor takes the raw annotated output from the joshua decoder.
		 * 
		 * @param c a candidate translation from the joshua decoder.
		 */
		public Derivation(String c)
		{
			complete = c;
			terminals = extractTerminals(c);
		}

		/**
		 * Gets the raw derivation from the joshua decoder.
		 * 
		 * @return the annotated derivation-tree representation of this candidate translation.
		 */
		public String complete()
		{
			return complete;
		}

		public String toString()
		{
			return terminals;
		}

		/**
		 * Extracts only the terminal symbols of a derivation-tree representation, in order.
		 * 
		 * @param s a derivation-tree representation of a candidate translation
		 * @return the terminal symbols of the representation.
		 */
		private static String extractTerminals(String s)
		{
			String tree = s.split(DerivationTree.DELIMITER)[1];
			String [] tokens = tree.replaceAll("\\)", "\n)").split("\\s+");
			String result = "";
			for (String t : tokens) {
				if (t.startsWith("(") || t.equals(")"))
					continue;
				result += " " + t;
			}
			return result;
		}
	}
}
