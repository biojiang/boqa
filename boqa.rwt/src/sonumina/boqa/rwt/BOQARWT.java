package sonumina.boqa.rwt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import ontologizer.go.DescriptionParser;
import ontologizer.go.Term;
import ontologizer.go.TermID;

import org.eclipse.rap.rwt.application.AbstractEntryPoint;
import org.eclipse.rap.rwt.service.ServerPushSession;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.internal.widgets.ICellToolTipAdapter;
import org.eclipse.swt.internal.widgets.ICellToolTipProvider;
import org.eclipse.swt.internal.widgets.ITableAdapter;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.events.IExpansionListener;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;

import sonumina.b4oweb.server.core.BOQACore;
import sonumina.b4oweb.server.core.ItemResultEntry;
import sonumina.boqa.rwt.TermDetails.ITermDetailsProvider;
import sonumina.math.graph.DirectedGraph;
import sonumina.math.graph.Edge;

/**
 * Main entry point for the FABN application.
 * 
 * @author Sebastian Bauer
 */
public class BOQARWT extends AbstractEntryPoint
{
	static BOQACore boqaCore;
	
	static
	{
		boqaCore = new BOQACore("/home/sba/work/boqa/data/human-phenotype-ontology.obo.gz","/home/sba/work/boqa/data/phenotype_annotation.omim.gz");
	}
	
	private Display display;

	/** String used to filter terms of the available table list */
    private String termFilterString = null;
    
    /** Table displaying all available terms */
    private Table availableTermsTable;
    
    /** Contains visible terms */
    private ArrayList<Term> availableVisibleTermsList;

    /** Maps a term to its actual position in the term list */
    private HashMap<Term,Integer> availableVisiblePos2SortedIndex;

    /** GUI element displaying the term details */
    private TermDetails selectedTermDetails;
    
    /** The graph of the selected terms */
    private TermGraph<Integer> selectedTermsGraph;
    
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

    /**
     * A simple label provider for terms. 
     * 
     * @author Sebastian Bauer
     */
    class TermLabelProvider implements TermGraph.ILabelProvider<Integer>
    {
			@Override
			public String getLabel(Integer t) { return boqaCore.getTerm(t).getName(); }
			@Override
			public String getTooltip(Integer t) { return boqaCore.getTerm(t).getDefinition()!=null?DescriptionParser.parse(boqaCore.getTerm(t).getDefinition()):null; }
			@Override
			public String getVariant(Integer t) { return null; }
    };
    
    /**
     * Common listener that can be used in a term graph widget. It updates the
     * state of the available term list according to the pressed term button within
     * the graph.
     */
    private SelectionListener termButtonSelectionListener = new SelectionAdapter() {
    	    	@Override
    	    	public void widgetSelected(SelectionEvent e) {
    	    		if (e.data != null && e.data instanceof Integer)
    	    		{
    	    			setSelectionOfAvailableTermListToTerm((Integer)e.data);
    	    		}
    	    	}
    		};
    
    private LinkedList<SelectedTerm> selectedTermsList = new LinkedList<SelectedTerm>();
    private LinkedList<Section> selectedTermSectionList = new LinkedList<Section>();

    /** Used for UICallback which is used to update the UI from threads */
    private int calculationCallbackId = 0;
    
    /**
     * Returns a unique id that can be used for UICallback.
     * 
     * @return
     */
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

    	final String nameOfCurrentlySelectedTerm;
    	
    	if (ti != null && ti.length > 0) nameOfCurrentlySelectedTerm = ti[0].getText();
    	else  nameOfCurrentlySelectedTerm = null;

    	final String callbackId = getUniqueCallbackId(); 
//    	UICallBack.activate(callbackId);
    	final ServerPushSession sps = new ServerPushSession();

