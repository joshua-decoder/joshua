package joshua.ui.hypergraph_visualizer;


public abstract class Vertex {
  private int color = 0;

  public Vertex() {
    return;
  }

  public int getColor() {
    return color;
  }

  public void setColor(int c) {
    color = c;
    return;
  }
}
