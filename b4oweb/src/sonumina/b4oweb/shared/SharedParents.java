package sonumina.b4oweb.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Holds the parents of an id.
 * 
 * @author Sebastian Bauer
 */
public class SharedParents implements IsSerializable
{
	public int serverId;
	public int [] parentIds;
}
