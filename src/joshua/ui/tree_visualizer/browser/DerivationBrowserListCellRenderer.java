package joshua.ui.tree_visualizer.browser;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

public class DerivationBrowserListCellRenderer extends JPanel implements ListCellRenderer {
	
	public DerivationBrowserListCellRenderer()
	{
		super();
		setOpaque(true);
	}
	
	public Component getListCellRendererComponent(JList list, Object value,
			int index, boolean isSelected, boolean cellHasFocus) {
		setLayout(new GridLayout(1 + Browser.getTranslationInfo().getNumberOfNBestFiles(), 1));
		TranslationInfo ti = (TranslationInfo) value;
		add(new JLabel(ti.getReferenceTranslation()));
		for (String oneBest : ti.getAllOneBest()) {
			add(new JLabel("> " + oneBest));
		}
		setSize(200, 200);
		return this;
	}
	
	@Override
	public void setBounds(int x, int y, int width, int height)
	{
		super.setBounds(x, y, width, height);
		validate();
	}

}
