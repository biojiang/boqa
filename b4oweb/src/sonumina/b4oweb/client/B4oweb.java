package sonumina.b4oweb.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import sonumina.b4oweb.shared.FieldVerifier;
import sonumina.b4oweb.shared.SharedTerm;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.dom.client.ScrollEvent;
import com.google.gwt.event.dom.client.ScrollHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.CellList;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.view.client.SelectionModel;
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

class LazyTermCell extends AbstractCell<LazyTerm>
{

	@Override
	public void render(Context context, LazyTerm value, SafeHtmlBuilder sb)
	{
		sb.appendHtmlConstant(value.term!=null?value.term.term:"Loading name...");
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
	private CellList<LazyTerm> availableTermsCellList;
	
	/**
	 * And to which it is embedded.
	 */
	private ScrollPanel availableTermsScrollPanel;
	
	/**
	 * The table holding the selected terms.
	 */
	private CellTable<LazyTerm> selectedTermsCellTable;

	/**
	 * And the panel to which it is embedded.
	 */
	private ScrollPanel selectedTermsScrollPanel;

	/**
	 * The terms that are currently selected.
	 */
	private List<LazyTerm> selectedTermsList = new ArrayList<LazyTerm>();

	/**
	 * Updates the terms currently visible within the scroll panel.
	 */
	private void updateVisibleTerms()
	{
		int ypos = availableTermsScrollPanel.getVerticalScrollPosition();
		int totalHeight = availableTermsScrollPanel.getElement().getScrollHeight();
		int visibleHeight = availableTermsCellList.getElement().getClientHeight();
		int numberOfElements = availableTermsCellList.getPageSize();

		int rowHeight = totalHeight / numberOfElements;
		if (totalHeight % numberOfElements != 0)
			GWT.log("The row height couldn't be determined properly");
		
		if (rowHeight == 0)
		{
			GWT.log("Row height is 0");
			return;
		}
		
		int first = ypos / rowHeight;
		int visible = visibleHeight / rowHeight + 1;
		
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
							GWT.log(t.serverId + " " + t.requestId + " " + t.term);
							LazyTerm lz = availableTermsBackendList.get(t.requestId);
							if (lz != null)
							{
								lz.term = t;
								availableTermsCellList.setRowData(t.requestId,availableTermsBackendList.subList(t.requestId, t.requestId+1));
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
			public void onFailure(Throwable caught) { }

			@Override
			public void onSuccess(Integer result)
			{
			    availableTermsBackendList = new ArrayList<LazyTerm>(result);
				for (int i=0;i<result;i++)
				{
					LazyTerm t = new LazyTerm();
					availableTermsBackendList.add(t);
				}
				
				long start = System.currentTimeMillis();

				availableTermsCellList.setRowData(availableTermsBackendList);
				availableTermsCellList.setRowCount(result,true);
				
				GWT.log((System.currentTimeMillis() - start) + " " + availableTermsBackendList.size());
			}
		});
	}
	
	/**
	 * 
	 */
	private void populateSelectedTerms()
	{
		selectedTermsCellTable.setRowData(selectedTermsList);
		GWT.log("" + selectedTermsCellTable.getRowCount());
	}
	
	/**
	 * This is the entry point method.
	 */
	public void onModuleLoad()
	{
		HorizontalPanel horizontalPanel = new HorizontalPanel();
		RootLayoutPanel.get().add(horizontalPanel);
		
		{
			VerticalPanel availableTermsPanel = new VerticalPanel();

			availableTermsCellList = new CellList<LazyTerm>(new LazyTermCell());
			availableTermsCellList.setHeight("200px");
			availableTermsCellList.setWidth("520px");
			
			final SelectionModel<LazyTerm> selectionModel = new SingleSelectionModel<LazyTerm>();
			availableTermsCellList.setSelectionModel(selectionModel);
			
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
								selectionModel.setSelected(availableTermsCellList.getVisibleItem(availableTermsSelectedIndex), true);
								break;

						case	KeyCodes.KEY_UP:
								if (availableTermsSelectedIndex == -1)
									availableTermsSelectedIndex = availableTermsCellList.getRowCount();
								availableTermsSelectedIndex--;
								selectionModel.setSelected(availableTermsCellList.getVisibleItem(availableTermsSelectedIndex), true);
								break;
						
						case	KeyCodes.KEY_ENTER:
								if (availableTermsSelectedIndex != -1)
								{
									selectedTermsList.add(availableTermsBackendList.get(availableTermsSelectedIndex));
									populateSelectedTerms();
								}
								break;
					}
				}
			});
			
			availableTermsPanel.add(termTextBox);
			
			availableTermsScrollPanel = new ScrollPanel(availableTermsCellList);
			availableTermsScrollPanel.addStyleName("scrollable");
			availableTermsPanel.add(availableTermsScrollPanel);

			horizontalPanel.add(availableTermsPanel);
			
			b4oService.getNumberOfTerms(new AsyncCallback<Integer>() {
				@Override
				public void onSuccess(Integer result)
				{
					/* Fill the client store first */
					allTermsList = new ArrayList<LazyTerm>(result);
					for (int i=0;i<result;i++)
					{
						LazyTerm t = new LazyTerm();
						allTermsList.add(t);
					}

				    availableTermsBackendList = new ArrayList<LazyTerm>(allTermsList);

					long start = System.currentTimeMillis();

					availableTermsCellList.setRowData(availableTermsBackendList);
					availableTermsCellList.setRowCount(result,true);

					GWT.log((System.currentTimeMillis() - start) + " " + availableTermsBackendList.size());

					/* Update the first */
					updateTerms(Arrays.asList(0,1,2,3,4,5,6,7,8,9,10,11,12));
				}
	
				@Override
				public void onFailure(Throwable caught) { }
			});
			
			availableTermsScrollPanel.addScrollHandler(new ScrollHandler() {
				@Override
				public void onScroll(ScrollEvent event)
				{
					updateVisibleTerms();
				}
			});
		}
		
		{
			VerticalPanel selectedTermsPanel = new VerticalPanel();
			
			selectedTermsCellTable = new CellTable<LazyTerm>();
			selectedTermsCellTable.setHeight("220px");
			selectedTermsCellTable.setWidth("520px");
			selectedTermsCellTable.addColumn(new TextColumn<LazyTerm>()
					{
						@Override
						public String getValue(LazyTerm term)
						{
							if (term.term != null)
								return term.term.term;
							return "Unknown";
						}
					});

			selectedTermsScrollPanel = new ScrollPanel(selectedTermsPanel);
			selectedTermsScrollPanel.addStyleName("scrollable");
			selectedTermsScrollPanel.setWidget(selectedTermsCellTable);

			selectedTermsPanel.add(selectedTermsScrollPanel);
			
			horizontalPanel.add(selectedTermsPanel);
		}
	}
}
