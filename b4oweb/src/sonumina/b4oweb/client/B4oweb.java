package sonumina.b4oweb.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import sonumina.b4oweb.shared.FieldVerifier;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.dom.client.ScrollEvent;
import com.google.gwt.event.dom.client.ScrollHandler;
import com.google.gwt.safehtml.shared.SafeHtml;
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
	public boolean loaded;
	public int id;
	public String text;
}

class LazyTermCell extends AbstractCell<LazyTerm>
{

	@Override
	public void render(Context context, LazyTerm value, SafeHtmlBuilder sb)
	{
		sb.appendHtmlConstant(value.text);
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
	 * Our term storage.
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
		
		int first = ypos / rowHeight;
		int visible = visibleHeight / rowHeight + 1;

		for (int i=first;i < first + visible && i < termsList.size();i++)
		{
			GWT.log(termsList.size() + " ");
			termsList.get(i).loaded = true;
			termsList.get(i).text = i + "w";
		}

		cellList.setRowData(first,termsList.subList(first, Math.min(first + visible,termsList.size())));
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
						t.text = "Unknown term " + i;
						termsList.add(t);
					}

				    termsCellList = new ArrayList<LazyTerm>(termsList);

					cellList.setRowData(termsCellList);
					cellList.setRowCount(result,true);
				}
	
				@Override
				public void onFailure(Throwable caught)
				{
				}
			});
			
			scrollPanel.addScrollHandler(new ScrollHandler() {
				@Override
				public void onScroll(ScrollEvent event)
				{
					updateTermsList();
				}
			});
			
			updateTermsList();
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

		b4oService.getTest(new AsyncCallback<String>() {
			@Override
			public void onSuccess(String result)
			{
			}
			@Override
			public void onFailure(Throwable caught)
			{
			}
		});
		

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
