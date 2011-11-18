package sonumina.algorithms;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Collection;;

public class Algorithms
{
	public static interface IVertexDistance<V>
	{
		/**
		 * Returns the distance between vertex a and vertex b.
		 * 
		 * @param a
		 * @param b
		 * @return
		 */
		public double distance(V a, V b);
	}

	/**
	 * An heuristics to solve the TSP.
	 * 
	 * @param vertices
	 * @param start
	 * @param distance
	 * @return
	 */
	public static <V> List<V> approximatedTSP(Collection<V> vertices, V start, IVertexDistance<V> distance)
	{
		LinkedHashSet<V> toDo = new LinkedHashSet<V>(vertices);
		ArrayList<V> list = new ArrayList<V>();
		list.add(start);
		toDo.remove(start);
		
		while (!toDo.isEmpty())
		{
			V newStart = null;

			double minDistance = Double.MAX_VALUE;
			for (V v : toDo)
			{
				double nd = distance.distance(start, v);
				if (nd < minDistance)
				{
					newStart = v;
					minDistance = nd;
				}
			}
			start = newStart;
			toDo.remove(start);
			list.add(start);
		}
		return list;
	}
}
