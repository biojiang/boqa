import org.eclipse.rwt.engine.Configurator;
import org.eclipse.rwt.engine.Context;
import org.eclipse.ui.forms.internal.widgets.togglehyperlinkkit.ToggleHyperlinkResource;
import org.eclipse.ui.forms.widgets.ToggleHyperlink;

public class DummyConfigurator implements Configurator
{
	@Override
	public void configure(Context context)
	{
		context.addThemableWidget(ToggleHyperlink.class);
		context.addResource(new ToggleHyperlinkResource());
		context.addEntryPoint("default", TwistieBug.class);
	}
}
