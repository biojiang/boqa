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
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

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
	 * Create a remote service proxy to talk to the server-side Greeting service.
	 */
	private final B4OServiceAsync b4oService = GWT.create(B4OService.class);

	/**
	 * Our local term storage.
	 */
	private List<LazyTerm> termsList = new ArrayList<LazyTerm>();
	
	/**
	 * The strings of terms that are currently displayed.
	 */
	private List<LazyTerm> termsCellList;
	
	private CellList<LazyTerm> cellList;
	private ScrollPanel scrollPanel;
	
	
	/**
	 * Updates the given term list.
	 */
	private void updateTermsList()
	{
		int ypos = scrollPanel.getVerticalScrollPosition();
		int totalHeight = scrollPanel.getElement().getScrollHeight();
		int visibleHeight = cellList.getElement().getClientHeight();
		int numberOfElements = cellList.getPageSize();

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

		for (int i=first;i < first + visible && i < termsList.size();i++)
		{
			LazyTerm t = termsList.get(i);
			if (t.isNotLoaded())
			{
				t.setLoadingFlag(LazyTerm.LoadingState.STATE_LOADING);
				toBeLoaded.add(i);
			}
		}
		
		if (toBeLoaded.size() > 0)
			updateTerms(toBeLoaded);
	}
	
	private void updateTerms(List<Integer> toBeLoaded)
	{
		b4oService.getNamesOfTerms(toBeLoaded, new AsyncCallback<SharedTerm[]>()
				{
					@Override
					public void onFailure(Throwable caught) { GWT.log("Error", caught);};

					@Override
					public void onSuccess(SharedTerm [] result)
					{
						for (SharedTerm t : result)
						{
							GWT.log(t.serverId + " " + t.term);
							LazyTerm lz = termsList.get(t.serverId);
							if (lz != null)
							{
								lz.term = t;
								cellList.setRowData(t.serverId,termsList.subList(t.serverId, t.serverId+1));
							}
						}
					}
				});
	}
	
	/**
	 * This is the entry point method.
	 */
	public void onModuleLoad()
	{
		final Button sendButton = new Button("Send");
		final TextBox nameField = new TextBox();
		nameField.setText("GWT User");
		final Label errorLabel = new Label();

		// We can add style names to widgets
		sendButton.addStyleName("sendButton");

		// Add the nameField and sendButton to the RootPanel
		// Use RootPanel.get() to get the entire body element
		RootPanel.get("nameFieldContainer").add(nameField);
		RootPanel.get("sendButtonContainer").add(sendButton);
		RootPanel.get("errorLabelContainer").add(errorLabel);

		{
			VerticalPanel verticalPanel = new VerticalPanel();

			cellList = new CellList<LazyTerm>(new LazyTermCell());
			cellList.setHeight("200px");
			cellList.setWidth("500px");

			scrollPanel = new ScrollPanel(cellList);

			verticalPanel.add(scrollPanel);
			RootPanel.get().add(verticalPanel);
			
			b4oService.getNumberOfTerms(new AsyncCallback<Integer>() {
				@Override
				public void onSuccess(Integer result)
				{
					/* Fill the client store first */
					termsList = new ArrayList<LazyTerm>(result);
					for (int i=0;i<result;i++)
					{
						LazyTerm t = new LazyTerm();
						termsList.add(t);
					}

				    termsCellList = new ArrayList<LazyTerm>(termsList);

					cellList.setRowData(termsCellList);
					cellList.setRowCount(result,true);

					/* Update the first */
					updateTerms(Arrays.asList(0,1,2,3,4,5,6,7,8,9,10,11,12));
				}
	
				@Override
				public void onFailure(Throwable caught) { }
			});
			
			scrollPanel.addScrollHandler(new ScrollHandler() {
				@Override
				public void onScroll(ScrollEvent event)
				{
					updateTermsList();
				}
			});
		}
		

		// Focus the cursor on the name field when the app loads
		nameField.setFocus(true);
		nameField.selectAll();

		// Create the popup dialog box
		final DialogBox dialogBox = new DialogBox();
		dialogBox.setText("Remote Procedure Call");
		dialogBox.setAnimationEnabled(true);
		final Button closeButton = new Button("Close");
		// We can set the id of a widget by accessing its Element
		closeButton.getElement().setId("closeButton");
		final Label textToServerLabel = new Label();
		final HTML serverResponseLabel = new HTML();
		VerticalPanel dialogVPanel = new VerticalPanel();
		dialogVPanel.addStyleName("dialogVPanel");
		dialogVPanel.add(new HTML("<b>Sending name to the server:</b>"));
		dialogVPanel.add(textToServerLabel);
		dialogVPanel.add(new HTML("<br><b>Server replies:</b>"));
		dialogVPanel.add(serverResponseLabel);
		dialogVPanel.setHorizontalAlignment(VerticalPanel.ALIGN_RIGHT);
		dialogVPanel.add(closeButton);
		dialogBox.setWidget(dialogVPanel);


		// Add a handler to close the DialogBox
		closeButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				dialogBox.hide();
				sendButton.setEnabled(true);
				sendButton.setFocus(true);
			}
		});

		// Create a handler for the sendButton and nameField
		class MyHandler implements ClickHandler, KeyUpHandler {
			/**
			 * Fired when the user clicks on the sendButton.
			 */
			public void onClick(ClickEvent event) {
				sendNameToServer();
			}

			/**
			 * Fired when the user types in the nameField.
			 */
			public void onKeyUp(KeyUpEvent event) {
				if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
					sendNameToServer();
				}
			}

			/**
			 * Send the name from the nameField to the server and wait for a response.
			 */
			private void sendNameToServer() {
				// First, we validate the input.
				errorLabel.setText("");
				String textToServer = nameField.getText();
				if (!FieldVerifier.isValidName(textToServer)) {
					errorLabel.setText("Please enter at least four characters");
					return;
				}

				// Then, we send the input to the server.
				sendButton.setEnabled(false);
				textToServerLabel.setText(textToServer);
				serverResponseLabel.setText("");
			}
		}

		// Add a handler to send the name to the server
		MyHandler handler = new MyHandler();
		sendButton.addClickHandler(handler);
		nameField.addKeyUpHandler(handler);
	}
}
