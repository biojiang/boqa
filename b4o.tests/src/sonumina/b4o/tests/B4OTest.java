package sonumina.b4o.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;

import ontologizer.association.Association;
import ontologizer.association.AssociationContainer;
import ontologizer.benchmark.Datafiles;
import ontologizer.go.OBOParser;
import ontologizer.go.OBOParserException;
import ontologizer.go.Ontology;
import ontologizer.go.Term;
import ontologizer.go.TermContainer;
import ontologizer.types.ByteString;

import org.junit.BeforeClass;
import org.junit.Test;

import sonumina.b4o.InternalDatafiles;
import sonumina.b4o.benchmark.Benchmark;
import sonumina.b4o.calculation.B4O;
import sonumina.b4o.calculation.B4O.IntArray;
import sonumina.b4o.calculation.B4O.Result;
import sonumina.b4o.calculation.Observations;
import sonumina.math.graph.AbstractGraph.DotAttributesProvider;
import sonumina.math.graph.SlimDirectedGraphView;

public class B4OTest
{
	private static Datafiles hpo;
	
	@BeforeClass
	public static void loadHPO() throws InterruptedException, IOException
	{
		hpo = new Datafiles("../b4o/data/human-phenotype-ontology.obo.gz","../b4o/data/phenotype_annotation.omim.gz");
	}

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
	
	/**
	 * Checks whether all elements of expected are contained in actual and
	 * vice versa.
	 * 
	 * @param expected
	 * @param actual
	 */
	private void checkIntArrayContentsUnordered(int [] expected, int [] actual)
	{
		HashSet<Integer> expectedSet = new HashSet<Integer>();
		HashSet<Integer> actualSet = new HashSet<Integer>();
		
		assertEquals(expected.length, actual.length);

		for (int e : expected)
			expectedSet.add(e);
		
		for (int a : actual)
			actualSet.add(a);
		
		assertTrue(expectedSet.containsAll(actualSet));
		assertTrue(actualSet.containsAll(expectedSet));
	}
		
	@Test
	public void testMostSpecificTerms()
	{
		InternalDatafiles data = new InternalDatafiles();
		B4O b4o = new B4O();
		b4o.setConsiderFrequenciesOnly(false);
		b4o.setCacheScoreDistribution(false);
		b4o.setPrecalculateItemMaxs(false);
		b4o.setPrecalculateScoreDistribution(false);
		b4o.setup(data.graph, data.assoc);
		
		checkIntArrayContentsUnordered(new int[]{1,2},b4o.mostSpecificTerms(new int[]{0,1,2}));
		checkIntArrayContentsUnordered(new int[]{9,10,11,12,13,14},b4o.mostSpecificTerms(new int[]{0,1,2,3,4,5,6,7,8,9,10,11,12,13,14}));
	}
	
