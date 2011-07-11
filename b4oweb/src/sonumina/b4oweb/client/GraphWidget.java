package sonumina.b4oweb.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.ui.Widget;

class GraphJavaScriptObject extends JavaScriptObject
{
	protected GraphJavaScriptObject() {};
	
	/**
	 * Returns the number of edges contained within the graph
	 * represented by this object.
	 * 
	 * @return
	 */
	public final native int getNumberOfEdges() /*-{
		return this.edges.length;
	}-*/;
	
	/**
	 * Adds a new node to the graph.
	 * 
	 * @param id
	 */
	public final native void addNode(String id) /*-{
		this.addNode(id);
	}-*/;

	/**
	 * Adds a new node to the graph with the given id and label l.
	 * 
	 * @param id the id of the node
	 * @param l
	 */
	public final native void addNode(String id, String l) /*-{
		this.addNode(id, { label : l });
	}-*/;
	
	public final native void addEdge(String from, String to) /*-{
		this.addEdge(from,to);
	}-*/;
	
	/**
	 * Creates a new graph object.
	 */
	public static final native GraphJavaScriptObject create() /*-{
		var g = new $wnd.Graph();
		return g;
	}-*/;
}

class LayoutJavaScriptObject extends JavaScriptObject
{
	protected LayoutJavaScriptObject() {};

	public static final native LayoutJavaScriptObject create(GraphJavaScriptObject g) /*-{
		var layouter = new $wnd.Graph.Layout.Spring(g);
		return layouter;
	}-*/;
	
	public final native void layout() /*-{
		this.layout();
	}-*/;
}

class RendererJavaScriptObject extends JavaScriptObject
{
	protected RendererJavaScriptObject() {};
	
	public static final native RendererJavaScriptObject create(GraphJavaScriptObject g, String id, int width, int height) /*-{
		var renderer = new $wnd.Graph.Renderer.Raphael(id, g, width, height)
		return renderer;
	}-*/;
	
	public final native void render() /*-{
		this.draw();
	}-*/;
}

class GraphWidget extends Widget
{
//	public static native GraphJavaScriptObject test() /*-{
//		var g = new $wnd.Graph();
//	
//		g.addNode("strawberry");
//		g.addNode("cherry");
//		g.addNode("1", { label : "Tomato" });
//		g.addNode("id35", { label : "meat\nand\ngreed" });
//		st = { directed: true, label : "Label", "label-style" : {"font-size": 20 } };
//		g.addEdge("kiwi", "penguin", st);
//		g.addEdge("strawberry", "cherry");
//		g.addEdge("cherry", "apple");
//		g.addEdge("cherry", "apple")
//		g.addEdge("1", "id35");
//		g.addEdge("penguin", "id35");
//		g.addEdge("penguin", "apple");
//		g.addEdge("kiwi", "id35");
//		g.addEdge("1", "cherry", { directed : true } );
//	  	// customize the colors of that edge
//		g.addEdge("id35", "apple", { stroke : "#bfa" , fill : "#56f", label : "Meat-to-Apple" });
//	   	// add an unknown node implicitly by adding an edge
//		g.addEdge("strawberry", "apple");
//		g.removeNode("1");
//		return g;
//	}-*/;
//	
//	public static native void render(GraphJavaScriptObject g, String id, int width, int height) /*-{
//		// layout the graph using the Spring layout implementation
//		var layouter = new $wnd.Graph.Layout.Spring(g);
//	   	// draw the graph using the RaphaelJS draw implementation
//		var renderer = new $wnd.Graph.Renderer.Raphael(id, g, width, height);
//	}-*/;
//	
	private String myId;
	private GraphJavaScriptObject graph;
	private LayoutJavaScriptObject layout;
	private RendererJavaScriptObject renderer;

	public GraphWidget()
	{
		myId = "canvas";

		DivElement div = Document.get().createDivElement();
		div.setId(myId);
		setElement(div);
		
		graph = GraphJavaScriptObject.create();
		layout = LayoutJavaScriptObject.create(graph);
	}
	
	
	public final void addNode(String id)
	{
		graph.addNode(id);
	}

	public final void addNode(String id, String l)
	{
		graph.addNode(id, l);
	}

	public final void addEdge(String from, String to)
	{
		graph.addEdge(from, to);
	}

	@Override
	protected void onLoad() {
		super.onLoad();

		graph.addNode("Hello");
		graph.addNode("Bello");
		graph.addEdge("Hello", "Bello");
		layout.layout();

		renderer = RendererJavaScriptObject.create(graph,"canvas", getElement().getClientWidth(), getElement().getClientHeight());
//		renderer.render();
	}

};