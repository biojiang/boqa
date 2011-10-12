package sonumina.b4orwt;

import java.util.HashMap;

import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

import sonumina.math.graph.DirectedGraph;
import sonumina.math.graph.DirectedGraphLayout;

public class TermGraph<T> extends Canvas
{
	public static interface ILabelProvider<T>
	{
		public String getLabel(T t); 
	}
	
	private DirectedGraph<T> graph;
	private ILabelProvider<T> labelProvider;
	
	private int horizPad = 5;
	private int vertPad = 5;
	
	private static class Node
	{
		public int left, top;
		public int width, height;
	}

	public TermGraph(Composite parent, int style)
	{
		super(parent, style);
		
		addPaintListener(new PaintListener() {
			
			@Override
			public void paintControl(final PaintEvent event)
			{
				if (graph != null && labelProvider != null)
				{
					final HashMap<T,Node> nodeInfos = new HashMap<T,Node>(); 

					DirectedGraphLayout.layout(graph, new DirectedGraphLayout.IGetDimension<T>() {
						public void get(T vertex, DirectedGraphLayout.Dimension d)
						{
							Node n = new Node();
							nodeInfos.put(vertex,n);
							
							Point p = event.gc.textExtent(labelProvider.getLabel(vertex));
							d.width = p.x + 2 * horizPad;
							d.height = p.y + 2 * vertPad;
							
							n.width = d.width;
							n.height = d.height;
						};
					}, new DirectedGraphLayout.IPosition<T>() {
						public void setSize(int width, int height)
						{
						}
						
						public void set(T vertex, int left, int top)
						{
							nodeInfos.get(vertex).left = left + horizPad;
							nodeInfos.get(vertex).top = top + vertPad;
						};
					});

					for (T v : graph)
					{
						Node n = nodeInfos.get(v); 
						int x = n.left;
						int y = n.top;
						
						event.gc.drawRoundRectangle(x, y, n.width, n.height, 3, 3);
						event.gc.drawText(labelProvider.getLabel(v),x + horizPad,y + vertPad);
					}
				}
			}
		});
	}

	public void setLabelProvider(ILabelProvider<T> labelProvider)
	{
		this.labelProvider = labelProvider;
	}

	public void setGraph(DirectedGraph<T> graph)
	{
		this.graph = graph;
		redraw();
	}

}
