
package sonumina.b4orwt;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import ontologizer.go.Term;
import ontologizer.go.TermID;

import org.eclipse.rwt.lifecycle.IEntryPoint;
import org.eclipse.rwt.lifecycle.UICallBack;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
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
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.internal.widgets.ICellToolTipAdapter;
import org.eclipse.swt.internal.widgets.ICellToolTipProvider;
import org.eclipse.swt.internal.widgets.ITableAdapter;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;

import sonumina.b4orwt.TermDetails.ITermDetailsProvider;
import sonumina.b4oweb.server.core.*;

/**
 * Main entry point for the FABN application.
 * 
 * @author Sebastian Bauer
 */
public class B4ORWT implements IEntryPoint
{
	private Display display;

	/** String used to filter terms of the available table list */
    private String termFilterString = null;
    
    /** Table displaying all available terms */
    private Table availableTermsTable;
    
    /** GUI element displaying the term details */
    private TermDetails selectedTermDetails;
    
    private TermGraph selectedTermsGraph;
    
    private ScrolledComposite resultComposite;
    private ScrolledForm selelectedScrolledForm;
    private FormToolkit selectedTermsFormToolkit;

    static private class SelectedTerm
    {
    	int id;
    	boolean active;
    	public SelectedTerm(int id, boolean active)
    	{
    		this.id = id;
    		this.active = active;
    	}
    }

    private LinkedList<SelectedTerm> selectedTermsList = new LinkedList<SelectedTerm>();
    private LinkedList<Section> selectedTermSectionList = new LinkedList<Section>();

    /** Used for UICallback which is used to update the UI from threads */
    private int calculationCallbackId = 0;
    
    private String getUniqueCallbackId()
    {
    	return "callback" + calculationCallbackId++;
    }
    
    /**
     * Updates the content of the term table.
     * 
     * Spawns a thread to do the actual work.
     */
    private void updateAvailableTermsTable()
    {
    	TableItem [] ti = availableTermsTable.getSelection();

    	final String nameOfCuurentlySelectedTerm;
    	
    	if (ti != null && ti.length > 0) nameOfCuurentlySelectedTerm = ti[0].getText();
    	else  nameOfCuurentlySelectedTerm = null;

    	final String callbackId = getUniqueCallbackId(); 
    	UICallBack.activate(callbackId);

    	Thread t = new Thread(new Runnable()
    	{
    		@Override
    		public void run()
    		{
    	    	/* Represents the index of the previous selection in the within the new (filtered) list */
    	    	int indexOfPreviousSelection = -1;

    	    	int numberOfTerms = 0;
    			for (Term t : B4OCore.getTerms(termFilterString))
    			{
    				if (t.getName().equals(nameOfCuurentlySelectedTerm))
    					indexOfPreviousSelection = numberOfTerms;
    				numberOfTerms++;
    			}

    			final int prev = indexOfPreviousSelection;
    			final int num = numberOfTerms;

    			/* Update table */
    			display.asyncExec(new Runnable()
    			{
    				@Override
    				public void run()
    				{
    				    availableTermsTable.setItemCount(num);
    			    	availableTermsTable.clearAll();
    			    	availableTermsTable.redraw();
    			    	
    			    	if (prev != -1)
    			    		availableTermsTable.setSelection(prev);
    				}
    			});
    			
    	    	/* Deactive UI Callback */
    	    	display.asyncExec(new Runnable()
    	    	{
    	    		@Override
    	    		public void run() {
    	    			UICallBack.deactivate(callbackId);
    	    		}
    	    	});
    		}
    	});
    	t.setDaemon(true);
    	t.start();
    }
    
