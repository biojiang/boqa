package sonumina.boqa.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import ontologizer.go.TermID;

import org.junit.Test;

import sonumina.boqa.InternalDatafiles;
import sonumina.boqa.calculation.BOQA;
import sonumina.boqa.calculation.Observations;

public class ObservationsTest {
	
	@Test
	public void testCreateFromSparseArray() throws InterruptedException, IOException
	{
		final InternalDatafiles data = new InternalDatafiles();
		BOQA boqa = new BOQA();
		boqa.setConsiderFrequenciesOnly(false);
		boqa.setSizeOfScoreDistribution(1000);
		boqa.setTryLoadingScoreDistribution(false);
		boqa.setSimulationMaxTerms(3);
		boqa.setPrecalculateItemMaxs(false);
		boqa.setPrecalculateScoreDistribution(false);
		boqa.setCacheScoreDistribution(false);

		boqa.setup(data.graph, data.assoc);

		Observations obs = Observations.createFromSparseOnArray(boqa, 0, 1);
		assertEquals(boqa.getOntology().getNumberOfTerms(), obs.observations.length);
		assertTrue(obs.observations[0]);
		assertTrue(obs.observations[1]);
		for (int i=2; i < obs.observations.length; i++)
			assertFalse(obs.observations[i]); 
		
		try
		{
			obs = Observations.createFromSparseOnArray(boqa, 19019901);
			assertFalse(true);
		} catch (IllegalArgumentException iaex)
		{
		}
		
		obs = Observations.createFromSparseOnArray(boqa, boqa.getSlimGraph().getVertex(1).getID());
		assertFalse(obs.observations[0]);
		assertTrue(obs.observations[1]);
		for (int i=2; i < obs.observations.length; i++)
			assertFalse(obs.observations[i]); 

		try
		{
			obs = Observations.createFromSparseOnArray(boqa, new TermID("GO:9999999"));
			assertFalse(true);
		} catch (IllegalArgumentException iaex)
		{
		}
	}
}
