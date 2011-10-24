package sonumina.math.distribution;

import java.util.Arrays;
import java.util.Comparator;

/**
 * A simple class representing an empirical probability
 * distribution.
 * 
 * @author Sebastian Bauer
 *
 */
public class EmpiricalDistribution implements IDistribution
{
	private double [] observations;
	
	/**  The cumulative counts */
	private int [] cumCounts;

	/**
	 * Constructs an empirical distribution.
	 * 
	 * @param newObservations
	 */
	public EmpiricalDistribution(double [] newObservations)
	{
		observations = new double[newObservations.length];
		for (int i=0;i<newObservations.length;i++)
			observations[i] = newObservations[i];
		Arrays.sort(observations);
	}

	/**
	 * Constructs an empirical distribution.
	 * 
	 * @param newObservations
	 * @param counts
	 */
	public EmpiricalDistribution(double [] newObservations, int [] counts)
	{
		if (newObservations.length != counts.length)
			throw new IllegalArgumentException("Length of import vectors doesn't match");

		class Item
		{
			public double obs;
			public int idx;
		};
		
		Item [] items = new Item[newObservations.length];
		for (int i=0;i<newObservations.length;i++)
		{
			items[i] = new Item();
			items[i].idx = i;
			items[i].obs = newObservations[i];
		}
		Arrays.sort(items, new Comparator<Item>() {
			public int compare(Item o1, Item o2)
			{
				if (o1.obs < o2.obs) return -1;
				if (o1.obs == o2.obs) return 0;
				return 1;
			};
		});

		cumCounts = new int[counts.length];
		observations = new double[newObservations.length];

		int totalCounts = 0;

		for (int i=0;i<items.length;i++)
		{
			observations[i] = items[i].obs;
			totalCounts += counts[items[i].idx];
			cumCounts[i] = totalCounts;
		}
	}

	/**
	 * Returns for x the value for the distribution function F(x) = P(X <= x).
	 * 
	 * @param observation
	 * @param upperTail
	 * @return
	 */
	public double cdf(double x, boolean upperTail)
	{
		int idx = Arrays.binarySearch(observations, x);
		
		if (cumCounts == null)
		{
			/* See doc to binarySearch */
			if (idx < 0)
				idx = - idx - 1;
			for (;idx<observations.length;idx++)
				if (observations[idx] != x)
					break;
			return idx/(double)observations.length;
		} else
		{
			return cumCounts[idx] / (double)cumCounts[cumCounts.length - 1];
		}
	}
}
