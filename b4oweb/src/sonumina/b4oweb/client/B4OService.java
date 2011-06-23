package sonumina.b4oweb.client;

import java.util.List;

import sonumina.b4oweb.shared.SharedTerm;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("b4o")
public interface B4OService extends RemoteService
{
	/**
	 * Returns the number of terms.
	 * 
	 * @return
	 */
	int getNumberOfTerms();
	
	/**
	 * Returns the number of terms matching the given pattern.
	 * 
	 * @param pattern
	 * @return
	 */
	int getNumberOfTerms(String pattern);
	
	/**
	 * Returns the terms with the given server ids.
	 * 
	 * @param ids
	 * @return
	 */
	SharedTerm [] getNamesOfTerms(List<Integer> ids);
	
	/**
	 * Returns the terms with the given ids.
	 * 
	 * @param pattern
	 * @param ids
	 * @return
	 */
	SharedTerm [] getNamesOfTerms(String pattern, List<Integer> ids);
}
