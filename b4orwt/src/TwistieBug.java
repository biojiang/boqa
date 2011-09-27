import org.eclipse.rwt.lifecycle.IEntryPoint;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.widgets.Twistie;

public class TwistieBug  implements IEntryPoint
{
	@Override
	public int createUI()
	{
		Display display = new Display();
	    Shell shell = new Shell( display, 0 );
	    shell.setLayout(new FillLayout());

	    Twistie tw = new Twistie(shell, 0);

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
