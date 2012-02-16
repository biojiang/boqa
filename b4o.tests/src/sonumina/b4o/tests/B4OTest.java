package sonumina.b4o.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
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

import org.junit.Test;

import sonumina.b4o.InternalDatafiles;
import sonumina.b4o.calculation.B4O;
import sonumina.b4o.calculation.B4O.Result;
import sonumina.b4o.calculation.Observations;
import sonumina.math.graph.AbstractGraph.DotAttributesProvider;
import sonumina.math.graph.SlimDirectedGraphView;

public class B4OTest {

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
		boolean [] obs = new boolean[b4o.getSlimGraph().getNumberOfVertices()];
		int item = 2;
		Observations o = new Observations();
		o.observations = obs;

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
		Result fabnResult = b4o.assignMarginals(o, false);

		for (int i=0;i<b4o.allItemList.size();i++)
			System.out.println(i + "  p=" + resnikResult.getMarginal(i) + " score=" + resnikResult.getScore(i));

		for (int i=0;i<b4o.allItemList.size();i++)
			System.out.println(i + " marg=" + fabnResult.getMarginal(i) + " score=" + fabnResult.getScore(i));


		double [] resnikP = new double[] {
			0.64384,0.843968,0.15518,0.844376,0.68832
		};
		
		double [] resnikScore = new double[] {
			0.4581453659370775, 0.2231435513142097, 0.916290731874155, 0.2231435513142097, 0.11157177565710485
		};
		
		double [] fabnScores = new double[] {
			-0.9724346539489889, -1.3317577761951482, 2.2926928336673593, -0.6160410530454002, -0.6160410530454002
		};
		
		double [] fabnMarginals = new double[] {
			0.032533088008779756, 0.022712933995512156, 0.8518284456000037, 0.046462766197852126, 0.046462766197852126
		};
		

		for (int i=0;i<b4o.allItemList.size();i++)
		{
			System.out.println(i);
			assertEquals(resnikP[i], resnikResult.getMarginal(i), 0.0001);
			assertEquals(resnikScore[i], resnikResult.getScore(i), 0.0001);
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
		b4o.setCacheScoreDistribution(false);
		b4o.setPrecalculateScoreDistribution(false);
		b4o.setStoreScoreDistriubtion(false);
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
		b4oNoPrecalc.setMaxQuerySizeForCachedDistribution(4);
		b4oNoPrecalc.setup(data.graph, data.assoc);
		
		checkInternalSimValues(b4o);
		checkInternalSimValues(b4oNoPrecalc);
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

		Datafiles df = new Datafiles("../b4o/data/human-phenotype-ontology.obo.gz","../b4o/data/phenotype_annotation.omim.gz");

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
