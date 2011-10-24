package sonumina.math.distribution;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 *  A simple class representing an approximated
 *  empirical probability.
 * 
 * @author Sebastian Bauer
 */
public class ApproximatedEmpiricalDistributionTest
{
	@Test
	public void test()
	{
		double [] obs = new double[100];
		for (int i=0;i<obs.length;i++)
			obs[i] = i;

		ApproximatedEmpiricalDistribution dist = new ApproximatedEmpiricalDistribution(obs,10);
		assertEquals(0.6, dist.cdf(51, false),0.001);
		assertEquals(0.2, dist.cdf(11, false),0.001);
		assertEquals(0.1, dist.cdf(0, false),0.001);
		assertEquals(  0, dist.cdf(-11, false),0.001);
		assertEquals(  1, dist.cdf(100, false),0.001);
	}

}
