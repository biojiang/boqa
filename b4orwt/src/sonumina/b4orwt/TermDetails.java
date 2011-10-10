package sonumina.b4orwt;

import ontologizer.go.DescriptionParser;
import ontologizer.go.TermID;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;

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

	private Browser browser;
	private ITermDetailsProvider termDetailsProvider;
	private TermID currentTermID;

	public TermDetails(Composite parent, int style)
	{
		super(parent, style);
		
		setLayout(new FillLayout());

		browser = new Browser(this, style);
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
		
		if (termDetailsProvider != null)
		{
			String name = termDetailsProvider.getName(term);
			if (name != null)
			{
				str.append("<h3>");
				str.append(name);
				str.append("</h3>");
			}
			
			String desc = termDetailsProvider.getDescription(term);
			if (desc != null)
			{
				DescriptionParser.parse(termDetailsProvider.getDescription(term), new DescriptionParser.IDescriptionPartCallback() {
					@Override
					public boolean part(String txt, String ref)
					{
						str.append(txt);
						return true;
					}
				});
				
			}
		}
		browser.setText(str.toString());
		
		currentTermID = term;
	}
}
