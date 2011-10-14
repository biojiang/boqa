package sonumina.b4orwt;

import org.eclipse.rwt.application.ApplicationConfiguration;
import org.eclipse.rwt.application.ApplicationConfigurator;
import org.eclipse.ui.forms.internal.widgets.togglehyperlinkkit.ToggleHyperlinkResource;
import org.eclipse.ui.forms.widgets.ToggleHyperlink;

/**
 * Configures additional elements for the B4ORWT class. 
 * 
 * @author Sebastian Bauer
 */
public class B4ORWTConfigurator implements ApplicationConfigurator
{
	@Override
	public void configure(ApplicationConfiguration config)
	{
		/* Needed for Forms */
		config.addThemableWidget(ToggleHyperlink.class);
		config.addResource(new ToggleHyperlinkResource());

		/* Add our own styles */
		config.addStyleSheet("org.eclipse.rap.rwt.theme.Default", "b4o.css");
		
		/* Add the entry point of our application */
		config.addEntryPoint("default", B4ORWT.class);
//		config.addEntryPoint("default", TestRWT.class);
	}
}
