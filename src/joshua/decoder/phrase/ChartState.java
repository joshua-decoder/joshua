package joshua.decoder.phrase;

public class ChartState {

  public Left left;
  public Right right;

  public ChartState() {
    left = new Left();
    right = new Right();
  }

}
