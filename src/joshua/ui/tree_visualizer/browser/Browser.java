package joshua.ui.tree_visualizer.browser;

import joshua.ui.tree_visualizer.tree.Tree;
import joshua.util.io.LineReader;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;

public class Browser {

  /**
   * A list that contains the one best translation of each source sentence.
   */
  private static JList oneBestList;

  private static JTextField searchBox;

  /**
   * The current frame that displays a derivation tree.
   */
  private static List<DerivationTreeFrame> activeFrame;

  private static List<TranslationInfo> translations;
  /**
   * Default width of the chooser frame.
   */
  private static final int DEFAULT_WIDTH = 640;

  /**
   * Default height of the chooser frame.
   */
  private static final int DEFAULT_HEIGHT = 480;

  /**
   * List of colors to be used in derivation trees
   */
  static final Color[] dataSetColors = { Color.red, Color.orange, Color.blue, Color.green };

  /**
   * @param args the paths to the source, reference, and n-best files
   */
  public static void main(String[] argv) throws IOException {
    String sourcePath = argv.length > 0 ? argv[0] : null;
    String referencePath = argv.length > 1 ? argv[1] : null;
    String[] translationPaths = new String[0];
    if (argv.length > 2) {
      translationPaths = Arrays.copyOfRange(argv, 2, argv.length);
    }
    translations = new ArrayList<TranslationInfo>();
    readSourcesFromPath(sourcePath);
    readReferencesFromPath(referencePath);
    for (String tp : translationPaths) {
      readTranslationsFromPath(tp);
    }
    initializeChooserFrame();
    return;
  }

  private static void readSourcesFromPath(String path) throws IOException {
    for (String line: new LineReader(path)) {
      TranslationInfo ti = new TranslationInfo();
      ti.setSourceSentence("<s> " + line + " </s>");
      translations.add(ti);
    }
  }

  private static void readReferencesFromPath(String path) throws IOException {
    Scanner scanner = new Scanner(new File(path), "UTF-8");
    for (TranslationInfo ti : translations) {
      if (scanner.hasNextLine()) {
        ti.setReference(scanner.nextLine());
      }
    }
    scanner.close();
  }

  private static void readTranslationsFromPath(String path) throws IOException {
    Scanner scanner = new Scanner(new File(path), "UTF-8");
    String sentenceIndex = null;
    for (TranslationInfo ti : translations) {
      while (scanner.hasNextLine()) {
        final String[] fields = scanner.nextLine().split("\\|\\|\\|");
        final String index = fields[0];
        final String tree = fields[1].trim();
        if (!index.equals(sentenceIndex)) {
          sentenceIndex = index;
          ti.translations().add(new Tree(tree));
          break;
        }
      }
    }
    scanner.close();
  }

  /**
   * Initializes the various JComponents in the chooser frame.
   */
  private static void initializeChooserFrame() {
    JFrame chooserFrame = new JFrame("Joshua Derivation Tree Browser");
    chooserFrame.setLayout(new BorderLayout());

    /*
     * JMenuBar mb = new JMenuBar(); JMenu openMenu = new JMenu("Control"); JMenuItem src = new
     * JMenuItem("Open source file ..."); JMenuItem ref = new JMenuItem("Open reference file ...");
     * JMenuItem tgt = new JMenuItem("Open n-best derivations file ..."); JMenuItem quit = new
     * JMenuItem("Quit");
     * 
     * new FileChoiceListener(chooserFrame, src, ref, tgt);
     * 
     * quit.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
     * System.exit(0); } }); openMenu.add(src); openMenu.add(ref); openMenu.add(tgt);
     * openMenu.add(quit); mb.add(openMenu); chooserFrame.setJMenuBar(mb);
     */

    searchBox = new JTextField("search");
    searchBox.getDocument().addDocumentListener(new SearchListener());
    searchBox.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        final int selectedIndex = oneBestList.getSelectedIndex();
        Browser.search(selectedIndex < 0 ? 0 : selectedIndex + 1);
      }
    });
    oneBestList = new JList(new DefaultListModel());
    oneBestList.setFixedCellWidth(200);
    oneBestList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    // oneBestList.setCellRenderer(new DerivationBrowserListCellRenderer());

    oneBestList.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        for (DerivationTreeFrame frame : activeFrame) {
          frame.drawGraph(translations.get(oneBestList.getSelectedIndex()));
        }
        return;
      }
    });
    chooserFrame.getContentPane().add(searchBox, BorderLayout.NORTH);
    chooserFrame.getContentPane().add(new JScrollPane(oneBestList), BorderLayout.CENTER);

    refreshLists();
    chooserFrame.setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
    chooserFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    activeFrame = new ArrayList<DerivationTreeFrame>();
    int numNBestFiles = translations.get(0).translations().size();
    for (int i = 0; i < numNBestFiles; i++)
      activeFrame.add(new DerivationTreeFrame(i, oneBestList));
    chooserFrame.setVisible(true);
    return;
  }

  /**
   * Removes and re-adds the appropriate values to the reference and one-best lists.
   */
  private static void refreshLists() {
    oneBestList.removeAll();
    DefaultListModel oneBestListModel = (DefaultListModel) oneBestList.getModel();
    for (TranslationInfo ti : translations) {
      oneBestListModel.addElement(ti.reference());
    }
    return;
  }

  private static void search(int fromIndex) {
    final String query = searchBox.getText();
    DefaultListModel oneBestListModel = (DefaultListModel) oneBestList.getModel();
    for (int i = fromIndex; i < oneBestListModel.getSize(); i++) {
      String reference = (String) oneBestListModel.getElementAt(i);
      if (reference.indexOf(query) != -1) {
        // found the query
        oneBestList.setSelectedIndex(i);
        oneBestList.ensureIndexIsVisible(i);
        searchBox.setBackground(Color.white);
        return;
      }
    }
    searchBox.setBackground(Color.red);
  }

  private static class SearchListener implements DocumentListener {

    public void insertUpdate(DocumentEvent e) {
      final int selectedIndex = oneBestList.getSelectedIndex();
      Browser.search(selectedIndex < 0 ? 0 : selectedIndex);
    }

    public void removeUpdate(DocumentEvent e) {
      final String query = searchBox.getText();
      if (query.equals("")) {
        return;
      } else {
        insertUpdate(e);
      }
    }

    public void changedUpdate(DocumentEvent e) {

    }
  }
}
