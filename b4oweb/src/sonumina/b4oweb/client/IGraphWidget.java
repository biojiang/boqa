package sonumina.b4oweb.client;

public interface IGraphWidget
{
	public void addNode(String id);
	public void addNode(String id, String l);
	public void addEdge(String from, String to);
	public void redraw();
}
