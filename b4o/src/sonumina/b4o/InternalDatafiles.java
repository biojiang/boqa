package sonumina.b4o;

import java.io.File;
import java.util.HashSet;
import java.util.Random;

import ontologizer.association.Association;
import ontologizer.association.AssociationContainer;
import ontologizer.benchmark.Datafiles;
import ontologizer.dotwriter.AbstractDotAttributesProvider;
import ontologizer.dotwriter.GODOTWriter;
import ontologizer.go.Ontology;
import ontologizer.go.ParentTermID;
import ontologizer.go.Term;
import ontologizer.go.TermContainer;
import ontologizer.go.TermID;
import ontologizer.go.TermRelation;
import ontologizer.types.ByteString;

/**
 * Class representing some internal data files.
 * 
 * @author Sebastian Bauer
 */
public class InternalDatafiles extends Datafiles
{
	public static long seed = 1;
	
	public InternalDatafiles()
	{
		/* Go Graph */
		HashSet<Term> terms = new HashSet<Term>();
		Term c1 = new Term("GO:0000001", "C1");
		Term c2 = new Term("GO:0000002", "C2", new ParentTermID(c1.getID(),TermRelation.IS_A));
		Term c3 = new Term("GO:0000003", "C3", new ParentTermID(c1.getID(),TermRelation.IS_A));
		Term c4 = new Term("GO:0000004", "C4", new ParentTermID(c2.getID(),TermRelation.IS_A));
		Term c5 = new Term("GO:0000005", "C5", new ParentTermID(c2.getID(),TermRelation.IS_A));
		Term c6 = new Term("GO:0000006", "C6", new ParentTermID(c3.getID(),TermRelation.IS_A),new ParentTermID(c2.getID(),TermRelation.IS_A));
		Term c7 = new Term("GO:0000007", "C7", new ParentTermID(c5.getID(),TermRelation.IS_A),new ParentTermID(c6.getID(),TermRelation.IS_A));
		Term c8 = new Term("GO:0000008", "C8", new ParentTermID(c7.getID(),TermRelation.IS_A));
		Term c9 = new Term("GO:0000009", "C9", new ParentTermID(c7.getID(),TermRelation.IS_A));
		Term c10 = new Term("GO:0000010", "C10", new ParentTermID(c9.getID(),TermRelation.IS_A));
		Term c11 = new Term("GO:0000011", "C11", new ParentTermID(c9.getID(),TermRelation.IS_A));
		Term c12 = new Term("GO:0000012", "C12", new ParentTermID(c11.getID(),TermRelation.IS_A));
		
		terms.add(c1);
		terms.add(c2);
		terms.add(c3);
		terms.add(c4);
		terms.add(c5);
		terms.add(c6);
		terms.add(c7);
		terms.add(c8);
		terms.add(c9);
		terms.add(c10);
		terms.add(c11);
		terms.add(c12);
		TermContainer termContainer = new TermContainer(terms,"","");

		graph = new Ontology(termContainer);

		HashSet<TermID> tids = new HashSet<TermID>();
		for (Term term : terms)
			tids.add(term.getID());

		/* Associations */
		assoc = new AssociationContainer();
		Random r = new Random(seed);

		/* Randomly assign the items (note that redundant associations are filtered out later) */
		for (int i=1;i<=5;i++)
		{
			String itemName = "item" + i;
			int numTerms = r.nextInt(2) + 1;
			
			System.out.print(itemName + ": ");
			
			for (int j=0;j<numTerms;j++)
			{
				int tid = r.nextInt(terms.size())+1;
				assoc.addAssociation(new Association(new ByteString(itemName),tid));
				System.out.print(tid + " ");
			}
			System.out.println();
		}

		GODOTWriter.writeDOT(graph, new File("example.dot"), null, tids, new AbstractDotAttributesProvider() {
			public String getDotNodeAttributes(TermID id) {

				return "label=\""+graph.getTerm(id).getName()+"\"";
			}
		});

	}

}
