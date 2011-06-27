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
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.dom.client.ScrollEvent;
import com.google.gwt.event.dom.client.ScrollHandler;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.CellList;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
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
	private String availableTermsFilterString;
	
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
	private CellList<LazyTerm> availableTermsList;
	
	/**
	 * And to which it is embedded.
	 */
	private ScrollPanel availableTermsScrollPanel;
	
	/**
	 * Updates the terms currently visible within the scroll panel.
	 */
	private void updateVisibleTerms()
	{
		int ypos = availableTermsScrollPanel.getVerticalScrollPosition();
		int totalHeight = availableTermsScrollPanel.getElement().getScrollHeight();
		int visibleHeight = availableTermsList.getElement().getClientHeight();
		int numberOfElements = availableTermsList.getPageSize();

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
								availableTermsList.setRowData(t.requestId,availableTermsBackendList.subList(t.requestId, t.requestId+1));
							}
						}
					}
				});
	}
	
	/**
	 * Populates the term cell list with terms.
	 */
	private void populateTerms()
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

				availableTermsList.setRowData(availableTermsBackendList);
				availableTermsList.setRowCount(result,true);
				
				GWT.log((System.currentTimeMillis() - start) + " " + availableTermsBackendList.size());

//				start = System.currentTimeMillis();
//				ArrayList<Integer> test = new ArrayList<Integer>();
//				for (int i=0;i<10000;i++)
//					test.add(i);
//				GWT.log((System.currentTimeMillis() - start) + " building list");
//
//				start = System.currentTimeMillis();
//				for (int i=0;i<9000;i++)
//					test.remove(test.size() - 1);
//				GWT.log((System.currentTimeMillis() - start) + " shrinking list");
			}
		});

	}
	
	/**
	 * This is the entry point method.
	 */
	public void onModuleLoad()
	{
//		final Button sendButton = new Button("Send");
//		final TextBox nameField = new TextBox();
//		nameField.setText("GWT User");
//		final Label errorLabel = new Label();
//
//		// We can add style names to widgets
//		sendButton.addStyleName("sendButton");

		// Add the nameField and sendButton to the RootPanel
		// Use RootPanel.get() to get the entire body element
//		RootPanel.get("nameFieldContainer").add(nameField);
//		RootPanel.get("sendButtonContainer").add(sendButton);
//		RootPanel.get("errorLabelContainer").add(errorLabel);

		{
			VerticalPanel verticalPanel = new VerticalPanel();

			availableTermsList = new CellList<LazyTerm>(new LazyTermCell());
			availableTermsList.setHeight("200px");
			availableTermsList.setWidth("520px");
			
			final SelectionModel<LazyTerm> selectionModel = new SingleSelectionModel<LazyTerm>();
			availableTermsList.setSelectionModel(selectionModel);
			
			final TextBox termTextBox = new TextBox();
			termTextBox.setWidth("520px");
			termTextBox.addKeyUpHandler(new KeyUpHandler() {
				@Override
				public void onKeyUp(KeyUpEvent event)
				{
					if (event.isDownArrow())
					{
						availableTermsSelectedIndex++;
						selectionModel.setSelected(availableTermsList.getVisibleItem(availableTermsSelectedIndex), true);
					} else if (event.isUpArrow())
					{
						if (availableTermsSelectedIndex == -1)
							availableTermsSelectedIndex = availableTermsList.getRowCount();
						availableTermsSelectedIndex--;
						selectionModel.setSelected(availableTermsList.getVisibleItem(availableTermsSelectedIndex), true);
					} else
					{
						availableTermsFilterString = termTextBox.getText();
						populateTerms();
					}
				}
			});
			verticalPanel.add(termTextBox);
			
			availableTermsScrollPanel = new ScrollPanel(availableTermsList);
			availableTermsScrollPanel.addStyleName("scrollable");
			verticalPanel.add(availableTermsScrollPanel);
			
			RootPanel.get().add(verticalPanel);
			
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

					availableTermsList.setRowData(availableTermsBackendList);
					availableTermsList.setRowCount(result,true);

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

//		// Focus the cursor on the name field when the app loads
//		nameField.setFocus(true);
//		nameField.selectAll();
//
//		// Create the popup dialog box
//		final DialogBox dialogBox = new DialogBox();
//		dialogBox.setText("Remote Procedure Call");
//		dialogBox.setAnimationEnabled(true);
//		final Button closeButton = new Button("Close");
//		// We can set the id of a widget by accessing its Element
//		closeButton.getElement().setId("closeButton");
//		final Label textToServerLabel = new Label();
//		final HTML serverResponseLabel = new HTML();
//		VerticalPanel dialogVPanel = new VerticalPanel();
//		dialogVPanel.addStyleName("dialogVPanel");
//		dialogVPanel.add(new HTML("<b>Sending name to the server:</b>"));
//		dialogVPanel.add(textToServerLabel);
//		dialogVPanel.add(new HTML("<br><b>Server replies:</b>"));
//		dialogVPanel.add(serverResponseLabel);
//		dialogVPanel.setHorizontalAlignment(VerticalPanel.ALIGN_RIGHT);
//		dialogVPanel.add(closeButton);
//		dialogBox.setWidget(dialogVPanel);
//
//
//		// Add a handler to close the DialogBox
//		closeButton.addClickHandler(new ClickHandler() {
//			public void onClick(ClickEvent event) {
//				dialogBox.hide();
//				sendButton.setEnabled(true);
//				sendButton.setFocus(true);
//			}
//		});
//
//		// Create a handler for the sendButton and nameField
//		class MyHandler implements ClickHandler, KeyUpHandler {
//			/**
//			 * Fired when the user clicks on the sendButton.
//			 */
//			public void onClick(ClickEvent event) {
//				sendNameToServer();
//			}
//
//			/**
//			 * Fired when the user types in the nameField.
//			 */
//			public void onKeyUp(KeyUpEvent event) {
//				if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
//					sendNameToServer();
//				}
//			}
//
//			/**
//			 * Send the name from the nameField to the server and wait for a response.
//			 */
//			private void sendNameToServer() {
//				// First, we validate the input.
//				errorLabel.setText("");
//				String textToServer = nameField.getText();
//				if (!FieldVerifier.isValidName(textToServer)) {
//					errorLabel.setText("Please enter at least four characters");
//					return;
//				}
//
//				// Then, we send the input to the server.
//				sendButton.setEnabled(false);
//				textToServerLabel.setText(textToServer);
//				serverResponseLabel.setText("");
//			}
//		}
//
//		// Add a handler to send the name to the server
//		MyHandler handler = new MyHandler();
//		sendButton.addClickHandler(handler);
//		nameField.addKeyUpHandler(handler);
	}
}
