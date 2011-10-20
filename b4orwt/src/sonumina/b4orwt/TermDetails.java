package sonumina.b4orwt;

import ontologizer.go.DescriptionParser;
import ontologizer.go.TermID;

import org.eclipse.rwt.RWT;
import org.eclipse.rwt.internal.widgets.JSExecutor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationAdapter;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.Hyperlink;

/**
 * A Component that show details a term.
 * 
 * @author Sebastian Bauer
 *
 */
public class TermDetails extends Composite
{
	public interface ITermDetailsProvider
	{
		public String getName(TermID term);
		public String getDescription(TermID term);
	}

	private final static boolean USE_EMBEDDED_BROWSER = false;
	
	private Browser browser;
	private ITermDetailsProvider termDetailsProvider;
	private TermID currentTermID;
	private FormText text;

//	private Shell externalShell;
//	private Shell externalBrowser;

	public TermDetails(Composite parent, int style)
	{
		super(parent, style);
		
		setLayout(new FillLayout());

//		externalShell = new Shell(parent.getDisplay());
		
		if (USE_EMBEDDED_BROWSER)
		{
			browser = new Browser(this, style);
			browser.addLocationListener(new LocationAdapter() {
				@Override
				public void changing(LocationEvent event) {
				System.out.println(event.location);
//					JSExecutor.executeJS("window.open(\"http://www.google.de\",\"fabn\");");
//					System.out.println(browser.evaluate("document.location.href;"));
//					event.doit = false;
				}
			});
		}
		
		text = new FormText(this, SWT.BORDER);
		text.addHyperlinkListener(new HyperlinkAdapter()
		{
			@Override
			public void linkActivated(HyperlinkEvent e)
			{
				System.out.println(e.getHref());
				super.linkActivated(e);
			}
		});
		
	}

	/**
	 * Sets the term details provider.
	 * 
	 * @param termDetailsProvider
	 */
	public void setTermDetailsProvider(ITermDetailsProvider termDetailsProvider)
	{
		this.termDetailsProvider = termDetailsProvider;
	}
	
	/**
	 * Sets a new term that is displayed.
	 * 
	 * @param term
	 */
	public void setTermID(TermID term)
	{
		final StringBuilder str = new StringBuilder();
		final StringBuilder str2 = new StringBuilder();
		
		str2.append("<form>");
		
		if (termDetailsProvider != null)
		{
			String name = termDetailsProvider.getName(term);
			if (name != null)
			{
				str.append("<h3>");
				str.append(name);
				str.append("</h3>");
				
				str2.append("<p>");
				str2.append("<span color=\"header\" font=\"header\">");
				str2.append(name);
				str2.append("</span>");
				str2.append("</p>");
			}
			
//			 buf.append("<p>");
//			 buf.append("<span color=\"header\" font=\"header\">"+
//			   "This text is in header font and color.</span>");
//			 buf.append("</p>");			
			
			str2.append("<p>");
			String desc = termDetailsProvider.getDescription(term);
			if (desc != null)
			{
				DescriptionParser.parse(termDetailsProvider.getDescription(term), new DescriptionParser.IDescriptionPartCallback() {
					@Override
					public boolean part(String txt, String ref)
					{
						boolean closeTag = false;
						
						if (ref != null)
						{
							if (ref.startsWith("FMA:"))
							{
								str.append(String.format("<a href=\"http://www.berkeleybop.org/obo/%s?\">",ref));
								str2.append(String.format("<a href=\"http://www.berkeleybop.org/obo/%s?\">",ref));
								closeTag = true;
							}
						}
						str.append(txt);
						str2.append(txt);
						
						if (closeTag)
						{
							str.append("</A>");
							str2.append("</a>");
						}
						
						return true;
					}
				});
			}
			str2.append("</p>");
		}
		if (USE_EMBEDDED_BROWSER)
			browser.setText(str.toString());

		str2.append("</form>");
		text.setText(str2.toString(), true, false);
		
		currentTermID = term;
	}
}
