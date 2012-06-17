package sonumina.boqa.tests;

import org.junit.Assert;
import org.junit.Test;

import sonumina.boqa.InternalDatafiles;
import sonumina.boqa.calculation.BOQA;

public class MBSimTest
{
	@Test
	public void mbSimTerm()
	{
		InternalDatafiles data = new InternalDatafiles();
		BOQA boqa = new BOQA();
		boqa.setConsiderFrequenciesOnly(false);
		boqa.setPrecalculateScoreDistribution(false);
		boqa.setup(data.graph, data.assoc);

		for (int i=0;i<boqa.getSlimGraph().getNumberOfVertices();i++)
			System.out.println(i + " " + boqa.getNumberOfItemsAnnotatedToTerm(i) + " " + boqa.getIC(i));
		
		Assert.assertEquals(0,boqa.mbTermSim(0, 0),0.000001);
		Assert.assertEquals(0,boqa.mbTermSim(0, 1),0.000001);
		Assert.assertEquals(0,boqa.mbTermSim(1, 0),0.000001);
		Assert.assertEquals(0,boqa.mbTermSim(10, 11),0.000001);
		Assert.assertEquals(0,boqa.mbTermSim(11, 10),0.000001);
		Assert.assertEquals(0.6*(-Math.log(4./5) - Math.log(4./5))/2, boqa.mbTermSim(3, 4),0.0001);
		Assert.assertEquals(0.5*(-Math.log(1./5) - Math.log(2./5))/2, boqa.mbTermSim(9, 12),0.0001);
		Assert.assertEquals(0.5*(-Math.log(1./5) - Math.log(2./5))/2, boqa.mbTermSim(12, 9),0.0001);
	}
}
