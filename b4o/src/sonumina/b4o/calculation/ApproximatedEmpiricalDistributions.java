package sonumina.b4o.calculation;

import java.io.Serializable;

import sonumina.math.distribution.ApproximatedEmpiricalDistribution;

/**
 * Basic container for distributions.
 * 
 * @author Sebastian Bauer
 */
public class ApproximatedEmpiricalDistributions implements Serializable
{
	private static final long serialVersionUID = 1L;
	
	private ApproximatedEmpiricalDistribution[] distr;
	
	ApproximatedEmpiricalDistributions(int numberOfDistributions)
	{
		distr = new ApproximatedEmpiricalDistribution[numberOfDistributions];
	}
	
	public ApproximatedEmpiricalDistribution getDistribution(int i)
	{
		return distr[i];
	}
	
	public void setDistribution(int i, ApproximatedEmpiricalDistribution dist)
	{
		distr[i] = dist;
	}
}