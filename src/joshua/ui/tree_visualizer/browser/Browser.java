/* This file is part of the Joshua Machine Translation System.
 *
 * Joshua is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free
 * Software Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 * MA 02111-1307 USA
 */
package joshua.ui.tree_visualizer.browser;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

public class Browser {
	
	/**
	 * Usage message.
	 */
	private static final String USAGE = "USAGE: Browser <source> <reference> <n-best>";
	
	/**
	 * The index of currentSourceSentence in the translation information.
	 */
	private static int currentSourceIndex;
	
	/**
	 * The index of currentCandidateTranslation in the source sentence's translation information.
	 */
	private static int currentCandidateIndex;
	
	/**
	 * Holds the translation information contained in the source, reference, and n-best files.
	 */
	private static TranslationInfoList translations;
	
	/**
	 * A list that contains the reference translation of each source sentences.
	 */
	private static JList referenceList;
	/**
	 * A list that contains the one best translation of each source sentence.
	 */
	private static JList oneBestList;
	
	/**
	 * The current frame that displays a derivation tree.
	 */
	static DerivationTreeFrame activeFrame;
	/**
	 * Default width of the chooser frame.
	 */
	private static final int DEFAULT_WIDTH = 640;
	
	/**
	 * Default height of the chooser frame.
	 */
	private static final int DEFAULT_HEIGHT = 480;

	/**
	 * @param args the paths to the source, reference, and n-best files
	 */
	public static void main(String[] args) {
		if (args.length < 3) {
			System.err.println(USAGE);
			System.exit(1);
		}
		try {
			String src = args[0];
			String ref = args[1];
			String nbest = args[2];
			translations = new TranslationInfoList(src, ref, nbest);
			initializeChooserFrame();
		}
		catch (IOException e) {
			System.err.print("Browser main caught an IOException: ");
			System.err.println(e.getMessage());
			System.exit(1);
		}
		return;
	}
	
	/**
	 * Initializes the various JComponents in the chooser frame.
	 */
	private static void initializeChooserFrame()
	{
		JFrame chooserFrame = new JFrame("Joshua Derivation Tree Browser");
		chooserFrame.setLayout(new GridLayout(2,1));
		
		JMenuBar mb = new JMenuBar();
		JMenu openMenu = new JMenu("Control");
		JMenuItem creat = new JMenuItem("New tree viewer window");
		JMenuItem src = new JMenuItem("Open source file ...");
		JMenuItem ref = new JMenuItem("Open reference file ...");
		JMenuItem tgt = new JMenuItem("Open n-best derivations file ...");
		JMenuItem quit = new JMenuItem("Quit");

		new FileChoiceListener(chooserFrame, src, ref, tgt);

		creat.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				activeFrame.disableNavigationButtons();
				activeFrame = new DerivationTreeFrame();
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
		openMenu.add(ref);
		openMenu.add(tgt);
		openMenu.add(quit);
		mb.add(openMenu);
		chooserFrame.setJMenuBar(mb);
		
		referenceList = new JList(new DefaultListModel());
		referenceList.setFixedCellWidth(200);
		referenceList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
//		referenceList.setCellRenderer(new DerivationBrowserListCellRenderer());
		oneBestList = new JList(new DefaultListModel());
		oneBestList.setFixedCellWidth(200);
		oneBestList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
//		oneBestList.setCellRenderer(new DerivationBrowserListCellRenderer());
		chooserFrame.getContentPane().add(new JScrollPane(referenceList));
		chooserFrame.getContentPane().add(new JScrollPane(oneBestList));
		
		new SynchronizedPairListListener(referenceList, oneBestList);
		
		refreshLists();
		chooserFrame.setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
		chooserFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		activeFrame = new DerivationTreeFrame();
		chooserFrame.setVisible(true);
		return;
	}
	
	/**
	 * Removes and re-adds the appropriate values to the reference and one-best lists.
	 */
	private static void refreshLists()
	{
		referenceList.removeAll();
		oneBestList.removeAll();
		
		DefaultListModel referenceListModel = (DefaultListModel) referenceList.getModel();
		DefaultListModel oneBestListModel = (DefaultListModel) oneBestList.getModel();
		for (TranslationInfo ti : translations.getAllInfo()) {
			referenceListModel.addElement(ti.getReferenceTranslation());
			oneBestListModel.addElement(ti.getOneBest());
		}
		return;
	}

	/**
	 * Increments currentSourceIndex, unless it is already at its maximum.
	 */
	static void incrementCurrentSourceIndex()
	{
		if (currentSourceIndex == translations.getAllInfo().size() - 1)
			return;
		currentSourceIndex++;
		referenceList.setSelectedIndex(currentSourceIndex);
		return;
	}
	
	/**
	 * Decrements currentSourceIndex, unless it is already at zero.
	 */
	static void decrementCurrentSourceIndex()
	{
		if (currentSourceIndex == 0)
			return;
		currentSourceIndex--;
		referenceList.setSelectedIndex(currentSourceIndex);
		return;
	}
	
	static void setCurrentSourceIndex(int index)
	{
		if ((index < 0) || (index > translations.getAllInfo().size() - 1))
			return;
		currentSourceIndex = index;
		referenceList.setSelectedIndex(currentSourceIndex);
	}
	
	/**
	 * Increments currentCandidateIndex, unless it is already at its maximum.
	 */
	static void incrementCurrentCandidateIndex()
	{
		if (currentCandidateIndex == translations.getInfo(currentSourceIndex).getAllTranslations().size() - 1)
			return;
		currentCandidateIndex++;
		return;
	}
	
	/**
	 * Decrements currentCandidateIndex unless is is already at zero.
	 */
	static void decrementCurrentCandidateIndex()
	{
		if (currentCandidateIndex == 0)
			return;
		currentCandidateIndex--;
		return;
	}
	
	/**
	 * Sets currentCandidateIndex to the specified value.
	 * 
	 * @param index the value to assign to currentCandidateIndex
	 */
	static void setCurrentCandidateIndex(int index)
	{
		if ((index < 0) || (index > translations.getInfo(currentSourceIndex).getAllTranslations().size() - 1))
			return;
		currentCandidateIndex = index;
		return;
	}
	
	/**
	 * Returns the source sentence of the translation information currently pointed to by the 
	 * source index.
	 * 
	 * @return the current source sentence
	 */
	static String getCurrentSourceSentence()
	{
		return translations.getInfo(currentSourceIndex).getSourceSentence();
	}
	
	/**
	 * Returns the reference translation for the translation information currently pointed to by the
	 * source index.
	 * 
	 * @return the current reference translation
	 */
	static String getCurrentReferenceTranslation()
	{
		return translations.getInfo(currentSourceIndex).getReferenceTranslation();
	}
	
	/**
	 * Returns the candidate translation currently pointed to by the candidate index.
	 * 
	 * @return the current candidate translation
	 */
	static String getCurrentCandidateTranslation()
	{
		return translations.getInfo(currentSourceIndex).getTranslation(currentCandidateIndex);
	}
	
	/**
	 * Returns the one-best translation text currently pointed to by the source index.
	 * 
	 * @return the current one-best translation text
	 */
	static String getCurrentOneBest()
	{
		return translations.getInfo(currentSourceIndex).getOneBest();
	}
	
	/**
	 * Returns information about all the translations stored in the source, reference, and
	 * n-best files.
	 * 
	 * @return a TranslationInfoList populated by the current source, reference, and n-best files.
	 */
	static TranslationInfoList getTranslationInfo()
	{
		return translations;
	}
}
