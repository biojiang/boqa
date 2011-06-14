package sonumina.b4oweb.server;

import java.io.IOException;
import java.util.logging.Logger;

import ontologizer.association.AssociationContainer;
import ontologizer.association.AssociationParser;
import ontologizer.go.OBOParser;
import ontologizer.go.Ontology;
import ontologizer.go.TermContainer;

public class B4OCore
{
	private static Logger logger = Logger.getLogger(B4OCore.class.getName());

	static final String DEFINITIONS_PATH = "human-phenotype-ontology.obo.gz";
	static final String ASSOCIATIONS_PATH = "phenotype_annotation.omim.gz";

	/**
	 * The static ontology object. Defines terms that the user can select.
	 */
	static private Ontology ontology;
	
	static private AssociationContainer associations;

	static
	{
		logger.info("Starting " + B4OCore.class.getName());

		OBOParser oboParser = new OBOParser(DEFINITIONS_PATH);
		TermContainer goTerms = new TermContainer(oboParser.getTermMap(), oboParser.getFormatVersion(), oboParser.getDate());
		logger.info("OBO file \"" + DEFINITIONS_PATH + "\" parsed");

		ontology = new Ontology(goTerms);
		logger.info("Ontology graph created");
		
		try {
			AssociationParser ap = new AssociationParser(ASSOCIATIONS_PATH,ontology.getTermContainer(),null,null);
			associations = new AssociationContainer(ap.getAssociations(), ap.getSynonym2gene(), ap.getDbObject2gene());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static Ontology getOntology()
	{
		return ontology;
	}
}
