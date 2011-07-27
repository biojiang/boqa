package sonumina.b4oweb.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

import sonumina.b4oweb.client.gwt.DataGrid;
import sonumina.b4oweb.client.gwt.MyCustomScrollPanel;
import sonumina.b4oweb.shared.SharedItemResultEntry;
import sonumina.b4oweb.shared.SharedParents;
import sonumina.b4oweb.shared.SharedTerm;
import sonumina.math.graph.AbstractGraph.IVisitor;
import sonumina.math.graph.DirectedGraph;
import sonumina.math.graph.Edge;

import com.google.gwt.cell.client.ButtonCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.dom.client.ScrollEvent;
import com.google.gwt.event.dom.client.ScrollHandler;
import com.google.gwt.event.logical.shared.OpenEvent;
import com.google.gwt.event.logical.shared.OpenHandler;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.DisclosurePanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.view.client.SingleSelectionModel;

class LazyTerm
{
	public enum LoadingState
	{
		STATE_NOT_LOADED,
		STATE_LOADING,
		STATE_LOADED,
		STATE_MAX
	};

	private final static int STATE_MASK = upperPowerOfTwo(LoadingState.STATE_MAX.ordinal()) - 1;

	public int flags;

	public SharedTerm term;
	
	/**
	 * From bit twiddling hacks.
	 * 
	 * @param v
	 * @return
	 */
	public static int upperPowerOfTwo(int v)
	{
		v--;
		v |= v >> 1;
		v |= v >> 2;
		v |= v >> 4;
		v |= v >> 8;
		v |= v >> 16;
		v |= v >> 32;
		v++;
		return v;
	}
	
	public void setLoadingFlag(LoadingState state)
	{
		flags &= ~STATE_MASK;
		flags |= state.ordinal();
	}
	
	public boolean isNotLoaded()
	{
		return (flags & STATE_MASK) == LoadingState.STATE_NOT_LOADED.ordinal();
	}
	
	public boolean isLoading()
	{
		return (flags & STATE_MASK) == LoadingState.STATE_LOADING.ordinal();
	}

	public boolean isLoaded()
	{
		return (flags & STATE_MASK) == LoadingState.STATE_LOADED.ordinal();
	}

}

/**
 * Widget drawing graphs of terms.
 * 
 * @author Sebastian Bauer
 */
class TermGraphWidget extends MyGraphWidget<LazyTerm>
{
	public TermGraphWidget(int width, int height) {
		super(width, height);
	}

