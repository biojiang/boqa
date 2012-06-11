package sonumina.boqa.calculation;

/**
 * Class to count the different cases.
 * 
 * @author Sebastian Bauer
 */
final public class Configuration implements Cloneable
{
	public static enum NodeCase
	{
		FAULT,
		TRUE_POSITIVE,
		FALSE_POSITIVE,
		TRUE_NEGATIVE,
		FALSE_NEGATIVE,
		INHERIT_TRUE,
		INHERIT_FALSE
	}

	final private int [] stats = new int[NodeCase.values().length];
	
	final public void increment(NodeCase c)
	{
		stats[c.ordinal()]++;
	}
	
	final public void decrement(NodeCase c)
	{
		stats[c.ordinal()]--;
	}
	
	@Override
	public String toString()
	{
		String str = "";
		for (int i=0;i<stats.length;i++)
			str += " " + NodeCase.values()[i].name() + ": " + stats[i] + "\n";
		
		return str;
	}
	
	/**
	 * Get the number of observed cases for the given case.
	 * 
	 * @param c
	 * @return
	 */
	final public int getCases(NodeCase c)
	{
		return stats[c.ordinal()];
	}
	
	/**
	 * Returns the total number of cases that were tracked.
	 * 
	 * @return
	 */
	final public int getTotalCases()
	{
		int c = 0;
		for (int i=0;i<stats.length;i++)
			c += stats[i];
		return c;
	}
	
	/**
	 * Returns the false positive rate.
	 * 
	 * @return
	 */
	final public double falsePositiveRate()
	{
		 return getCases(Configuration.NodeCase.FALSE_POSITIVE)/(double)(getCases(Configuration.NodeCase.FALSE_POSITIVE) + getCases(Configuration.NodeCase.TRUE_NEGATIVE)); 
	}

	/**
	 * Return false negative rate.
	 * 
	 * @return
	 */
	final public double falseNegativeRate()
	{
		 return getCases(Configuration.NodeCase.FALSE_NEGATIVE)/(double)(getCases(Configuration.NodeCase.FALSE_NEGATIVE) + getCases(Configuration.NodeCase.TRUE_POSITIVE)); 
	}

	/**
	 * Returns the log score of the summarized configuration.
	 * 
	 * @param alpha
	 * @param beta
	 * @return
	 */
	final public double getScore(double alpha, double beta)
	{
		return  Math.log(beta) * getCases(NodeCase.FALSE_NEGATIVE) +
				Math.log(alpha) * getCases(NodeCase.FALSE_POSITIVE) + 
				Math.log(1-beta) * getCases(NodeCase.TRUE_POSITIVE) + 
				Math.log(1-alpha) * getCases(NodeCase.TRUE_NEGATIVE) + 
				Math.log(1) * getCases(NodeCase.INHERIT_FALSE) + /* 0 */
				Math.log(1) * getCases(NodeCase.INHERIT_TRUE);	/* 0 */
	}

	/**
	 * Adds the given stat to this one.
	 * 
	 * @param toAdd
	 */
	final public void add(Configuration toAdd)
	{
		for (int i=0;i<stats.length;i++)
			stats[i] += toAdd.stats[i];
	}

	/**
	 * Clear the stats.
	 */
	final public void clear()
	{
		for (int i=0;i<stats.length;i++)
			stats[i] = 0;
	}
	
	@Override
	final public Configuration clone()
	{
		Configuration c = new Configuration();
		for (int i=0;i<stats.length;i++)
			c.stats[i] = stats[i];
		return c;
	}
	
	public boolean equals(Configuration obj)
	{
		for (int i=0;i<obj.stats.length;i++)
			if (obj.stats[i] != stats[i])
				return false;
		return true;
	}
}
