package sonumina.b4o.benchmark;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import ontologizer.association.AssociationContainer;
import ontologizer.dotwriter.AbstractDotAttributesProvider;
import ontologizer.dotwriter.GODOTWriter;
import ontologizer.go.Ontology;
import ontologizer.go.Term;
import ontologizer.go.TermID;
import sonumina.b4o.calculation.B4O;
import sonumina.b4o.calculation.Observations;
import sonumina.b4o.calculation.B4O.Result;
import sonumina.math.graph.SlimDirectedGraphView;

public class Benchmark
{
	private Ontology graph;
	private SlimDirectedGraphView<Term> slimGraph;
	private B4O b4o;

	/** Verbose output */
	private boolean VERBOSE;
	
	/** Use threading */
	private final boolean THREADING_IN_SIMULATION = true;


	/* TOBEREMOVED */
	
//	private final int MAX_SAMPLES = 5;
//	private boolean CONSIDER_FREQUENCIES_ONLY = true;
	private String RESULT_NAME = "benchmark.txt";
//	private final String [] evidenceCodes = null;//new String[]{"PCS","ICE"};
//	private int SIZE_OF_SCORE_DISTRIBUTION = 250000;
//	private final int NUMBER_OF_BINS_IN_APPROXIMATED_SCORE_DISTRIBUTION = 10000;
//	private int maxTerms = -1;						/* Defines the maximal number of terms a query can have */
//	private double ALPHA = 0.002;
//	private double BETA = 0.10;   // 0.1

//	private double ALPHA;
//	private double BETA;
	
	private int samplesPerItem = 5;
	
	static class ExperimentStore
	{
		Observations obs;
		Result modelWithoutFrequencies;
		Result modelWithFrequencies;
		Result resnik;
		Result lin;
		Result jc;
	}
	
	/**
	 * Sets the base name of the results to be written.
	 */
	public void setResultBaseName(String name)
	{
		RESULT_NAME = name + ".txt";
	}
	
	/**
	 * Sets the samples that are generated for each item during
	 * the simulation.
	 * 
	 * @param samplesPerItem
	 */
	public void setSamplesPerItem(int samplesPerItem)
	{
		this.samplesPerItem = samplesPerItem;
	}

