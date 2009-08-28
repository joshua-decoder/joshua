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

import java.awt.BorderLayout;
import java.awt.FileDialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import joshua.util.Platform;

/**
 * Represents a labeled field that can be selected by a file chooser.
 *
 * @author Lane Schwartz
 */
public class FileSelectionPane extends JPanel implements ActionListener {

	private final JLabel label;
	private final JTextField textField;
	private final JButton button;
	
	final JFileChooser fc = new JFileChooser();
	
	private final ParallelCorpusFrame parent;
	
	public FileSelectionPane(ParallelCorpusFrame parent, String label) {

		this.parent = parent;
		
		this.setLayout(new BorderLayout());
		
		this.label = new JLabel(label);
		this.add(this.label, BorderLayout.LINE_START);
		
		this.textField = new JTextField(30);
		this.add(this.textField, BorderLayout.CENTER);
		
		this.button = new JButton("Browse...");
		this.button.addActionListener(this);
		this.add(this.button, BorderLayout.LINE_END);
		
		this.setSize(this.getPreferredSize());
		
	}

	public void actionPerformed(ActionEvent e) {
		if (button.equals(e.getSource())) {
			selectFile();
		}
	}
	
	public void selectFile() {
		
		if (Platform.isMac()) {
			FileDialog fileDialog = new FileDialog(parent);
			fileDialog.setMode(FileDialog.LOAD);
			fileDialog.setVisible(true);
			 String fileName = fileDialog.getFile();
             String dirName = fileDialog.getDirectory();
             if (fileName != null) {
            	 File file = new File(dirName, fileName);
            	 textField.setText(file.getAbsolutePath());
             }
		} else {
			int returnVal = fc.showOpenDialog(this);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
	            File file = fc.getSelectedFile();
	            textField.setText(file.getAbsolutePath());
	        }			
		}
	}
	
	public boolean isEmpty() {
		String text = textField.getText();
		
		return (text == null || text.length()<=0);
	}
	
	public String getFileName() {
		return this.textField.getText();
	}
	
}
