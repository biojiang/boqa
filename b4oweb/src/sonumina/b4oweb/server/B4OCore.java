package sonumina.b4oweb.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import ontologizer.association.AssociationContainer;
import ontologizer.association.AssociationParser;
import ontologizer.go.OBOParser;
import ontologizer.go.OBOParserException;
import ontologizer.go.Ontology;
import ontologizer.go.Term;
import ontologizer.go.TermContainer;
import sonumina.b4o.calculation.B4O;
import sonumina.b4o.calculation.B4O.Result;
import sonumina.b4o.calculation.Observations;
import sonumina.math.graph.DirectedGraph;
import sonumina.math.graph.SlimDirectedGraphView;

public class B4OCore
{
	private static Logger logger = Logger.getLogger(B4OCore.class.getName());

	static final String DEFINITIONS_PATH = "/home/sba/workspace/b4oweb/human-phenotype-ontology.obo.gz";
	static final String ASSOCIATIONS_PATH = "/home/sba/workspace/b4oweb/phenotype_annotation.omim.gz";

	/**
	 * The static ontology object. Defines terms that the user can select.
	 */
	static private Ontology ontology;

	/**
	 * The corresponding slim view.
	 */
	static private SlimDirectedGraphView<Term> slimGraph;

	/**
	 * Contains the indices of the term in sorted order
	 */
	static private int [] sorted2Idx;
	
	/**
	 * Contains the rank of the term within the sorted order. 
	 */
	static private int [] idx2Sorted;
	
	/**
	 * The static association container. Defines the items.
	 */
	static private AssociationContainer associations;