	/**
	 * A helper function to test similarity values of the internal ontology.
	 * 
	 * @param b4o
	 */
	private void checkInternalSimValues(B4O b4o)
	{
		/* Common ancestors */
		assertEquals(7,b4o.getCommonAncestorWithMaxIC(11,12));
		assertTrue(b4o.getCommonAncestorWithMaxIC(9,11) == 4 || b4o.getCommonAncestorWithMaxIC(9,11) == 6);
		
		/* Resnik */
		assertEquals(0.223144,b4o.resScoreMaxAvg(new int[]{7}, new int[]{8}),0.001);
		assertTrue(b4o.resScoreMaxAvg(new int[]{7}, new int[]{8}) == b4o.resScoreMaxAvg(new int[]{11}, new int[]{8}));

		/* Lin */
		assertEquals(0.243529,b4o.linScoreMaxAvg(new int[]{7}, new int[]{8}),0.001);
		assertEquals(1,b4o.linScoreMaxAvg(new int[]{12}, new int[]{12}),0.001);
		assertEquals(1,b4o.linScoreMaxAvg(new int[]{0}, new int[]{0}),0.001);
		
		/* JC */
		assertEquals(0.42,b4o.jcScoreMaxAvg(new int[]{7}, new int[]{8}),0.001);
		assertEquals(1,b4o.jcScoreMaxAvg(new int[]{12}, new int[]{12}),0.001);
		assertEquals(1,b4o.jcScoreMaxAvg(new int[]{0}, new int[]{0}),0.001);
		
		/* The follow to scores represent an example for avoiding similarity measure */
		
		/* The terms match the terms of the item */
		assertEquals(0.9163, b4o.resScoreVsItem(new int[]{3,10}, 2), 0.001);
		
		/* The terms don't match the terms of the item */
		assertEquals(1.26286432, b4o.resScoreVsItem(new int[]{9,10}, 2), 0.001);
		
		/* The terms match the terms of the item */
		assertEquals(1.26286432, b4o.resScoreVsItem(new int[]{12,9}, 0), 0.001);
		
		/* Some other values */
		assertEquals(0.91629073, b4o.resScoreVsItem(new int[]{10}, 0), 0.001);
		assertEquals(1.14733979, b4o.resScoreVsItem(new int[]{9,10,12}, 0), 0.001);


		/* Now a bigger test */
		
		/* Item 2 */
		boolean [] obs = new boolean[b4o.getSlimGraph().getNumberOfVertices()];
		int item = 2;
		Observations o = new Observations();
		o.observations = obs;
		System.out.println("Testing item " + item);

		for (int i=0;i<b4o.items2DirectTerms[item].length;i++)
		{
			int t=b4o.items2DirectTerms[item][i];
			obs[t] = true;
			b4o.activateAncestors(t, obs);
		}
		
		System.out.println("Observations");
		for (int i=0;i<b4o.getSlimGraph().getNumberOfVertices();i++)
			if (obs[i]) System.out.print(i + " ");
		System.out.println();

		Result resnikResult = b4o.resnikScore(obs, true, new Random(3));
		Result linResult = b4o.linScore(obs, true, new Random(3));
		Result jcResult = b4o.jcScore(obs, true, new Random(3));
		Result fabnResult = b4o.assignMarginals(o, false);

		System.out.println("Resnik");
		for (int i=0;i<b4o.allItemList.size();i++)
			System.out.println(i + "  p=" + resnikResult.getMarginal(i) + " score=" + resnikResult.getScore(i));

		System.out.println("Lin");
		for (int i=0;i<b4o.allItemList.size();i++)
			System.out.println(i + "  p=" + linResult.getMarginal(i) + " score=" + linResult.getScore(i));

		System.out.println("JC");
		for (int i=0;i<b4o.allItemList.size();i++)
			System.out.println(i + "  p=" + jcResult.getMarginal(i) + " score=" + jcResult.getScore(i));

		System.out.println("FABN");
		for (int i=0;i<b4o.allItemList.size();i++)
			System.out.println(i + " marg=" + fabnResult.getMarginal(i) + " score=" + fabnResult.getScore(i));

		double [] resnikP = new double[] { 0.64384, 0.843968, 0.15518, 0.844376, 0.68832 };
		double [] resnikScore = new double[] { 0.4581453659370775, 0.2231435513142097, 0.916290731874155, 0.2231435513142097, 0.11157177565710485 };
		double [] linScore;
		double [] linP;
		double [] jcScore;
		double [] jcP;
		double [] fabnScores = new double[] { -0.9724346539489889, -1.3317577761951482, 2.2926928336673593, -0.6160410530454002, -0.6160410530454002 };
		double [] fabnMarginals = new double[] { 0.032533088008779756, 0.022712933995512156, 0.8518284456000037, 0.046462766197852126, 0.046462766197852126 };

		for (int i=0;i<b4o.allItemList.size();i++)
		{
			System.out.println(i);
			assertEquals(resnikP[i], resnikResult.getMarginal(i), 0.05);
			assertEquals(resnikScore[i], resnikResult.getScore(i), 0.0001);
			assertEquals(fabnScores[i], fabnResult.getScore(i), 0.0001);
			assertEquals(fabnMarginals[i], fabnResult.getMarginal(i), 0.0001);
		}

		/* Item 3 */
		obs = new boolean[b4o.getSlimGraph().getNumberOfVertices()];
		item = 3;
		o = new Observations();
		o.observations = obs;

		System.out.println("Testing item " + item);

		for (int i=0;i<b4o.items2DirectTerms[item].length;i++)
		{
			int t=b4o.items2DirectTerms[item][i];
			obs[t] = true;
			b4o.activateAncestors(t, obs);
		}
		
		resnikResult = b4o.resnikScore(obs, true, new Random(3));
		linResult = b4o.linScore(obs, true, new Random(3));
		jcResult = b4o.jcScore(obs, true, new Random(3));
		fabnResult = b4o.assignMarginals(o, false);
		
		System.out.println("Resnik");
		for (int i=0;i<b4o.allItemList.size();i++)
			System.out.println(i + "  p=" + resnikResult.getMarginal(i) + " score=" + resnikResult.getScore(i));

		System.out.println("Lin");
		for (int i=0;i<b4o.allItemList.size();i++)
			System.out.println(i + "  p=" + linResult.getMarginal(i) + " score=" + linResult.getScore(i));

		System.out.println("JC");
		for (int i=0;i<b4o.allItemList.size();i++)
			System.out.println(i + "  p=" + jcResult.getMarginal(i) + " score=" + jcResult.getScore(i));

		System.out.println("FABN");
		for (int i=0;i<b4o.allItemList.size();i++)
			System.out.println(i + " marg=" + fabnResult.getMarginal(i) + " score=" + fabnResult.getScore(i));

		resnikScore = new double[]{0.11157177565710485,0.2231435513142097,0.2231435513142097,0.916290731874155,0.11157177565710485};
		resnikP = new double[]{0.821472,0.843968,0.799752,0.200844,0.68832};
		linScore = new double[]{0.19583714006727046,0.2841853289422175,0.24352920263396993,1.0,0.08834818887494705};
		linP = new double[]{0.821472,0.754716,0.755148,0.044964,0.755296};
		jcScore = new double[]{0.43712271203780284,0.4576751569317141,0.5552427170907795,0.9999999999999998,0.6003949092992065};
		jcP = new double[]{0.889184,0.866124,0.6225,0.044964,0.622036};
		fabnScores = new double[]{-1.633587994564142,-1.633587994564142,-1.3091882336053455,2.196264120323024,-1.3063742210362417};
		fabnMarginals = new double[]{0.019674959868555657,0.019674959868555657,0.027214407363261438,0.9061445759999325,0.027291096899695524};

		for (int i=0;i<b4o.allItemList.size();i++)
		{
			System.out.println(i);
			assertEquals(resnikP[i], resnikResult.getMarginal(i), 0.05);
			assertEquals(resnikScore[i], resnikResult.getScore(i), 0.0001);
			assertEquals(linP[i], linResult.getMarginal(i), 0.05);
			assertEquals(linScore[i], linResult.getScore(i), 0.0001);
			assertEquals(jcP[i], jcResult.getMarginal(i), 0.05);
			assertEquals(jcScore[i], jcResult.getScore(i), 0.0001);
			assertEquals(fabnScores[i], fabnResult.getScore(i), 0.0001);
			assertEquals(fabnMarginals[i], fabnResult.getMarginal(i), 0.0001);
		}
	}
	
