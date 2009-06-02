package joshua.ui.tree_visualizer;

import javax.swing.*;
import javax.swing.event.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.LinkedList;

import java.awt.*;
import java.awt.event.*;

public class DerivationBrowser {

	private static JFrame chooserFrame;

	private static JList sourceList;
	private static JList targetList;

	private static File sourceFile;
	private static File targetFile;

	private static JFileChooser fileChooser;

	private static JFrame activeFrame;

	public static final int DEFAULT_WIDTH = 640;
	public static final int DEFAULT_HEIGHT = 480;

	public static void main(String [] argv)
	{
		initializeJComponents();
		drawGraph();
		chooserFrame.setVisible(true);
		return;
	}

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

		activeFrame = createActiveFrame();
		return;
	}

	private static JMenuBar createJMenuBar()
	{
		JMenuBar mb = new JMenuBar();
		JMenu openMenu = new JMenu("Control");
		JMenuItem creat = new JMenuItem("New tree viewer window");
		JMenuItem src = new JMenuItem("Open source file ...");
		JMenuItem tgt = new JMenuItem("Open n-best derivations file ...");
		FileChoiceListener fcl = new FileChoiceListener(src, tgt);
		src.addActionListener(fcl);
		tgt.addActionListener(fcl);

		creat.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				activeFrame = createActiveFrame();
				return;
			}
		});
		openMenu.add(creat);
		openMenu.add(src);
		openMenu.add(tgt);
		mb.add(openMenu);
		return mb;
	}

	private static void drawGraph()
	{
		for (Component c : activeFrame.getContentPane().getComponents())
			activeFrame.getContentPane().remove(c);
		String src = (String) sourceList.getSelectedValue();
		Derivation tgtDer = (Derivation) targetList.getSelectedValue();

		if ((src == null) || (tgtDer == null)) {
			JLabel lbl = new JLabel("No tree to display.");
			activeFrame.getContentPane().add(lbl);
			lbl.revalidate();
			activeFrame.getContentPane().repaint();
			return;
		}
		String tgt = tgtDer.complete();
		DerivationTree tree = new DerivationTree(tgt.split(DerivationTree.DELIMITER)[1], src);
		DerivationViewer dv = new DerivationViewer(tree);
		activeFrame.getContentPane().add(dv);
		dv.revalidate();
		activeFrame.repaint();
		activeFrame.getContentPane().repaint();
		return;
	}

	private static void populateSourceList()
	{
		if (sourceFile == null)
			return;
		try {
			DefaultListModel model = (DefaultListModel) sourceList.getModel();
			Scanner scanner = new Scanner(sourceFile, "UTF-8");
			model.removeAllElements();
			while (scanner.hasNextLine())
				model.addElement(scanner.nextLine());

		}
		catch (FileNotFoundException e) {

		}
	}

	private static void populateTargetList()
	{
		if (targetFile == null)
			return;

		DefaultListModel model = (DefaultListModel) targetList.getModel();
		model.removeAllElements();
		if (sourceList.getSelectedValue() == null) {
			return;
		}
		try {
			int selectedIndex = sourceList.getSelectedIndex();
			Scanner scanner = new Scanner(targetFile, "UTF-8");
			int src;
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				String [] tokens = line.split(DerivationTree.DELIMITER);
				try {
					src = Integer.parseInt(tokens[0].trim());
					if (src > selectedIndex)
						return;
					if (src == selectedIndex)
						model.addElement(new Derivation(line));
				}
				catch (NumberFormatException e) {
					// fall through
				}
			}
		}
		catch (FileNotFoundException e) {

		}
	}

	private static JFrame createActiveFrame()
	{
		JFrame ret = new JFrame("Joshua Derivation Tree");
		ret.setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
		ret.setVisible(true);
		return ret;
	}

	public static class FileChoiceListener implements ActionListener {
		private JMenuItem source;
		private JMenuItem target;

		public FileChoiceListener(JMenuItem s, JMenuItem t)
		{
			super();
			source = s;
			target = t;
		}

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
					populateTargetList();
				}
			}
			return;
		}
	}

	public static class SentenceListListener implements ListSelectionListener {
		private JList source;
		private JList target;

		public SentenceListListener(JList s, JList t)
		{
			source = s;
			target = t;
		}

		public void valueChanged(ListSelectionEvent e)
		{
			if (e.getSource().equals(source.getSelectionModel())) {
				populateTargetList();
				targetList.setSelectedIndex(0);
			}
			drawGraph();
			return;
		}
	}

	public static class Derivation {
		private String complete;
		private String terminals;

		public Derivation(String c)
		{
			complete = c;
			terminals = extractTerminals(c);
		}

		public String complete()
		{
			return complete;
		}

		public String toString()
		{
			return terminals;
		}

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
