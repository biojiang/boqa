package sonumina.b4o;

import java.io.IOException;

import ontologizer.GlobalPreferences;
import ontologizer.OntologizerThreadGroups;
import ontologizer.benchmark.Datafiles;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * Main entry point of the B4O benchmark.
 * 
 * @author Sebastian Bauer
 */
public class B4O
{
	static private String ontologyPath;
	static private String annotationPath;

	static private double ALPHA = 0.002;
	static private double BETA = 0.1;
	static private int MAX_TERMS = 6;
	static private boolean CONSIDER_FREQUENCIES_ONLY = false;
	
	static sonumina.b4o.calculation.B4O b4o = new sonumina.b4o.calculation.B4O();

	/**
	 * Parses the command line.
	 * 
	 * @param args
	 */
	public static void parseCommandLine(String [] args)
	{
	   Options opt = new Options();
	   opt.addOption("o", "ontology", true, "Path or URL to the ontology file. Default is \"" + ontologyPath + "\"");
	   opt.addOption("a", "annotations", true, "Path or URL to files containing annotations. Default is \"" + annotationPath + "\"");
	   opt.addOption("c", "considerFreqOnly", false, "If specified, only items with frequencies are considered.");
	   opt.addOption("m", "maxTerms", true, "Defines the maximal number of terms a random query can have. Default is " + MAX_TERMS);
	   opt.addOption(null, "alpha", true, "Specifies alpha (false-positive rate) during simulation. Default is " + ALPHA + ".");
	   opt.addOption(null, "beta", true, "Specifies beta (false-negative rate) during simulation. Default is " + BETA + ".");
	   opt.addOption("h", "help", false, "Shows this help");

	   try
	   {
		   GnuParser parser = new GnuParser();
		   CommandLine cl;
		   cl = parser.parse(opt, args);

		   if (cl.hasOption('h'))
		   {
			   HelpFormatter f = new HelpFormatter();
			   f.printHelp(B4O.class.getName(), opt);
			   System.exit(0);
		   }
		   
		   if (cl.hasOption('m'))
			   MAX_TERMS = Integer.parseInt(cl.getOptionValue('m'));

		   if (cl.hasOption('c'))
			   CONSIDER_FREQUENCIES_ONLY = true;
		   
		   if (cl.hasOption("alpha"))
			   ALPHA = Double.parseDouble(cl.getOptionValue("alpha"));

		   if (cl.hasOption("beta"))
			   BETA = Double.parseDouble(cl.getOptionValue("beta"));

		   ontologyPath = cl.getOptionValue('o',ontologyPath);
		   annotationPath = cl.getOptionValue('a', annotationPath);
		   
		   b4o.setSimulationAlpha(ALPHA);
		   b4o.setSimulationBeta(BETA);
		   b4o.setConsiderFrequenciesOnly(CONSIDER_FREQUENCIES_ONLY);
		   b4o.maxTerms = MAX_TERMS;
		   
	   } catch (ParseException e)
	   {
		   System.err.println("Faield to parse commandline: " + e.getLocalizedMessage());
		   System.exit(1);
	   }
	   
	}

	/**
	 * The main entry.
	 *  
	 * @param args
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws InterruptedException, IOException
	{
		parseCommandLine(args);

		GlobalPreferences.setProxyPort(888);
		GlobalPreferences.setProxyHost("realproxy.charite.de");

		Datafiles df = new Datafiles(ontologyPath,annotationPath);
		b4o.benchmark(df.graph, df.assoc);

		OntologizerThreadGroups.workerThreadGroup.interrupt();
	}
}
