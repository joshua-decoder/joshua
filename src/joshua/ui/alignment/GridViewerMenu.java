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
package joshua.ui.alignment;

import java.awt.event.KeyEvent;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

/**
 * Menu bar for grid viewer application.
 *
 * @author Lane Schwartz
 */
public class GridViewerMenu extends JMenuBar {

	public static final String fileMenuName = "File";
	public static final String openMenuItemName = "Open...";
	public static final String printMenuItemName = "Print...";
	public static final String exitMenuItemName = "Exit";
	
	public GridViewerMenu(final GridViewer frame) {

		// Add File menu...
		JMenu fileMenu = new JMenu(fileMenuName);
		this.add(fileMenu);
		
		// Add Open menu item
		JMenuItem openMenuItem = new JMenuItem(openMenuItemName);
		openMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		openMenuItem.addActionListener(frame);
		fileMenu.add(openMenuItem);
		
		// Add Print menu item
		JMenuItem printMenuItem = new JMenuItem(printMenuItemName);
		printMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		printMenuItem.addActionListener(frame);
		fileMenu.add(printMenuItem);
		
		// Add Exit menu item, if not on Mac OS
		if (! isMac()) {
			JMenuItem exitMenuItem = new JMenuItem(exitMenuItemName);
			exitMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			exitMenuItem.addActionListener(frame);
			fileMenu.add(exitMenuItem);
		}
	}

	
	private boolean isMac() {
		return System.getProperties().getProperty("os.name").toLowerCase().contains("mac");
	}

}
