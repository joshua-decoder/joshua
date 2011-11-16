package joshua.ui.hypergraph_visualizer;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import joshua.corpus.Vocabulary;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HyperEdge;

public class HyperEdgeListCellRenderer implements ListCellRenderer {

	public HyperEdgeListCellRenderer() {
	}

	public Component getListCellRendererComponent(JList list, Object value,
			int index, boolean isSelected, boolean cellHasFocus) {
		Rule r = ((HyperEdge) value).getRule();
		double score = ((HyperEdge) value).bestDerivationLogP;
		String lhs = Vocabulary.word(r.getLHS());
		String french = Vocabulary.getWords(r.getFrench());
		String english = Vocabulary.getWords(r.getEnglish());
		String rule = String.format("%f %s -> { %s ; %s }", score, lhs, french,
				english);
		JLabel label = new JLabel(rule);
		label.setOpaque(true);
		if (isSelected) {
			label.setBackground(Color.gray);
		} else {
			label.setBackground(Color.white);
		}
		return label;
	}

}
