package joshua.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

/**
 * utility functions for file operations
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @author wren ng thornton <wren@users.sourceforge.net>
 * @since 28 February 2009
 */
public class FileUtility {
  public static String DEFAULT_ENCODING = "UTF-8";

  /*
   * Note: charset name is case-agnostic "UTF-8" is the canonical name "UTF8", "unicode-1-1-utf-8"
   * are aliases Java doesn't distinguish utf8 vs UTF-8 like Perl does
   */
  private static final Charset FILE_ENCODING = Charset.forName(DEFAULT_ENCODING);

  /** Warning, will truncate/overwrite existing files */
  public static BufferedWriter getWriteFileStream(String filename) throws IOException {
    return new BufferedWriter(new OutputStreamWriter(
    // TODO: add GZIP
        filename.equals("-") ? new FileOutputStream(FileDescriptor.out) : new FileOutputStream(
            filename, false), FILE_ENCODING));
  }

  /**
   * Recursively delete the specified file or directory.
   * 
   * @param f File or directory to delete
   * @return <code>true</code> if the specified file or directory was deleted, <code>false</code>
   *         otherwise
   */
  public static boolean deleteRecursively(File f) {
    if (null != f) {
      if (f.isDirectory())
        for (File child : f.listFiles())
          deleteRecursively(child);
      return f.delete();
    } else {
      return false;
    }
  }

  /**
   * Writes data from the integer array to disk as raw bytes, overwriting the old file if present.
   * 
   * @param data The integer array to write to disk.
   * @param filename The filename where the data should be written.
   * @throws IOException
   * @return the FileOutputStream on which the bytes were written
   */
  public static FileOutputStream writeBytes(int[] data, String filename) throws IOException {
    FileOutputStream out = new FileOutputStream(filename, false);
    writeBytes(data, out);
    return out;
  }

  /**
   * Writes data from the integer array to disk as raw bytes.
   * 
   * @param data The integer array to write to disk.
   * @param out The output stream where the data should be written.
   * @throws IOException
   */
  public static void writeBytes(int[] data, OutputStream out) throws IOException {

    byte[] b = new byte[4];

    for (int word : data) {
      b[0] = (byte) ((word >>> 24) & 0xFF);
      b[1] = (byte) ((word >>> 16) & 0xFF);
      b[2] = (byte) ((word >>> 8) & 0xFF);
      b[3] = (byte) ((word >>> 0) & 0xFF);

      out.write(b);
    }
  }

  public static void copyFile(String srFile, String dtFile) throws IOException {
    try {
      File f1 = new File(srFile);
      File f2 = new File(dtFile);
      copyFile(f1, f2);
    } catch (FileNotFoundException ex) {
      System.out.println(ex.getMessage() + " in the specified directory.");
      System.exit(0);
    } catch (IOException e) {
      System.out.println(e.getMessage());
    }
  }

  public static void copyFile(File srFile, File dtFile) throws IOException {
    try {

      InputStream in = new FileInputStream(srFile);

      // For Append the file.
      // OutputStream out = new FileOutputStream(f2,true);

      // For Overwrite the file.
      OutputStream out = new FileOutputStream(dtFile);

      byte[] buf = new byte[1024];
      int len;
      while ((len = in.read(buf)) > 0) {
        out.write(buf, 0, len);
      }
      in.close();
      out.close();
      System.out.println("File copied.");
    } catch (FileNotFoundException ex) {
      System.out.println(ex.getMessage() + " in the specified directory.");
      System.exit(0);
    } catch (IOException e) {
      System.out.println(e.getMessage());
    }
  }

  static public boolean deleteFile(String fileName) {

    File f = new File(fileName);

    // Make sure the file or directory exists and isn't write protected
    if (!f.exists())
      System.out.println("Delete: no such file or directory: " + fileName);

    if (!f.canWrite())
      System.out.println("Delete: write protected: " + fileName);

    // If it is a directory, make sure it is empty
    if (f.isDirectory()) {
      String[] files = f.list();
      if (files.length > 0)
        System.out.println("Delete: directory not empty: " + fileName);
    }

    // Attempt to delete it
    boolean success = f.delete();

    if (!success)
      System.out.println("Delete: deletion failed");

    return success;

  }

  /**
   * Returns the base directory of the file. For example, dirname('/usr/local/bin/emacs') ->
   * '/usr/local/bin'
   */
  static public String dirname(String fileName) {
    if (fileName.indexOf(File.separator) != -1)
      return fileName.substring(0, fileName.lastIndexOf(File.separator));

    return ".";
  }

  public static void createFolderIfNotExisting(String folderName) {
    File f = new File(folderName);
    if (!f.isDirectory()) {
      System.out.println(" createFolderIfNotExisting -- Making directory: " + folderName);
      f.mkdirs();
    } else {
      System.out.println(" createFolderIfNotExisting -- Directory: " + folderName
          + " already existed");
    }
  }

  public static void closeCloseableIfNotNull(Closeable fileWriter) {
    if (fileWriter != null) {
      try {
        fileWriter.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Returns the directory were the program has been started,
   * the base directory you will implicitly get when specifying no
   * full path when e.g. opening a file
   * @return
   */
  public static String getWorkingDirectory() {
    return System.getProperty("user.dir");
  }

  /**
   * Method to handle standard IO xceptions. catch (Exception e) {Utility.handleIO_exception(e);}
   */
  public static void handleExceptions(Exception e) {
    e.printStackTrace();
    System.exit(-1);
  }

  /**
   * Convenience method to get a full file as a String
   * @param file
   * @return The file as a String. Lines are separated by newline character.
   */
  public static String getFileAsString(File file) {
    String result = "";
    List<String> lines = getLines(file, true);
    for (int i = 0; i < lines.size() - 1; i++) {
      result += lines.get(i) + "\n";
    }
    if (!lines.isEmpty()) {
      result += lines.get(lines.size() - 1);
    }
    return result;
  }

  /**
   * This method returns a List of String. Each element of the list corresponds to a line from the
   * input file. The boolean keepDuplicates in the input determines if duplicate lines are allowed
   * in the output LinkedList or not.
   */
  static public List<String> getLines(File file, boolean keepDuplicates) {
    LinkedList<String> list = new LinkedList<String>();
    String line = "";
    try {
      BufferedReader InputReader = new BufferedReader(new FileReader(file));
      for (;;) { // this loop writes writes in a Sting each sentence of
        // the file and process it
        int current = InputReader.read();
        if (current == -1 || current == '\n') {
          if (keepDuplicates || !list.contains(line))
            list.add(line);
          line = "";
          if (current == -1)
            break; // EOF
        } else
          line += (char) current;
      }
      InputReader.close();
    } catch (Exception e) {
      handleExceptions(e);
    }
    return list;
  }

  /**
   * Returns a Scanner of the inputFile using a specific encoding
   * 
   * @param inputFile
   * @return : Scanner
   */
  public static Scanner getScanner(File inputFile, String encoding) {
    Scanner scan = null;
    try {
      scan = new Scanner(inputFile, encoding);
    } catch (IOException e) {
      FileUtility.handleExceptions(e);
    }
    return scan;
  }

  /**
   * Returns a Scanner of the inputFile using default encoding
   * 
   * @param inputFile
   * @return : Scanner
   */
  public static Scanner getScanner(File inputFile) {
    return getScanner(inputFile, DEFAULT_ENCODING);
  }

  static public String getFirstLineInFile(File inputFile) {
    Scanner scan = FileUtility.getScanner(inputFile);
    if (!scan.hasNextLine())
      return null;
    String line = scan.nextLine();
    scan.close();
    return line;
  }
}
