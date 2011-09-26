package sonumina.b4orwt;

import java.util.LinkedList;
import java.util.List;

import ontologizer.go.Term;

import org.eclipse.rwt.lifecycle.IEntryPoint;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import sonumina.b4oweb.server.core.*;

public class B4ORWT implements IEntryPoint
{
    private String termFilterString = null;
    
    private Table availableTermsTable;
    
    private TableColumn selectedTermsNameColumn;
    private TableColumn selectedTermsRemoveColumn;
    private Table selectedTermsTable;
    private ScrolledComposite resultComposite;
    
    private LinkedList<Integer> selectedTermsList = new LinkedList<Integer>();
    private LinkedList<Button> selectedTermButtonList = new LinkedList<Button>();
    private LinkedList<TableEditor> selectedTermTableEditorList = new LinkedList<TableEditor>();

    /**
     * Updates the content of the term table.
     */
    private void updateAvailableTermsTable()
    {
    	/* Represents the index of the previous selection in the within the new (filtered) list */
    	int indexOfPreviousSelection = -1;
    	TableItem [] ti = availableTermsTable.getSelection();
    	if (ti != null && ti.length > 0)
    	{
    		String name = ti[0].getText();
    		if (name != null)
    		{
    			for (Term t : B4OCore.getTerms(termFilterString))
    			{
    				indexOfPreviousSelection++;
    				if (t.getName().equals(name))
    					break;
    			}
    		}
    	}

    	/* Update table */
	    availableTermsTable.setItemCount(B4OCore.getNumberTerms(termFilterString));
    	availableTermsTable.clearAll();
    	availableTermsTable.redraw();
    	
    	if (indexOfPreviousSelection != -1)
    		availableTermsTable.setSelection(indexOfPreviousSelection);
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
    	int rank;
    	List<ItemResultEntry> result = B4OCore.score(selectedTermsList);
    	StringBuilder str = new StringBuilder();

    	Composite comp = new Composite(resultComposite,0);
    	GridLayout gl = new GridLayout();
    	gl.numColumns = 4;
    	gl.marginLeft = gl.marginRight = 0;
    	gl.marginTop = gl.marginBottom = 0;
    	comp.setLayout(gl);
    	comp.setLayoutData(new GridData(GridData.FILL_BOTH));
    	
    	rank = 0;
    	for (ItemResultEntry e : result)
    	{
    		int id = e.getItemId();
    		String name = B4OCore.getItemName(id);
    		
    		Label pos = new Label(comp,0);
    		pos.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    		pos.setText((rank + 1) + ". ");
    		pos.setAlignment(SWT.RIGHT);
    		
    		Label l = new Label(comp,0);
    		l.setText(name);
    		
    		ProgressBar pb = new ProgressBar(comp, SWT.HORIZONTAL);
    		pb.setMaximum(100);
    		pb.setSelection((int)(e.getScore() * 100));
    		
    		Label percentLabel = new Label(comp, 0);
    		percentLabel.setAlignment(SWT.RIGHT);
    		percentLabel.setText((int)(e.getScore() * 100) + "%");
    		
    		rank++;
    		if (rank >= 30)
    			break;
    	}
    	
    	comp.pack();
    	Control oldContent = resultComposite.getContent();
    	resultComposite.setContent(comp);
    	if (oldContent != null)
    		oldContent.dispose();
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
	    termFilterText.addSelectionListener(new SelectionAdapter() {
	    	@Override
	    	public void widgetDefaultSelected(SelectionEvent e)
	    	{
	    		addSelectedTermToSelectedTerms();
	    	}
		});
	    termFilterText.addKeyListener(new KeyListener() {
			
			@Override
			public void keyReleased(KeyEvent e) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void keyPressed(KeyEvent e)
			{
				int idx;
				
				switch (e.keyCode)
				{
				case	SWT.ARROW_DOWN:
						idx = availableTermsTable.getSelectionIndex();
						availableTermsTable.setSelection(idx+1);
						break;

				case	SWT.ARROW_UP:
						idx = availableTermsTable.getSelectionIndex();
						availableTermsTable.setSelection(idx-1);
						break;
				}
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
	    		addSelectedTermToSelectedTerms();
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

	    selectedTermsTable.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL|GridData.GRAB_VERTICAL|GridData.FILL_BOTH));
	    DropTarget selectedTermsTableDropTarget = new DropTarget(selectedTerms,DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_DEFAULT);
	    selectedTermsTableDropTarget.setTransfer(new Transfer[]{TextTransfer.getInstance()});
	    selectedTermsTableDropTarget.addDropListener(new DropTargetAdapter()
	    {
	    	
	    });

	    /* Result */
	    Composite tempComp = new Composite(verticalSash, 0);
	    tempComp.setLayout(new GridLayout());
	    
	    resultComposite = new ScrolledComposite(tempComp,SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
	    resultComposite.setLayoutData(new GridData(GridData.FILL_BOTH));

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


	/**
	 * Adds the term selecetd in the available terms list to
	 * the list of selected terms.
	 */
	private void addSelectedTermToSelectedTerms()
	{
		selectedTermsList.add(B4OCore.getIdOfTerm(B4OCore.getTerm(termFilterString, availableTermsTable.getSelectionIndex())));
		updateSelectedTermsTable();
	}
}
