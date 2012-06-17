package sonumina.boqa.tests;

import junit.framework.Assert;

import org.junit.Test;

import sonumina.boqa.InternalDatafiles;
import sonumina.boqa.calculation.BOQA;

public class JaccardTest
{
	@Test
	public void ttt()
	{
		InternalDatafiles data = new InternalDatafiles();
		BOQA boqa = new BOQA();
		boqa.setConsiderFrequenciesOnly(false);
		boqa.setPrecalculateScoreDistribution(false);
		boqa.setup(data.graph, data.assoc);
		
		Assert.assertEquals(1.0,boqa.jaccard(0, 0),0.0001);
	}
}
