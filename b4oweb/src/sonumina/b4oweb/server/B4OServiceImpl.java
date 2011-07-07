package sonumina.b4oweb.server;

import java.util.List;

import ontologizer.go.Term;

import sonumina.b4oweb.client.B4OService;
import sonumina.b4oweb.shared.SharedTerm;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

@SuppressWarnings("serial")
public class B4OServiceImpl extends RemoteServiceServlet implements B4OService
{

	@Override
	public int getNumberOfTerms()
	{
		return B4OCore.getNumberTerms(null); 
	}

	@Override
	public int getNumberOfTerms(String pattern)
	{
		return B4OCore.getNumberTerms(pattern);
	}


	@Override
	public SharedTerm[] getNamesOfTerms(List<Integer> ids)
	{
		return getNamesOfTerms(null,ids);
	}

	@Override
	public SharedTerm[] getNamesOfTerms(String pattern, List<Integer> ids)
	{
		if (ids == null) return new SharedTerm[0];

		int i=0, j=0;
		SharedTerm [] names = new SharedTerm[ids.size()];

		/* Go through all terms of the given pattern and then fill
		 * our returning array as requested by the caller */
		for (Term t : B4OCore.getTerms(pattern))
		{
			if (ids.get(j) == i)
			{
				names[j] = new SharedTerm();
				names[j].requestId = i;
				names[j].serverId = B4OCore.getIdOfTerm(t);
				names[j].term = t.getName();
				names[j].numberOfItems = B4OCore.getNumberOfTermsAnnotatedToTerm(names[j].serverId);
				j++;

				if (j >= ids.size())
					break;
			}
			i++;
		}
		return names;
	}

	@Override
	public String getResults(List<Integer> serverIds)
	{
		StringBuilder str = new StringBuilder("<h2>Results</h2>");
		for (ItemResultEntry e : B4OCore.score(serverIds))
		{
			str.append(B4OCore.getItemName(e.getItemId()) + " " + e.getScore() + "<br />");
		}
		
		return str.toString();
	}
}
