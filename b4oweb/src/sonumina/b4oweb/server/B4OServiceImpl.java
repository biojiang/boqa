package sonumina.b4oweb.server;

import java.util.ArrayList;
import java.util.List;

import ontologizer.go.Term;

import sonumina.b4oweb.client.B4OService;
import sonumina.b4oweb.shared.SharedItemResultEntry;
import sonumina.b4oweb.shared.SharedParents;
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
	public SharedItemResultEntry[] getResults(List<Integer> serverIds, int first, int length)
	{
		List<ItemResultEntry> rl = B4OCore.score(serverIds);
		ArrayList<SharedItemResultEntry> al = new ArrayList<SharedItemResultEntry>(length);

		for (int rank = first; rank < first + length; rank++)
		{
			SharedItemResultEntry sire = new SharedItemResultEntry();
			ItemResultEntry e = rl.get(rank);
			sire.itemId = e.getItemId();
			sire.marginal = e.getScore();
			sire.rank = rank;
			sire.itemName = B4OCore.getItemName(sire.itemId);
			sire.directTerms = B4OCore.getTermsDirectlyAnnotatedTo(sire.itemId);
			sire.directedTermsFreq = B4OCore.getFrequenciesOfTermsDirectlyAnnotatedTo(sire.itemId);
			al.add(sire);
		}
		
		return al.toArray(new SharedItemResultEntry[0]);
	}

	@Override
	public SharedParents[] getAncestors(List<Integer> serverIds)
	{
		final ArrayList<SharedParents> ancestorList = new ArrayList<SharedParents>();
	
		B4OCore.visitAncestors(serverIds,new B4OCore.IAncestorVisitor()
		{
			@Override
			public void visit(int t)
			{
				SharedParents p = new SharedParents();
				p.serverId = t;
				p.parentIds = B4OCore.getParents(t);
				ancestorList.add(p);
			}
		});
		
		return ancestorList.toArray(new SharedParents[0]);
	}
}
