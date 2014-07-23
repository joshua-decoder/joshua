package joshua.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Point;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JWindow;

/**
 * Startup window for Joshua programs.
 * 
 * @author Lane Schwartz
 * @author Aaron Phillips
 */
public class StartupWindow extends JWindow {

  /** Serialization identifier. */
  private static final long serialVersionUID = 1L;

  /**
   * Constructs a splash screen.
   * 
   * @param title Title to be displayed
   */
  public StartupWindow(String title) {
    this(title, "Joshua Developers", "2010", Color.BLACK, 5);
  }

  public StartupWindow(String title, String author, String year, Image image, Color borderColor,
      int borderWidth) {
    JPanel content = (JPanel) getContentPane();
    content.setBackground(Color.WHITE);

    int width = 250;
    int height = 100;

    Point center = GraphicsEnvironment.getLocalGraphicsEnvironment().getCenterPoint();
    setBounds(center.x - width / 2, center.y - height / 2, width, height);

    JLabel titleLabel = new JLabel(title, JLabel.CENTER);
    titleLabel.setFont(new Font("Sans-Serif", Font.BOLD, 24));
    content.add(titleLabel, BorderLayout.NORTH);

    JLabel copyright = new JLabel("\u24D2 " + year + " - " + author, JLabel.CENTER);
    copyright.setFont(new Font("Sans-Serif", Font.PLAIN, 8));
    content.add(copyright, BorderLayout.SOUTH);

    if (image != null) {
      content.add(new JLabel(new ImageIcon(image)));
    }

    content.setBorder(BorderFactory.createLineBorder(borderColor, borderWidth));

    // Display it
    setVisible(true);
  }

  public StartupWindow(String title, String author, String year, Color borderColor, int borderWidth) {
    this(title, author, year, null, borderColor, borderWidth);
  }

}
