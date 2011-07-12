package sonumina.b4oweb.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Represents a result for server and client.
 * 
 * @author Sebastian Bauer
 */
public class SharedItemResultEntry implements IsSerializable
{
	public int itemId;      /* The ID of the item */
	public String itemName; /* Name of the item */
	public double marginal; /* The items marginal */
	public int rank;		/* The rank of this item (0-based) */
	public int [] directTerms; /* Terms directly associated with the item in some form */
	public double [] directedTermsFreq; /* The corresponding frequencies */
}
