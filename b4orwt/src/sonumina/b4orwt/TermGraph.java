package sonumina.b4orwt;

import java.util.HashMap;
import java.util.Iterator;

import org.eclipse.rwt.graphics.Graphics;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
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
		public String getTooltip(T t);
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

	private int marginLeft = 4;
	private int marginTop = 4;
	
	private int usedWidth;
	private int usedHeight;
	
	/** The offset used to center the visuals */
	private int offsetLeft;
	
	/** The vertical offset to center the visuals */
	private int offsetTop;

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
						
						int x1 = n1.left + n1.width / 2 + offsetLeft;
						int y1 = n1.top + n1.height + offsetTop;

						Iterator<T> iter = graph.getChildNodes(v);
						while (iter.hasNext())
						{
							T c = iter.next();
							NodeData n2 = graphLayout.get(c);
							
							int x2 = n2.left + n2.width / 2 + offsetLeft;
							int y2 = n2.top + offsetTop;
							
							event.gc.drawLine(x1, y1, x2, y2);
						}
					}
				}
			}
		});

		/* Add the control listener that adjusts the offsets when resizing */
		addControlListener(new ControlListener() {
			@Override
			public void controlResized(ControlEvent e)  { updateButtonLocations(); }
			
			@Override
			public void controlMoved(ControlEvent e) { }
		});
	}

	public void setLabelProvider(ILabelProvider<T> labelProvider)
	{
		this.labelProvider = labelProvider;
	}

	/**
	 * Updates the button locations according to the current offsets.
	 */
	private void updateButtonLocations()
	{
		int newOffsetLeft = Math.max((getClientArea().width - usedWidth)/2,0);
		int newOffsetTop = Math.max((getClientArea().height - usedHeight)/2,0);

		if (newOffsetLeft != offsetLeft || newOffsetTop != offsetTop)
		{
			offsetLeft = newOffsetLeft;
			offsetTop = newOffsetTop;

			for (NodeData nd : graphLayout.values())
				nd.but.setLocation(offsetLeft + nd.left, offsetTop + nd.top);
		}
	}


	/**
	 * Refreshes the display to view the given graph.
	 * 
	 * @param graph
	 */
	public void setGraph(DirectedGraph<T> graph)
	{
		this.graph = graph;
		final TermGraph<T> THIS = this;

		/* TODO: Reuse already present nodes */
		for (NodeData n : graphLayout.values())
			n.but.dispose();
		graphLayout.clear();

		usedWidth = usedHeight = 0;

		DirectedGraphDotLayout.layout(graph, new DirectedGraphLayout.IGetDimension<T>() {
			public void get(T vertex, DirectedGraphLayout.Dimension d)
			{
				NodeData n;
				
				n = graphLayout.get(vertex);
				if (n == null)
				{
					n = new NodeData();
					graphLayout.put(vertex,n);
	
					Button b = new Button(THIS,SWT.WRAP|SWT.CENTER);
					b.setText(labelProvider.getLabel(vertex));
					b.setToolTipText(labelProvider.getTooltip(vertex));
					Point def = b.computeSize(SWT.DEFAULT, SWT.DEFAULT,true);
					if (def.x > 160)
						b.setSize(b.computeSize(160,SWT.DEFAULT,true));
					else
						b.setSize(def);
	
					n.width = b.getSize().x;
					n.height = b.getSize().y;
					n.but = b;
				}
				d.width = n.width;
				d.height = n.height;
			};
		}, new DirectedGraphLayout.IPosition<T>() {
			public void setSize(int width, int height)
			{
				System.out.println(width + " " + height);
			}
			
			public void set(T vertex, int left, int top)
			{
				NodeData n = graphLayout.get(vertex);

				n.left = left + marginLeft;
				n.top = top + marginTop;

				if (n.left + n.width > usedWidth)
					usedWidth = n.left + n.width; 
				if (n.top + n.height > usedHeight)
					usedHeight = n.top + n.height; 
			};
		}, 6, 10);

		updateButtonLocations();
		redraw();
	}

}
