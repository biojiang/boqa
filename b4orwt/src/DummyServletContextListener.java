import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.eclipse.rwt.engine.ResourceLoader;
import org.eclipse.rwt.internal.engine.RWTFactory;
import org.eclipse.rwt.internal.engine.RWTServletContextListener;
import org.eclipse.rwt.internal.theme.ThemeManager;
import org.eclipse.ui.forms.widgets.ToggleHyperlink;

public class DummyServletContextListener extends RWTServletContextListener
{
	@Override
	public void contextDestroyed(ServletContextEvent evt)
	{
		super.contextDestroyed(evt);
	}

	@Override
	public void contextInitialized(ServletContextEvent evt)
	{
		super.contextInitialized(evt);
	}
}
