package sonumina.b4oweb.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * A class representing a term.
 * 
 * @author Sebastian Bauer
 */
public class SharedTerm implements IsSerializable
{
	/**
	 * This is the request id which the server stores.
	 */
	public int requestId;

	/**
	 * This is the global server id of term.
	 */
	public int serverId;

	/**
	 * The name of the term.
	 */
	public String term;
	
	/**
	 * Number of items annotated to the term.
	 */
	public int numberOfItems;
}
