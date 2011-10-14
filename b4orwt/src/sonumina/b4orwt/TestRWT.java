package sonumina.b4orwt;

import org.eclipse.rwt.lifecycle.IEntryPoint;
import org.eclipse.rwt.lifecycle.WidgetUtil;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * Just a basic test class.
 * 
 * @author sba
 *
 */
public class TestRWT implements IEntryPoint
{
	@Override
	public int createUI() {
		Display display = new Display();
	    Shell shell = new Shell( display, 0 );
	    shell.setLayout(new FillLayout());
	    
	    shell.setMaximized(true);
	    
	    Button but0 = new Button(shell,0);
	    Button but1 = new Button(shell,0);
	    but1.setData(WidgetUtil.CUSTOM_VARIANT, "match");
	    Button but2 = new Button(shell,0);
	    but2.setData(WidgetUtil.CUSTOM_VARIANT, "queryOnly");
	    
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
