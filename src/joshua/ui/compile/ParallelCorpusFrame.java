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
package joshua.ui.compile;

import java.awt.FileDialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import joshua.corpus.suffix_array.Compile;
import joshua.util.Platform;

/**
 * Swing component which allows a user to
 * graphically select files for an aligned parallel corpus.
 *
 * @author Lane Schwartz
 */
public class ParallelCorpusFrame extends JFrame implements ActionListener {

	private final static Logger logger =
		Logger.getLogger(ParallelCorpusFrame.class.getName());
	
	private final FileSelectionPane sourceCorpusPane;
	private final FileSelectionPane targetCorpusPane;
	private final FileSelectionPane alignmentsPane;
	
	private final JButton compile;
	
	private final JProgressBar progressBar;
	
	public ParallelCorpusFrame() {
		super("Aligned Parallel Corpus");
		
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));

		this.sourceCorpusPane = new FileSelectionPane(this, "Source corpus");
		this.targetCorpusPane = new FileSelectionPane(this, "Target corpus");
		this.alignmentsPane = new FileSelectionPane(this, "Alignments");

		panel.add(sourceCorpusPane);
		panel.add(targetCorpusPane);
		panel.add(alignmentsPane);
	
		this.compile = new JButton("Compile Corpus");
		this.compile.addActionListener(this);
		panel.add(this.compile);
		
		this.progressBar = new JProgressBar(0,100);

		this.progressBar.setIndeterminate(true);
		this.progressBar.setVisible(false);
//		this.progressBar.setValue(50);
//		this.progressBar.setStringPainted(true);//setStringPainted
//		this.progressBar.setVisible(true);
		panel.add(this.progressBar);
		
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 70, 10));
		
		this.setContentPane(panel);
		
		this.setSize(this.getPreferredSize());
		
	}

	public void actionPerformed(ActionEvent e) {
		
		if (compile.equals(e.getSource()) 
				&& !sourceCorpusPane.isEmpty()
				&& !targetCorpusPane.isEmpty()
				&& !alignmentsPane.isEmpty()) {
			
			progressBar.setVisible(true);
			
			String source = sourceCorpusPane.getFileName();
			String target = targetCorpusPane.getFileName();
			String alignments = alignmentsPane.getFileName();
			
			String outputDir = null;
			if (Platform.isMac()) {
				FileDialog fileDialog = new FileDialog(this);
				fileDialog.setMode(FileDialog.SAVE);
				fileDialog.setVisible(true);
				 String fileName = fileDialog.getFile();
	             String dirName = fileDialog.getDirectory();
	             if (fileName != null) {
	            	 File file = new File(dirName, fileName);
	            	 outputDir = file.getAbsolutePath();
//	            	 textField.setText(file.getAbsolutePath());
	             }
			} else {
				JFileChooser fc = new JFileChooser();
				int returnVal = fc.showOpenDialog(this);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
		            File file = fc.getSelectedFile();
		            outputDir = file.getAbsolutePath();
		        }			
			}
			
			if (outputDir != null) {
				Compile compile = new Compile();
				compile.setSourceCorpus(source);
				compile.setTargetCorpus(target);
				compile.setAlignments(alignments);
				compile.setOutputDir(outputDir);
				logger.info("Output directory == " + outputDir);
//				this.progressBar.setValue(50);
//				this.progressBar.setStringPainted(true);//setStringPainted
//				this.progressBar.setVisible(true);
				
				this.repaint();
				try {
					compile.execute();
					this.progressBar.setVisible(false);
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
			
		}
	}
	
}

	