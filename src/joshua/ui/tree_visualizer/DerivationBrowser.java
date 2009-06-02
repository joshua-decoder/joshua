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

	private static JFrame frame;
	private static JPanel graphPanel;
	private static JPanel chooserPanel;

	private static JList sourceList;
	private static JList targetList;

	private static File sourceFile;
	private static File targetFile;

	private static JFileChooser fileChooser;

	public static void main(String [] argv)
	{
		initializeJComponents();
		drawGraph();
		frame.setVisible(true);
		return;
	}

	private static void initializeJComponents()
	{
		// JFrame init
		frame = new JFrame("Joshua Derivation Tree Browser");
		frame.setSize(640, 480);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setJMenuBar(createJMenuBar());
		frame.setLayout(new BorderLayout());

		// chooserPanel
		chooserPanel = new JPanel();
		chooserPanel.setLayout(new GridLayout());
		frame.getContentPane().add(chooserPanel, BorderLayout.WEST);
		sourceList = new JList(new DefaultListModel());
		sourceList.setFixedCellWidth(200);
		sourceList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		targetList = new JList(new DefaultListModel());
		targetList.setFixedCellWidth(200);
		targetList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		chooserPanel.add(new JScrollPane(sourceList));
		chooserPanel.add(new JScrollPane(targetList));

		SentenceListListener sll = new SentenceListListener(sourceList, targetList);		
		sourceList.getSelectionModel().addListSelectionListener(sll);
		targetList.getSelectionModel().addListSelectionListener(sll);

		// graphPanel
		graphPanel = new JPanel();
		graphPanel.setLayout(new BorderLayout());
		graphPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		frame.getContentPane().add(graphPanel, BorderLayout.CENTER);

		// fileChooser
		fileChooser = new JFileChooser();
		return;
	}

	private static JMenuBar createJMenuBar()
	{
		JMenuBar mb = new JMenuBar();
		JMenu openMenu = new JMenu("Open");
		JMenuItem src = new JMenuItem("Source ...");
		JMenuItem tgt = new JMenuItem("Target ...");
		FileChoiceListener fcl = new FileChoiceListener(src, tgt);
		src.addActionListener(fcl);
		tgt.addActionListener(fcl);
		openMenu.add(src);
		openMenu.add(tgt);
		mb.add(openMenu);
		return mb;
	}

	private static void drawGraph()
	{
		for (Component c : graphPanel.getComponents())
			graphPanel.remove(c);
		String src = (String) sourceList.getSelectedValue();
		String tgt = (String) targetList.getSelectedValue();
		if ((src == null) || (tgt == null)) {
			graphPanel.add(new JLabel("No tree to display."), BorderLayout.CENTER);
			graphPanel.revalidate();
			graphPanel.repaint();
			return;
		}
		DerivationTree tree = new DerivationTree(tgt.split(DerivationTree.DELIMITER)[1], src);
		DerivationViewer dv = new DerivationViewer(tree);
		graphPanel.add(dv, BorderLayout.CENTER);
		graphPanel.revalidate();
		graphPanel.repaint();
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
						model.addElement(line);
				}
				catch (NumberFormatException e) {
					// fall through
				}
			}
		}
		catch (FileNotFoundException e) {

		}
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
			int ret = fileChooser.showOpenDialog(frame);
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
			}
			drawGraph();
			return;
		}
	}
}
