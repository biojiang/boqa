package sonumina.boqa.tests;

import junit.framework.Assert;

import ontologizer.go.Term;

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

		for (Term t : data.graph)
			System.out.println(t.getName() + " " + data.graph.getSlimGraphView().getVertexIndex(t));
		
		Assert.assertEquals(1.0, boqa.jaccard(0, 0),0.0001);
		Assert.assertEquals(1.0, boqa.jaccard(1, 1),0.0001);
		Assert.assertEquals(0.5, boqa.jaccard(9, 12),0.0001);
		Assert.assertEquals(0.0, boqa.jaccard(9, 10),0.0001);
		Assert.assertEquals(0.25, boqa.jaccard(6, 14),0.0001);
	}
}