    /**
     * Updates the content of the selected terms table.
     */
    private void updateSelectedTermsTable()
    {
    	selelectedScrolledForm.setRedraw(false);

    	for (Section s: selectedTermSectionList)
    		s.dispose();
    	selectedTermSectionList.clear();

    	for (final SelectedTerm st : selectedTermsList)
    	{
    		final int i = st.id;

    		/* Section */
    		String def = B4OCore.getTerm(i).getDefinition();
    		
    		final Section s = selectedTermsFormToolkit.createSection(selelectedScrolledForm.getBody(), Section.TWISTIE|(def!=null?Section.DESCRIPTION:0)|Section.LEFT_TEXT_CLIENT_ALIGNMENT);
    		s.setLayoutData(new GridData(GridData.FILL_HORIZONTAL|GridData.GRAB_HORIZONTAL));
    		Composite tc = selectedTermsFormToolkit.createComposite(s);
    		GridLayout rl = new GridLayout(3,false);
    		rl.marginLeft = rl.marginRight = rl.marginTop = rl.marginBottom = 0;
    		tc.setLayout(rl);

    		/* Check mark */
    	    Button check = selectedTermsFormToolkit.createButton(tc, "", SWT.CHECK);
    	    check.setLayoutData(new GridData(GridData.FILL_HORIZONTAL|GridData.GRAB_HORIZONTAL));
    	    if (st.active) check.setSelection(st.active);
    	    check.addSelectionListener(new SelectionAdapter()
    	    {
    	    	@Override
    	    	public void widgetSelected(SelectionEvent e) {
    	    		st.active = !st.active;
    	    		calculate();
    	    	}
    	    });

    		/* Label of the section */
    		Label l = selectedTermsFormToolkit.createLabel(tc,B4OCore.getTerm(i).getName(), SWT.LEFT);
    		l.addMouseListener(new MouseAdapter()
    		{
				public void mouseUp(MouseEvent e) {	s.setExpanded(!s.isExpanded());	}
    		});

    		/* Remove Button */
    		Button rem = selectedTermsFormToolkit.createButton(tc, "X", 0);
    		rem.addSelectionListener(new SelectionAdapter()
    		{
    			@Override
    			public void widgetSelected(SelectionEvent e) {
    				selectedTermsList.removeFirstOccurrence(st);
    				updateSelectedTermsTable();
    			}
    		});
    		GridData cld = new GridData(GridData.FILL_HORIZONTAL|GridData.GRAB_HORIZONTAL);
    		rem.setLayoutData(cld);

    		s.setTextClient(tc);
    		if (def != null) s.setDescription(def);
    		
    		Composite c = selectedTermsFormToolkit.createComposite(s, 0);
    		c.setLayout(new GridLayout());
    		s.setClient(c);

    		selectedTermSectionList.add(s);
    	}
    	
    	selelectedScrolledForm.reflow(true);
    	selelectedScrolledForm.setRedraw(true);
    	
    	calculate();
    }
    
