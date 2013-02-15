package sonumina.b4orwt;

import org.eclipse.rap.rwt.RWT;
import org.eclipse.rap.rwt.application.Application;
import org.eclipse.rap.rwt.application.ApplicationConfiguration;
import org.eclipse.rap.rwt.client.service.JavaScriptLoader;
import org.eclipse.rap.rwt.internal.theme.ThemeManager;
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
	private static String FORMTEXT_JS = "org/eclipse/ui/forms/widgets/FormText.js";
	private static String FORMTEXTADAPTER_JS = "org/eclipse/ui/forms/widgets/FormTextAdapter.js";
	
	private static String TOGGLEHYPERLINK_JS = "org/eclipse/ui/forms/widgets/ToggleHyperlink.js";
	private static String TOGGLEHYPERLINKADAPTER_JS = "org/eclipse/ui/forms/widgets/ToggleHyperlinkAdapter.js";

	@Override
	public void configure(Application application)
	{
		/* Needed for Forms */
		application.addThemableWidget(ToggleHyperlink.class);
		application.addThemableWidget(FormText.class);
		application.addResource(FORMTEXT_JS, ThemeManager.STANDARD_RESOURCE_LOADER);
		application.addResource(FORMTEXTADAPTER_JS, ThemeManager.STANDARD_RESOURCE_LOADER);
		application.addResource(TOGGLEHYPERLINK_JS, ThemeManager.STANDARD_RESOURCE_LOADER);
		application.addResource(TOGGLEHYPERLINKADAPTER_JS, ThemeManager.STANDARD_RESOURCE_LOADER);
		
		/* Add our own styles */
		application.addStyleSheet("org.eclipse.rap.rwt.theme.Default", "b4o.css");
		
		/* Add the entry point of our application */
		application.addEntryPoint("/main", BOQARWT.class, null);
//		application.addEntryPoint("/main", TestRWT.class, null);
	}

	/**
	 * Add the given java script file as a required one.
	 * 
	 * @param 
	 */
	private static void require(String str)
	{
		RWT.getClient().getService(JavaScriptLoader.class).require("rwt-resources/" + str);
	}
	
	/**
	 * Add the required java script files.
	 */
	public static void addRequiredJS()
	{
		require(FORMTEXT_JS);
		require(FORMTEXTADAPTER_JS);
		require(TOGGLEHYPERLINK_JS);
		require(TOGGLEHYPERLINKADAPTER_JS);	
//		RWT.getClient().getService(JavaScriptLoader.class).require("rwt-resources/org/eclipse/ui/forms/widgets/FormText.js");
//		RWT.getClient().getService(JavaScriptLoader.class).require("rwt-resources/org/eclipse/ui/forms/widgets/FormTextAdapter.js");
	}
}