	@Test
	public void testB4OOnInternalOntology() throws FileNotFoundException
	{
		final InternalDatafiles data = new InternalDatafiles();
		assertEquals(15,data.graph.getNumberOfTerms());

		final B4O b4o = new B4O();
		b4o.setConsiderFrequenciesOnly(false);
		b4o.setPrecalculateItemMaxs(true);
		b4o.setCacheScoreDistribution(true);
		b4o.setPrecalculateScoreDistribution(true);
		b4o.setStoreScoreDistriubtion(false);
		b4o.setTryLoadingScoreDistribution(false);
		b4o.setMaxQuerySizeForCachedDistribution(4);
		b4o.setup(data.graph, data.assoc);
		
		/* Write out the graph with ICs */
		data.getGraphWithItems().writeDOT(new FileOutputStream("full-with-ics.dot"), new DotAttributesProvider<String>()
				{
					@Override
					public String getDotNodeAttributes(String vt)
					{
						if (vt.startsWith("C"))
						{
							StringBuilder info = new StringBuilder();
							for (Term t : data.graph)
							{
								if (t.getName().equals(vt))
								{
									int idx = b4o.slimGraph.getVertexIndex(t);
									info.append("\\n" + b4o.getNumberOfItemsAnnotatedToTerm(idx) + " " + String.format("%g",b4o.terms2IC[idx]));
									break;
								}
							}
							
							return "label=\""+vt+info.toString()+"\"";
							
						}
						else
							return "shape=\"box\",label=\""+vt+"\"";
					}
					
					@Override
					public String getDotEdgeAttributes(String src, String dest)
					{
						return "dir=\"back\"";
					}
				});


		System.out.println("Term Mapping");
		for (int i=0;i<b4o.slimGraph.getNumberOfVertices();i++)
			System.out.println(i + " ->  " + b4o.slimGraph.getVertex(i).getIDAsString());
		
		System.out.println("Item Mapping");
		for (int i=0;i<b4o.allItemList.size();i++)
			System.out.println(i + " -> " +b4o.allItemList.get(i));

		B4O b4oNoPrecalc = new B4O();
		b4oNoPrecalc.setConsiderFrequenciesOnly(false);
		b4oNoPrecalc.setPrecalculateItemMaxs(false);
		b4oNoPrecalc.setCacheScoreDistribution(false);
		b4oNoPrecalc.setPrecalculateScoreDistribution(false);
		b4oNoPrecalc.setStoreScoreDistriubtion(false);
		b4oNoPrecalc.setTryLoadingScoreDistribution(false);
		b4oNoPrecalc.setMaxQuerySizeForCachedDistribution(4);
		b4oNoPrecalc.setup(data.graph, data.assoc);
		
		checkInternalSimValues(b4o);
		checkInternalSimValues(b4oNoPrecalc);
	}