    /**
     * Performs the calculation and updates the result list.
     */
    @SuppressWarnings("serial")
	private void calculate()
    {
    	final ArrayList<Integer> clonedList = new ArrayList<Integer>(selectedTermsList.size());
    	for (SelectedTerm st : selectedTermsList)
    		if (st.active)
    			clonedList.add(st.id);

    	/* Activate UI Callback */
    	final String callbackId = getUniqueCallbackId(); 
    	UICallBack.activate(callbackId);
    	Thread t = new Thread(new Runnable()
    	{
    		@Override
    		public void run()
    		{
    	    	final List<ItemResultEntry> result = B4OCore.score(clonedList);

    	    	display.asyncExec(new Runnable() {
					@Override
					public void run() {
		    			FormToolkit toolkit = new FormToolkit(resultComposite.getDisplay());
		    			Form form = toolkit.createForm(resultComposite);
		    			form.setText("Results");
		    			form.getBody().setLayout(new GridLayout());
		    			
		    			int maxLabelWidth = 100;
		    			ArrayList<Label> labels = new ArrayList<Label>(40);

		    			int rank = 0;
		    	    	for (ItemResultEntry e : result)
		    	    	{
		    	    		int id = e.getItemId();
		    	    		String name = B4OCore.getItemName(id);

		    	    		/* Create a new section */
		    	    		final Section s = toolkit.createSection(form.getBody(), Section.TWISTIE);
		    	    		
		    	    		/* Text client for the section, which is displayed in the title */
		    	    		Composite tc = toolkit.createComposite(s);
		    	    		ResultLayout rl = new ResultLayout();
		    	    		rl.marginLeft = rl.marginRight = rl.marginTop = rl.marginBottom = 0;
		    	    		rl.center = true;
		    	    		tc.setLayout(rl);

		    	    		/* The item's label */
		    	    		Label l = toolkit.createLabel(tc,(rank + 1) + ". " + name, SWT.LEFT);
		    	    		l.addMouseListener(new MouseAdapter()
		    	    		{
		    					public void mouseUp(MouseEvent e) {	s.setExpanded(!s.isExpanded());	}
		    	    			
		    	    		});
		    	    		Point p = l.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		    	    		maxLabelWidth = Math.max(maxLabelWidth, p.x);
		    	    		labels.add(l);
		    	    		
		    	    		/* Percentage */
		    	    		ProgressBar pb = new ProgressBar(tc, SWT.HORIZONTAL);
		    	    		pb.setMaximum(100);
		    	    		pb.setSelection((int)(e.getScore() * 100));
		    	    		toolkit.createLabel(tc,(int)(e.getScore() * 100) + "%", SWT.RIGHT);

		    	    		s.setTextClient(tc);
		    	    		s.setExpanded(false);
		    	    		
		    	    		Composite c = toolkit.createComposite(s);
		    	    		c.setLayout(new GridLayout());
		    	    		
		    	    		/* Now fill the body of the section */
		    	    		for (int terms : B4OCore.getTermsDirectlyAnnotatedTo(id))
		    	    			toolkit.createLabel(c,B4OCore.getTerm(terms).getName());
		    	    		
		    	    		s.setClient(c);
		    	 
		    	    		rank++;
		    	    		if (rank >= 10 && e.getScore() < 0.001 || rank >= 30)
		    	    			break;
		    	    	}

		    	    	/* Now assign the max width to each label */
		    	    	for (Label l : labels)
		    	    		l.setLayoutData(new RowData(maxLabelWidth,SWT.DEFAULT));

		    	    	form.pack();
		    	    	Control oldContent = resultComposite.getContent();
		    	    	resultComposite.setContent(form);
		    	    	if (oldContent != null)
		    	    		oldContent.dispose();

					}
				});

    	    	/* Deactive UI Callback */
    	    	display.asyncExec(new Runnable()
    	    	{
    	    		@Override
    	    		public void run() {
    	    			UICallBack.deactivate(callbackId);
    	    		}
    	    	});
    		}
    	});
    	t.setDaemon(true);
    	t.start();
    }
    
