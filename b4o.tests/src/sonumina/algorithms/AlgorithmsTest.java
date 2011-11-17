package sonumina.algorithms;

import static org.junit.Assert.*;

import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

public class AlgorithmsTest
{
	class Vector
	{
		String name;
		
		boolean [] v = new boolean[100];
		
		public Vector(String name, int [] ons)
		{
			this.name = name;
			for (int i=0;i<ons.length;i++)
				v[ons[i]] = true;
		}
	}

	@Test
	public void test()
	{
		Vector v1 = new Vector("v1", new int[]{2,6,10,23,27,30,40,76,99});
		Vector v2 = new Vector("v2", new int[]{2,6,10,23,});
		Vector v3 = new Vector("v3", new int[]{2,6,10,24});
		Vector v4 = new Vector("v4", new int[]{2,6,10});
		Vector v5 = new Vector("v5", new int[]{2,6,13,23,27,30,40,76,99});
		Vector v6 = new Vector("v6", new int[]{2,4,14,22,24,34,41,45,84});
		
		LinkedList<Vector> vectors = new LinkedList<Vector>();
		vectors.add(v1);
		vectors.add(v2);
		vectors.add(v3);
		vectors.add(v4);
		vectors.add(v5);
		vectors.add(v6);
		
		List<Vector> vs = Algorithms.approximatedTSP(vectors, v4, new Algorithms.IVertexDistance<Vector>() {
			public double distance(Vector a, Vector b)
			{
				int d = 0;
				for (int i=0;i<a.v.length;i++)
					if (a.v[i] != b.v[i])
						d++;
				return d;
			};
		});

		assertNotNull(vs);
	}
}
