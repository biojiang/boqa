package sonumina.b4o.calculation;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * This is a list of weighted stats.
 *  
 * @author Sebastian Bauer
 */
public class WeightedConfigurationList implements Iterable<WeightedConfiguration>
{
	private ArrayList<WeightedConfiguration> tupelList = new ArrayList<WeightedConfiguration>(10);

	public Iterator<WeightedConfiguration> iterator()
	{
		return tupelList.iterator();
	}
	
	public void add(Configuration stat, double factor)
	{
		WeightedConfiguration t = new WeightedConfiguration();
		t.stat = stat;
		t.factor = factor;
		tupelList.add(t);
	}
	
	public double score(double alpha, double beta)
	{
		double sumOfScores = Math.log(0);
		
		for (WeightedConfiguration tupel : tupelList)
		{
			double score = tupel.stat.getScore(alpha, beta) + tupel.factor; /* Multiply score by factor, remember that we are operating in log space */
			sumOfScores = Util.logAdd(sumOfScores, score);
		}
		return sumOfScores;
	}

	public int size()
	{
		return tupelList.size();
	}
	
}
