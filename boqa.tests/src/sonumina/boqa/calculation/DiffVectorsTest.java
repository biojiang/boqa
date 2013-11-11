package sonumina.boqa.calculation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import ontologizer.benchmark.Datafiles;
import ontologizer.enumeration.GOTermEnumerator;
import ontologizer.enumeration.ItemEnumerator;
import ontologizer.go.Term;
import ontologizer.go.TermID;
import ontologizer.set.PopulationSet;
import ontologizer.types.ByteString;

import org.junit.Assert;
import org.junit.Test;

import sonumina.boqa.InternalDatafiles;
import sonumina.math.graph.SlimDirectedGraphView;

public class DiffVectorsTest
{
	/**
	 * Run the test for the given data files.
	 *
	 * @param datafiles
	 */
	private void testOnDatafiles(Datafiles datafiles) {
		SlimDirectedGraphView<Term> slimGraph = datafiles.graph.getSlimGraphView();

		PopulationSet allItems = new PopulationSet("all");
		allItems.addGenes(datafiles.assoc.getAllAnnotatedGenes());
		GOTermEnumerator termEnumerator = allItems.enumerateGOTerms(datafiles.graph, datafiles.assoc, null);
		ItemEnumerator itemEnumerator = ItemEnumerator.createFromTermEnumerator(termEnumerator);
		ArrayList<ByteString> itemList = new ArrayList<ByteString>();

		int numberOfTerms = slimGraph.getNumberOfVertices();
		int numberOfItems = termEnumerator.getGenes().size();
		int maxFrequencyTerms = 1;
		
		int [][] items2Terms = new int[numberOfItems][];
		double [][] items2TermFrequencies = new double[numberOfItems][];
		int [][] item2TermFrequenciesOrder = new int[numberOfItems][];
		int [][] items2DirectTerms = new int[numberOfItems][];
		int [][] terms2Ancestors = slimGraph.vertexAncestors;

		for (ByteString item : itemEnumerator)
		{
			itemList.add(item);
			int itemId = itemList.size() - 1;

			ArrayList<TermID> annotated = itemEnumerator.getTermsAnnotatedToTheItem(item);
			items2Terms[itemId] = new int[annotated.size()];
			for (int i=0; i < annotated.size(); i++)
				items2Terms[itemId][i] = slimGraph.getVertexIndex(datafiles.graph.getTerm(annotated.get(i)));

			annotated = itemEnumerator.getTermsDirectlyAnnotatedToTheItem(item);
			items2DirectTerms[itemId] = new int[annotated.size()];
			for (int i=0; i < annotated.size(); i++)
				items2DirectTerms[itemId][i] = slimGraph.getVertexIndex(datafiles.graph.getTerm(annotated.get(i)));

			items2TermFrequencies[itemId] = new double[annotated.size()];
			for (int i=0; i < annotated.size(); i++)
				items2TermFrequencies[itemId][i] = 1.0;

			item2TermFrequenciesOrder[itemId] = new int[annotated.size()];
			for (int i=0; i < annotated.size(); i++)
				item2TermFrequenciesOrder[itemId][i] = i;
		}

		DiffVectors dv = DiffVectors.createDiffVectors(maxFrequencyTerms, numberOfTerms, items2Terms, items2TermFrequencies, item2TermFrequenciesOrder, items2DirectTerms, terms2Ancestors);
		Assert.assertEquals(items2Terms[0].length, dv.diffOnTerms[0].length);
		Assert.assertEquals(items2Terms.length,dv.diffOnTerms.length);
		Assert.assertEquals(0, dv.diffOffTerms[0].length);

		for (int i=1; i<items2Terms.length; i++)
		{
			int [] diff = Util.setDiff(items2Terms[i], items2Terms[i-1]);
			Arrays.sort(diff);
			Arrays.sort(dv.diffOnTerms[i]);
			Assert.assertArrayEquals(diff, dv.diffOnTerms[i]);
		}
	}

	@Test
	public void testDiffVectorsOnInternal()
	{
		InternalDatafiles datafiles = new InternalDatafiles();
		testOnDatafiles(datafiles);
	}

	@Test
	public void testDiffVectorsOnHPO() throws InterruptedException, IOException
	{
		Datafiles hpo = new Datafiles("../boqa/data/human-phenotype-ontology.obo.gz","../boqa/data/phenotype_annotation.omim.gz");
		testOnDatafiles(hpo);
	}
}
