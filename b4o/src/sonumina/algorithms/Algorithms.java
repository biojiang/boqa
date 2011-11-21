package sonumina.algorithms;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Collection;
import java.util.Vector;

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
	@SuppressWarnings("unchecked")
	public static <V> List<V> approximatedTSP(Collection<V> vertices, V start, IVertexDistance<V> distance)
	{
		Object [] toDo = new Object[vertices.size() - 1];
		int toDoLength = toDo.length;
		ArrayList<V> list = new ArrayList<V>();
		list.add(start);

		int i=0;
		
		for (V v : vertices)
		{
			if (v!=start)
				toDo[i++] = v;
		}

		while (toDoLength > 0)
		{
			double minDistance = Double.MAX_VALUE;
			int newStartIndex = -1;

			for (i=0;i<toDoLength;i++)
			{
				V v = (V)toDo[i];
				double nd = distance.distance(start, v);
				if (nd < minDistance)
				{
					newStartIndex = i;
					minDistance = nd;
				}
			}

			list.add((V)toDo[newStartIndex]);
			toDoLength--;
			if (toDoLength>0)
				toDo[newStartIndex] = toDo[toDoLength];
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
