package sonumina.b4oweb.client.gwt;

import com.google.gwt.user.client.ui.CustomScrollPanel;
import com.google.gwt.user.client.ui.Widget;


/**
 * Same as the custom scroll panel but makes getScrollableElement()
 * public.
 * 
 * @author Sebastian Bauer
 */
public class MyCustomScrollPanel extends CustomScrollPanel
{
	public MyCustomScrollPanel(Widget widget)
	{
		super(widget);
	}
	
	@Override
	public com.google.gwt.user.client.Element getScrollableElement() {
		// TODO Auto-generated method stub
		return super.getScrollableElement();
	}	
}