    	Thread t = new Thread(new Runnable()
    	{
    		@Override
    		public void run()
    		{
    	    	/* Represents the index of the previous selection in the within the new (filtered) list */
    	    	int indexOfPreviousSelection = -1;

    	    	final ArrayList<Term> visibleTerms = new ArrayList<Term>();

    	    	int numberOfTerms = 0;
    			for (Term t : boqaCore.getTerms(termFilterString))
    			{
    				if (t.getName().equals(nameOfCurrentlySelectedTerm))
    					indexOfPreviousSelection = numberOfTerms;
    				numberOfTerms++;
    				visibleTerms.add(t);
    			}

    	    	final HashMap<Term,Integer> term2SortedIdx = new HashMap<Term,Integer>();
    	    	for (int i=0;i<numberOfTerms;i++)
    	    		term2SortedIdx.put(visibleTerms.get(i), i);
    			
    			final int prev = indexOfPreviousSelection;

    			/* Update table */
    			display.asyncExec(new Runnable()
    			{
    				@Override
    				public void run()
    				{
    					/* Finally, update data */
    					availableVisibleTermsList = visibleTerms;
    			    	availableVisiblePos2SortedIndex = term2SortedIdx;

    				    availableTermsTable.setItemCount(visibleTerms.size());
    			    	availableTermsTable.clearAll();
    			    	availableTermsTable.redraw();
    			    	
    			    	if (prev != -1)
    			    		availableTermsTable.setSelection(prev);
    				}
    			});

    			sps.stop();
//    	    	/* Deactivate UI Callback */
//    	    	display.asyncExec(new Runnable()
//    	    	{
//    	    		@Override
//    	    		public void run() {
//    	    			UICallBack.deactivate(callbackId);
//    	    		}
//    	    	});
    		}
    	});
    	sps.start();
    	t.setDaemon(true);
    	t.start();
    }
    
    /**
     * Updates the content of the selected terms table.
     */
    private void updateSelectedTermsTable()
    {
    	selelectedScrolledForm.setRedraw(false);
    	HashSet<Integer> selectedTermIds = new HashSet<Integer>();

    	for (Section s: selectedTermSectionList)
    		s.dispose();
    	selectedTermSectionList.clear();

    	for (final SelectedTerm st : selectedTermsList)
    	{
    		final int i = st.id;
    		
    		selectedTermIds.add(i);

    		/* Section */
    		String def = boqaCore.getTerm(i).getDefinition();
    		
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
    		Label l = selectedTermsFormToolkit.createLabel(tc,boqaCore.getTerm(i).getName(), SWT.LEFT);
    		l.addMouseListener(new MouseAdapter()
    		{
				public void mouseUp(MouseEvent e) {	s.setExpanded(!s.isExpanded()); }
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
    	
    	/* Now the graph */
    	DirectedGraph<Integer> graph = new DirectedGraph<Integer>();
		addInducedSubGraphToGraph(selectedTermIds, graph);
		selectedTermsGraph.setGraph(graph);
    	calculate();
    }

    /**
     * Add the sub graph induced by the given term ids to the graph.
     * 
     * @param termIds
     * @param graph
     */
	private void addInducedSubGraphToGraph(Collection<Integer> termIds, final DirectedGraph<Integer> graph)
	{
		boqaCore.visitAncestors(termIds,new BOQACore.IAncestorVisitor()
		{
			public void visit(int t) { 	graph.addVertex(t); }
		});
		
		for (Integer v : graph)
		{
			int [] parents = boqaCore.getParents(v);
			for (int p : parents)
			graph.addEdge(new Edge<Integer>(p,v));
		}
	}

    /**
     * Add the sub graph induced by the given term ids to the graph.
     * 
     * @param termIds
     * @param graph
     */
	private void addInducedSubGraphToGraph(int [] termIds, final DirectedGraph<Integer> graph)
	{
		ArrayList<Integer> list = toIntegerArray(termIds);
		addInducedSubGraphToGraph(list, graph);
	}

	/**
	 * Primitive to convert an array of ints to an ArrayList of Integers.
	 *  
	 * @param array
	 * @return
	 */
	private static ArrayList<Integer> toIntegerArray(int[] array)
	{
		ArrayList<Integer> list = new ArrayList<Integer>(array.length);
		for (int e : array)
			list.add(e);
		return list;
	}
	
    /**
     * Performs the calculation and updates the result list.
     */
    @SuppressWarnings("serial")
	private void calculate()
    {
    	/* Only used for debugging */
    	final boolean MULTITHREADING = true;

    	final ArrayList<Integer> clonedList = new ArrayList<Integer>(selectedTermsList.size());
    	for (SelectedTerm st : selectedTermsList)
    		if (st.active)
    			clonedList.add(st.id);

    	/* Activate UI Callback */
    	final String callbackId = getUniqueCallbackId();
    	final ServerPushSession sps = new ServerPushSession();
//    	UICallBack.activate(callbackId);

    	Runnable threadRunnable = new Runnable()
    	{
    		@Override
    		public void run()
    		{
    	    	final List<ItemResultEntry> result = boqaCore.score(clonedList, MULTITHREADING);

    	    	display.asyncExec(new Runnable() {
					@Override
					public void run() {
		    			FormToolkit toolkit = new FormToolkit(resultComposite.getDisplay());
		    			final Form form = toolkit.createForm(resultComposite);
		    			form.setText("Results");
		    			form.getBody().setLayout(new GridLayout());
		    			
		    			int maxLabelWidth = 100;
		    			ArrayList<Label> labels = new ArrayList<Label>(40);

		    			int rank = 0;
		    	    	for (ItemResultEntry e : result)
		    	    	{
		    	    		int id = e.getItemId();
		    	    		String name = boqaCore.getItemName(id);

		    	    		/* Find out which nodes to display in the graph display */
		    	    	    final HashSet<Integer> queryTerms = new HashSet<Integer>();
		    	    	    final HashSet<Integer> itemTerms = new HashSet<Integer>();
		    	        	final DirectedGraph<Integer> graph = new DirectedGraph<Integer>();
		    	    		addInducedSubGraphToGraph(clonedList, graph);
		    	    		for (Integer tid : graph) /* Keep query terms */
		    	    			queryTerms.add(tid);
		    	    		addInducedSubGraphToGraph(boqaCore.getTermsDirectlyAnnotatedTo(id), graph);
		    	    		boqaCore.visitAncestors(toIntegerArray(boqaCore.getTermsDirectlyAnnotatedTo(id)),new BOQACore.IAncestorVisitor()
		    	    		{
		    	    			public void visit(int t) { 	itemTerms.add(t); }
		    	    		});

		    	    		/* Create a new section */
		    	    		final Section s = toolkit.createSection(form.getBody(), Section.TWISTIE|Section.LEFT_TEXT_CLIENT_ALIGNMENT);
		    	    		
		    	    		/* Text client for the section, which is displayed in the title */
		    	    		Composite tc = toolkit.createComposite(s);
		    	    		ResultLayout rl = new ResultLayout();
		    	    		rl.marginLeft = rl.marginRight = rl.marginTop = rl.marginBottom = 0;
		    	    		rl.center = true;
		    	    		tc.setLayout(rl);

		    	    		/* The item's label */
		    	    		Label l = toolkit.createLabel(tc,(rank + 1) + ". " + name, SWT.LEFT);
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
		    	    		
		    	    		final Composite c = toolkit.createComposite(s);
		    	    		c.setLayout(new GridLayout());

		    	    		s.setClient(c);

		    	    		/* Create a runnable that initialize the term graph for the given
		    	    		 * section if not already done. We use this construct to avoid code
		    	    		 * duplication as this is needed twice.
		    	    		 */
		    	    		final Runnable initializeTermGraphRunnable = new Runnable() {
								@Override
								public void run() {
									if (s.getData("#initialized") == null)
									{
					    	    		TermGraph<Integer> tg = createTermGraph(c, queryTerms, itemTerms, graph);
					    	    		tg.addSelectionListener(termButtonSelectionListener);
										s.setData("#initialized",Boolean.TRUE);
									}
								}
							};

							/* Add the expansion listener */
		    	    		s.addExpansionListener(new IExpansionListener() {
								public void expansionStateChanging(ExpansionEvent e) { initializeTermGraphRunnable.run(); }
								
								public void expansionStateChanged(ExpansionEvent e) { form.pack(); }
							});

		    	    		/* And the mouse listener */
		    	    		l.addMouseListener(new MouseAdapter()
		    	    		{
		    					public void mouseUp(MouseEvent e)
		    					{
		    						initializeTermGraphRunnable.run();
		    						s.setExpanded(!s.isExpanded());
		    						form.pack();
		    					}
		    	    			
		    	    		});

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

    	    	/* Deactivate UI Callback */
//    	    	display.asyncExec(new Runnable()
//    	    	{
//    	    		@Override
//    	    		public void run() {
//    	    			UICallBack.deactivate(callbackId);
//    	    		}
//				});
    	    	if (MULTITHREADING)
    	    		sps.stop();
    		}
    	};
    	
    	if (MULTITHREADING)
    	{
    		Thread t = new Thread(threadRunnable);
    		sps.start();
    		t.setDaemon(true);
    		t.start();
    	} else threadRunnable.run();
    }
    
    
    @Override
    protected void createContents(Composite parent)
    {
    	/* Ensure that the proper java script files are loaded */
    	BOQARWTConfiguration.addRequiredJS();

    	display = parent.getDisplay();

    	parent.setLayout(new FillLayout());

	    SashForm verticalSash = new SashForm(parent, SWT.VERTICAL);
	    
	    SashForm horizontalSash = new SashForm(verticalSash, SWT.HORIZONTAL);
		
	    /* Available Terms */
	    Composite termComposite = new Composite(horizontalSash, 0);
	    termComposite.setLayout(new GridLayout());

	    final Text termFilterText = new Text(termComposite,SWT.BORDER|SWT.SEARCH|SWT.ICON_CANCEL);
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
	    
	    final TableColumn nameColumn = new TableColumn(availableTermsTable, 0);
	    nameColumn.setResizable(true);
	    nameColumn.setWidth(320);
	    nameColumn.setText("Name");
	    final TableColumn idColumn = new TableColumn(availableTermsTable, 0);
	    idColumn.setResizable(true);
	    idColumn.setWidth(100);
	    idColumn.setText("Id");
	    final TableColumn itemsColumn = new TableColumn(availableTermsTable, SWT.RIGHT);
	    itemsColumn.setText("#Items");
	    itemsColumn.pack();
	    itemsColumn.setWidth(itemsColumn.getWidth() + 6);
	    if (itemsColumn.getWidth() < 50)
	    	itemsColumn.setWidth(50);

//	    availableTermsTable.setData( Table.ENABLE_RICH_TEXT, Boolean.TRUE ); /* RWT */
//	    availableTermsTable.setData( Table.ITEM_HEIGHT, new Integer(20)); /* RWT */

	    availableTermsTable.setHeaderVisible(true);
	    availableTermsTable.addControlListener(new ControlAdapter() {
	    	@Override
	    	public void controlResized(ControlEvent e) {
	    		int width = availableTermsTable.getClientArea().width;
	    		int newWidth = width - idColumn.getWidth() - itemsColumn.getWidth();
	    		nameColumn.setWidth(newWidth);
	    	}
		});
	    availableTermsTable.addListener( SWT.SetData, new Listener()
	    {
	    	@Override
			public void handleEvent(Event event)
			{
				TableItem item = (TableItem)event.item;
				int index = event.index;
				Term t;
				
				if (availableVisibleTermsList != null)
					t = availableVisibleTermsList.get(index);
				else
					t = boqaCore.getTerm(termFilterString, index);
				if (t != null)
				{
					item.setText(0,t.getName());
					item.setText(1,t.getID().toString());
					item.setText(2,Integer.toString(boqaCore.getNumberOfTermsAnnotatedToTerm(boqaCore.getIdOfTerm(t))));
					if (t.getDefinition() != null)
						item.setData("#tooltip", DescriptionParser.parse(t.getDefinition()));
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
				if (index >= 0)
				{
					Term t = boqaCore.getTerm(termFilterString, index);
					selectedTermDetails.setTermID(t.getID());
				}
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
				return boqaCore.getTerm(term).getName();
			}
			
			public String getDescription(TermID term)
			{
				return boqaCore.getTerm(term).getDefinition();
			}
		});
	    selectedTermDetails.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

	    
	    termComposite.pack();
	    
	    /* Selected Terms */
	    Composite tabComposite = new Composite(horizontalSash,0); /* Dummy composite for the margins */
	    tabComposite.setLayout(new GridLayout());
	    
	    TabFolder tabFolder = new TabFolder(tabComposite, 0);
	    tabFolder.setLayoutData(new GridData(GridData.FILL_BOTH));
	    TabItem selectedTermsItem = new TabItem(tabFolder, 0);
	    selectedTermsItem.setText("Textual");
	    TabItem selectedTermsGraphicalItem = new TabItem(tabFolder, 0);
	    selectedTermsGraphicalItem.setText("Graphical");

	    /* Graphical */
	    Composite dummyComposite = new Composite(tabFolder, 0);
	    dummyComposite.setLayout(new FillLayout());

	    selectedTermsGraph = new TermGraph<Integer>(dummyComposite, 0);
	    selectedTermsGraph.setLabelProvider(new TermLabelProvider());
	    selectedTermsGraph.addSelectionListener(termButtonSelectionListener);
	    selectedTermsGraphicalItem.setControl(dummyComposite);

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
	    resultComposite.setLayout(new FillLayout());

//	    shell.setMaximized(true);
//	    shell.layout();
	    updateAvailableTermsTable();
//	    shell.open();
//	    while( !shell.isDisposed() ) {
//	      if( !display.readAndDispatch() )
//	        display.sleep();
//	    }
//	    display.dispose();
//	    return 0;
	}


	/**
	 * Adds the term selecetd in the available terms list to
	 * the list of selected terms.
	 */
	private void addSelectedTermToSelectedTerms()
	{
		selectedTermsList.add(new SelectedTerm(boqaCore.getIdOfTerm(boqaCore.getTerm(termFilterString, availableTermsTable.getSelectionIndex())),true));
		updateSelectedTermsTable();
	}

	/**
	 * Creates a term graph in which the membership of a term to certain sets
	 * (only query, only item, both) is characterized.
	 * 
	 * @param queryTerms
	 * @param itemTerms
	 * @param graph
	 * @param parent
	 * @return the constructed term graph
	 */
	private TermGraph<Integer> createTermGraph(Composite parent, final HashSet<Integer> queryTerms, final HashSet<Integer> itemTerms, DirectedGraph<Integer> graph)
	{
		TermGraph<Integer> tg = new TermGraph<Integer>(parent,0);
		tg.setLabelProvider(new TermLabelProvider()
		{
			public String getVariant(Integer t)
			{
				if (queryTerms.contains(t))
				{
					if (itemTerms.contains(t)) return "match";
					return "queryOnly";
				}
				return null;
			}
		});
		tg.setLayoutData(new GridData(GridData.FILL_HORIZONTAL|GridData.GRAB_HORIZONTAL|GridData.FILL_VERTICAL|GridData.GRAB_VERTICAL));
		tg.setGraph(graph);
		return tg;
	}

	/**
	 * Sets the selection of the available term list to the given term.
	 * Also updates the browser. 
	 * 
	 * @param index the index as understood by B4OCore
	 */
	private void setSelectionOfAvailableTermListToTerm(int index)
	{
		Term t = boqaCore.getTerm(index);

		selectedTermDetails.setTermID(t.getID());
		if (availableVisiblePos2SortedIndex != null)
		{
			Integer sortedIndex = availableVisiblePos2SortedIndex.get(t);
			if (sortedIndex != null)
				availableTermsTable.setSelection(sortedIndex);
		}
	}
}
