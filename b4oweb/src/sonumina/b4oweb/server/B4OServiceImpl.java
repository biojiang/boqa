package sonumina.b4oweb.server;

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

}
