package sonumina.boqa.calculation;

/**
 * Class representing different sets of queries.
 * 
 * @author Sebastian Bauer
 */
public class QuerySets
{
	private int [][][] queries;
	
	public QuerySets(int maxSizes)
	{
		queries = new int[maxSizes][][];
	}
	
	public int [][] getQueries(int querySize)
	{
		return queries[querySize];
	}
	
	public void setQueries(int querySize, int [][] querySets)
	{
		queries[querySize] = querySets;
	}
}
