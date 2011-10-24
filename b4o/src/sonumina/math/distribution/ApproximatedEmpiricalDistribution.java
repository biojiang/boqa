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
		double [] observations = new double[newObservations.length];
		for (int i=0;i<observations.length;i++)
			observations[i] = newObservations[i];
		Arrays.sort(observations);
		
		min = observations[0];
		max = observations[observations.length - 1];
		numberOfBins = newNumberOfBins;

		int [] counts = new int[numberOfBins];
		for (int i=0;i<observations.length;i++)
			counts[findBin(observations[i])]++;

		for (int i=1;i<numberOfBins; i++)
			counts[i] += counts[i-1];
		
		cumCounts = counts;
	}
	
	private int findBin(double observation)
	{
		int bin = (int)Math.floor((observation - min) / (max - min + 1) * numberOfBins);
		return bin;
	}

	@Override
	public double cdf(double x, boolean lowerTail)
	{
		int bin = findBin(x);
		if (bin < 0) return 0;
		if (bin >= numberOfBins) return 1;  
		return cumCounts[bin] / (double)cumCounts[cumCounts.length - 1];
	}
}
