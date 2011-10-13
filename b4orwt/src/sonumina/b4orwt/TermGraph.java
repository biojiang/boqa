package sonumina.b4orwt;

import java.util.HashMap;
import java.util.Iterator;

import org.eclipse.rwt.graphics.Graphics;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

import sonumina.math.graph.DirectedGraph;
import sonumina.math.graph.DirectedGraphDotLayout;
import sonumina.math.graph.DirectedGraphLayout;

public class TermGraph<T> extends Canvas
{
	public static interface ILabelProvider<T>
	{
		public String getLabel(T t); 
	}

	/**
	 * Container for storing data of the node.
	 */
	private static class NodeData
	{
		public int left, top;
		public int width, height;
		
		public Button but;
	}

	private DirectedGraph<T> graph;
	private ILabelProvider<T> labelProvider;
	
	private HashMap<T,NodeData> graphLayout = new HashMap<T,NodeData>(); 

	public TermGraph(Composite parent, int style)
	{
		super(parent, style);
		
		addPaintListener(new PaintListener() {
			
			@Override
			public void paintControl(final PaintEvent event)
			{
				if (graph != null && labelProvider != null)
				{
					/* Draw edges */
					for (T v : graph)
					{
						NodeData n1 = graphLayout.get(v);
						
						int x1 = n1.left + n1.width / 2;
						int y1 = n1.top + n1.height;

						Iterator<T> iter = graph.getChildNodes(v);
						while (iter.hasNext())
						{
							T c = iter.next();
							NodeData n2 = graphLayout.get(c);
							
							int x2 = n2.left + n2.width / 2;
							int y2 = n2.top;
							
							event.gc.drawLine(x1, y1, x2, y2);
						}
					}
				}
			}
		});
	}

	@Override
	public void layout()
	{
		super.layout();
	}

	public void setLabelProvider(ILabelProvider<T> labelProvider)
	{
		this.labelProvider = labelProvider;
	}

	public void setGraph(DirectedGraph<T> graph)
	{
		this.graph = graph;
		final TermGraph<T> THIS = this;

		/* TODO: Reuse already present nodes */
		for (NodeData n : graphLayout.values())
			n.but.dispose();
		graphLayout.clear();

		DirectedGraphDotLayout.layout(graph, new DirectedGraphLayout.IGetDimension<T>() {
			public void get(T vertex, DirectedGraphLayout.Dimension d)
			{
				String label;

				NodeData n = new NodeData();
				graphLayout.put(vertex,n);

				label = labelProvider.getLabel(vertex);

				Button b = new Button(THIS,0);
				b.setText(label);
				b.pack();
				n.but = b;

				d.width = b.getSize().x;
				d.height = b.getSize().y;
				
				n.width = d.width;
				n.height = d.height;
			};
		}, new DirectedGraphLayout.IPosition<T>() {
			public void setSize(int width, int height)
			{
			}
			
			public void set(T vertex, int left, int top)
			{
				NodeData n = graphLayout.get(vertex);
				
				n.left = left;
				n.top = top;
				n.but.setLocation(left, top);
			};
		}, 6, 10);


		redraw();
	}

}