	@Override
	protected String getLabel(LazyTerm n)
	{
		if (n.term != null)
			return n.term.term;
		return "Unknown";
	}
}

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class B4oweb implements EntryPoint
{
	/**
	 * The message displayed to the user when the server cannot be reached or
	 * returns an error.
	 */
	private static final String SERVER_ERROR = "An error occurred while "
			+ "attempting to contact the server. Please check your network "
			+ "connection and try again.";

	/**
	 * Create a remote service proxy to talk to the server-side b4o service.
	 */
	private final B4OServiceAsync b4oService = GWT.create(B4OService.class);

	/**
	 * Our local term storage.
	 */
	private List<LazyTerm> allTermsList = new ArrayList<LazyTerm>();

	/**
	 * The corresponding directed graph.
	 */
	private DirectedGraph<LazyTerm> allTermsGraph = new DirectedGraph<LazyTerm>();
	
	/**
	 * The filter string that is used in the term filter.
	 */
	private String availableTermsFilterString = "";
	
	/**
	 * The strings of terms that are currently displayed. This may be less than above
	 * if a filter string is active.
	 */
	private List<LazyTerm> availableTermsBackendList;
	
	/**
	 * Represents the selected index
	 */
	private int availableTermsSelectedIndex = -1;
	
	/**
	 * The available term cell list.
	 */
	private DataGrid<LazyTerm> availableTermsDataGrid;
	
	/**
	 * The table holding the selected terms.
	 */
	private DataGrid<LazyTerm> selectedTermsDataGrid;

	/**
	 * The terms that are currently selected.
	 */
	private List<LazyTerm> selectedTermsList = new ArrayList<LazyTerm>();

	/**
	 * The panel which holds the graph.
	 */
	private MyCustomScrollPanel selectedTermsGraphScrollPanel;

	/**
	 * The graph which displays all selected terms.
	 */
	private TermGraphWidget selectedTermsGraph;

	/**
	 * The panel in which the results are placed.
	 */
	private VerticalPanel resultPanel;
	
	/**
	 * A simple callback interface
	 *
	 * @author Sebastian Bauer
	 */
	private static interface MyCallback
	{
		public void cb();
	};
	
	/**
	 * Updates the terms currently visible within the scroll panel.
	 */
	private void updateVisibleTerms()
	{
		int first = availableTermsDataGrid.getVerticalClientTopRow();
		int visible = availableTermsDataGrid.getVerticalClientVisibleElements();
		
		ArrayList<Integer> toBeLoaded = new ArrayList<Integer>();

		for (int i=first;i < first + visible && i < availableTermsBackendList.size();i++)
		{
			LazyTerm t = availableTermsBackendList.get(i);
			if (t.isNotLoaded())
			{
				t.setLoadingFlag(LazyTerm.LoadingState.STATE_LOADING);
				toBeLoaded.add(i);
			}
		}
		
		if (toBeLoaded.size() > 0)
			updateTerms(toBeLoaded);
	}

	/**
	 * Updates local information of the given terms.
	 * 
	 * @param toBeLoaded
	 */
	private void updateTerms(List<Integer> toBeLoaded)
	{
		b4oService.getNamesOfTerms(availableTermsFilterString, toBeLoaded, new AsyncCallback<SharedTerm[]>()
				{
					@Override
					public void onFailure(Throwable caught) { GWT.log("Error", caught);};

					@Override
					public void onSuccess(SharedTerm [] result)
					{
						for (SharedTerm t : result)
						{
							LazyTerm lz = availableTermsBackendList.get(t.requestId);
							if (lz != null)
							{
								lz.term = t;
								availableTermsDataGrid.setRowData(t.requestId,availableTermsBackendList.subList(t.requestId, t.requestId+1));
							}
						}
					}
				});
	}
	
	/**
	 * Populates the term cell list with terms.
	 */
	private void populateAvailableTerms()
	{
		b4oService.getNumberOfTerms(availableTermsFilterString, new AsyncCallback<Integer>() {

			@Override
			public void onFailure(Throwable caught) { GWT.log("Error", caught);};

			@Override
			public void onSuccess(Integer result)
			{
				/* Update available backend list */
			    availableTermsBackendList = new ArrayList<LazyTerm>(result);
				for (int i=0;i<result;i++)
				{
					LazyTerm t = new LazyTerm();
					availableTermsBackendList.add(t);
				}
				
				long start = System.currentTimeMillis();

				availableTermsDataGrid.setRowData(availableTermsBackendList);
				availableTermsDataGrid.setRowCount(result,true);
				
				GWT.log((System.currentTimeMillis() - start) + " " + availableTermsBackendList.size());
			}
		});
	}
	
	/**
	 * Populate the nodes in the graph and in the list.
	 */
	private void populateSelectedTerms()
	{
		final boolean VERSION1 = false;

		selectedTermsDataGrid.setRowData(selectedTermsList);
		selectedTermsGraph.clear();

		/* Determine the nodes which needs to be added to the graph
		 * (incl. their ancestors)
		 */
		LinkedHashSet<Integer> serverIds = new LinkedHashSet<Integer>();
		for (LazyTerm st : selectedTermsList)
		{
			if (!selectedTermsGraph.containsNode(st))
			{
				if (st.term != null)
					serverIds.add(st.term.serverId);
				else if (VERSION1) selectedTermsGraph.addNode(st);
			}
		}

		/* Now retrieve the ancestors */
		addTermsToTermGraph(selectedTermsGraph, new ArrayList<Integer>(serverIds));
		selectedTermsGraph.redraw();
	}
	
	/**
	 * This method updates the local graph with the ancestors of the specified terms.
	 * If this is done, the callback is issued.
	 * 
	 * @param serverIds
	 */
	private void updateAncestors(ArrayList<Integer> serverIds, final MyCallback cb)
	{
		/* First filter the ids for ids that are already contained. If a term is present
		 * then always all ancestors are present as well. FIXME: At the moment all terms
		 * are added to the graph during initialization. Therefore we need to check whether
		 * the node has any in and outgoing edges. */
		ArrayList<Integer> newServerIds = new ArrayList<Integer>();
		for (Integer sid : serverIds)
		{
			LazyTerm lz = allTermsList.get(sid);
			if (!allTermsGraph.containsVertex(lz))
				newServerIds.add(sid);
			if (allTermsGraph.getInDegree(lz)==0 && allTermsGraph.getOutDegree(lz) == 0)
				newServerIds.add(sid);
		}

		/* Short cut */
		if (newServerIds.size() == 0)
		{
			cb.cb();
			return;
		}

		/* Now receive the ids of the ancestors */
		b4oService.getAncestors(serverIds, new AsyncCallback<SharedParents[]>()
				{
					@Override
					public void onFailure(Throwable caught) { GWT.log("Error", caught); }
					public void onSuccess(SharedParents[] result)
					{
						/* The terms of which further information shall be requested, usually
						 * all ancestors.
						 */
						LinkedHashSet<Integer> requestTermList = new LinkedHashSet<Integer>();

						for (SharedParents ps : result)
						{
							LazyTerm lz = allTermsList.get(ps.serverId);

							allTermsGraph.addVertex(lz);
							if (lz.term == null)
								requestTermList.add(ps.serverId);

							for (int p : ps.parentIds)
							{
								LazyTerm plz = allTermsList.get(p);
								allTermsGraph.addVertex(plz);

								/* update our global view */
								if (!allTermsGraph.areNeighbors(plz,lz))
									allTermsGraph.addEdge(new Edge<LazyTerm>(plz,lz));

								if (plz.term == null)
									requestTermList.add(p);
							}
						}

						/* Request additional information */
						if (requestTermList.size()>0)
						{
							b4oService.getNamesOfTerms(new ArrayList<Integer>(requestTermList), new AsyncCallback<SharedTerm[]>()
									{
										public void onFailure(Throwable caught) { GWT.log("Error", caught); };
										@Override
										public void onSuccess(SharedTerm[] result)
										{
											for (SharedTerm st : result)
											{
												LazyTerm lz = allTermsList.get(st.serverId);
												if (lz.term == null)
													lz.term = st;
											}
											cb.cb();
										}
									});

						} else cb.cb();
					};
				});
	}
	
	/**
	 * Adds the terms represented by the given ids to the graph. If necessary, this call performs
	 * RPC in order to determine the graph structure.
	 * 
	 * @param resultTermGraph
	 * @param serverIds
	 */
	private void addTermsToTermGraph(final TermGraphWidget resultTermGraph, final ArrayList<Integer> serverIds)
	{
		updateAncestors(serverIds, new MyCallback() {
			@Override
			public void cb() {
				ArrayList<LazyTerm> lzList = new ArrayList<LazyTerm>(serverIds.size());
				for (int sid : serverIds)
					lzList.add(allTermsList.get(sid));

				/* Determine all ancestors */
				final ArrayList<LazyTerm> ancestorList = new ArrayList<LazyTerm>();
				allTermsGraph.bfs(lzList, true, new IVisitor<LazyTerm>() {

					@Override
					public boolean visited(LazyTerm vertex)
					{
						ancestorList.add(vertex);
						return true;
					}
				});

				/* Add all nodes */
				for (LazyTerm t : ancestorList)
					resultTermGraph.addNode(t);

				/* Now the edges */
				for (LazyTerm t : ancestorList)
				{
					Iterator<LazyTerm> p = allTermsGraph.getParentNodes(t);
					while (p.hasNext())
						resultTermGraph.addEdge(p.next(), t);
				}
				resultTermGraph.redraw(true);
			}
		});
	}

	/**
	 * This takes the currently selected terms and updates the results.
	 */
	private void updateResults()
	{
		final ArrayList<Integer> queryIDs = new ArrayList<Integer>();
		final HashSet<Integer> inducedQueryIDs = new HashSet<Integer>();
		for (LazyTerm t : selectedTermsList)
		{
			if (t.term != null)
				queryIDs.add(t.term.requestId);
		}

		b4oService.getResults(queryIDs, 0, 20, new AsyncCallback<SharedItemResultEntry[]>() {
			@Override
			public void onSuccess(SharedItemResultEntry [] result)
			{
				int i;
				
				resultPanel.clear();

				for (i=0;i<Math.min(20,result.length);i++)
				{
					final SharedItemResultEntry r = result[i];

					DisclosurePanel dp = new DisclosurePanel((r.rank + 1) + ". " + r.itemName + " (" + r.marginal + ")");
					StringBuilder str = new StringBuilder();

					for (int j=0;j<r.directTerms.length;j++)
						str.append(r.directTerms[j] + " " + r.directedTermsFreq[j] + " ");

					final VerticalPanel vp = new VerticalPanel();
					vp.add(new HTML(str.toString()));
					final TermGraphWidget resultTermGraph = new TermGraphWidget(500,300){
						protected String color(LazyTerm n)
						{
							return "#26bf00";
						};
					};
					vp.add(resultTermGraph);
					
					dp.add(vp);
					resultPanel.add(dp);

					dp.addOpenHandler(new OpenHandler<DisclosurePanel>() {
						@Override
						public void onOpen(OpenEvent<DisclosurePanel> event)
						{
							ArrayList<Integer> serverIds = new ArrayList<Integer>(r.directTerms.length + selectedTermsList.size());
							for (int id : r.directTerms)
								serverIds.add(id);

							for (LazyTerm lt : selectedTermsList)
							{
								if (lt.term != null)
									serverIds.add(lt.term.serverId);
							}

							addTermsToTermGraph(resultTermGraph, serverIds);
						}
					});

				}
			}
			@Override
			public void onFailure(Throwable caught) { GWT.log("Error", caught); }
		});
		
	}
	
	/**
	 * This is the entry point method.
	 */
	public void onModuleLoad()
	{
		VerticalPanel rootVerticalPanel = new VerticalPanel();
		RootLayoutPanel.get().add(rootVerticalPanel);
		RootLayoutPanel.get().setWidgetLeftRight(rootVerticalPanel, 0, Unit.PCT, 0, Unit.PCT);
		HorizontalPanel horizontalPanel = new HorizontalPanel();
		rootVerticalPanel.add(horizontalPanel);
		
		{
			VerticalPanel availableTermsPanel = new VerticalPanel();

			availableTermsDataGrid = new DataGrid<LazyTerm>();
			availableTermsDataGrid.setHeight("220px");
			availableTermsDataGrid.setWidth("528px");
			availableTermsDataGrid.addStyleName("scrollable");
			availableTermsDataGrid.addColumn(new TextColumn<LazyTerm>()
					{
						@Override
						public String getValue(LazyTerm term)
						{
							if (term.term != null)
								return term.term.term + "(" + term.term.numberOfItems + ")";
							return "Unknown";
						}
					});

			final SingleSelectionModel<LazyTerm> selectionModel = new SingleSelectionModel<LazyTerm>();
			availableTermsDataGrid.setSelectionModel(selectionModel);
			availableTermsDataGrid.addDomHandler(new DoubleClickHandler() {
				@Override
				public void onDoubleClick(DoubleClickEvent event) {
					LazyTerm t = selectionModel.getSelectedObject();
					if (t.term != null)
						addTermToSelectedList(t.term.requestId);
				}
			}, DoubleClickEvent.getType());

			final TextBox termTextBox = new TextBox();
			termTextBox.setWidth("520px");
			termTextBox.addKeyUpHandler(new KeyUpHandler() {
				@Override
				public void onKeyUp(KeyUpEvent event)
				{
					String newAvailableTermsFilterString = termTextBox.getText();
					if (!newAvailableTermsFilterString.equals(availableTermsFilterString))
					{
						availableTermsFilterString = newAvailableTermsFilterString;
						populateAvailableTerms();
					}
				}
			});
 
			termTextBox.addKeyDownHandler(new KeyDownHandler() {
				@Override
				public void onKeyDown(KeyDownEvent event) {
					switch (event.getNativeKeyCode())
					{
						case	KeyCodes.KEY_DOWN:
								availableTermsSelectedIndex++;
								selectionModel.setSelected(availableTermsDataGrid.getVisibleItem(availableTermsSelectedIndex), true);
								break;

						case	KeyCodes.KEY_UP:
								if (availableTermsSelectedIndex == -1)
									availableTermsSelectedIndex = availableTermsDataGrid.getRowCount();
								availableTermsSelectedIndex--;
								selectionModel.setSelected(availableTermsDataGrid.getVisibleItem(availableTermsSelectedIndex), true);
								break;
						
						case	KeyCodes.KEY_ENTER:
								if (availableTermsSelectedIndex != -1)
									addTermToSelectedList(availableTermsSelectedIndex);
								break;
					}
				}
			});

			availableTermsPanel.add(termTextBox);
			availableTermsPanel.add(availableTermsDataGrid);

			horizontalPanel.add(availableTermsPanel);
			
			b4oService.getNumberOfTerms(new AsyncCallback<Integer>() {
				@Override
				public void onSuccess(Integer result)
				{
					long start = System.currentTimeMillis();

					/* Fill the global client store first */
					allTermsGraph = new DirectedGraph<LazyTerm>();
					allTermsList = new ArrayList<LazyTerm>(result);
					for (int i=0;i<result;i++)
					{
						LazyTerm t = new LazyTerm();
						allTermsList.add(t);
						allTermsGraph.addVertex(t);
					}
					GWT.log("Setup " + (System.currentTimeMillis() - start) + "ms for " + allTermsList.size() + " terms");

				    availableTermsBackendList = new ArrayList<LazyTerm>(allTermsList);

					start = System.currentTimeMillis();

					availableTermsDataGrid.setRowData(availableTermsBackendList);
					availableTermsDataGrid.setRowCount(result,true);

					GWT.log((System.currentTimeMillis() - start) + " " + availableTermsBackendList.size());

					/* Update the first */
					updateTerms(Arrays.asList(0,1,2,3,4,5,6,7,8,9,10,11,12));
				}
	
				@Override
				public void onFailure(Throwable caught) { }
			});
			
			availableTermsDataGrid.addScrollHandler(new ScrollHandler() {
				@Override
				public void onScroll(ScrollEvent event)
				{
					updateVisibleTerms();
				}
			});
		}
		
		{
			VerticalPanel selectedTermsPanel = new VerticalPanel();
			
			selectedTermsDataGrid = new DataGrid<LazyTerm>();
			selectedTermsDataGrid.setHeight("249px");
			selectedTermsDataGrid.setWidth("420px");
			selectedTermsDataGrid.setLoadingIndicator(new HTML("No feature selected"));
			selectedTermsDataGrid.setEmptyTableWidget(new HTML("No feature selected"));
			selectedTermsDataGrid.addStyleName("scrollable");
			selectedTermsDataGrid.addColumn(new TextColumn<LazyTerm>()
					{
						@Override
						public String getValue(LazyTerm term)
						{
							if (term.term != null)
								return term.term.term;
							return "Unknown";
						}
					});
			Column<LazyTerm, String> buttonColumn = new Column<LazyTerm, String>(new ButtonCell())
					{
						@Override
						public String getValue(LazyTerm object)
						{
							return "X";
						}
				
					};
			selectedTermsDataGrid.setColumnWidth(buttonColumn, "60px");
			/* Add field updater for the column which is invoked whenever somebody
			 * clicks on the button.
			 */
			buttonColumn.setFieldUpdater(new FieldUpdater<LazyTerm, String>() {
				@Override
				public void update(int index, LazyTerm object, String value)
				{
					selectedTermsList.remove(index);
					populateSelectedTerms();
					updateResults();
				}
			});
			selectedTermsDataGrid.addColumn(buttonColumn);
			selectedTermsPanel.add(selectedTermsDataGrid);
			horizontalPanel.add(selectedTermsPanel);
			
			selectedTermsGraph = new TermGraphWidget(400, 200)
			{
				@Override
				protected double opacity(LazyTerm n)
				{
					if (selectedTermsList.contains(n))
					{
						return 1;
					} else return super.opacity(n);
				}
				
				@Override
				protected String color(LazyTerm n) {
					return "#004cbf";
				}
			};
			selectedTermsGraphScrollPanel = new MyCustomScrollPanel(selectedTermsGraph);
			selectedTermsGraphScrollPanel.setWidth("400px");
			selectedTermsGraphScrollPanel.setHeight("249px");
			selectedTermsGraphScrollPanel.addStyleName("scrollable");
			horizontalPanel.add(selectedTermsGraphScrollPanel);
		}

		{
			resultPanel = new VerticalPanel();
			rootVerticalPanel.add(resultPanel);
		}
	}

	/**
	 * Adds the given term to the selected list and refreshes the list and recalculates.
	 * 
	 * @param newSelectedTermIndex
	 */
	private void addTermToSelectedList(int newSelectedTermIndex)
	{
		selectedTermsList.add(availableTermsBackendList.get(newSelectedTermIndex));
		populateSelectedTerms();
		updateResults();
	}
}
