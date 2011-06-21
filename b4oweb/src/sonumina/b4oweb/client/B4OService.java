package sonumina.b4oweb.client;

import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("b4o")
public interface B4OService extends RemoteService
{
	String getTest();
	
	/**
	 * Returns the number of terms.
	 * @return
	 */
	int getNumberOfTerms();
	
	String [] getNamesOfTerms(List<Integer> ids);
}
