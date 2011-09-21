package sonumina.b4orwt;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedList;
import java.util.List;

import ontologizer.go.Term;

import org.eclipse.rwt.lifecycle.IEntryPoint;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import sonumina.b4oweb.server.*;

public class B4ORWT implements IEntryPoint
{
    private String termFilterString = null;
    
    private Table availableTermsTable;
    
    private TableColumn selectedTermsNameColumn;
    private TableColumn selectedTermsRemoveColumn;
    private Table selectedTermsTable;
    private Browser browser;
    
    private LinkedList<Integer> selectedTermsList = new LinkedList<Integer>();
    private LinkedList<Button> selectedTermButtonList = new LinkedList<Button>();
    private LinkedList<TableEditor> selectedTermTableEditorList = new LinkedList<TableEditor>();

    /**
     * Updates the content of the term table.
     */
    private void updateAvailableTermsTable()
    {
	    availableTermsTable.setItemCount(B4OCore.getNumberTerms(termFilterString));
    	availableTermsTable.clearAll();
    	availableTermsTable.redraw();
    }
    
    /**
     * Updates the content of the selected terms table.
     */
    private void updateSelectedTermsTable()
    {
    	selectedTermsTable.removeAll();
    	for (Button b : selectedTermButtonList)
    		b.dispose();
    	for (TableEditor e : selectedTermTableEditorList)
    		e.dispose();
    	selectedTermButtonList.clear();
    	selectedTermTableEditorList.clear();

    	for (final Integer i : selectedTermsList)
    	{
    		TableItem item = new TableItem(selectedTermsTable,0);
    		item.setText(0,B4OCore.getTerm(i).getName());
    		
    		TableEditor editor = new TableEditor(selectedTermsTable);
    		Button button = new Button(selectedTermsTable,0);
    		button.setText("X");
    		button.addSelectionListener(new SelectionAdapter()
    		{
    			@Override
    			public void widgetSelected(SelectionEvent e)
    			{
    				selectedTermsList.removeFirstOccurrence(i);
    				updateSelectedTermsTable();
    			}
    		});
    		
    		button.pack();
    		editor.minimumWidth = button.getSize().x;
    		editor.minimumHeight = button.getSize().y;
    		editor.horizontalAlignment = SWT.RIGHT;
    		editor.setEditor(button, item, 1);
    		
    		selectedTermButtonList.add(button);
    		selectedTermTableEditorList.add(editor);
    	}
    	
    	selectedTermsTable.setItemCount(selectedTermsList.size());
    	
    	selectedTermsNameColumn.pack();
    	selectedTermsNameColumn.setWidth(selectedTermsNameColumn.getWidth() + 20);
    	selectedTermsRemoveColumn.pack();
    	selectedTermsTable.redraw();
    	
    	calculate();
    }
    
    private void calculate()
    {
    	List<ItemResultEntry> result = B4OCore.score(selectedTermsList);
    	StringBuilder str = new StringBuilder();
    	
    	for (ItemResultEntry e : result)
    	{
    		str.append(e.getItemId() + "</br>");
    	}
    	browser.setText(str.toString());
    }
    
	public int createUI()
	{       
	    Display display = new Display();
	    Shell shell = new Shell( display, 0 );
	    shell.setLayout(new FillLayout());
	    
	    SashForm verticalSash = new SashForm(shell, SWT.VERTICAL);
	    
	    SashForm horizontalSash = new SashForm(verticalSash, SWT.HORIZONTAL);
		
	    /* Available Terms */
	    Composite termComposite = new Composite(horizontalSash, 0);
	    termComposite.setLayout(new GridLayout());

	    final Text termFilterText = new Text(termComposite,SWT.BORDER);
	    termFilterText.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL|GridData.FILL_HORIZONTAL));
	    termFilterText.addModifyListener(new ModifyListener()
	    {
			@Override
			public void modifyText(ModifyEvent event)
			{
				termFilterString = termFilterText.getText();
				updateAvailableTermsTable();
			}
		});
	    
	    availableTermsTable = new Table(termComposite,SWT.VIRTUAL|SWT.BORDER);
	    availableTermsTable.setData( Table.ENABLE_RICH_TEXT, Boolean.TRUE ); /* RWT */
	    availableTermsTable.setData( Table.ITEM_HEIGHT, new Integer(20)); /* RWT */

	    availableTermsTable.addListener( SWT.SetData, new Listener()
	    {
	    	@Override
			public void handleEvent(Event event)
			{
				TableItem item = (TableItem)event.item;
				int index = event.index;
				Term t = B4OCore.getTerm(termFilterString, index);
				item.setText(t.getName());
			}
	    });
	    availableTermsTable.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL|GridData.GRAB_VERTICAL|GridData.FILL_BOTH));
	    availableTermsTable.addSelectionListener(new SelectionAdapter() {
	    	@Override
	    	public void widgetDefaultSelected(SelectionEvent e)
	    	{

	    		selectedTermsList.add(B4OCore.getIdOfTerm(B4OCore.getTerm(termFilterString, availableTermsTable.indexOf((TableItem)e.item))));
	    		updateSelectedTermsTable();
	    	}
		});
	    DragSource termTableDragSource = new DragSource(availableTermsTable,DND.DROP_COPY|DND.DROP_MOVE);
	    termTableDragSource.addDragListener(new DragSourceAdapter() {
			@Override
			public void dragStart(DragSourceEvent event) {
				
				DragSource ds = (DragSource)event.widget;
			    Table table = (Table) ds.getControl();
			    TableItem[] selection = table.getSelection();
				System.out.println(selection.length);
			    if (selection.length > 0)
			    {
			    	event.data = selection[0].getText();
			    }
			}
		});
	    termComposite.pack();
	    
	    /* Selected Terms */
	    Composite selectedTerms = new Composite(horizontalSash, 0);
	    selectedTerms.setLayout(new GridLayout());
	    selectedTermsTable = new Table(selectedTerms,SWT.BORDER);
	    selectedTermsNameColumn = new TableColumn(selectedTermsTable,0);
	    selectedTermsRemoveColumn = new TableColumn(selectedTermsTable,0);
	    selectedTermsTable.setData( Table.ENABLE_RICH_TEXT, Boolean.TRUE ); /* RWT */
	    selectedTermsTable.setData( Table.ITEM_HEIGHT, new Integer(20)); /* RWT */

/*	    selectedTermsTable.addListener(SWT.SetData, new Listener()
	    {
	    	@Override
	    	public void handleEvent(Event event)
	    	{
				TableItem item = (TableItem)event.item;
				int index = event.index;
				int tid = selectedTermsList.get(index);
				item.setText(0,B4OCore.getTerm(tid).getName());
	    	}
	    });*/
	    selectedTermsTable.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL|GridData.GRAB_VERTICAL|GridData.FILL_BOTH));
	    DropTarget selectedTermsTableDropTarget = new DropTarget(selectedTerms,DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_DEFAULT);
	    selectedTermsTableDropTarget.setTransfer(new Transfer[]{TextTransfer.getInstance()});
	    selectedTermsTableDropTarget.addDropListener(new DropTargetAdapter()
	    {
	    	
	    });
	    /* Result */
	    browser = new Browser(verticalSash, SWT.BORDER);
	    browser.setText("<b>Hallo</b>");

	    shell.setMaximized(true);
	    shell.layout();
	    updateAvailableTermsTable();
	    shell.open();
	    while( !shell.isDisposed() ) {
	      if( !display.readAndDispatch() )
	        display.sleep();
	    }
	    display.dispose();
	    return 0;
	}
}
