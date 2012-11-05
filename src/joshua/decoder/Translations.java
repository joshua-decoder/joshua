package joshua.decoder;

import java.util.Iterator;
import java.util.LinkedList;
import joshua.decoder.io.TranslationRequest;

/**
 * This class represents a streaming sequence of translations. It is returned by the main entry
 * point to the Decoder object, the call to decodeAll. The translations here are parallel to the
 * input sentences in the corresponding TranslationRequest object. Because of parallelization, the
 * translated sentences might be computed out of order. Each Translation is sent to this
 * Translations object by a DecoderThreadRunner via the record() function, which places the
 * Translation in the right place. When the next translation in a sequence is available, next() is
 * notified.
 * 
 * The object is both iterable and an iterator. Normally this is frowned upon, because something
 * that is iterable is different from the (state-keeping) iterator used to iterate over it.
 * However, the Translations object removes old Translations for efficiency reasons (they can be
 * large objects, retaining the complete hypergraph), which really supports only one iterator.
 * 
 * @author Matt Post <post@cs.jhu.edu>
 */
public class Translations implements Iterable<Translation>, Iterator<Translation> {

  /* The source sentences to be translated. */
  private TranslationRequest request = null;

  /*
   * This records the index of the sentence at the head of the underlying list. The iterator's
   * next() blocks when the value at this position in the translations LinkedList is null.
   */
  private int currentID = 0;

  /* The set of translated sentences. */
  private LinkedList<Translation> translations = null;

  public Translations(TranslationRequest request) {
    this.request = request;
    this.translations = new LinkedList<Translation>();
  }

  public void record(Translation translation) {
    synchronized (this) {
      /* Pad the set of translations with nulls to accommodate the new translation. */
      int offset = translation.id() - currentID;
      while (offset >= translations.size())
        translations.add(null);
      translations.set(offset, translation);

      /*
       * If the id of the current translation is at the head of the list (first element), then we
       * have the next Translation to be return, and we should notify anyone waiting on next(),
       * which will then remove the item and increment the currentID.
       */
      if (translation.id() == currentID) {
        this.notify();
      }
    }
  }

  /**
   * An iterator over Translations. This is a stream of translations so only one iterator is
   * permitted, so we just have this class implement both Iterable and Iterator.
   */
  @Override
  public Iterator<Translation> iterator() {
    return this;
  }

  /**
   * There will be another Translation available if the size of the request set is larger than our
   * current position.
   */
  @Override
  public boolean hasNext() {
    synchronized (this) {
      return request.hasNext() || currentID < request.size();
    }
  }

  /**
   * Returns the next Translation, blocking if necessary until it's available, since the next
   * Translation might not have been produced yet.
   */
  @Override
  public Translation next() {
    /*
     * If the current position is past the position of the last translated sentence, we have to
     * wait
     */
    if (translations.size() == 0 || translations.peek() == null) {
      synchronized (this) {
        try {
          this.wait();
        } catch (InterruptedException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    }

    currentID++;
    return translations.poll();
  }

  @Override
  public void remove() {
    // unimplemented
  }
}