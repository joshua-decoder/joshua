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
package joshua.ui.alignment_visualizer;

import edu.uci.ics.jung.graph.*;

import java.util.LinkedList;
import java.util.Scanner;
import java.io.File;
import java.io.FileNotFoundException;

import javax.swing.*;

public class WordAlignmentGraph extends UndirectedSparseGraph<Word,Integer> {
	public static final String SPACE = "\\s+";

	private int edgeNum;

	private LinkedList<Word> source;
	private LinkedList<Word> target;

	public WordAlignmentGraph(String src, String tgt, String alignment)
	{
		super();
		edgeNum = 0;
		source = new LinkedList<Word>();
		target = new LinkedList<Word>();
		populate(src, tgt, alignment);
	}

	private void populate(String src, String tgt, String alignment)
	{
		String [] srctoks = src.split(SPACE);
		String [] tgttoks = tgt.split(SPACE);
		int i = 0;
		for (String s : srctoks) {
			Word w = new Word(s, true, i);
			addVertex(w);
			source.add(w);
			i++;
		}
		int j = 0;
		for (String s : tgttoks) {
			Word w = new Word(s, false, j);
			addVertex(w);
			target.add(w);
			j++;
		}
		String [] edges = alignment.split(SPACE);
		for (String e : edges)
			addAlignment(e);
	}

	private void addAlignment(String e)
	{
		String [] nums = e.split("\\-");
		int src = Integer.parseInt(nums[0]);
		int tgt = Integer.parseInt(nums[1]);
		addEdge(edgeNum, source.get(src), target.get(tgt));
		edgeNum++;
	}

	public static void main(String [] argv)
	{
		if (argv.length < 3) {
			System.err.println("args: <src> <tgt> <alignment>");
			return;
		}
		try {
			Scanner src = new Scanner(new File(argv[0]), "UTF-8");
			Scanner tgt = new Scanner(new File(argv[1]), "UTF-8");
			Scanner aln = new Scanner(new File(argv[2]), "UTF-8");
			WordAlignmentGraph g;
			g = new WordAlignmentGraph(src.nextLine(), tgt.nextLine(), aln.nextLine());
			AlignmentViewer v = new AlignmentViewer(g);

			JFrame frame = new JFrame("alignment");
			frame.setSize(500, 500);
			frame.getContentPane().add(v);
			frame.pack();
			frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			frame.setVisible(true);
		}
		catch (FileNotFoundException e) {
			System.err.println(e.getMessage());
		}
	}
}
