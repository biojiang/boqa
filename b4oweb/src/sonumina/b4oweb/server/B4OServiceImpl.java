package sonumina.b4oweb.server;

import java.util.List;

import sonumina.b4oweb.client.B4OService;
import sonumina.b4oweb.shared.SharedTerm;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

@SuppressWarnings("serial")
public class B4OServiceImpl extends RemoteServiceServlet implements B4OService
{

	@Override
	public String getTest()
	{
		return "MyString";
	}

	@Override
	public int getNumberOfTerms()
	{
		return B4OCore.getNumberTerms(); 
	}

	@Override
	public SharedTerm[] getNamesOfTerms(List<Integer> ids)
	{
		if (ids == null) return new SharedTerm[0];

		SharedTerm [] names = new SharedTerm[ids.size()];
		
		int i=0;
		for (int id : ids)
		{
			names[i] = new SharedTerm();
			names[i].serverId = id;
			names[i].term = B4OCore.getTerm(id).getName();
			i++;
		}
		return names;
	}

}
