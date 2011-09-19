package sonumina.b4orwt;

import ontologizer.go.Term;

import org.eclipse.rwt.lifecycle.IEntryPoint;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import sonumina.b4oweb.server.*;

public class B4ORWT implements IEntryPoint
{
    private String termFilterString = null;
    
    private Table termTable;

    /**
     * Updates the content of the term table.
     */
    private void updateTermTable()
    {
	    termTable.setItemCount(B4OCore.getNumberTerms(termFilterString));
    	termTable.clearAll();
    	termTable.redraw();
    }
    
	public int createUI()
	{       
	    Display display = new Display();
	    Shell shell = new Shell( display, 0 );
	    shell.setLayout(new FillLayout());
	    
	    /* Term */
	    Composite termComposite = new Composite(shell, 0);
	    termComposite.setLayout(new GridLayout());
	    
	    final Text termFilterText = new Text(termComposite,SWT.BORDER);
	    termFilterText.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL|GridData.FILL_HORIZONTAL));
	    termFilterText.addModifyListener(new ModifyListener()
	    {
			@Override
			public void modifyText(ModifyEvent event)
			{
				termFilterString = termFilterText.getText();
				updateTermTable();
			}
		});
	    
	    termTable = new Table(termComposite,SWT.VIRTUAL|SWT.BORDER);
	    termTable.addListener( SWT.SetData, new Listener()
	    {
	    	@Override
			public void handleEvent(Event event)
			{
				TableItem item = (TableItem)event.item;
				int index = event.index;
				Term t = B4OCore.getTerm(termFilterString, index);
				item.setText(t.getName());
				System.out.println(index);
			}
	    });
	    termTable.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL|GridData.GRAB_VERTICAL|GridData.FILL_BOTH));

	    termComposite.pack();
	    updateTermTable();

	    shell.setMaximized(true);
	    shell.layout();
	    shell.open();
	    while( !shell.isDisposed() ) {
	      if( !display.readAndDispatch() )
	        display.sleep();
	    }
	    display.dispose();
	    return 0;
	}
}
