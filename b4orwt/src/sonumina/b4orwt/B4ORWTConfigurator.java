package sonumina.b4orwt;

import org.eclipse.rwt.engine.Configurator;
import org.eclipse.rwt.engine.Context;
import org.eclipse.ui.forms.internal.widgets.togglehyperlinkkit.ToggleHyperlinkResource;
import org.eclipse.ui.forms.widgets.ToggleHyperlink;

/**
 * Configures additional elements for the B4ORWT class. 
 * 
 * @author Sebastian Bauer
 */
public class B4ORWTConfigurator implements Configurator
{
	@Override
	public void configure(Context context)
	{
		/* Needed for Forms */
		context.addThemableWidget(ToggleHyperlink.class);
		context.addResource(new ToggleHyperlinkResource());
		
		/* Add the entry point of our application */
		context.addEntryPoint("default", B4ORWT.class);
	}
}
