package sonumina.b4o.calculation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ontologizer.association.Association;
import ontologizer.association.AssociationContainer;
import ontologizer.association.Gene2Associations;
import ontologizer.dotwriter.AbstractDotAttributesProvider;
import ontologizer.dotwriter.GODOTWriter;
import ontologizer.enumeration.GOTermEnumerator;
import ontologizer.enumeration.ItemEnumerator;
import ontologizer.go.Ontology;
import ontologizer.go.Term;
import ontologizer.go.TermID;
import ontologizer.set.PopulationSet;
import ontologizer.types.ByteString;
import sonumina.math.graph.SlimDirectedGraphView;

/**
 * Class representing different sets of queries.
 * 
 * @author Sebastian Bauer
 */
class QuerySets
{
	private int [][][] queries;
	
	public QuerySets(int maxSizes)
	{
		queries = new int[maxSizes][][];
	}
	
	public int [][] getQueries(int querySize)
	{
		return queries[querySize];
	}
	
	public void setQueries(int querySize, int [][] querySets)
	{
		queries[querySize] = querySets;
	}
}

/**
 * Basic distribution
 *
 * @author sba
 */
class Distribution
{
	private int [] cum;
	private double max;
	
	public Distribution(int [] counts, double max)
	{
		cum = new int[counts.length+1];
		for (int i=1;i<counts.length+1;i++)
			cum[i] = cum[i-1] + counts[i-1];
		this.max = max;
	}
	
	/**
	 * Returns the p value of the given score
	 * 
	 * @param score
	 * @return
	 */
	double getP(double score)
	{
		if (score > max) return 1.0 / cum.length;
		int bin = getBin(score,max);
		return 1 - (cum[bin] / (double)cum[cum.length - 1]);
	}
	
	static int getBin(double score, double maxScore)
	{
		int bin = (int)java.lang.Math.floor(score / maxScore * 999.0);
		return bin;
	}
}

/**
 * Basic container for distributions.
 * 
 * @author Sebastian Bauer
 */
class Distributions
{
	private Distribution[] distr;
	
	Distributions(int numberOfDistributions)
	{
		distr = new Distribution[numberOfDistributions];
	}
	
	public Distribution getDistribution(int i)
	{
		return distr[i];
	}
	
	public void setDistribution(int i, Distribution dist)
	{
		distr[i] = dist;
	}
}

/**
 * This is our class implementing the calculation.
 * 
 * @author Sebastian Bauer
 */
public class B4O
{
	public static Ontology graph;
	public static AssociationContainer assoc;
	
	/** Term enumerator */
	private static GOTermEnumerator termEnumerator;
	
	/** Slim variant of the graph */
	public static SlimDirectedGraphView<Term> slimGraph;

	/** An array of all items */
	public static ArrayList<ByteString> allItemList;
	
	/** Map items to their index */
	public static HashMap<ByteString,Integer> item2Index;

	/** Links items to terms */
	public static int [][] items2Terms;

	/** Links items to directly associated terms */
	public static int [][] items2DirectTerms;

	/**
	 * Links items to the frequencies of corresponding directly associated terms.
	 * Frequencies are interpreted as probabilities that the corresponding term
	 * is on.
	 */ 
	public static double [][] items2TermFrequencies;
	
	/**
	 * This contains the (ascending) order of the items2TermFrequencies,
	 * E.g., use item2TermFrequenciesOrder[0][2] to determine the term
	 * that is associated to first item and has the third lowest frequency.
	 */
	public static int [][] item2TermFrequenciesOrder;
	
	/** Indicates whether an item have explicit frequencies */
	public static boolean [] itemHasFrequencies;
	
	/** Contains all the ancestors of the terms */
	public static int [][] term2Ancestors;
	
	/** Contains the parents of the terms */
	public static int [][] term2Parents;
	
	/** Contains the children of the term */
	public static int [][] term2Children;
	
	/** Contains the descendants of the (i.e., children, grand-children, etc.) */
	public static int [][] term2Descendants;

	/** Contains the order of the terms */
	public static int [] termsInTopologicalOrder;
	
	/** Contains the topological rank of the term */
	public static int [] termsToplogicalRank;

	/** Contains the IC of the terms */
	public static double [] terms2IC;

	/** Contains the term with maximum common ancestor of two terms */
	private static int micaMatrix[][];

	/** Contains for each item the max mica for the given term */ 
	private static int [][] micaForItem;
	
	/** Contains the query cache, needs to be synched when accessed */
	private static QuerySets queryCache;

	/** Contains the directory where the score distribution is stored */
	private static File scoreDistributionDirectory;
	
	/** Stores the score distribution */
	private static Distributions scoreDistributions;
	
	/** Used to parse frequency information */
	public static Pattern frequencyPattern = Pattern.compile("(\\d+)\\.?(\\d*)\\s*%");
	public static Pattern frequencyFractionPattern = Pattern.compile("(\\d+)/(\\d+)");
	
	/* Settings */
//	private static String ontologyPath = "http://www.human-phenotype-ontology.org/human-phenotype-ontology.obo.gz";
//	private static String annotationPath = "http://www.human-phenotype-ontology.org/phenotype_annotation.omim.gz";

	private final static double ALPHA = 0.001; // 0.01
	private final static double BETA = 0.10;   // 0.1
	private final static double ALPHA_GRID[] = new double[]{1e-10,0.0005,0.001,0.005,0.01,0.05,0.1};
	private final static double BETA_GRID[] = new double[] {1e-10,0.005,0.01,0.05,0.1,0.2};
	
//	private final static int MAX_SAMPLES = 1;
//	private final static boolean CONSIDER_FREQUENCIES_ONLY = false;
//	private final static String RESULT_NAME = "fnd.txt";
//	private final static String [] evidenceCodes = null;
//	private final static int SIZE_OF_SCORE_DISTRIBUTION = 250000;

	private final static int MAX_SAMPLES = 5;
	private final static boolean CONSIDER_FREQUENCIES_ONLY = true;
	private final static String RESULT_NAME = "fnd-freq-only.txt";
	private final static String [] evidenceCodes = null;//new String[]{"PCS","ICE"};
	private final static int SIZE_OF_SCORE_DISTRIBUTION = 250000;
	
	
	/** False positives can be explained via inheritance */
	private static int VARIANT_INHERITANCE_POSITIVES = 1<<0;

	/** False negatives can be explained via inheritance */
	private static int VARIANT_INHERITANCE_NEGATIVES = 1<<1;

	/** Model respects frequencies */
	private static int VARIANT_RESPECT_FREQUENCIES = 1<<2;

	/** Defines the model as a combination of above flags */
	private static int MODEL_VARIANT = VARIANT_RESPECT_FREQUENCIES | VARIANT_INHERITANCE_NEGATIVES;// | VARIANT_INHERITANCE_POSITIVES;

	/** If set to true, empty observation are allowed */
	private static boolean ALLOW_EMPTY_OBSERVATIONS = false;
	
	/** Activate debugging */
	private static final boolean DEBUG = false;
	
	/** Use threading */
	private static final boolean THREADING = true;
	
	/** Use cached MaxIC terms. Speeds up Resnik */
	private static final boolean PRECALCULATE_MAXICS = true;
	
	/** Use precalculated max items. Speeds up Resnik */
	private static final boolean PRECALCULATE_ITEM_MAXS = true;
	
	/** Cache the queries */
	private static final boolean CACHE_RANDOM_QUERIES = true; 

	/** Forbid illegal queries */
	private static final boolean FORBID_ILLEGAL_QUERIES = true;

	/** Cache the score distribution */
	private static final boolean CACHE_SCORE_DISTRIBUTION = false; 

	/** Defines the maximal query size for the cached distribution */
	private static final int MAX_QUERY_SIZE_FOR_CACHED_DISTRIBUTION = 20;

	/** Some more verbose output */
	private static final boolean VERBOSE = false;

	/**
	 * Returns whether false negatives are propagated in a
	 * top-down fashion.
	 * 
	 * @return
	 */
	public static boolean areFalseNegativesPropagated()
	{
		return (MODEL_VARIANT & VARIANT_INHERITANCE_POSITIVES) != 0;
	}
	