	@Test
	public void testBenchmarkOnInternalOntology() throws InterruptedException, IOException
	{
		final InternalDatafiles data = new InternalDatafiles();
		B4O b4o = new B4O();
		b4o.setConsiderFrequenciesOnly(false);
		b4o.setSizeOfScoreDistribution(1000);
		b4o.setTryLoadingScoreDistribution(false);
		b4o.setSimulationMaxTerms(3);
		b4o.setMaxQuerySizeForCachedDistribution(6);

		b4o.setup(data.graph, data.assoc);
		
		Benchmark benchmark = new Benchmark();
		benchmark.setResultBaseName("internal");
		benchmark.benchmark(b4o);
	}


	/**
	 * A helper function to check similarity values for the HPO.
	 * 
	 * @param b4o
	 */
	private void checkHPOSimValues(final B4O b4o)
	{
		assertEquals(0.006997929,b4o.resScoreMaxAvgVsItem(new int[]{10,12},4),0.000001);
		assertEquals(0.162568779,b4o.resScoreMaxAvgVsItem(new int[]{101,1222,1300,2011},78),0.000001);
	}


	@Test
	public void testVsOldItemMax() throws InterruptedException, IOException
	{
		final B4O b4o = new B4O();

		Datafiles df = hpo;

		b4o.setConsiderFrequenciesOnly(false);
		b4o.setCacheScoreDistribution(false);
		b4o.setPrecalculateScoreDistribution(false);
		b4o.setStoreScoreDistriubtion(false);
		b4o.setPrecalculateItemMaxs(true);
		b4o.setup(df.graph, df.assoc);

		/* This is older code which we keep for testing here */
		int [][] micaForItem = new int[b4o.allItemList.size()][b4o.slimGraph.getNumberOfVertices()];
		for (int item = 0; item < b4o.allItemList.size(); item++)
		{
			/* The fixed set */
			int [] t2 = b4o.items2DirectTerms[item];

			for (int to = 0; to < b4o.slimGraph.getNumberOfVertices(); to++)
			{
				double maxIC = Double.NEGATIVE_INFINITY;
				int maxCommon = -1;

				for (int ti : t2)
				{
					int common = b4o.getCommonAncestorWithMaxIC(to, ti);
					if (b4o.terms2IC[common] > maxIC)
					{
						maxIC = b4o.terms2IC[common];
						maxCommon = common;
					}
				}
				micaForItem[item][to] = maxCommon;
			}
		}
		
		/* Now the test */
		for (int i=0;i<micaForItem.length;i++)
			for (int j=0;j<micaForItem[i].length;j++)
				assertEquals(b4o.terms2IC[micaForItem[i][j]],b4o.resnikTermSim.maxScoreForItem[i][j],0.00001);

		checkHPOSimValues(b4o);
	}
	
