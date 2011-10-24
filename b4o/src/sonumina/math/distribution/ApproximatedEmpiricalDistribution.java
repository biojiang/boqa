package sonumina.math.distribution;

import java.util.Arrays;

/**
 *  A simple class representing an approximated
 *  empirical probability. The distribution is approximated
 *  using equidistant bins.
 * 
 * @author Sebastian Bauer
 */
public class ApproximatedEmpiricalDistribution implements IDistribution
{
	private double min;
	private double max;
	private int numberOfBins;
	private int [] cumCounts;

	public ApproximatedEmpiricalDistribution(double [] newObservations, int newNumberOfBins)
	{
		int [] counts = new int[numberOfBins];
		
		double [] observations = new double[newObservations.length];
		for (int i=0;i<observations.length;i++)
			observations[i] = newObservations[i];
		Arrays.sort(observations);
		
		min = observations[0];
		max = observations[observations.length - 1];
		numberOfBins = newNumberOfBins;

		for (int i=0;i<observations.length;i++)
			counts[findBin(observations[i])]++;

		for (int i=1;i<observations.length;i++)
			counts[i] += counts[i-1];
		
		cumCounts = counts;
	}
	
	private int findBin(double observation)
	{
		int bin = (int)Math.floor((observation - min) / (max - min) * numberOfBins);
		return bin;
	}

	@Override
	public double cdf(double x, boolean lowerTail)
	{
		return findBin(x) / (double)cumCounts[cumCounts.length];
	}
}