	/**
	 * Returns whether false positives are propagated in a
	 * bottom-up fashion.
	 * 
	 * @return
	 */
	public static boolean areFalsePositivesPropagated()
	{
		return (MODEL_VARIANT & VARIANT_INHERITANCE_NEGATIVES) != 0;
	}
	
	/**
	 * Returns whether all false stuff is propagated.
	 *  
	 * @return
	 */
	public static boolean allFalsesArePropagated()
	{
		return areFalseNegativesPropagated() && areFalsePositivesPropagated();
	}
	
	/**
	 * Returns whether frequencies should be respected.
	 * 
	 * @return
	 */
	public static boolean respectFrequencies()
	{
		return (MODEL_VARIANT & VARIANT_RESPECT_FREQUENCIES) != 0;
	}

	/**
	 * Samples from the LPD of the node
	 * 
	 * @param rnd
	 * @param node
	 * @param hidden
	 * @param observed
	 * @return
	 */
	private static boolean observeNode(Random rnd, int node, boolean [] hidden, boolean [] observed)
	{
		if (areFalsePositivesPropagated())
		{
			/* Here, we consider that false positives will be inherited */
			for (int i=0;i<term2Children[node].length;i++)
			{
				int chld = term2Children[node][i];
				if (observed[chld])
					return true;
			}
		}
		
		if (areFalseNegativesPropagated())
		{
			/* Here, we consider that false negatives will be inherited */
			for (int i=0;i<term2Parents[node].length;i++)
			{
				int parent = term2Parents[node][i];
				if (!observed[parent])
					return false;
			}
		}

		if (hidden[node])
			return rnd.nextDouble() > BETA; /* false negative */
		else
			return rnd.nextDouble() < ALPHA; /* false positive */
	}

	/**
	 * Returns the case for the given node, given the hidden and observed states.
	 * 
	 * @param node
	 * @param hidden
	 * @param observed
	 * @return
	 */
	private static Configuration.NodeCase getNodeCase(int node, boolean [] hidden, boolean [] observed)
	{
		if (areFalsePositivesPropagated())
		{
			/* Here, we consider that false positives are inherited */
			for (int i=0;i<term2Children[node].length;i++)
			{
				int chld = term2Children[node][i];
				if (observed[chld])
				{
					if (observed[node]) return Configuration.NodeCase.INHERIT_TRUE;
					else
					{
						/* NaN */
						System.err.println("A child of a node is on although the parent is not: Impossible configuration encountered!");
						return Configuration.NodeCase.FAULT;
					}
				}
			}
		}

		if (areFalseNegativesPropagated())
		{
			/* Here, we consider that false negatives are inherited */
			for (int i=0;i<term2Parents[node].length;i++)
			{
				int parent = term2Parents[node][i];
				if (!observed[parent])
				{
					if (!observed[node]) return Configuration.NodeCase.INHERIT_FALSE;
					else
					{
						/* NaN */
						System.err.println("A parent of a node is off although the child is not: Impossible configuration encountered!");
						return Configuration.NodeCase.FAULT;
					}
				}
			}
		}

		if (hidden[node])
		{
			/* Term is truly on */
			if (observed[node]) return Configuration.NodeCase.TRUE_POSITIVE;
			else return Configuration.NodeCase.FALSE_NEGATIVE;
		} else
		{
			/* Term is truly off */
			if (!observed[node]) return Configuration.NodeCase.TRUE_NEGATIVE;
			else return Configuration.NodeCase.FALSE_POSITIVE;
		}
	}

	/**
	 * Determines the cases of the observed states given the hidden states. Accumulates them in
	 * states.
	 * 
	 * @param observedTerms
	 * @param hidden
	 * @param stats
	 */
	private static void determineCases(boolean [] observedTerms, boolean [] hidden, Configuration stats)
	{
		for (int i=0;i<slimGraph.getNumberOfVertices();i++)
		{
			Configuration.NodeCase c = getNodeCase(i,hidden,observedTerms);
			stats.increment(c);
		}
	}
	
	static final boolean MEASURE_TIME = true;

	static long timeDuration;
	
	/**
	 * 
	 * 
	 * @param item
	 * @param observed
	 * @param takeFrequenciesIntoAccount
	 * @return
	 */
	private static WeightedConfigurationList determineCasesForItem(int item, boolean [] observed, boolean takeFrequenciesIntoAccount)
	{
		int numTerms = items2TermFrequencies[item].length;
		int numTermsWithExplicitFrequencies = 0;

		long now;
		
		if (MEASURE_TIME)
			now = System.nanoTime();

		if (takeFrequenciesIntoAccount)
		{
			/* Determine the number of terms that have non-1.0 frequency. We restrict them
			 * to the top 6 (the less probable) due to complexity issues and hope that this
			 * a good enough approximation. */
			for (int i=0;i<numTerms && i<6;i++)
			{
				if (items2TermFrequencies[item][item2TermFrequenciesOrder[item][i]] >= 1.0)
					break;
				numTermsWithExplicitFrequencies++;
			}
		}

		/* We try each possible activity/inactivity combination of terms with explicit frequencies */
		SubsetGenerator sg = new SubsetGenerator(numTermsWithExplicitFrequencies,numTermsWithExplicitFrequencies);//numTermsWithExplicitFrequencies);
		SubsetGenerator.Subset s;
		
		/* Tracks the hidden state configuration that matches the observed state best */
//		double bestScore = Double.NEGATIVE_INFINITY;
//		boolean [] bestTaken = new boolean[numTermsWithExplicitFrequencies];
		
		WeightedConfigurationList statsList = new WeightedConfigurationList();

		while ((s = sg.next()) != null)
		{
			double factor = 0.0;

			boolean [] hidden = new boolean[slimGraph.getNumberOfVertices()];
			boolean [] taken = new boolean[numTermsWithExplicitFrequencies];
			
			/* first, activate variable terms according to the current selection */
			for (int i=0;i<s.r;i++)
			{
				int ti = item2TermFrequenciesOrder[item][s.j[i]]; /* index of term within the all directly associated indices */
				int h = items2DirectTerms[item][ti];			  /* global index of term */
				hidden[h] = true;
				activateAncestors(h, hidden);
				factor += Math.log(items2TermFrequencies[item][ti]);
				taken[s.j[i]] = true;
			}
			
			for (int i=0;i<numTermsWithExplicitFrequencies;i++)
			{
				if (!taken[i])
					factor += Math.log(1 - items2TermFrequencies[item][item2TermFrequenciesOrder[item][i]]);
			}

			/* second, activate mandatory terms */
			for (int i=numTermsWithExplicitFrequencies;i<numTerms;i++)
			{
				int ti = item2TermFrequenciesOrder[item][i];
				int h = items2DirectTerms[item][ti];  /* global index of term */
				hidden[h] = true;
				activateAncestors(h, hidden);
			}

			/* Determine cases and store */
			Configuration stats = new Configuration();
			determineCases(observed, hidden, stats);
			statsList.add(stats,factor);
		}
		
		if (MEASURE_TIME)
		{
			timeDuration += System.nanoTime() - now;
			System.out.println(timeDuration / (1000 * 1000) + " " + statsList.size());
		}

		return statsList;
	}
	

	/**
	 * Returns the log probability that the given term has the observed state given the hidden states.
	 * 
	 * If one of its more specific terms (descendants in this case) are on then the probability that the observed term is on is one.
	 * Otherwise the probability depends on the false-positive/false-negative rate.
	 * 
	 * @param termIndex
	 * @param alpha
	 * @param beta
	 * @param hidden
	 * @param observed
	 * @return
	 */
	public static double scoreNode(int termIndex, double alpha, double beta, boolean [] hidden, boolean [] observed)
	{
		double score = 0.0;

		Configuration.NodeCase c = getNodeCase(termIndex,hidden,observed);

		switch (c)
		{
			case	FALSE_NEGATIVE: score = Math.log(beta); break;
			case	FALSE_POSITIVE: score = Math.log(alpha); break;
			case	TRUE_POSITIVE: score = Math.log(1-beta); break;
			case	TRUE_NEGATIVE: score = Math.log(1-alpha); break;
			case	INHERIT_FALSE: score = Math.log(1); break;
			case	INHERIT_TRUE: score = Math.log(1); break;
		}
		return score;
	}

