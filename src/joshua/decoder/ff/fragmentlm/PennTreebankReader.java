/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package joshua.decoder.ff.fragmentlm;

import java.util.*;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;

/**
 * @author Dan Klein
 */
public class PennTreebankReader {

  static class TreeCollection extends AbstractCollection<Tree> {

    List<File> files;
    Charset charset;

    static class TreeIteratorIterator implements Iterator<Iterator<Tree>> {
      Iterator<File> fileIterator;
      Iterator<Tree> nextTreeIterator;
      Charset charset;

      public boolean hasNext() {
        return nextTreeIterator != null;
      }

      public Iterator<Tree> next() {
        Iterator<Tree> currentTreeIterator = nextTreeIterator;
        advance();
        return currentTreeIterator;
      }

      public void remove() {
        throw new UnsupportedOperationException();
      }

      private void advance() {
        nextTreeIterator = null;
        while (nextTreeIterator == null && fileIterator.hasNext()) {
          File file = fileIterator.next();
          // System.out.println(file);
          try {
            nextTreeIterator = new Trees.PennTreeReader(new BufferedReader(new InputStreamReader(
                new FileInputStream(file), this.charset)));
          } catch (FileNotFoundException e) {
          } catch (UnsupportedCharsetException e) {
            throw new Error("Unsupported charset in file " + file.getPath());
          }
        }
      }

      TreeIteratorIterator(List<File> files, Charset charset) {
        this.fileIterator = files.iterator();
        this.charset = charset;
        advance();
      }
    }

    public Iterator<Tree> iterator() {
      return new ConcatenationIterator<Tree>(new TreeIteratorIterator(files, this.charset));
    }

    public int size() {
      int size = 0;
      Iterator<Tree> i = iterator();
      while (i.hasNext()) {
        size++;
        i.next();
      }
      return size;
    }

    @SuppressWarnings("unused")
    private List<File> getFilesUnder(String path, FileFilter fileFilter) {
      File root = new File(path);
      List<File> files = new ArrayList<File>();
      addFilesUnder(root, files, fileFilter);
      return files;
    }

    private void addFilesUnder(File root, List<File> files, FileFilter fileFilter) {
      if (!fileFilter.accept(root))
        return;
      if (root.isFile()) {
        files.add(root);
        return;
      }
      if (root.isDirectory()) {
        File[] children = root.listFiles();
        for (int i = 0; i < children.length; i++) {
          File child = children[i];
          addFilesUnder(child, files, fileFilter);
        }
      }
    }

    public TreeCollection(String file) throws FileNotFoundException, IOException {
      this.files = new ArrayList<File>();
      this.files.add(new File(file));
      this.charset = Charset.defaultCharset();
    }
  }
  
  public static Collection<Tree> readTrees(String path) throws FileNotFoundException, IOException {
    return new TreeCollection(path);
  }

  public static void main(String[] args) {
/*    Collection<Tree> trees = readTrees(args[0], Charset.defaultCharset());
    for (Tree tree : trees) {
      tree = (new Trees.StandardTreeNormalizer()).transformTree(tree);
      System.out.println(Trees.PennTreeRenderer.render(tree));
    }
  */
  }

}
