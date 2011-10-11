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
	
	private static class Node
	{
		public int left, top;
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
							d.width = p.x;
							d.height = p.y;
						};
					}, new DirectedGraphLayout.IPosition<T>() {
						public void setSize(int width, int height)
						{
						}
						
						public void set(T vertex, int left, int top)
						{
							nodeInfos.get(vertex).left = left;
							nodeInfos.get(vertex).top = top;
						};
					});

					for (T v : graph)
						event.gc.drawText(labelProvider.getLabel(v),nodeInfos.get(v).left,nodeInfos.get(v).top);
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
