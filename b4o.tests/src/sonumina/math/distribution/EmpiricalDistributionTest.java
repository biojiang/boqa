package sonumina.math.distribution;

import static org.junit.Assert.*;

import org.junit.Test;

import sonumina.math.distribution.EmpiricalDistribution;

public class EmpiricalDistributionTest
{
	
	@Test
	public void test()
	{
		double [] obs = new double[]{1,1,1.5,2,2.5,3,3.5,4,0.5 };

		EmpiricalDistribution dis = new EmpiricalDistribution(obs);
		 
		assertEquals(dis.cdf(0.5,false),1/(double)obs.length,0.0001);
		assertEquals(dis.cdf(1,false),3/(double)obs.length,0.0001);
	}

	@Test
	public void testWithCounts()
	{
		double [] obs = new double[]{1, 1.5, 2, 2.5, 3, 3.5, 4, 0.5 };
		int [] counts = new int[]{2, 1, 1, 1, 1, 1, 1, 1};
		
		EmpiricalDistribution dis = new EmpiricalDistribution(obs,counts);
		assertEquals(dis.cdf(0.5,false),1/(double)obs.length,0.0001);
		assertEquals(dis.cdf(1,false),3/(double)obs.length,0.0001);
	}
}
