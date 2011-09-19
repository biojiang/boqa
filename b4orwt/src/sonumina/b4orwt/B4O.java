package sonumina.b4orwt;

import org.eclipse.rwt.lifecycle.IEntryPoint;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import sonumina.b4oweb.server.*;

public class B4O implements IEntryPoint
{
	  public int createUI()
	  {       
	    Display display = new Display();
	    Shell shell = new Shell( display, 0 );
	    shell.setLayout(new GridLayout());

	    Label label = new Label(shell, SWT.NONE);
	    label.setText("Hello RAP World");
	    label.setSize( 80, 20 );

	    ScrolledComposite composite = new ScrolledComposite(shell, 0);
	    Button button = new Button(composite, SWT.NONE);
	    button.setText("Test");
	    button.setSize(200,200);
	    
	    Table table = new Table(shell,SWT.VIRTUAL|SWT.BORDER);
	    table.addListener( SWT.SetData, new Listener()
	    {
			@Override
			public void handleEvent(Event event)
			{
				TableItem item = (TableItem)event.item;
				int index = event.index;
				item.setText("item" + index);
			}
	    });
	    table.setItemCount(100);
	    table.setSize(500,200);
	    
	    shell.setSize( 500, 395 );
	    shell.setMaximized(true);
	    shell.open();
	    while( !shell.isDisposed() ) {
	      if( !display.readAndDispatch() )
	        display.sleep();
	    }
	    display.dispose();
	    return 0;
	  }
}
