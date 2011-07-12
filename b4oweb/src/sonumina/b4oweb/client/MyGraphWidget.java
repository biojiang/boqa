package sonumina.b4oweb.client;

import sonumina.b4oweb.client.raphael.Raphael;

public class MyGraphWidget extends Raphael implements IGraphWidget
{
	public MyGraphWidget(int width, int height)
	{
		super(width, height);
	}

	@Override
	public void addNode(String id)
	{
	}

	@Override
	public void addNode(String id, String l)
	{
	}

	@Override
	public void addEdge(String from, String to)
	{
	}

	@Override
	public void redraw()
	{
	}
}
