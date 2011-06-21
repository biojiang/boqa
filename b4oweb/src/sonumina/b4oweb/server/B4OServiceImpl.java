package sonumina.b4oweb.server;

import java.util.List;

import sonumina.b4oweb.client.B4OService;

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
		return B4OCore.getSlimGraph().getNumberOfVertices(); 
	}

	@Override
	public String[] getNamesOfTerms(List<Integer> ids)
	{
		if (ids == null) return new String[0];

		String [] names = new String[ids.size()];
		
		int i=0;
		for (int id : ids)
			names[i++] = B4OCore.getSlimGraph().getVertex(id).getName();
		return names;
	}

}
