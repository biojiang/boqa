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
	
	
	/**
	 * Returns a spare representation of the given vector. An element
	 * refers to an index that is true.
	 * 
	 * @param dense
	 * @return
	 */
	public static int [] spareInt(boolean [] dense)
	{
		int c = 0;
		for (int i=0;i<dense.length;i++)
			if (dense[i])
				c++;

		int [] array = new int[c];
		c = 0;
		for (int i=0;i<dense.length;i++)
			if (dense[i])
				array[c++] = i;
		return array;
	}
	
	/**
	 * Calculates the hamming distance of the sparsely represented vectors.
	 * Elements are assumed to be sorted.
	 * 
	 * @return
	 */
	public static int hammingDistanceSparse(int [] va, int [] vb)
	{
		int distance = 0;
		int i=0,j=0;

		while (i < va.length && j < vb.length)
		{
			if (va[i] < vb[j])
			{
				distance++;
				i++;
			} else if (va[i] > vb[j]) 
			{
				distance++;
				j++;
			} else
			{
				i++;
				j++;
			}
		}

		distance += va.length - i;
		distance += vb.length - j;
		return distance;
	}
}