	/**
	 * Score a hidden configuration given the observations.
	 * 
	 * @param observedTerms
	 * @param stats
	 * @param score
	 * @param hidden
	 * @return
	 */
	@SuppressWarnings("unused")
	private static double scoreHidden(boolean [] observedTerms, double alpha, double beta, boolean [] hidden)
	{
		Configuration stats = new Configuration();
		determineCases(observedTerms, hidden, stats);
		double newScore = stats.getScore(alpha,beta);
		return newScore;
	}

	/**
	 * Calculates the score, when the given item is activated.
	 * 
	 * @param item which is supposed to be active.
	 * @param observedTerms
	 * @param stats, some statistics about false positives etc.
	 * @param takeFrequenciesIntoAccount
	 * @return
	 */
	public static double score(int item, double alpha, double beta, boolean [] observedTerms, boolean takeFrequenciesIntoAccount)
	{
		WeightedConfigurationList stats = determineCasesForItem(item,observedTerms,takeFrequenciesIntoAccount);
		return stats.score(alpha,beta);
	}

	/**
	 * Returns the result of a logical or operation of the parents state.
	 * 
	 * @param v
	 * @param states
	 * @return
	 */
	public static boolean orParents(int v, boolean [] states)
	{
		int [] parents = term2Parents[v];
		for (int i=0;i<parents.length;i++)
			if (states[parents[i]]) return true;
		return false;
	}

	/**
	 * Returns the result of a logical and operation of the parents state.
	 * 
	 * @param v
	 * @param states
	 * @return
	 */
	public static boolean andParents(int v, boolean [] states)
	{
		int [] parents = term2Parents[v];
		for (int i=0;i<parents.length;i++)
			if (!states[parents[i]]) return false;
		return true;
	}

	/**
	 * Returns the result of a logical and operation of the children state.
	 * 
	 * @param v
	 * @param states
	 * @return
	 */
	public static boolean andChildren(int v, boolean [] states)
	{
		int [] children = term2Children[v];
		for (int i=0;i<children.length;i++)
			if (!states[children[i]]) return false;
		return true;
	}

	/**
	 * Returns the result of a logical or operation of the children state.
	 * 
	 * @param v
	 * @param states
	 * @return
	 */
	public static boolean orChildren(int v, boolean [] states)
	{
		int [] children = term2Children[v];
		for (int i=0;i<children.length;i++)
			if (states[children[i]]) return true;
		return false;
	}

	/**
	 * Generates observation according to the model parameter.
	 * 
	 * @param item
	 * @return
	 */
	public static Observations generateObservations(int itemNr, Random rnd)
	{
		int retry = 0;

		Observations o = null;;
		
		do
		{
			int i;
			int [] falsePositive = new int[slimGraph.getNumberOfVertices()];
			int numFalsePositive = 0;
			int [] falseNegative = new int[slimGraph.getNumberOfVertices()];
			int numFalseNegative = 0;
			int numMissedInHidden = 0;
	
			int numPositive = 0;
			int numHidden = 0;
	
			boolean [] observations = new boolean[slimGraph.getNumberOfVertices()];
			boolean [] hidden = new boolean[slimGraph.getNumberOfVertices()];
		
			boolean CONSIDER_ONLY_DIRECT_ASSOCIATIONS = true;
	
			if (CONSIDER_ONLY_DIRECT_ASSOCIATIONS)
			{
				System.out.println("Item " + itemNr + " has " + items2DirectTerms[itemNr].length + " annotations");
				for (i=0;i<items2DirectTerms[itemNr].length;i++)
				{
					boolean state = true;
				
					if (respectFrequencies())
					{
						state = rnd.nextDouble() < items2TermFrequencies[itemNr][i];
						
						if (VERBOSE)
							System.out.println(items2DirectTerms[itemNr][i] + "(" + items2TermFrequencies[itemNr][i] + ")="+state);
					}
					
					if (state)
					{
						hidden[items2DirectTerms[itemNr][i]] = state;
						observations[items2DirectTerms[itemNr][i]] = state;
						
						activateAncestors(items2DirectTerms[itemNr][i], hidden);
						activateAncestors(items2DirectTerms[itemNr][i], observations);
						
						numPositive++;
					} else
					{
						numMissedInHidden++;
					}
				}
				
			} else
			{
				for (i=0;i<items2Terms[itemNr].length;i++)
				{
					hidden[items2Terms[itemNr][i]] = true;
					observations[items2Terms[itemNr][i]] = true;
					numPositive++;
				}
			}
	
			/* Fill in false and true positives */
			for (i=0;i<observations.length;i++)
			{
				double r = rnd.nextDouble();
				if (observations[i])
				{
					if (r<BETA)
					{
						falseNegative[numFalseNegative++] = i;
//						System.out.println("false negative " + i);
					}
				}	else
				{
					if (r<ALPHA)
					{
						falsePositive[numFalsePositive++] = i;
//						System.out.println("false positive " + i);
					}
				}
			}
	
			/* apply false negatives */
			if (areFalseNegativesPropagated())
			{
				/* false negative, but also make all descendants negative. They are considered as inherited in this case */
				for (i=0;i<numFalseNegative;i++)
				{
					observations[falseNegative[i]] = false;
					deactivateDecendants(falseNegative[i], observations);
				}
			} else
			{
				/* false negative */
				for (i=0;i<numFalseNegative;i++)
					observations[falseNegative[i]] = false;
			
				/* fix for true path rule */ 
				for (i=0;i<observations.length;i++)
				{
					if (observations[i])
						activateAncestors(i, observations);
				}
			}
	
			/* apply false positives */
			if (areFalsePositivesPropagated())
			{
				/* fix for true path rule */
				for (i=0;i<numFalsePositive;i++)
				{
					observations[falsePositive[i]] = true;
					activateAncestors(falsePositive[i], observations);
				}
			} else
			{
				/* False positive */
				for (i=0;i<numFalsePositive;i++)
					observations[falsePositive[i]] = true;
	
				/* fix for the true path rule (reverse case) */
				for (i=0;i<observations.length;i++)
				{
					if (!observations[i])
						deactivateDecendants(i, observations);
				}
			}
			
			for (i=0;i<hidden.length;i++)
				if (hidden[i]) numHidden++;
	
			System.out.println("Number of terms that were missed in hidden: " + numMissedInHidden);
			System.out.println("Number of hidden positives:" + numPositive);
			System.out.println("Number of hidden negatives: " + numHidden);
			
			numPositive = 0;
			numFalseNegative = 0;
			numFalsePositive = 0;
			for (i=0;i<observations.length;i++)
			{
				if (observations[i])
				{
					if (!hidden[i]) numFalsePositive++;
					numPositive++;
				} else
				{
					if (hidden[i]) numFalseNegative++;
				}
			}
	
			System.out.println("Number of observed positives:" + numPositive);
			System.out.println("Raw number of false positives: " + numFalsePositive);
			System.out.println("Raw number of false negatives " + numFalseNegative);
	
			if (numPositive == 0 && !ALLOW_EMPTY_OBSERVATIONS)
			{
				/* Queries with no query make no sense */
				retry++;
				continue;
			}
			
			Configuration stats = new Configuration();
			determineCases(observations, hidden, stats);
			System.out.println("Number of modelled false postives " + stats.getCases(Configuration.NodeCase.FALSE_POSITIVE) + " (alpha=" +  stats.falsePositiveRate() + "%)");
			System.out.println("Number of modelled false negatives " + stats.getCases(Configuration.NodeCase.FALSE_NEGATIVE) + " (beta=" +  stats.falseNegativeRate() + "%)");
		
			o = new Observations();
			o.item = itemNr;
			o.observations = observations;
			o.observationStats = stats;
		} while (!ALLOW_EMPTY_OBSERVATIONS && retry++ < 50);
		return o;
	}


	/**
	 * Activates the ancestors of the given node.
	 * 
	 * @param i
	 * @param observations
	 */
	public static void activateAncestors(int i, boolean[] observations)
	{
		int j;

		for (j=0;j<term2Ancestors[i].length;j++)
			observations[term2Ancestors[i][j]] = true;
	}