	static
	{
		logger.info("Starting " + B4OCore.class.getName());

		OBOParser oboParser = new OBOParser(DEFINITIONS_PATH);
		try {
			oboParser.doParse();
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (OBOParserException e1) {
			e1.printStackTrace();
		}
		TermContainer goTerms = new TermContainer(oboParser.getTermMap(), oboParser.getFormatVersion(), oboParser.getDate());
		logger.info("OBO file \"" + DEFINITIONS_PATH + "\" parsed");

		Ontology localOntology = new Ontology(goTerms);
		logger.info("Ontology graph with " + localOntology.getNumberOfTerms() + " terms created");
		
		/* Load associations */
		AssociationContainer localAssociations;
		try {
			AssociationParser ap = new AssociationParser(ASSOCIATIONS_PATH,localOntology.getTermContainer(),null,null);
			localAssociations = new AssociationContainer(ap.getAssociations(), ap.getSynonym2gene(), ap.getDbObject2gene());
		} catch (IOException e) {
			e.printStackTrace();
			localAssociations = new AssociationContainer();
		}
		
		B4O.setup(localOntology, localAssociations);

		ontology = B4O.getOntology();
		associations = B4O.getAssociations();
		slimGraph = B4O.getSlimGraph();

		logger.info("Got ontology, associations and slim graph");

		/* Sort the term according to the alphabet */
		class TermName
		{
			int index;
			String name;
		}
		TermName [] terms = new TermName[slimGraph.getNumberOfVertices()];
		for (int i=0;i<slimGraph.getNumberOfVertices();i++)
		{
			terms[i] = new TermName();
			terms[i].name = slimGraph.getVertex(i).getName();
			terms[i].index = i;
		}
		Arrays.sort(terms, new Comparator<TermName>() {
			@Override
			public int compare(TermName o1, TermName o2)
			{
				return o1.name.compareTo(o2.name);
			}
		});
		sorted2Idx = new int[terms.length];
		idx2Sorted = new int[terms.length];
		for (int i=0;i<terms.length;i++)
		{
			sorted2Idx[i] = terms[i].index;
			idx2Sorted[terms[i].index] = i;
		}
		
	}
	
	/**
	 * Returns the global ontology.
	 * 
	 * @return
	 */
	public static Ontology getOntology()
	{
		return ontology;
	}
	
	/**
	 * Returns the global association container.
	 * 
	 * @return
	 */
	public static AssociationContainer getAssociations()
	{
		return associations;
	}

	/**
	 * Returns the term at the given sorted index.
	 * 
	 * @param sortedIdx
	 * @return
	 */
	public static Term getTerm(int sortedIdx)
	{
		return slimGraph.getVertex(sorted2Idx[sortedIdx]);
	}

	/**
	 * Returns the terms matching the pattern. 
	 * 
	 * @param pattern
	 * @return
	 * 
	 * @TODO: Optimize via proper index data structure.
	 */
	public static Iterable<Term> getTerms(final String pattern)
	{
		return new Iterable<Term>() {
			
			@Override
			public Iterator<Term> iterator() {
				return new Iterator<Term>()
				{
					int i = 0;
					String pat = pattern.toLowerCase();
					
					@Override
					public boolean hasNext()
					{
						for (;i<slimGraph.getNumberOfVertices();i++)
						{
							if (pattern == null || pattern.length() == 0 || getTerm(i).getName().toLowerCase().contains(pat))
								return true;
						}
						return false;
					}
					
					@Override
					public Term next() {
						return getTerm(i++);
					}
					
					@Override
					public void remove() { }
				};
			}
		};
	}

	/**
	 * Returns the a single term of given index with respect to the given pattern.
	 * 
	 * @param pattern
	 * @param which
	 * @return
	 */
	public static Term getTerm(final String pattern, int which)
	{
		for (Term t : getTerms(pattern))
			if (which-- == 0)
				return t;
		return null;
	}
	
	/**
	 * Returns the number of terms that match the given pattern.
	 * 
	 * @param pattern may be null
	 * @return
	 */
	public static int getNumberTerms(String pattern)
	{
		if (pattern == null || pattern.length() == 0)
			return slimGraph.getNumberOfVertices();

		int numberOfTerms = 0;
		for (@SuppressWarnings("unused") Term t : getTerms(pattern))
			numberOfTerms++;

		return numberOfTerms;
	
	}

	/**
	 * Returns the id of the given term.
	 * 
	 * @param t
	 * @return
	 */
	public static int getIdOfTerm(Term t)
	{
		return idx2Sorted[slimGraph.getVertexIndex(t)];
	}

	/**
	 * Score.
	 * 
	 * @param serverIds
	 * @return
	 */
	public static List<ItemResultEntry> score(List<Integer> serverIds)
	{
		boolean [] observations = new boolean[slimGraph.getNumberOfVertices()];
		for (int id : serverIds)
		{
			observations[sorted2Idx[id]] = true;
			B4O.activateAncestors(sorted2Idx[id],observations);
		}
		
		List<ItemResultEntry> resultList = new ArrayList<ItemResultEntry>();

		Observations o = new Observations();
		o.observations = observations;

		Result result = B4O.assignMarginals(o, true);
		for (int i=0;i<result.size();i++)
		{
			ItemResultEntry newEntry = ItemResultEntry.create(i, result.getMarginal(i));
			resultList.add(newEntry);
		}
		
		Collections.sort(resultList, new Comparator<ItemResultEntry>() {
			@Override
			public int compare(ItemResultEntry o1, ItemResultEntry o2)
			{
				if (o1.getScore() < o2.getScore()) return 1;
				if (o1.getScore() > o2.getScore()) return -1;
				return 0;
			}
		});
		
		return resultList;
	}

	/**
	 * Returns the name of the given item.
	 * 
	 * @param itemId
	 * @return
	 */
	public static String getItemName(int itemId)
	{
		return B4O.allItemList.get(itemId).toString();
	}

	/**
	 * Returns the number of items annotated to the given term
	 * represented by the user id.
	 * 
	 * @param serverId
	 * @return
	 */
	public static int getNumberOfTermsAnnotatedToTerm(int serverId)
	{
		return B4O.getNumberOfItemsAnnotatedToTerm(sorted2Idx[serverId]);
	}

	/**
	 * Returns the terms that are directly annotated to the given item.
	 * 
	 * @param itemId
	 * @return
	 */
	public static int[] getTermsDirectlyAnnotatedTo(int itemId)
	{
		int [] t = B4O.getTermsDirectlyAnnotatedTo(itemId);
		int [] st = new int[t.length];
		
		for (int i=0;i<t.length;i++)
			st[i] = idx2Sorted[t[i]];
		return st;
	}

	/**
	 * Returns the frequencies of the terms directly annotated to the given
	 * item. The order matches the order of getTermsDirectlyAnnotatedTo().
	 * 
	 * @param itemId
	 * @return
	 */
	public static double[] getFrequenciesOfTermsDirectlyAnnotatedTo(int itemId)
	{
		double [] f = B4O.getFrequenciesOfTermsDirectlyAnnotatedTo(itemId);
		double [] sf = new double[f.length];
		for (int i=0;i<sf.length;i++)
			sf[i] = f[i];
		return sf;
	}

	/**
	 * Returns the parents of the term.
	 * 
	 * @param sid
	 * @return
	 */
	public static int[] getParents(int t)
	{
		int [] p = B4O.getParents(sorted2Idx[t]);
		int [] np = new int[p.length];
		for (int i=0;i<p.length;i++)
			np[i] = idx2Sorted[p[i]];
		return np;
	}

	public static interface IAncestorVisitor
	{
		public void visit(int t);
	}
	
	/**
	 * Visits the ancestors of the given terms. For every visit,
	 * the visit method of the IAncestorVisitor interface is
	 * called.
	 * 
	 * @param t
	 */
	public static void visitAncestors(Collection<Integer> terms, final IAncestorVisitor visitor)
	{
		/* Get initial terms */
		ArrayList<Term> initTerms = new ArrayList<Term>(terms.size());
		for (int t : terms)
			initTerms.add(slimGraph.getVertex(sorted2Idx[t]));

		/* Invoke bfs */
		ontology.getGraph().bfs(initTerms,true,new DirectedGraph.IVisitor<Term>() {
			@Override
			public boolean visited(Term vertex) {
				visitor.visit(idx2Sorted[slimGraph.getVertexIndex(vertex)]);
				return true;
			}
		});
	}
}
