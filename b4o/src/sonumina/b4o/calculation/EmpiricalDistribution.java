package sonumina.b4o.calculation;

import java.util.Arrays;

/**
 * A simple class representing an empirical probability
 * distribution.
 * 
 * @author Sebastian Bauer
 *
 */
public class EmpiricalDistribution
{
	private double [] observations;

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
	 * Returns for x the value for the distribution function F(x) = P(X <= x).
	 * 
	 * @param observation
	 * @param lowerTail
	 * @return
	 */
	public double cdf(double x, boolean lowerTail)
	{
		int idx = Arrays.binarySearch(observations, x);
		for (;idx<observations.length;idx++)
			if (observations[idx] != x)
				break;
		return idx/(double)observations.length;
	}
	
}