	/**
	 * Processes the simulation and evaluation for the given item.
	 * 
	 * @param item
	 * 
	 * @returns an array of Results.
	 *  1. model without frequencies
	 *  2. model with frequencies
	 *  3. resnik (score and p value)
	 */
	private ExperimentStore processItem(int item, boolean provideGraph, Random rnd)
	{
		int i;
	
		Observations obs = b4o.generateObservations(item, rnd);
		
		boolean [] observations = obs.observations;

		/* First, without taking frequencies into account */
		Result modelWithoutFrequencies = b4o.assignMarginals(obs, false);
		
		/* Second, with taking frequencies into account */
		Result modelWithFrequencies = b4o.assignMarginals(obs, true);

		ExperimentStore id = new ExperimentStore();
		id.obs = obs;
		id.modelWithoutFrequencies = modelWithoutFrequencies;
		id.modelWithFrequencies = modelWithFrequencies;
		id.resnik = b4o.resnikScore(obs.observations, true, rnd);
		id.lin = b4o.linScore(obs.observations, true, rnd);
		id.jc = b4o.jcScore(obs.observations, true, rnd);
		
		/******** The rest is for debugging purposes ********/
		if (VERBOSE || provideGraph)
		{
			class Pair implements Comparable<Pair>
			{ 
				double score; int idx;
				public Pair(int idx, double score)
				{
					this.idx = idx;
					this.score = score;
				}
				
				public int compareTo(Pair o)
				{
					if (score <  o.score) return 1;
					else if (score > o.score) return -1;
					return 0;
				};
				
			};
	
			ArrayList<Pair> scoreList = new ArrayList<Pair>(b4o.allItemList.size());
			ArrayList<Pair> idealList = new ArrayList<Pair>(b4o.allItemList.size());
			for (i=0;i<b4o.allItemList.size();i++)
			{
				scoreList.add(new Pair(i,modelWithoutFrequencies.getScore(i)));
				idealList.add(new Pair(i,modelWithoutFrequencies.getMarginalIdeal(i)));
			}
			Collections.sort(scoreList);
			Collections.sort(idealList, new Comparator<Pair>() {
				public int compare(Pair o1, Pair o2)
				{
					if (o1.score > o2.score) return 1;
					if (o1.score < o2.score) return -1;
					return 0;
				};
			});
	
	
			/* Display top 10 */
			for (i=0;i<Math.min(10,scoreList.size());i++)
			{
				Pair p = scoreList.get(i);
				boolean itIs = p.idx == item;
				System.out.println((i+1) + (itIs?"(*)":"") + ": " + b4o.allItemList.get(p.idx) + ": " +  p.score + " " + modelWithoutFrequencies.getMarginal(p.idx));
			}
	
			int scoreRank  = 0;
			int marginalIdealRank = 0;
			
			/* And where the searched item is */
			for (i=0;i<scoreList.size();i++)
			{
				Pair p = scoreList.get(i);
	//			boolean itIs = p.idx == item;
				if (p.idx == item)
				{
					scoreRank = i + 1;
					break;
				}
			}
			
			for (i=0;i<idealList.size();i++)
			{
				Pair p = scoreList.get(i);
				if (p.idx == item)
				{
					marginalIdealRank = i + 1;
					break;
				}
			}
	
	//		System.out.println((i+1) + (itIs?"(*)":"") + ": " + allItemList.get(p.idx) + ": " +  p.score + " " + modelWithoutFrequencies.getMarginal(p.idx));
			System.out.println("Rank of searched item. Score: " + scoreRank + "  Ideal: " + marginalIdealRank + " ( " + modelWithoutFrequencies.getMarginalIdeal(item) + ")");
			
			System.out.println("Statistics of the searched item");
			System.out.println(modelWithoutFrequencies.getStats(item).toString());
			System.out.println("Statistics for the top item");
			System.out.println(modelWithoutFrequencies.getStats(scoreList.get(0).idx).toString());
			
	//		for (i=0;i<Stats.NodeCase.values().length;i++)
	//			System.out.println(" " + Stats.NodeCase.values()[i].name() + ": " + modelWithoutFrequencies.statsMatrix[item][i]);
	//
	//		System.out.println("Statistics for the top item");
	//		for (i=0;i<Stats.NodeCase.values().length;i++)
	//			System.out.println(" " + Stats.NodeCase.values()[i].name() + ": " + modelWithoutFrequencies.stateMatrix[scoreList.get(0).idx][i]);
	
			if (provideGraph)
			{
				/* Output the graph */
				final HashSet<TermID> hiddenSet = new HashSet<TermID>();
				for (i=0;i<b4o.getTermsDirectlyAnnotatedTo(item).length;i++)
					hiddenSet.add(slimGraph.getVertex(b4o.getTermsDirectlyAnnotatedTo(item)[i]).getID());
				final HashSet<TermID> observedSet = new HashSet<TermID>();
				for (i = 0;i<observations.length;i++)
				{
					if (observations[i])
						observedSet.add(slimGraph.getVertex(i).getID());
				}
				int topRankIdx = scoreList.get(0).idx;
				final HashSet<TermID> topRankSet = new HashSet<TermID>();
				for (i=0;i<b4o.getTermsDirectlyAnnotatedTo(topRankIdx).length;i++)
					topRankSet.add(slimGraph.getVertex(b4o.getTermsDirectlyAnnotatedTo(topRankIdx)[i]).getID());
		
				HashSet<TermID> allSet = new HashSet<TermID>();
				allSet.addAll(hiddenSet);
				allSet.addAll(observedSet);
				allSet.addAll(topRankSet);
				GODOTWriter.writeDOT(graph, new File("setting.dot"), null, allSet, new AbstractDotAttributesProvider() {
					public String getDotNodeAttributes(TermID id) {
						String fillcolor = "";
						String label = graph.getTerm(id).getName();
						String flags = "";
						if (hiddenSet.contains(id))
							fillcolor = ",style=filled,fillcolor=gray";
						
						if (topRankSet.contains(id))
							flags += "1";
						if (observedSet.contains(id))
							flags +="O";
		
						if (flags.length()!= 0)
							label += "("+flags+")";
		
						return "label=\""+label+"\"" + fillcolor;
					}
				});
			}
		}

		return id;
	}

