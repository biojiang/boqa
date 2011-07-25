package sonumina.b4oweb.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Stack;

import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;

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
		redraw(false);
	}

	/**
	 * Redraw the graph.
	 * 
	 * @param dimsChanged set to true if the dimensions of any of the graph object was changed.
	 */
	public void redraw(boolean dimsChanged)
	{
		final MyGraphWidget<T> THIS = this;

		/* Add all nodes and determine dimension. We also remove nodes that are currently
		 * displayed "on the same go" */
		for (Node n : graph)
		{
			if (!n.visible || dimsChanged)
			{
				Text text = new Raphael.Text(0, 0, getLabel(n.data));
				BBox bb = text.getBBox();
				n.textWidth = bb.width();
				n.textHeight = bb.height();
				text.remove();
				
				if (dimsChanged) removeFromDisplay(n);
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
			public void setSize(int width, int height)
			{
				THIS.setSize(width+10,height+10);
			}
			public void set(final Node n, int left, int top)
			{
				final Raphael.Set set = new Raphael.Set();
				n.rect = new Raphael.Rect(left + 10 - 5, top + 10 - 5, n.textWidth + 10, n.textHeight + 10);
				n.rect.attr("fill", "#bfac00");
				n.rect.attr("fill-opacity", "0.6");
				n.rect.attr("stroke", "#bfac00");
				n.rect.attr("stroke-width", 2);
				n.rect.attr("r", 5);
				n.text = new Raphael.Text(left + 10 + n.textWidth / 2, top + 10 + n.textHeight / 2, getLabel(n.data));
				n.text.attr("align","left");
				n.visible = true;

				set.push(n.rect);
				set.push(n.text);

				MouseOverHandler mov = new MouseOverHandler() {
					@Override
					public void onMouseOver(MouseOverEvent event)
					{
						JSONObject attrs = new JSONObject();
						attrs.put("fill-opacity", new JSONNumber(0.2));
						n.rect.animate(attrs, 200);
					}
				};
				MouseOutHandler mot = new MouseOutHandler() {
					@Override
					public void onMouseOut(MouseOutEvent event) {
						JSONObject attrs = new JSONObject();
						attrs.put("fill-opacity", new JSONNumber(0.6));
						n.rect.animate(attrs, 200);
					}
				};
				
				n.rect.addDomHandler(mov, MouseOverEvent.getType());
				n.rect.addDomHandler(mot, MouseOutEvent.getType());
				n.text.addDomHandler(mov, MouseOverEvent.getType());
				n.text.addDomHandler(mot, MouseOutEvent.getType());
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
		ArrayList<Node> nodes = new ArrayList<Node>(graph.getNumberOfVertices());
		for (Node n : graph)
		{
			removeFromDisplay(n);
			nodes.add(n);
		}

		/* Remove additional shapes */
		Shape s;
		while (!shapeStack.isEmpty() && ((s = shapeStack.pop()) != null))
			s.remove();

		graph = new DirectedGraph<MyGraphWidget<T>.Node>();
		id2Node.clear();
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