	public int createUI()
	{       
		display = new Display();
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
			public void keyReleased(KeyEvent e) { }

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
	    availableTermsTable.setData( ICellToolTipProvider.ENABLE_CELL_TOOLTIP, Boolean.TRUE );

	    /* Set tooltip provider for the table */
	    final ICellToolTipAdapter tooltipAdapter = (ICellToolTipAdapter)availableTermsTable.getAdapter(ITableAdapter.class);
	    
	    tooltipAdapter.setCellToolTipProvider(new ICellToolTipProvider()
	    {
	    	@Override
	    	public void getToolTipText(Item item, int columnIndex) {
	    		String tooltip = (String)item.getData("#tooltip");
	    		if (tooltip != null)
	    			tooltipAdapter.setCellToolTipText(tooltip);
	    	}
	    });
	    
	    TableColumn nameColumn = new TableColumn(availableTermsTable, 0);
	    nameColumn.setResizable(true);
	    nameColumn.setWidth(320);
	    TableColumn idColumn = new TableColumn(availableTermsTable, 0);
	    idColumn.setResizable(true);
	    idColumn.setWidth(100);
	    TableColumn itemsColumn = new TableColumn(availableTermsTable, SWT.RIGHT);
	    itemsColumn.setWidth(40);

//	    availableTermsTable.setData( Table.ENABLE_RICH_TEXT, Boolean.TRUE ); /* RWT */
//	    availableTermsTable.setData( Table.ITEM_HEIGHT, new Integer(20)); /* RWT */

	    availableTermsTable.addListener( SWT.SetData, new Listener()
	    {
	    	@Override
			public void handleEvent(Event event)
			{
				TableItem item = (TableItem)event.item;
				int index = event.index;
				Term t = B4OCore.getTerm(termFilterString, index);
				if (t != null)
				{
					item.setText(0,t.getName());
					item.setText(1,t.getID().toString());
					item.setText(2,Integer.toString(B4OCore.getNumberOfTermsAnnotatedToTerm(B4OCore.getIdOfTerm(t))));
					item.setData("#tooltip", t.getDefinition());
				}
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
	    availableTermsTable.addMouseListener(new MouseAdapter() {
	    	@Override
	    	public void mouseDown(MouseEvent e) {
				int index = availableTermsTable.getSelectionIndex();
				Term t = B4OCore.getTerm(termFilterString, index);
				selectedTermDetails.setTermID(t.getID());
	    	}
		});
	    
	    DragSource termTableDragSource = new DragSource(availableTermsTable,DND.DROP_COPY|DND.DROP_MOVE);
	    termTableDragSource.addDragListener(new DragSourceAdapter() {
			@Override
			public void dragStart(DragSourceEvent event) {
				
				DragSource ds = (DragSource)event.widget;
			    Table table = (Table) ds.getControl();
			    TableItem[] selection = table.getSelection();
				System.out.println("Drag: " + selection.length);
			    if (selection.length > 0)
			    {
			    	event.data = selection[0].getText();
			    }
			}
		});
	    
	    selectedTermDetails = new TermDetails(termComposite, 0);
	    selectedTermDetails.setTermDetailsProvider(new ITermDetailsProvider() {
			
			public String getName(TermID term)
			{
				return B4OCore.getTerm(term).getName();
			}
			
			public String getDescription(TermID term)
			{
				return B4OCore.getTerm(term).getDefinition();
			}
		});
	    selectedTermDetails.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

	    
	    termComposite.pack();
	    
	    /* Selected Terms */
	    CTabFolder tabFolder = new CTabFolder(horizontalSash, 0);

	    CTabItem selectedTermsItem = new CTabItem(tabFolder, 0);
	    selectedTermsItem.setText("Textual");

	    CTabItem selectedTermsGraphicalItem = new CTabItem(tabFolder, 0);
	    selectedTermsGraphicalItem.setText("Graphical");

	    /* Graphical */
	    selectedTermsGraph = new TermGraph(tabFolder, 0);
	    selectedTermsGraphicalItem.setControl(selectedTermsGraph);

	    /* Textual */
	    Composite selectedTerms = new Composite(tabFolder, 0);
	    selectedTermsItem.setControl(selectedTerms);
	    selectedTerms.setLayout(new GridLayout());

	    DropTarget selectedTermsTableDropTarget = new DropTarget(selectedTerms,DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_DEFAULT);
	    selectedTermsTableDropTarget.setTransfer(new Transfer[]{TextTransfer.getInstance()});
	    selectedTermsTableDropTarget.addDropListener(new DropTargetAdapter()
	    {
	    	
	    });

	    selectedTermsFormToolkit = new FormToolkit(selectedTerms.getDisplay());
	    selelectedScrolledForm = selectedTermsFormToolkit.createScrolledForm(selectedTerms);
	    selelectedScrolledForm.setLayoutData(new GridData(GridData.FILL_HORIZONTAL|GridData.FILL_VERTICAL|GridData.GRAB_HORIZONTAL));
	    selelectedScrolledForm.setText("Selected Terms");
	    selelectedScrolledForm.getBody().setLayout(new GridLayout());

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
		selectedTermsList.add(new SelectedTerm(B4OCore.getIdOfTerm(B4OCore.getTerm(termFilterString, availableTermsTable.getSelectionIndex())),true));
		updateSelectedTermsTable();
	}
}
