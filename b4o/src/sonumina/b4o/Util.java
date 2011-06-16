package sonumina.b4o;

class Util
{

	/**
	 * Returns log(a + b)
	 * 
	 * @param loga log(a)
	 * @param logb log(b)
	 * @return
	 */
	static double logAdd(double loga, double logb)
	{
		/* Addition in logspace. To see why this works consider that
		 * log(a+b) = log(a(1+b/a)) then apply usual rules of logarithm  */
		
		if (Double.isInfinite(loga)) return logb;
		return loga + Math.log(1 + Math.exp(logb - loga));

	}
}
