package sonumina.b4oweb.client;

import java.util.ArrayList;
import java.util.HashMap;

import com.google.gwt.core.client.GWT;

import sonumina.b4oweb.client.raphael.BBox;
import sonumina.b4oweb.client.raphael.Raphael;
import sonumina.math.graph.DirectedGraph;
import sonumina.math.graph.DirectedGraphLayout;
import sonumina.math.graph.Edge;

public class MyGraphWidget extends Raphael implements IGraphWidget
{
	class Node
	{
		public final String id;
		public final String label;
		
		public double textWidth;
		public double textHeight;
	
		public Text text;
		public Rect rect;

		public Node(String id, String label)
		{
			this.id = id;
			this.label = label;
		}
	}

	private DirectedGraph<Node> graph = new DirectedGraph<MyGraphWidget.Node>();
	private HashMap<String,Node> id2Node = new HashMap<String, Node>();
//	private ArrayList<Node> initialNodes = new ArrayList<Node>();
	
	public MyGraphWidget(int width, int height)
	{
		super(width, height);
	}

	@Override
	public void addNode(String id)
	{
		addNode(id, id);
	}

	@Override
	public void addNode(String id, String l)
	{
		Node n = new Node(id,l);
		id2Node.put(id, n);
		graph.addVertex(n);

//		GWT.log("addNode()\n");
//		Text text = new Raphael.Text(10,10, l);
//		BBox bb = text.getBBox();
//
//		Rect rect = new Raphael.Rect(10,10,bb.width(),20);
//		rect.attr("fill", "#d1b48c");
//        /* the default node drawing */
//        var color = Raphael.getColor();
//        var ellipse = r.ellipse(0, 0, 30, 20).attr({fill: color, stroke: color, "stroke-width": 2});
//        /* set DOM node ID */
//        ellipse.node.id = node.label || node.id;
//        shape = r.set().
//            push(ellipse).
//            push(r.text(0, 30, node.label || node.id));
//        return shape;
	}

	@Override
	public void addEdge(String from, String to)
	{
		Node f = id2Node.get(from);
		Node t = id2Node.get(to);
		Edge<Node> e = new Edge<Node>(f,t);
		graph.addEdge(e);
	}

	@Override
	public void redraw()
	{
	}
	
	@Override
	protected void onLoad()
	{
		super.onLoad();
		
		/* Add all nodes and determine dimension */
		for (Node n : graph)
		{
			Text text = new Raphael.Text(0, 0, n.label);
			BBox bb = text.getBBox();
			n.textWidth = bb.width();
			n.textHeight = bb.height();
			text.remove();
		}
		
		DirectedGraphLayout.layout(graph, new DirectedGraphLayout.IGetDimension<Node>() {
			public void get(Node n, DirectedGraphLayout.Dimension d)
			{
				d.width = (int)n.textWidth + 16;
				d.height = (int)n.textHeight + 16;
			};
		}, new DirectedGraphLayout.IPosition<Node>() {
			public void set(Node n, int left, int top)
			{
				n.rect = new Raphael.Rect(left + 10 - 5, top + 10 - 5, n.textWidth + 10, n.textHeight + 10);
				n.rect.attr("fill", "#d1b48c");
				n.text = new Raphael.Text(left + 10 + n.textWidth / 2, top + 10 + n.textHeight / 2, n.label);
				n.text.attr("align","left");
			};
		});
	}
}
