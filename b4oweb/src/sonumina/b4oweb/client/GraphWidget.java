package sonumina.b4oweb.client;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.ui.Widget;

class GraphWidget extends Widget
{
	public static native void test(String id, int w, int h) /*-{
		var width = w;
		var height = h;
			g = new $wnd.Graph();
	
		g.addNode("strawberry");
		g.addNode("cherry");
		g.addNode("1", { label : "Tomato" });
		g.addNode("id35", { label : "meat\nand\ngreed" });
		st = { directed: true, label : "Label", "label-style" : {"font-size": 20 } };
		g.addEdge("kiwi", "penguin", st);
		g.addEdge("strawberry", "cherry");
		g.addEdge("cherry", "apple");
		g.addEdge("cherry", "apple")
		g.addEdge("1", "id35");
		g.addEdge("penguin", "id35");
		g.addEdge("penguin", "apple");
		g.addEdge("kiwi", "id35");
		g.addEdge("1", "cherry", { directed : true } );
	  	// customize the colors of that edge
		g.addEdge("id35", "apple", { stroke : "#bfa" , fill : "#56f", label : "Meat-to-Apple" });
	   	// add an unknown node implicitly by adding an edge
		g.addEdge("strawberry", "apple");
		g.removeNode("1");
		// layout the graph using the Spring layout implementation
		var layouter = new $wnd.Graph.Layout.Spring(g);
	//    // draw the graph using the RaphaelJS draw implementation
		var renderer = new $wnd.Graph.Renderer.Raphael(id, g, width, height);
	}-*/;

	public GraphWidget()
	{
		DivElement div = Document.get().createDivElement();
		div.setId("canvas");
		setElement(div);
	}

	@Override
	protected void onLoad() {
		super.onLoad();

		test("canvas", getElement().getClientWidth(), getElement().getClientHeight());
	}

};