	/**
	 * Main Entry.
	 * 
	 * @param args
	 * @throws InterruptedException
	 * @throws IOException
	 */
	@SuppressWarnings("unused")
	public void benchmark(B4O b4o) throws InterruptedException, IOException
	{
		int i;
		int numProcessors = B4O.getNumProcessors();

		/* TODO: Get rid of this ugliness */
		this.b4o = b4o;
		
		double ALPHA = b4o.getSimulationAlpha();
		double BETA = b4o.getSimulationBeta();
		int maxTerms = b4o.getSimulationMaxTerms();
		boolean CONSIDER_FREQUENCIES_ONLY = b4o.getConsiderFrequenciesOnly();
		
		graph = b4o.getOntology();
		slimGraph = b4o.getSlimGraph();
		
		/**************************************************************************************************************************/
		/* Write score distribution */
		
		if (false)
			b4o.writeScoreDistribution(new File("score-0.txt"),0);
		
		/**************************************************************************************************************************/

		/* Write example */

		HashSet<TermID> hpoTerms = new HashSet<TermID>();
		hpoTerms.add(new TermID("HP:0000822")); /* Hypertension */
		hpoTerms.add(new TermID("HP:0000875")); /* Episodic Hypertension */
		hpoTerms.add(new TermID("HP:0002621")); /* Atherosclerosis */

		b4o.writeDOTExample(new File("hpo-example.dot"), hpoTerms);

		/**************************************************************************************************************************/

		int firstItemWithFrequencies = -1;
		int numItemsWithFrequencies = 0;
		for (i=0;i<b4o.getNumberOfItems();i++)
		{
			if (b4o.hasItemFrequencies(i))
			{
				numItemsWithFrequencies++;
				if (firstItemWithFrequencies == -1)
					firstItemWithFrequencies = i;
			}
		}

		System.out.println("Items with frequencies " + numItemsWithFrequencies + "  First one: " +
							firstItemWithFrequencies + " which is " + (firstItemWithFrequencies!=-1?b4o.allItemList.get(firstItemWithFrequencies):""));
		
		/**************************************************************************************************************************/
		
		String evidenceString = "All";
		String [] evidenceCodes = b4o.getEvidenceCodes();
		if (evidenceCodes != null && evidenceCodes.length > 0)
		{
			StringBuilder evidenceBuilder = new StringBuilder();
			evidenceBuilder.append("\"");
			evidenceBuilder.append(evidenceCodes[0]);
			
			for (int a=0;a<evidenceCodes.length;a++)
				evidenceBuilder.append("," + evidenceCodes[a]);
			evidenceString = evidenceBuilder.toString();
		}

		/* Remember the parameter */
		BufferedWriter param = new BufferedWriter(new FileWriter(RESULT_NAME.split("\\.")[0]+ "_param.txt"));
		param.write("alpha\tbeta\tconsider.freqs.only\titems\tterms\tmax.terms\tmax.samples\tevidences\n");
		param.write(String.format("%g\t%g\t%b\t%d\t%d\t%d\t%d\t%s\n",ALPHA,BETA,CONSIDER_FREQUENCIES_ONLY,b4o.getNumberOfItems(),slimGraph.getNumberOfVertices(),maxTerms,samplesPerItem,evidenceString));
		param.flush();
		param.close();

		/* Write out r code to load matrix in */
		BufferedWriter load = new BufferedWriter(new FileWriter(RESULT_NAME.split("\\.")[0]+ "_load.R"));
		load.append("b4o.load.data<-function() {\n d<-read.table(");
		load.append("\"" + new File(RESULT_NAME).getAbsolutePath() + "\", ");
		load.append("colClasses=c(\"integer\",\"integer\",rep(\"numeric\",12),\"integer\"),h=F");
		load.append(")");
		load.append("\n colnames(d)<-c(\"run\",\"label\",\"score\",\"marg\",\"marg.ideal\", \"score.freq\",\"marg.freq\", \"marg.freq.ideal\", \"resnik.avg\", \"resnik.avg.p\", \"lin.avg\", \"lin.avg.p\", \"jc.avg\", \"jc.avg.p\", \"freq\");");
		load.append("\n return (d);");
		load.append("\n}\n");
		load.append("b4o.name<-\"");
		load.append(new File(RESULT_NAME).getAbsolutePath());
		load.append("\";\n");
		load.flush();
		load.close();
		
		final BufferedWriter out = new BufferedWriter(new FileWriter(RESULT_NAME));
		final BufferedWriter summary = new BufferedWriter(new FileWriter(RESULT_NAME.split("\\.")[0]+ "_summary.txt"));

		ExecutorService es = Executors.newFixedThreadPool(numProcessors);

		Random rnd = new Random(9);

		int run = 0;

		for (int sample = 0; sample < samplesPerItem; sample++)
		{
			for (i=0;i<b4o.getNumberOfItems();i++)
			{
				final long seed = rnd.nextLong();
				final int item = i;
				final int fixedRun = run++;

				Runnable thread = new Runnable()
				{
					public void run()
					{
						StringBuilder resultBuilder = new StringBuilder();
					
						System.out.println("Seed = " + seed + " run = " + fixedRun);
						
						ExperimentStore store = processItem(item,false,new Random(seed));
	
						for (int j=0;j<Benchmark.this.b4o.getNumberOfItems();j++)
						{
							resultBuilder.append(fixedRun);
							resultBuilder.append("\t");
							resultBuilder.append(item==j?1:0);
							resultBuilder.append("\t");
							resultBuilder.append(store.modelWithoutFrequencies.getScore(j));
							resultBuilder.append("\t");
							resultBuilder.append(store.modelWithoutFrequencies.getMarginal(j));
							resultBuilder.append("\t");
							resultBuilder.append(store.modelWithoutFrequencies.getMarginalIdeal(j));
							resultBuilder.append("\t");
							resultBuilder.append(store.modelWithFrequencies.getScore(j));
							resultBuilder.append("\t");
							resultBuilder.append(store.modelWithFrequencies.getMarginal(j));
							resultBuilder.append("\t");
							resultBuilder.append(store.modelWithFrequencies.getMarginalIdeal(j));
							resultBuilder.append("\t");
							resultBuilder.append(store.resnik.getScore(j));
							resultBuilder.append("\t");
							resultBuilder.append(store.resnik.getMarginal(j));
							resultBuilder.append("\t");
							resultBuilder.append(store.lin.getScore(j));
							resultBuilder.append("\t");
							resultBuilder.append(store.lin.getMarginal(j));
							resultBuilder.append("\t");
							resultBuilder.append(store.jc.getScore(j));
							resultBuilder.append("\t");
							resultBuilder.append(store.jc.getMarginal(j));
							resultBuilder.append("\t");
							resultBuilder.append(Benchmark.this.b4o.hasItemFrequencies(item)?1:0);
							resultBuilder.append("\n");
						}
	
						synchronized (out) {
							try {
								out.append(resultBuilder.toString());
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}

						String sum = fixedRun + "\t" + store.obs.observationStats.falsePositiveRate() + "\t" + store.obs.observationStats.falseNegativeRate() + "\n";

						synchronized (summary) {
							try {
								summary.write(sum);
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}
				};
				
				if (THREADING_IN_SIMULATION) es.execute(thread);
				else thread.run();
			}
		}

		es.shutdown();
		while (!es.awaitTermination(10, TimeUnit.SECONDS));

		synchronized (out) {
			out.close();
		}
		
		synchronized (summary) {
			summary.close();
		}
	}
}
