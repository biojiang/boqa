package sonumina.b4oweb.client;

import java.util.List;

import sonumina.b4oweb.shared.SharedItemResultEntry;
import sonumina.b4oweb.shared.SharedTerm;
import sonumina.b4oweb.shared.SharedParents;

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
	 * @param ids defines the position within the context of the given pattern.
	 * @return
	 */
	SharedTerm [] getNamesOfTerms(String pattern, List<Integer> ids);

	/**
	 * Returns the results in form of an HTML text.
	 * 
	 * @param serverIds defines the terms to be used for the search.
	 * @param first defines the index of the first transfered result.
	 * @params length defines the number of results that are transfered.
	 * @return
	 */
	SharedItemResultEntry [] getResults(List<Integer> serverIds, int first, int length);
	
	
	/**
	 * Returns the ancestors of the given server terms.
	 * 
	 * @param serverIds
	 * @return
	 */
	SharedParents [] getAncestors(List<Integer> serverIds);
}
