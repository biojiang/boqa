package sonumina.b4o.tests;

import static org.junit.Assert.*;

import java.util.Random;

import org.junit.Test;

import sonumina.b4o.InternalDatafiles;
import sonumina.b4o.calculation.B4O;
import sonumina.b4o.calculation.B4O.Result;

public class B4OTest {

	private static InternalDatafiles data = new InternalDatafiles();

	/**
	 * Tests the choose function.
	 */
	@Test
	public void testChoose()
	{
		Random rnd = new Random();

		/** Create storage where to choose numbers from */
		int [] storage = new int[1000];
		for (int i=0;i<storage.length;i++)
			storage[i] = i;

		int [] chosen = new int[10]; 
		
		for (int s=1;s<10;s++)
		{
			for (int t=0;t<10;t++)
			{
				B4O.choose(rnd,s,chosen,storage);

				/* Check storage array for validity */
				boolean [] seen = new boolean[storage.length];
				for (int i=0;i<storage.length;i++)
				{
					assertEquals(false,seen[storage[i]]);
					seen[storage[i]] = true;
				}

				
				/* Check chosen array for validity */
				for (int i=0;i<seen.length;i++)
					seen[i] = false;

				for (int i=0;i<s;i++)
				{
					assertEquals(false,seen[chosen[i]]);
					seen[chosen[i]] = true;
				}
			}
		}
		
	}

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
