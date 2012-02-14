package sonumina.b4o.tests;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Random;

import ontologizer.association.Association;
import ontologizer.association.AssociationContainer;
import ontologizer.dotwriter.AbstractDotAttributesProvider;
import ontologizer.dotwriter.GODOTWriter;
import ontologizer.go.OBOParser;
import ontologizer.go.OBOParserException;
import ontologizer.go.Ontology;
import ontologizer.go.Term;
import ontologizer.go.TermContainer;
import ontologizer.go.TermID;
import ontologizer.types.ByteString;

import org.junit.Test;

import sonumina.b4o.InternalDatafiles;
import sonumina.b4o.calculation.B4O;
import sonumina.b4o.calculation.Observations;
import sonumina.b4o.calculation.B4O.Result;
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

	@Test
	public void test()
	{
		InternalDatafiles data = new InternalDatafiles();
		int terms = data.graph.getNumberOfTerms();
		assertEquals(15,terms);

		final B4O b4o = new B4O();

		Random rnd = new Random(2);

		b4o.setConsiderFrequenciesOnly(false);
		b4o.setMaxQuerySizeForCachedDistribution(4);

		b4o.setup(data.graph, data.assoc);
		
		/* Write out the graph */
		GODOTWriter.writeDOT(b4o.graph, new File("example2.dot"), null, new HashSet<TermID>(b4o.graph.getLeafTermIDs()), new AbstractDotAttributesProvider() {
			public String getDotNodeAttributes(TermID id) {
				Term t = b4o.graph.getTerm(id);
				int idx = b4o.slimGraph.getVertexIndex(t);
				return "shape=\"box\",label=\""+b4o.graph.getTerm(id).getName()+"\\n" + b4o.getNumberOfItemsAnnotatedToTerm(idx) + " " + String.format("%g",b4o.terms2IC[idx]) + "\"";
			}
		});

		
		/* An example for avoiding similarity measure */
		assertEquals(0.9163, b4o.simScoreVsItem(new int[]{3,10},2), 0.001);
		assertEquals(1.26286432, b4o.simScoreVsItem(new int[]{9,10},2), 0.001);
		
		System.out.println("Mapping");
		for (int i=0;i<b4o.slimGraph.getNumberOfVertices();i++)
		{
			System.out.println(i + " " + b4o.slimGraph.getVertex(i).getIDAsString());
		}

		boolean [] obs = new boolean[terms];

		for (int i=0;i<b4o.items2DirectTerms[2].length;i++)
		{
			int t=b4o.items2DirectTerms[2][i];
			obs[t] = true;
			b4o.activateAncestors(t, obs);
		}

		Result result = b4o.resnikScore(obs, true, rnd);
		
		for (int i=0;i<b4o.allItemList.size();i++)
			System.out.println(result.getMarginal(i) + " " + result.getScore(i));

		Observations o = new Observations();
		o.observations = obs;
		
		result = b4o.assignMarginals(o, false);
		for (int i=0;i<b4o.allItemList.size();i++)
			System.out.println(result.getMarginal(i) + " " + result.getScore(i));
		
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
