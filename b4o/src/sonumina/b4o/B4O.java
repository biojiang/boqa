package sonumina.b4o;

import java.io.IOException;

import ontologizer.calculation.CalculationRegistry;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;


public class B4O
{
	static private String ontologyPath;
	static private String annotationPath;

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
		   
		   ontologyPath = cl.getOptionValue('o',ontologyPath);
		   annotationPath = cl.getOptionValue('a', annotationPath);
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

		sonumina.b4o.calculation.B4O.benchmark(ontologyPath, annotationPath);
	}
}
