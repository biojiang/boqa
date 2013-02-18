package sonumina.boqa.rwt;

import org.eclipse.rap.rwt.RWT;
import org.eclipse.rap.rwt.application.AbstractEntryPoint;
import org.eclipse.rap.rwt.client.service.JavaScriptLoader;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.widgets.FormText;

/**
 * Just a basic test class.
 * 
 * @author sba
 *
 */
public class TestRWT extends AbstractEntryPoint
{
//	@Override
//	public int createUI() {
//		Display display = new Display();
//	    Shell shell = new Shell( display, 0 );
//	    shell.setLayout(new FillLayout());
//	    
//	    shell.setMaximized(true);
//	    
//	    Button but0 = new Button(shell,0);
//	    Button but1 = new Button(shell,0);
//	    but1.setData(WidgetUtil.CUSTOM_VARIANT, "match");
//	    Button but2 = new Button(shell,0);
//	    but2.setData(WidgetUtil.CUSTOM_VARIANT, "queryOnly");
//	    
//	    shell.layout();
//	    shell.open();
//	    while( !shell.isDisposed() ) {
//	      if( !display.readAndDispatch() )
//	        display.sleep();
//	    }
//	    display.dispose();
//
//		return 0;
//	}

	@Override
	protected void createContents(Composite parent)
	{
		parent.setLayout(new FillLayout());
		
		RWT.getClient().getService(JavaScriptLoader.class).require("rwt-resources/org/eclipse/ui/forms/widgets/FormText.js");
		RWT.getClient().getService(JavaScriptLoader.class).require("rwt-resources/org/eclipse/ui/forms/widgets/FormTextAdapter.js");

	    Button but0 = new Button(parent,0);
	    Button but1 = new Button(parent,0);
	    but1.setData(RWT.CUSTOM_VARIANT, "match");
	    Button but2 = new Button(parent,0);
	    but2.setData(RWT.CUSTOM_VARIANT, "queryOnly");
	    FormText ft = new FormText(parent, 0);
	    ft.setText("huhu", false, false);
	}
}