	/**
	 * Activates the ancestors of the given node.
	 * 
	 * @param i
	 * @param observations
	 */
	private static void deactivateDecendants(int i, boolean[] observations)
	{
		int j;

		for (j=0;j<term2Descendants[i].length;j++)
			observations[term2Descendants[i][j]] = false;
	}

	/**
	 * Extracts all items that have at least a single annotation
	 * with a frequency.
	 * 
	 * @return
	 */
	private static Set<ByteString> extractItemsWithFrequencies()
	{
		HashSet<ByteString> items = new HashSet<ByteString>();

		for (ByteString item : assoc.getAllAnnotatedGenes())
		{
			boolean take = false;

			Gene2Associations item2Associations = assoc.get(item);
			for (Association a : item2Associations)
			{
				if (a.getAspect().length()!=0)
					take = true;
			}
			
			if (take)
				items.add(item);
		}
		
		return items;
	}

	/**
	 * Setups the B4O for the given ontology and associations.
	 * 
	 * @param ontology
	 * @param associations
	 */
	public static void setup(Ontology ontology, AssociationContainer associations)
	{
		assoc = associations;
		graph = ontology;

//		graph.findRedundantISARelations();

		HashSet<ByteString> itemsToBeConsidered = new HashSet<ByteString>(associations.getAllAnnotatedGenes());
		provideGlobals(itemsToBeConsidered);
		
		/* Define the directory in which the distributions are stored */
		scoreDistributionDirectory = new File("distribution",allItemList.size() + "-" + CONSIDER_FREQUENCIES_ONLY + "-" + SIZE_OF_SCORE_DISTRIBUTION);
		scoreDistributionDirectory.mkdirs();
		
		/* If we want to consider items with frequencies only, we like to shrink
		 * the item list to contain only the relevant items.
		 */
		if (CONSIDER_FREQUENCIES_ONLY)
		{
			int oldSize = allItemList.size();

			itemsToBeConsidered = new HashSet<ByteString>();
			for (int i = 0;i<allItemList.size();i++)
			{
				if (itemHasFrequencies[i])
					itemsToBeConsidered.add(allItemList.get(i));
			}
			provideGlobals(itemsToBeConsidered);
			
			System.out.println("There were " + oldSize + " items but we consider only " + allItemList.size() + " of them with frequencies.");
			System.out.println("Considering " + slimGraph.getNumberOfVertices() + " terms");
		}
		
		/** Here we precaculate the maxICs of two given terms in a dense matrix */
		if (PRECALCULATE_MAXICS)
		{
			int [][] newMaxICMatrix = new int[slimGraph.getNumberOfVertices()][];
			for (int i=0;i<slimGraph.getNumberOfVertices();i++)
			{
				newMaxICMatrix[i] = new int[slimGraph.getNumberOfVertices() - i - 1];
				for (int j=i+1;j<slimGraph.getNumberOfVertices();j++)
					newMaxICMatrix[i][j - i - 1] = commonAncestorWithMaxIC(i,j);
			}
			micaMatrix = newMaxICMatrix;
		}
		
		/** Here we precalculate for each item the term which contributes as maximum ic term to the resnick calculation */
		if (PRECALCULATE_ITEM_MAXS)
		{
			micaForItem = new int[allItemList.size()][slimGraph.getNumberOfVertices()];

			for (int item = 0; item < allItemList.size(); item++)
			{
				/* The fixed set */
				int [] t2 = items2DirectTerms[item];

				for (int to = 0; to < slimGraph.getNumberOfVertices(); to++)
				{
					double maxIC = Double.NEGATIVE_INFINITY;
					int maxCommon = -1;

					for (int ti : t2)
					{
						int common = commonAncestorWithMaxIC(to, ti);
						if (terms2IC[common] > maxIC)
						{
							maxIC = terms2IC[common];
							maxCommon = common;
						}
					}
					micaForItem[item][to] = maxCommon;
				}
			}
		}
		
		/** Instantiates the query cache */
		if (CACHE_RANDOM_QUERIES)
		{
//			int maxSizes = 0;
//			for (i=0;i<allItemList.size();i++)
//				maxSizes = java.lang.Math.max(maxSizes,items2Direct[i].length);
			queryCache = new QuerySets(MAX_QUERY_SIZE_FOR_CACHED_DISTRIBUTION + 1);

			if (CACHE_SCORE_DISTRIBUTION)
				scoreDistributions = new Distributions(allItemList.size() * (MAX_QUERY_SIZE_FOR_CACHED_DISTRIBUTION + 1));
		}


	}

	
	/**
	 * Main Entry.
	 * 
	 * @param args
	 * @throws InterruptedException
	 * @throws IOException
	 */
	@SuppressWarnings("unused")
	public static void benchmark(Ontology newOntology, AssociationContainer newAssociations) throws InterruptedException, IOException
	{
		int i;
		int numProcessors = MEASURE_TIME?1:Runtime.getRuntime().availableProcessors();

		/* TODO: Get rid of this uglyness */
		graph = newOntology;
		assoc = newAssociations;
		
		setup(newOntology, newAssociations);
		
		/**************************************************************************************************************************/
		/* Write score distribution */
		
		if (false)
		{
			int [] shuffledTerms = new int[slimGraph.getNumberOfVertices()];

			/* Initialize shuffling */
			for (i=0;i<shuffledTerms.length;i++)
				shuffledTerms[i] = i;

			i = 0;

			FileWriter out = new FileWriter("score.txt");
			
			Random rnd = new Random();

			for (int j=0;j<SIZE_OF_SCORE_DISTRIBUTION;j++)
			{
				int q = 10;
				int [] randomizedTerms = new int[q];

				chooseTerms(rnd, q, randomizedTerms, shuffledTerms);
				double randomScore = simScoreVsItem(randomizedTerms, i);
				out.write(randomScore + " \n");
			}
			out.flush();
			out.close();

			System.out.println("Score distribution for item " + allItemList.get(i) + "  " + items2DirectTerms[i].length);
			System.exit(-1);
		}
		
		/**************************************************************************************************************************/

		/* Write example */

		HashSet<TermID> hpoTerms = new HashSet<TermID>();
		hpoTerms.add(new TermID("HP:0000822")); /* Hypertension */
		hpoTerms.add(new TermID("HP:0000875")); /* Episodic Hypertension */
		hpoTerms.add(new TermID("HP:0002621")); /* Atherosclerosis */

		/* Basically, this defines a new command \maxbox whose text width as given by the second argument is not wider than
		 * the first argument. The text which is then displayed in the box is used from the third argument.
		 */
		String preamble = "d2tfigpreamble=\"\\ifthenelse{\\isundefined{\\myboxlen}}{\\newlength{\\myboxlen}}{}"+
						  "\\newcommand*{\\maxbox}[3]{\\settowidth{\\myboxlen}{#2}"+
						  "\\ifdim#1<\\myboxlen" +
						  "\\parbox{#1}{\\centering#3}"+
						  "\\else"+
						  "\\parbox{\\myboxlen}{\\centering#3}"+
						  "\\fi}\"";

		try
		{
			GODOTWriter.writeDOT(graph.getInducedGraph(termEnumerator.getAllAnnotatedTermsAsList()), new File("hpo-example.dot"), null, hpoTerms, new AbstractDotAttributesProvider() {
				public String getDotNodeAttributes(TermID id) {
					String termName;
					Term term = graph.getTerm(id);
					if (graph.isRootTerm(id)) termName = "Human Phenotype";
					else  termName = term.getName();
					String name = "\\emph{" + termName + "}";
					int termIdx = slimGraph.getVertexIndex(graph.getTerm(id));
					int numberOfItems = termEnumerator.getAnnotatedGenes(id).totalAnnotatedCount();
					
					String label = "\\small" + name + "\\\\\\ " + numberOfItems + " \\\\\\ IC=" + String.format("%.4f", terms2IC[termIdx]);
					return "margin=\"0\" shape=\"box\"" + " label=\"\\maxbox{4.5cm}{"+name+"}{"+ label + "}\" " +
							"style=\"rounded corners,top color=white,bottom color=black!10,draw=black!50,very thick\"";
				}
			},"nodesep=0.2; ranksep=0.1;" + preamble,false,false, null);
		} catch (IllegalArgumentException ex)
		{
			System.err.println("Failed to write graphics due to: " + ex.getLocalizedMessage());
		}

		/**************************************************************************************************************************/

		int firstItemWithFrequencies = -1;
		int numItemsWithFrequencies = 0;
		for (i=0;i<itemHasFrequencies.length;i++)
		{
			if (itemHasFrequencies[i])
			{
				numItemsWithFrequencies++;
				if (firstItemWithFrequencies == -1)
					firstItemWithFrequencies = i;
			}
		}

		System.out.println("Items with frequencies " + numItemsWithFrequencies + "  First one: " +
							firstItemWithFrequencies + " which is " + (firstItemWithFrequencies!=-1?allItemList.get(firstItemWithFrequencies):""));
		
		/**************************************************************************************************************************/

		final BufferedWriter param = new BufferedWriter(new FileWriter(RESULT_NAME.split("\\.")[0]+ "_param.txt"));
		param.write("alpha\tbeta\tconsider.freqs.only\tterms\n");
		param.write(String.format("%g\t%g\t%b\n",ALPHA,BETA,CONSIDER_FREQUENCIES_ONLY,slimGraph.getNumberOfVertices()));
		param.flush();
		
		final BufferedWriter out = new BufferedWriter(new FileWriter(RESULT_NAME));
		final BufferedWriter summary = new BufferedWriter(new FileWriter(RESULT_NAME.split("\\.")[0]+ "_summary.txt"));

		ExecutorService es = Executors.newFixedThreadPool(numProcessors);
//		ExecutorService es = Executors.newFixedThreadPool(1);

		Random rnd = new Random(9);

		int run = 0;

		for (int sample = 0; sample < MAX_SAMPLES; sample++)
		{
			for (i=0;i<allItemList.size();i++)
			{
//				if (i != firstItemWithFrequencies) continue;
//				if (!itemHasFrequencies[i]) continue;
//				if (i != 24) continue;

				final long seed = /*2905060951719767123l;//*/rnd.nextLong();
				final int item = i;
				final int fixedRun = run++;

				Runnable thread = new Runnable()
				{
					public void run()
					{
						StringBuilder resultBuilder = new StringBuilder();
					
						System.out.println("Seed = " + seed + " run = " + fixedRun);
						
						ExperimentStore store = processItem(item,false,new Random(seed));
	
						for (int j=0;j<allItemList.size();j++)
						{
							resultBuilder.append(fixedRun);
							resultBuilder.append("\t");
							resultBuilder.append(item==j?1:0);
							resultBuilder.append("\t");
							resultBuilder.append(store.modelWithoutFrequencies.scores[j]);
							resultBuilder.append("\t");
							resultBuilder.append(store.modelWithoutFrequencies.marginals[j]);
							resultBuilder.append("\t");
							resultBuilder.append(store.modelWithoutFrequencies.marginalsIdeal[j]);
							resultBuilder.append("\t");
							resultBuilder.append(store.modelWithFrequencies.scores[j]);
							resultBuilder.append("\t");
							resultBuilder.append(store.modelWithFrequencies.marginals[j]);
							resultBuilder.append("\t");
							resultBuilder.append(store.modelWithFrequencies.marginalsIdeal[j]);
							resultBuilder.append("\t");
							resultBuilder.append(store.resnick.scores[j]);
							resultBuilder.append("\t");
							resultBuilder.append(store.resnick.marginals[j]);
							resultBuilder.append("\t");
							resultBuilder.append(itemHasFrequencies[item]?1:0);
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
				
				if (THREADING) es.execute(thread);
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

	/**
	 * Provides some global variables, given the global graph, the global
	 * associations and the items.
	 * 
	 * @param allItemsToBeConsidered
	 */
	@SuppressWarnings("unused")
	private static void provideGlobals(Set<ByteString> allItemsToBeConsidered)
	{
		int i;

		/* list all evidence codes */
		HashMap<ByteString,Integer> evidences = new HashMap<ByteString,Integer>();
		for (Gene2Associations g2a : assoc)
		{
			for (Association a : g2a)
			{
				/* Worst implementation ever! */
				Integer evidence = evidences.get(a.getEvidence());
				if (evidence == null)
					evidence = 1;
				else
					evidence++;
				
				evidences.put(a.getEvidence(), evidence);
				
			}
		}
		
		System.out.println(allItemsToBeConsidered.size() + " items so far");
		
		System.out.println("Available evidences:");
		for (Entry<ByteString,Integer> ev : evidences.entrySet())
			System.out.println(ev.getKey().toString() + "->" + ev.getValue());

		if (evidenceCodes != null)
		{
			System.out.println("Requested evidences: ");
			evidences.clear();
			for (String ev : evidenceCodes)
			{
				System.out.println(ev);
				evidences.put(new ByteString(ev),1);
			}
		} else
		{
			/* Means take everything */
			evidences = null;
		}
		
		PopulationSet allItems = new PopulationSet("all");
		allItems.addGenes(allItemsToBeConsidered);
		termEnumerator = allItems.enumerateGOTerms(graph, assoc, evidences!=null?evidences.keySet():null);
		ItemEnumerator itemEnumerator = ItemEnumerator.createFromTermEnumerator(termEnumerator);
		
		/* Term stuff */
		Ontology inducedGraph = graph.getInducedGraph(termEnumerator.getAllAnnotatedTermsAsList()); 
		slimGraph = inducedGraph.getSlimGraphView();

		term2Parents = slimGraph.vertexParents;
		term2Children = slimGraph.vertexChildren;
		term2Ancestors = slimGraph.vertexAncestors;
		term2Descendants = slimGraph.vertexDescendants;
		termsInTopologicalOrder = slimGraph.getVertexIndices(inducedGraph.getTermsInTopologicalOrder());

		if (termsInTopologicalOrder.length != slimGraph.getNumberOfVertices())
			throw new RuntimeException("The ontology graph contains cycles.");
		termsToplogicalRank = new int[termsInTopologicalOrder.length];
		for (i=0;i<termsInTopologicalOrder.length;i++)
			termsToplogicalRank[termsInTopologicalOrder[i]] = i; 

		/* Item stuff */
		allItemList = new ArrayList<ByteString>();
		item2Index = new HashMap<ByteString,Integer>();
		i = 0;
		for (ByteString item : itemEnumerator)
		{
			allItemList.add(item);
			item2Index.put(item, i);
			i++;
		}
		
		System.out.println(i + " items passed criterias");

		/* Fill item matrix */
		items2Terms = new int[allItemList.size()][];
		i = 0;
		for (ByteString item : itemEnumerator)
		{
			int j = 0;

			ArrayList<TermID> tids = itemEnumerator.getTermsAnnotatedToTheItem(item);
			items2Terms[i] = new int[tids.size()];
			
			for (TermID tid : tids)
				items2Terms[i][j++] = slimGraph.getVertexIndex(graph.getTerm(tid));
			
			i++;
		}

		/* Fill direct item matrix */
		items2DirectTerms = new int[allItemList.size()][];
		i = 0;
		for (ByteString item : itemEnumerator)
		{
			int j = 0;

			ArrayList<TermID> tids = itemEnumerator.getTermsDirectlyAnnotatedToTheItem(item);
			
			if (false)
			{
				/* Perform sanity check */
				for (TermID s : tids)
				{
					for (TermID d : tids)
					{
						if (graph.existsPath(s,d) || graph.existsPath(d,s))
						{
							System.out.println("Item \"" + item + "\" is annotated to " + s.toString() + " and " + d.toString());
						}
					}
				}
			}
			
			System.out.println(item.toString());
			items2DirectTerms[i] = new int[tids.size()];

			for (TermID tid : tids)
				items2DirectTerms[i][j++] = slimGraph.getVertexIndex(graph.getTerm(tid));
			i++;
		}

		/* Fill in frequencies for directly annotated terms. Also sort them */
		items2TermFrequencies = new double[allItemList.size()][];
		itemHasFrequencies = new boolean[allItemList.size()];
		item2TermFrequenciesOrder = new int[allItemList.size()][];
		for (i=0;i<items2DirectTerms.length;i++)
		{
			/**
			 * A term and the corresponding frequency. We use this
			 * for sorting.
			 * 
			 * @author Sebastian Bauer
			 */
			class Freq implements Comparable<Freq>
			{
				public int termIdx;
				public double freq;

				public int compareTo(Freq o)
				{
					if (freq > o.freq) return 1;
					if (freq < o.freq) return -1;
					return 0;
				}
			}

			items2TermFrequencies[i] = new double[items2DirectTerms[i].length];
			item2TermFrequenciesOrder[i] = new int[items2DirectTerms[i].length];
			Freq [] freqs = new Freq[items2DirectTerms[i].length];

			ByteString item = allItemList.get(i);
			Gene2Associations as = assoc.get(item);

//			Disabled
//			if (as.getAssociations().size() != items2DirectTerms[i].length)
//				throw new IllegalArgumentException("Number of associations differs (" + as.getAssociations().size() + ") from the number of directly annotated terms (" + items2DirectTerms[i].length + ").");
			
			for (int j=0;j<items2DirectTerms[i].length;j++)
			{
				boolean hasExlipictFrequency = false;
				
				/* Default frequency */
				double f = 1.0;
				
				TermID tid = slimGraph.getVertex(items2DirectTerms[i][j]).getID();
				
				/* Find frequency. We now have a O(n^3) algo. Will be optimized later */
				for (Association a : as)
				{
					if (a.getTermID().equals(tid))
					{
						f = getFrequencyFromString(a.getAspect().toString());
						if (f < 1.0)
							hasExlipictFrequency = true;
						/* We assume that the term appears only once */
						break;
					}
				}
				
				items2TermFrequencies[i][j] = f;
				freqs[j] = new Freq();
				freqs[j].termIdx = j;//items2DirectTerms[i][j];
				freqs[j].freq = f;

				if (hasExlipictFrequency)
					itemHasFrequencies[i] = true;
			}

			/* Now sort and remember the indices */
			Arrays.sort(freqs);
			for (int j=0;j<items2DirectTerms[i].length;j++)
				item2TermFrequenciesOrder[i][j] = freqs[j].termIdx;
		}

		/* Calculate IC */
		terms2IC = new double[slimGraph.getNumberOfVertices()];
		for (i=0;i<slimGraph.getNumberOfVertices();i++)
		{
			Term t = slimGraph.getVertex(i);
			terms2IC[i] = -Math.log(((double)termEnumerator.getAnnotatedGenes(t.getID()).totalAnnotatedCount() / allItemList.size()));
		}
	}

	/**
	 * Returns the number of items that are annotated to
	 * term i. 
	 * @param i
	 * @return
	 */
	public static int getNumberOfItemsAnnotatedToTerm(int i)
	{
		Term t = slimGraph.getVertex(i);
		return termEnumerator.getAnnotatedGenes(t.getID()).totalAnnotatedCount();
	}
	
	/**
	 * Converts the frequency string to a double value.
	 * 
	 * @param freq
	 * @return
	 */
	private static double getFrequencyFromString(String freq)
	{
		double f = 1.0;

		if (freq == null || freq.length() == 0)
			return 1.0;

		Matcher matcher = frequencyPattern.matcher(freq);
		if (matcher.matches())
		{
			String fractionalPart = matcher.group(2);
			if (fractionalPart == null || fractionalPart.length() == 0) fractionalPart = "0";
			
			f = Double.parseDouble(matcher.group(1)) + Double.parseDouble(fractionalPart) / Math.pow(10,fractionalPart.length());
			f /= 100.0;
		} else
		{
			matcher = frequencyFractionPattern.matcher(freq);
			if (matcher.matches())
			{
				f = Double.parseDouble(matcher.group(1)) / Double.parseDouble(matcher.group(2));
			} else
			{
				if (freq.equalsIgnoreCase("very rare")) f = 0.01;
				else if (freq.equalsIgnoreCase("rare")) f = 0.05;
				else if (freq.equalsIgnoreCase("occasional")) f = 0.075;
				else if (freq.equalsIgnoreCase("frequent")) f = 0.33;
				else if (freq.equalsIgnoreCase("typical")) f = 0.50;
				else if (freq.equalsIgnoreCase("common")) f = 0.75;
				else if (freq.equalsIgnoreCase("hallmark")) f = 0.90;
				else if (freq.equalsIgnoreCase("obligate")) f = 1;
				else System.err.println("Unknown frequency identifier: " + freq);
			}
		}
		return f;
	}

	static public class Result
	{
		double [] marginals;
		double [] marginalsIdeal;
		double [] scores;
		Configuration [] stats;
		
		public double getScore(int i)
		{
			return scores[i];
		}
		
		public double getMarginal(int i)
		{
			return marginals[i];
		}
		
		public double getMarginalIdeal(int i)
		{
			return marginalsIdeal[i];
		}
		
		public int size()
		{
			return marginals.length;
		}
	}

	/**
	 * Provides the marginals for the observations.
	 * 
	 * @param observations
	 * @param takeFrequenciesIntoAccount
	 * @return
	 */
	public static Result assignMarginals(Observations observations, boolean takeFrequenciesIntoAccount)
	{
		int i;
		
		Result res = new Result();
		res.scores = new double[allItemList.size()];
		res.marginals = new double[allItemList.size()];
		res.marginalsIdeal = new double[allItemList.size()];
		res.stats = new Configuration[allItemList.size()];
		
		for (i=0;i<res.stats.length;i++)
			res.stats[i] = new Configuration();
		for (i=0;i<res.scores.length;i++)
			res.scores[i] = Math.log(0);

		double [][][] scores = new double[allItemList.size()][ALPHA_GRID.length][BETA_GRID.length];
		double normalization = Math.log(0);

		double [] idealScores = new double[allItemList.size()];
		double idealNormalization = Math.log(0);

		boolean exitNow = false;
		
		for (i=0;i<allItemList.size();i++)
		{
			WeightedConfigurationList stats = determineCasesForItem(i,observations.observations,takeFrequenciesIntoAccount);
			
			for (int a=0;a<ALPHA_GRID.length;a++)
			{
				for (int b=0;b<BETA_GRID.length;b++)
				{
					scores[i][a][b] = stats.score(ALPHA_GRID[a], BETA_GRID[b]);
					res.scores[i] = Util.logAdd(res.scores[i], scores[i][a][b]);
				}
			}
			normalization = Util.logAdd(normalization, res.scores[i]);

			/* This is used only for benchmarks, where we know the true configuration */
			if (observations.observationStats != null)
			{
				/* Calculate ideal scores */
				double fpr = observations.observationStats.falsePositiveRate();
				if (fpr == 0) fpr = 0.0000001;
				else if (fpr == 1.0) fpr = 0.999999;
				else if (Double.isNaN(fpr))
				{
					exitNow = true;
					fpr = 0.5;
				}
	
				double fnr = observations.observationStats.falseNegativeRate();
				if (fnr == 0) fnr = 0.0000001;
				else if (fnr == 1) fnr =0.999999;
				else if (Double.isNaN(fnr))
				{
					exitNow = true;
					fnr = 0.5;
				}
				
				idealScores[i] = stats.score(fpr,fnr);
				idealNormalization = Util.logAdd(idealNormalization, idealScores[i]);
			}
		}

		for (i=0;i<allItemList.size();i++)
		{
			res.marginals[i] = Math.min(Math.exp(res.scores[i] - normalization),1);
			res.marginalsIdeal[i] = Math.min(Math.exp(idealScores[i]- idealNormalization),1);

//			System.out.println(i + ": " + idealScores[i] + " (" + res.getMarginalIdeal(i) + ") " + res.scores[i] + " (" + res.getMarginal(i) + ")");
//			System.out.println(res.marginals[i] + "  " + res.marginalsIdeal[i]);
		}

		/* There is a possibility that ideal marginal is not as good as the marginal
		 * for the unknown parameter situation,  i.e., if the initial signal got such
		 * disrupted that another item is more likely. This may produce strange plots.
		 * Therefore, we take the parameter estimated marginals as the ideal one if
		 * they match the reality better.
		 */
		if (res.marginalsIdeal[observations.item] < res.marginals[observations.item])
		{
			for (i=0;i<allItemList.size();i++)
				res.marginalsIdeal[i] = res.marginals[i];
		}
		                       
		                       
//		System.out.println(idealNormalization + "  " + normalization);
//		if (exitNow)
//			System.exit(10);
		return res;
	}

	static long time;
	static long lastTime;
	
	/**
	 * Return a common ancestor of t1 and t2 that have max ic.
	 * 
	 * @param t1
	 * @param t2
	 * @return
	 */
	private static int commonAncestorWithMaxIC(int t1, int t2)
	{
		if (micaMatrix != null)
		{
			if (t1 < t2)
				return micaMatrix[t1][t2 - t1 - 1];
			else if (t2 < t1)
				return micaMatrix[t2][t1 - t2 - 1];
			else return t1;
		}

		/* A rather slow implementation */
		int [] ancestorsA;
		int [] ancestorsB;
		
		if (term2Ancestors[t1].length > term2Ancestors[t2].length)
		{
			ancestorsA = term2Ancestors[t1];
			ancestorsB = term2Ancestors[t2];
		} else
		{
			ancestorsA = term2Ancestors[t1];
			ancestorsB = term2Ancestors[t2];
		}

		int bestTerm = -1;
		double bestIC = Double.NEGATIVE_INFINITY;

		for (int i=0;i<ancestorsA.length;i++)
		{
			for (int j=0;j<ancestorsB.length;j++)
			{
				if (ancestorsA[i] == ancestorsB[j])
				{
					/* Ancestor is a common one */
					int term = ancestorsA[i];
					double ic = terms2IC[term];

					if (ic > bestIC)
					{
						bestIC = ic;
						bestTerm = term;
					}
					break;
				}
			}
		}
		
		if (bestTerm == -1)
		{
			throw new RuntimeException("No best term found, which is strange.");
		}
		
		return bestTerm;
	}
	
	/**
	 * Returns a minimal length array of terms of which the induced graph
	 * is the same as of the given terms. These are the leaf terms.
	 * 
	 * @param terms
	 * @return
	 */
	private static int [] mostSpecificTerms(int [] terms)
	{
		ArrayList<TermID> termList = new ArrayList<TermID>(terms.length);
		for (int i = 0;i<terms.length;i++)
			termList.add(slimGraph.getVertex(terms[i]).getID());
		
		Ontology termGraph = graph.getInducedGraph(termList);
		
		ArrayList<Term> leafTermList = termGraph.getLeafTerms();
		
		int [] specifcTerms = new int[leafTermList.size()];
		int i=0;
		
		for (Term t : termGraph.getLeafTerms())
			specifcTerms[i++] = slimGraph.getVertexIndex(t);
		
		if (DEBUG)
		{
			boolean [] observations1 = new boolean[term2Ancestors.length];
			for (int t : specifcTerms)
			{
				for (i = 0;i<term2Ancestors[t].length;i++)
					observations1[term2Ancestors[t][i]] = true;
			}
			
			boolean [] observations2 = new boolean[term2Ancestors.length];
			for (int t : terms)
			{
				for (i = 0;i<term2Ancestors[t].length;i++)
					observations2[term2Ancestors[t][i]] = true;
			}

			for (i=0;i<observations1.length;i++)
				if (observations1[i] != observations2[i])
					throw new RuntimeException("Observations didn't match!");
		}
		
		return specifcTerms;
	}


	/**
	 * Gets a sparse representation of the most specific terms in the observation
	 * map.
	 * 
	 * @param observations
	 * @return
	 */
	private static int[] getMostSpecificTermsSparse(boolean[] observations)
	{
		int numObservedTerms = 0;
		for (int i = 0;i<observations.length;i++)
			if (observations[i]) numObservedTerms++;
		
		int [] observedTerms = new int[numObservedTerms];
		for (int i = 0, j=0;i<observations.length;i++)
		{
			if (observations[i])
				observedTerms[j++] = i;
		}
		
		return mostSpecificTerms(observedTerms);
	}

	/**
	 * Score two list of terms according to resnick-max-avg-of-best
	 * method.
	 * 
	 * @param tl1
	 * @param tl2
	 * @return
	 */
	private static double simScoreMaxAvg(int[] tl1, int[] tl2)
	{
		double score = 0;
		for (int t1 : tl1)
		{
			double maxIC = Double.NEGATIVE_INFINITY;

			for (int t2 : tl2)
			{
				int common = commonAncestorWithMaxIC(t1, t2);
				if (terms2IC[common] > maxIC) maxIC = terms2IC[common];
			}
			
			score += maxIC;
		}
		score /= tl1.length;
		return score;
	}

	/**
	 * Sim score avg using two lists of terms.
	 * 
	 * @param tl1
	 * @param item
	 * @return
	 */
	private static double simScoreMaxAvgVsItem(int [] tl1, int item)
	{
		if (PRECALCULATE_ITEM_MAXS)
		{
			double score = 0;
			for (int t1 : tl1)
				score += terms2IC[micaForItem[item][t1]];
			score /= tl1.length;
			return score;
		}
		return simScoreMaxAvg(tl1,items2DirectTerms[item]);
	}
	
	/**
	 * Sim score avg using two lists of terms.
	 * 
	 * @param t1
	 * @param t2
	 * @return
	 */
	private static double simScoreAvg(int[] t1, int[] t2)
	{
		double score = 0;
		for (int to : t1)
		{
			for (int ti : t2)
			{
				int common = commonAncestorWithMaxIC(to, ti);
				score += terms2IC[common];
			}
		}
		score /= t1.length * t2.length;
		return score;
	}

	/**
	 * Score two list of terms.
	 * 
	 * @param t1
	 * @param t2
	 * @return
	 */
	private static double simScore(int[] t1, int[] t2)
	{
		return simScoreMaxAvg(t1,t2);
	}
	
	/**
	 * Score one list of terms vs an item.
	 * 
	 * @param t1
	 * @param item
	 * @return
	 */
	private static double simScoreVsItem(int [] t1, int item)
	{
		return simScoreMaxAvgVsItem(t1,item);
	}

	/**
	 * Makes the calculation according to Resnick max. We handle the observations as an item
	 * and compare it to all other items. Also calculates the significance (stored in the 
	 * marginal attribute).
	 * 
	 * @param observations
	 * @param determines the significance.
	 * 
	 * @return
	 */
	private static Result resnickMaxScore(boolean [] observations, boolean pval, Random rnd)
	{
		int [] observedTerms = getMostSpecificTermsSparse(observations);
		int [] randomizedTerms = new int[observedTerms.length];
		int [] shuffledTerms = new int[slimGraph.getNumberOfVertices()];
		
		int querySize = observedTerms.length;
		
		Result res = new Result();
		res.scores = new double[allItemList.size()];
		res.marginals = new double[allItemList.size()];
	
		/* Initialize shuffling */
		for (int i=0;i<shuffledTerms.length;i++)
			shuffledTerms[i] = i;

		long startTime = System.currentTimeMillis();
		long lastTime = startTime;
		
		for (int i = 0;i<allItemList.size();i++)
		{
			long time = System.currentTimeMillis();

			if (time - lastTime > 5000)
			{
				System.out.println((time - startTime) + "ms " + i / (double)allItemList.size());
				lastTime = time;
			}

			/* Determine and remember the plain score */
			double score = simScoreVsItem(observedTerms,i);
			res.scores[i] = score;

			/* Turn it into a p value by considering the distribution */
			if (CACHE_RANDOM_QUERIES)
			{
				if (querySize > MAX_QUERY_SIZE_FOR_CACHED_DISTRIBUTION)
					querySize = MAX_QUERY_SIZE_FOR_CACHED_DISTRIBUTION;
				
				int [][] queries;
				synchronized (queryCache)
				{
					queries = queryCache.getQueries(querySize);
					if (queries == null)
					{
						queries = new int[SIZE_OF_SCORE_DISTRIBUTION][querySize];
						for (int j=0;j<SIZE_OF_SCORE_DISTRIBUTION;j++)
							chooseTerms(rnd, querySize, queries[j], shuffledTerms);
						
						queryCache.setQueries(querySize, queries);
					}
				}
				
				if (CACHE_SCORE_DISTRIBUTION)
				{
					Distribution d;
					
					synchronized (scoreDistributions)
					{
						d = scoreDistributions.getDistribution(i * (MAX_QUERY_SIZE_FOR_CACHED_DISTRIBUTION+1) + querySize);

						if (d == null)
						{
							/* Determine score distribution */
							double [] scores = new double[SIZE_OF_SCORE_DISTRIBUTION];
							double maxScore = Double.NEGATIVE_INFINITY;

							for (int j=0;j<SIZE_OF_SCORE_DISTRIBUTION;j++)
							{
								scores[j] = simScoreVsItem(queries[j], i);
								if (scores[j] > maxScore) maxScore = scores[j];
							}

							/* throw into equidistant binnings */
							int [] hist = new int[1000];
							for (int j=0;j<SIZE_OF_SCORE_DISTRIBUTION;j++)
							{
								int bin = Distribution.getBin(scores[j], maxScore);
								hist[bin]++;
							}

							d = new Distribution(hist,maxScore);
							scoreDistributions.setDistribution(i * (MAX_QUERY_SIZE_FOR_CACHED_DISTRIBUTION+1) + querySize, d);
						}
					}
					res.marginals[i] = d.getP(score);
//					{
//						int count = 0;
//						for (int j=0;j<SIZE_OF_SCORE_DISTRIBUTION;j++)
//						{
//							double randomScore = simScoreVsItem(queries[j], i);
//							if (randomScore >= score) count++;
//						}
//					}
				} else
				{
					int count = 0;

					for (int j=0;j<SIZE_OF_SCORE_DISTRIBUTION;j++)
					{
						double randomScore = simScoreVsItem(queries[j], i);
						if (randomScore >= score) count++;
					}
					
					res.marginals[i] = count / (double)SIZE_OF_SCORE_DISTRIBUTION;
				}
			} else
			{
				int count = 0;
				for (int j=0;j<SIZE_OF_SCORE_DISTRIBUTION;j++)
				{
					chooseTerms(rnd, observedTerms.length, randomizedTerms, shuffledTerms);
					double randomScore = simScoreVsItem(randomizedTerms, i);
					if (randomScore >= score) count++;
				}
				res.marginals[i] = count / (double)SIZE_OF_SCORE_DISTRIBUTION;
			}
			
		}
		
		return res;
	}

	/**
	 * Select size number of terms that are stored in chosen.
	 * 
	 * @param rnd
	 * @param size
	 * @param chosen
	 * @param storage
	 */
	private static void chooseTerms(Random rnd, int size, int[] chosen, int[] storage)
	{
		if (FORBID_ILLEGAL_QUERIES)
		{
			boolean valid;
			int tries = 0;
			
			do
			{
				choose(rnd, size, chosen, storage);
				valid = true;
				
				outer:
				for (int i=0;i<size;i++)
				{
					for (int j=0;j<size;j++)
					{
						if (i==j) continue;

						/* If a chosen term is descendant of another one, we reject the query. */
						if (slimGraph.isDescendant(chosen[i], chosen[j]))
						{
							valid = false;
							break outer;
						}
					}
				}
				tries++;
			} while (!valid);
		} else
		{
			choose(rnd, size, chosen, storage);
		}
	}

	/**
	 * Chooses size randomly selected values from storage. Storage is
	 * manipulated by this call. Selected values are stored in chosen.
	 * 
	 * @param rnd
	 * @param size number of elements that are chosen
	 * @param chosen where the chosen values are deposited.
	 * @param storage defines the elements from which to choose
	 */
	private static void choose(Random rnd, int size, int[] chosen, int[] storage)
	{
		/* Choose terms randomly as the size of observed terms. We avoid drawing the same term but
		 * alter shuffledTerms such that it can be used again in the next iteration.
		 * Note that this duplicates code from the above. */
		for (int k=0;k<size;k++)
		{
			int chosenIndex = rnd.nextInt(storage.length - k);
			int chosenTerm = storage[chosenIndex];

			/* Place last term at the position of the chosen term */
			storage[chosenIndex] = storage[size - k -1];
			
			/* Place chosen term at the last position */
			storage[size - k - 1] = chosenTerm;

			chosen[k] = chosenTerm;
		}
	}
	
	static class ExperimentStore
	{
		Observations obs;
		Result modelWithoutFrequencies;
		Result modelWithFrequencies;
		Result resnick;
	}

	/**
	 * Processes the simulation and evaluation for the given item.
	 * 
	 * @param item
	 * 
	 * @returns an array of Results.
	 *  1. model without frequencies
	 *  2. model with frequencies
	 *  3. resnick (score and p value)
	 */
	private static ExperimentStore processItem(int item, boolean provideGraph, Random rnd)
	{
		int i;
	
		Observations obs = generateObservations(item, rnd);
		
		boolean [] observations = obs.observations;

		System.out.println("Item " + allItemList.get(item));

		/* First, without taking frequencies into account */
		Result modelWithoutFrequencies = assignMarginals(obs, false);
		
		/* Second, with taking frequencies into account */
		Result modelWithFrequencies = assignMarginals(obs, true);

		/* Third, we apply the resnick sim measure */
		Result resnick = resnickMaxScore(obs.observations, true, rnd);
		
		ExperimentStore id = new ExperimentStore();
		id.obs = obs;
		id.modelWithoutFrequencies = modelWithoutFrequencies;
		id.modelWithFrequencies = modelWithFrequencies;
		id.resnick = resnick;
		
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
	
			ArrayList<Pair> scoreList = new ArrayList<Pair>(allItemList.size());
			ArrayList<Pair> idealList = new ArrayList<Pair>(allItemList.size());
			for (i=0;i<allItemList.size();i++)
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
				System.out.println((i+1) + (itIs?"(*)":"") + ": " + allItemList.get(p.idx) + ": " +  p.score + " " + modelWithoutFrequencies.getMarginal(p.idx));
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
			System.out.println(modelWithoutFrequencies.stats[item].toString());
			System.out.println("Statistics for the top item");
			System.out.println(modelWithoutFrequencies.stats[scoreList.get(0).idx].toString());
			
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
				for (i=0;i<items2Terms[item].length;i++)
					hiddenSet.add(slimGraph.getVertex(items2Terms[item][i]).getID());
				final HashSet<TermID> observedSet = new HashSet<TermID>();
				for (i = 0;i<observations.length;i++)
				{
					if (observations[i])
						observedSet.add(slimGraph.getVertex(i).getID());
				}
				int topRankIdx = scoreList.get(0).idx;
				final HashSet<TermID> topRankSet = new HashSet<TermID>();
				for (i=0;i<items2Terms[topRankIdx].length;i++)
					topRankSet.add(slimGraph.getVertex(items2Terms[topRankIdx][i]).getID());
		
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
	 * Returns the current slim graph.
	 * 
	 * @return
	 */
	public static SlimDirectedGraphView<Term> getSlimGraph()
	{
		return slimGraph;
	}

	/**
	 * Returns the ontology.
	 * 
	 * @return
	 */
	public static Ontology getOntology()
	{
		return graph;
	}
	
	/**
	 * Returns the association container.
	 * 
	 * @return
	 */
	public static AssociationContainer getAssociations()
	{
		return assoc;
	}

	/**
	 * Returns the terms that are directly annotated to the given item.
	 * @param itemId
	 * @return
	 */
	public static int[] getTermsDirectlyAnnotatedTo(int itemId)
	{
		return items2DirectTerms[itemId]; 
	}

	/**
	 * Returns the frequencies of terms directly annotated to the given
	 * item. The order of the entries match the order of 
	 * getTermsDirectlyAnnotatedTo().
	 * 
	 * @param itemId
	 * @return
	 */
	public static double[] getFrequenciesOfTermsDirectlyAnnotatedTo(int itemId)
	{
		return items2TermFrequencies[itemId];
	}
	
	/**
	 * Returns the parents of a given term.
	 * 
	 * @param t
	 * @return
	 */
	public static int[] getParents(int t)
	{
		return term2Parents[t];
	}
}
