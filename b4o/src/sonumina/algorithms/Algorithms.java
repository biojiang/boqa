package sonumina.algorithms;

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

	public static <V> List<V> approximatedTSP(Collection<V> vertices, V start, IVertexDistance<V> distance)
	{
		return null;
	}
}
