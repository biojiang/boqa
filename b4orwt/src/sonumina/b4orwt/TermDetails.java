package sonumina.b4orwt;

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
	public void setTerm(TermID term)
	{
		StringBuilder str = new StringBuilder();
		
		if (termDetailsProvider != null)
		{
			String name = termDetailsProvider.getName(term);
			if (name != null)
			{
				str.append("<h1>");
				str.append(name);
				str.append("</h1>");
			}
			
			String desc = termDetailsProvider.getDescription(term);
			if (desc != null)
				str.append(termDetailsProvider.getDescription(term));
		}
		browser.setText(str.toString());
	}
}
