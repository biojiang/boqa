package sonumina.b4o.tests;

import static org.junit.Assert.*;

import java.util.Random;

import org.junit.Test;

import sonumina.b4o.InternalDatafiles;
import sonumina.b4o.calculation.B4O;
import sonumina.b4o.calculation.B4O.Result;

public class B4OTest {

	private static InternalDatafiles data = new InternalDatafiles();

	@Test
	public void test()
	{
		Random rnd = new Random(2);

		B4O.setConsiderFrequenciesOnly(false);
		B4O.setup(data.graph, data.assoc);

		int terms = data.graph.getNumberOfTerms();
		boolean [] obs = new boolean[terms];

		
		for (int i=0;i<B4O.items2DirectTerms[2].length;i++)
			obs[B4O.items2DirectTerms[2][i]] = true;

		Result result = B4O.resnikScore(obs, true, rnd);
		
		for (int i=0;i<B4O.allItemList.size();i++)
		{
			System.out.println(result.getMarginal(i) + "  " + result.getScore(i));
		}
	}
}
