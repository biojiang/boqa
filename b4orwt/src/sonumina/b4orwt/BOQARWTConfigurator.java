package sonumina.b4orwt;

import org.eclipse.rwt.application.ApplicationConfiguration;
import org.eclipse.rwt.application.ApplicationConfigurator;
import org.eclipse.ui.forms.internal.widgets.formtextkit.FormTextAdapterResource;
import org.eclipse.ui.forms.internal.widgets.formtextkit.FormTextResource;
import org.eclipse.ui.forms.internal.widgets.togglehyperlinkkit.ToggleHyperlinkAdapterResource;
import org.eclipse.ui.forms.internal.widgets.togglehyperlinkkit.ToggleHyperlinkResource;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.ToggleHyperlink;

/**
 * Configures additional elements for the B4ORWT class. 
 * 
 * @author Sebastian Bauer
 */
public class BOQARWTConfigurator implements ApplicationConfigurator
{
	@Override
	public void configure(ApplicationConfiguration config)
	{
		/* Needed for Forms */
		config.addThemableWidget(ToggleHyperlink.class);
		config.addThemableWidget(FormText.class);
		config.addResource(new ToggleHyperlinkResource());
		config.addResource(new ToggleHyperlinkAdapterResource());
		config.addResource(new FormTextResource());
		config.addResource(new FormTextAdapterResource());
		
		/* Add our own styles */
		config.addStyleSheet("org.eclipse.rap.rwt.theme.Default", "b4o.css");
		
		/* Add the entry point of our application */
		config.addEntryPoint("/main", BOQARWT.class, null);
//		config.addEntryPoint("default", TestRWT.class);
	}
}
