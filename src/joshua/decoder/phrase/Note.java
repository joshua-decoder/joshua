package joshua.decoder.phrase;

// PORT: done

public class Note {
  public Object value;
  
  public String toString() {
    return value.toString();
  }
  
  public Note() {
  }
  
  public Note(Object value) {
    this.value = value;
  }
  
  public Object get() {
    return value;
  }

  public void set(Object object) {
    this.value = object;
  }
}
