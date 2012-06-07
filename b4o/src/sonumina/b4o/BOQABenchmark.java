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

import sonumina.b4o.benchmark.Benchmark;

/**
 * Main entry point of the BOQA benchmark.
 * 
 * @author Sebastian Bauer
 */
public class BOQABenchmark
{
	static private String ontologyPath;
	static private String annotationPath;

	static private double ALPHA = 0.002;
	static private double BETA = 0.1;
	static private int MAX_TERMS = -1;
	static private int SAMPLES_PER_ITEM = 5;
	static private boolean CONSIDER_FREQUENCIES_ONLY = false;
	static private int SIZE_OF_SCORE_DISTRIBUTION = 250000;
	static private String RESULT_BASE_NAME = "benchmark";
	
	static sonumina.b4o.calculation.BOQA b4o = new sonumina.b4o.calculation.BOQA();

	/**
	 * Parses the command line.
	 * 
	 * @param args
	 */
	public static void parseCommandLine(String [] args)
	{
	   Options opt = new Options();
	   opt.addOption("o", "ontology", true, "Path or URL to the ontology file.");
	   opt.addOption("a", "annotations", true, "Path or URL to files containing annotations.");
	   opt.addOption("c", "considerFreqOnly", false, "If specified, only items with frequencies are considered.");
	   opt.addOption("m", "maxTerms", true, "Defines the maximal number of terms a random query can have. Default is " + MAX_TERMS);
	   opt.addOption("s", "samplesPerItem", true, "Define the number of samples per item. Defaults to " + SAMPLES_PER_ITEM + ".");
	   opt.addOption("r", "resultBaseName", true, "Defines the base name of the result files that are created during the benchmark. Defaults to \"" + RESULT_BASE_NAME + "\".");
	   opt.addOption(null, "alpha", true, "Specifies alpha (false-positive rate) during simulation. Default is " + ALPHA + ".");
	   opt.addOption(null, "beta", true, "Specifies beta (false-negative rate) during simulation. Default is " + BETA + ".");
	   opt.addOption(null, "sizeOfScoreDistribution", true, "Specifies the size of the score distribution. Default is " + SIZE_OF_SCORE_DISTRIBUTION + ".");
	   opt.addOption("h", "help", false, "Shows this help");

	   try
	   {
		   GnuParser parser = new GnuParser();
		   CommandLine cl;
		   cl = parser.parse(opt, args);

		   if (cl.hasOption('h'))
		   {
			   HelpFormatter f = new HelpFormatter();
			   f.printHelp(BOQABenchmark.class.getName(), opt);
			   System.exit(0);
		   }
		   
		   if (cl.hasOption('m'))
			   MAX_TERMS = Integer.parseInt(cl.getOptionValue('m'));

		   if (cl.hasOption('c'))
			   CONSIDER_FREQUENCIES_ONLY = true;

		   if (cl.hasOption('s'))
			   SAMPLES_PER_ITEM = Integer.parseInt(cl.getOptionValue('s'));

		   SIZE_OF_SCORE_DISTRIBUTION = Integer.parseInt(cl.getOptionValue("sizeOfScoreDistribution", "250000"));
		   RESULT_BASE_NAME = cl.getOptionValue('r', RESULT_BASE_NAME);
		   
		   if (cl.hasOption("alpha"))
			   ALPHA = Double.parseDouble(cl.getOptionValue("alpha"));

		   if (cl.hasOption("beta"))
			   BETA = Double.parseDouble(cl.getOptionValue("beta"));

		   ontologyPath = cl.getOptionValue('o',ontologyPath);
		   annotationPath = cl.getOptionValue('a', annotationPath);
		   
		   b4o.setSimulationAlpha(ALPHA);
		   b4o.setSimulationBeta(BETA);
		   b4o.setConsiderFrequenciesOnly(CONSIDER_FREQUENCIES_ONLY);
		   b4o.setSimulationMaxTerms(MAX_TERMS);
		   if (MAX_TERMS != -1)
			   b4o.setMaxQuerySizeForCachedDistribution(MAX_TERMS);
		   b4o.setSizeOfScoreDistribution(SIZE_OF_SCORE_DISTRIBUTION);
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
		b4o.setup(df.graph, df.assoc);
		
		Benchmark benchmark = new Benchmark();
		benchmark.setSamplesPerItem(SAMPLES_PER_ITEM);
		benchmark.setResultBaseName(RESULT_BASE_NAME);
		benchmark.benchmark(b4o);

		OntologizerThreadGroups.workerThreadGroup.interrupt();
	}
}
