package sonumina.b4orwt;

import org.eclipse.rwt.application.Application;
import org.eclipse.rwt.application.ApplicationConfiguration;
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
public class BOQARWTConfiguration implements ApplicationConfiguration
{
	@Override
	public void configure(Application application)
	{
		/* Needed for Forms */
		application.addThemableWidget(ToggleHyperlink.class);
		application.addThemableWidget(FormText.class);
		application.addResource(new ToggleHyperlinkResource());
		application.addResource(new ToggleHyperlinkAdapterResource());
		application.addResource(new FormTextResource());
		application.addResource(new FormTextAdapterResource());
		
		/* Add our own styles */
		application.addStyleSheet("org.eclipse.rap.rwt.theme.Default", "b4o.css");
		
		/* Add the entry point of our application */
		application.addEntryPoint("/main", BOQARWT.class, null);
//		application.addEntryPoint("default", TestRWT.class);

	}
}