	@Test
	public void testMostSpecificOnHPO() throws InterruptedException, IOException
	{
		final B4O b4o = new B4O();

		b4o.setConsiderFrequenciesOnly(false);
		b4o.setCacheScoreDistribution(false);
		b4o.setPrecalculateScoreDistribution(false);
		b4o.setStoreScoreDistriubtion(false);
		b4o.setPrecalculateItemMaxs(false);
		b4o.setSimulationMaxTerms(-1);		
		b4o.setup(hpo.graph, hpo.assoc);

		/* Check, mostSpecifc function */
		for (int i=0;i<5000;i++)
		{
			Random rnd = new Random(System.currentTimeMillis());
			
			int item = rnd.nextInt(b4o.allItemList.size());
			
			Observations obs = b4o.generateObservations(item, rnd);
			B4O.IntArray sparse = new B4O.IntArray(obs.observations);
			int [] mst = b4o.mostSpecificTerms(sparse.get());

			/* Get full observation according to mostSpecificTerms() */
			boolean [] actualObservations = new boolean[b4o.getSlimGraph().getNumberOfVertices()];
			for (int t : mst)
			{
				for (i = 0;i<b4o.term2Ancestors[t].length;i++)
					actualObservations[b4o.term2Ancestors[t][i]] = true;
			}
			
			/* Get full observations according to source array */
			boolean [] expectedObservations = new boolean[b4o.getSlimGraph().getNumberOfVertices()];
			for (int t : sparse.get())
			{
				for (i = 0;i<b4o.term2Ancestors[t].length;i++)
					expectedObservations[b4o.term2Ancestors[t][i]] = true;
			}

			for (i=0;i<actualObservations.length;i++)
				assertEquals(expectedObservations[i],actualObservations[i]);
		}
	}

	@Test
	public void testLargeNumberOfItems() throws IOException, OBOParserException
	{
		Random rnd = new Random(2);

		final B4O b4o = new B4O();

		OBOParser hpoParser = new OBOParser("../b4o/data/human-phenotype-ontology.obo.gz");
		hpoParser.doParse();
		TermContainer tc = new TermContainer(hpoParser.getTermMap(),hpoParser.getFormatVersion(),hpoParser.getDate());
		Ontology ontology = new Ontology(tc);
		SlimDirectedGraphView<Term> slim = ontology.getSlimGraphView();
		
		AssociationContainer assocs = new AssociationContainer();
		
		for (int i=0;i<10000;i++)
		{
			ByteString item = new ByteString("item" + i);

//			Association a = new Association(item,slim.getVertex(10).getIDAsString());
//			assocs.addAssociation(a);

			for (int j=0;j<rnd.nextInt(16)+2;j++)
			{
				Term t;
				do
				{
					t = slim.getVertex(rnd.nextInt(slim.getNumberOfVertices()));
				} while (t.isObsolete());
				Association a = new Association(item,t.getIDAsString());
				assocs.addAssociation(a);
			}
		}

		System.err.println("Constructed data set");
		b4o.setConsiderFrequenciesOnly(false);
		b4o.setPrecalculateScoreDistribution(false);
		b4o.setCacheScoreDistribution(false);
		b4o.setPrecalculateItemMaxs(false);
		b4o.setup(ontology, assocs);
		System.err.println("Setted up ontology and associations");

		Observations o = new Observations();
		o.observations = new boolean[b4o.getOntology().getNumberOfTerms()];

		long start = System.nanoTime();
		System.err.println("Calculating");
		Result result = b4o.assignMarginals(o, false, 1);
		long end = System.nanoTime();

		System.err.println(((end - start)/1000/1000) + "ms");
	}
}
