package sonumina.b4oweb.client;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Stack;

import sonumina.b4oweb.client.raphael.BBox;
import sonumina.b4oweb.client.raphael.PathBuilder;
import sonumina.b4oweb.client.raphael.Raphael;
import sonumina.math.graph.DirectedGraph;
import sonumina.math.graph.DirectedGraphLayout;
import sonumina.math.graph.Edge;

/**
 * A Graph widget
 * @author Sebastian Bauer
 *
 * @param <T>
 */
public class MyGraphWidget<T> extends Raphael
{
	class Node
	{
		public final T data;
		
		public double textWidth;
		public double textHeight;
	
		public Text text;
		public Rect rect;

		public boolean visible;

		public Node(T data)
		{
			this.data = data;
		}
	}

	private DirectedGraph<Node> graph = new DirectedGraph<MyGraphWidget<T>.Node>();
	private HashMap<T,Node> id2Node = new HashMap<T, Node>();
	private Stack<Shape> shapeStack = new Stack<Shape>();
	
	public MyGraphWidget(int width, int height)
	{
		super(width, height);
	}

	/**
	 * Returns whether n is contained in the graph.
	 * 
	 * @param n
	 * @return
	 */
	public boolean containsNode(T n)
	{
		return id2Node.containsKey(n);
	}

	/**
	 * Adds a new node to the graph. If the to-be-added
	 * node is already contained in the graph, this is a noop.
	 * 
	 * @param n
	 */
	public void addNode(T n)
	{
		if (id2Node.containsKey(n))
			return;

		Node n2 = new Node(n);
		id2Node.put(n,n2);
		graph.addVertex(n2);
	}

	/**
	 * Adds a new edge from "from" to "to".
	 * @param from
	 * @param to
	 */
	public void addEdge(T from, T to)
	{
		Node f = id2Node.get(from);
		Node t = id2Node.get(to);
		Edge<Node> e = new Edge<Node>(f,t);
		graph.addEdge(e);
	}
	
	private void removeFromDisplay(Node n)
	{
		if (n.text != null)
		{
			n.text.remove();
			n.text = null;
		}
		
		if (n.rect != null)
		{
			n.rect.remove();
			n.rect = null;
		}
	}

	public void redraw()
	{
		/* Add all nodes and determine dimension. We also remove nodes that are currently
		 * displayed "on the same go" */
		for (Node n : graph)
		{
			if (!n.visible)
			{
				Text text = new Raphael.Text(0, 0, getLabel(n.data));
				BBox bb = text.getBBox();
				n.textWidth = bb.width();
				n.textHeight = bb.height();
				text.remove();
			} else
			{
				removeFromDisplay(n);
			}
		}
		
		/* Remove additional shapes */
		Shape s;
		while (!shapeStack.isEmpty() && ((s = shapeStack.pop()) != null))
			s.remove();
		
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
				n.rect.attr("fill", "#bfac00");
				n.rect.attr("fill-opacity", "0.6");
				n.rect.attr("stroke", "#bfac00");
				n.rect.attr("stroke-width", 2);
				n.rect.attr("r", 5);
				n.text = new Raphael.Text(left + 10 + n.textWidth / 2, top + 10 + n.textHeight / 2, getLabel(n.data));
				n.text.attr("align","left");
				n.visible = true;
			};
		});
		
		/* Draw edges */
		for (Node n : graph)
		{
			BBox nbb = n.rect.getBBox();
			
			Iterator<Node> child = graph.getChildNodes(n);
			while (child.hasNext())
			{
				Node c = child.next();
				BBox cbb = c.rect.getBBox();
			    PathBuilder pb = new PathBuilder();
			    pb.m(nbb.x() + nbb.width() / 2,nbb.y() + nbb.height()).L(cbb.x() + cbb.width() / 2,cbb.y());
				Path p = new Raphael.Path(pb);
				p.attr("stroke-width",2);
				shapeStack.push(p);
			}
		}
	}
	

	/**
	 * Clears the graph
	 */
	public void clear()
	{
		for (Node n : graph)
		{
			removeFromDisplay(n);
			graph.removeVertex(n);
		}

		/* Remove additional shapes */
		Shape s;
		while (!shapeStack.isEmpty() && ((s = shapeStack.pop()) != null))
			s.remove();
	}

	
	@Override
	protected void onLoad()
	{
		super.onLoad();
		redraw();
	}
	
	/**
	 * Returns the label of the node. May be overwritten by subclasses.
	 * 
	 * @param n
	 * @return
	 */
	protected String getLabel(T n)
	{
		return n.toString();
	}
}
