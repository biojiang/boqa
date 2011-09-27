import org.eclipse.rwt.engine.Configurator;
import org.eclipse.rwt.engine.Context;
import org.eclipse.ui.forms.widgets.ToggleHyperlink;

public class DummyConfigurator implements Configurator
{
	@Override
	public void configure(Context context)
	{
		context.addThemableWidget(ToggleHyperlink.class);
		context.addEntryPoint("default", TwistieBug.class);
	}
}
