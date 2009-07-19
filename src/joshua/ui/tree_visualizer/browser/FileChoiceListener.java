package joshua.ui.tree_visualizer.browser;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

/**
 * A class that listens for clicking on menu items to choose any of the three types of file for
 * the derivation browser: source file, reference file, or n-best file. After choosing a file from
 * a presented chooser box, this class updates the browser's translation info with the new file.
 * 
 * @author Jonathan Weese
 *
 */
class FileChoiceListener implements ActionListener {
	/**
	 * The frame that contains the menu bar; that is, the Browser's main frame.
	 */
	private JFrame enclosingFrame;
	
	/**
	 * JMenuItem for "choose source sentence file."
	 */
	private JMenuItem src;
	
	/**
	 * JMenuItem for "choose reference translation file."
	 */
	private JMenuItem ref;
	
	/**
	 * JMenuItem for "choose n-best translation file."
	 */
	private JMenuItem nBest;
	
	/**
	 * The file chooser box.
	 */
	private JFileChooser fileChooser;
	
	/**
	 * The constructor. Also automatically registers <code>this</code> as an action listener for
	 * each button.
	 * 
	 * @param enclosingFrame the main Browser frame
	 * @param source menu item for choosing new source files
	 * @param reference menu item for choosing new reference files
	 * @param nBest menu item for choosing new n-best translation files
	 */
	public FileChoiceListener(JFrame enclosingFrame, JMenuItem source, JMenuItem reference, JMenuItem nBest)
	{
		this.enclosingFrame = enclosingFrame;
		this.src = source;
		this.ref = reference;
		this.nBest = nBest;
		this.fileChooser = new JFileChooser();
		
		src.addActionListener(this);
		ref.addActionListener(this);
		this.nBest.addActionListener(this);
	}

	public void actionPerformed(ActionEvent e) {
		if (fileChooser.showOpenDialog(enclosingFrame) == JFileChooser.APPROVE_OPTION) {
			JMenuItem eventSource = (JMenuItem) e.getSource();
			File chosenFile = fileChooser.getSelectedFile();
			try {
				if (eventSource.equals(src)) {
					Browser.getTranslationInfo().setSourceFile(chosenFile);
				}
				if (eventSource.equals(ref)) {
					Browser.getTranslationInfo().setReferenceFile(chosenFile);
				}
				if (eventSource.equals(nBest)) {
					Browser.getTranslationInfo().addNBestFile(chosenFile);
					Browser.activeFrame.add(new DerivationTreeFrame(Browser.activeFrame.size()));
				}
			}
			catch (IOException ioe) {
				JOptionPane.showMessageDialog(enclosingFrame, "An I/O exception occurred: " + ioe.getMessage(), "IOException", JOptionPane.WARNING_MESSAGE);
			}
		}
		return;
	}

}
