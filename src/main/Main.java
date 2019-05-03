package main;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Vector;

import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import metrics.TermSimilarityMetric;
import metrics.BestMatchAverage;
import metrics.CoSim;
import metrics.GeneMeasure;
import metrics.GeneSimilarityMetric;
import metrics.JiangConrath;
import metrics.Lin;
import metrics.Maximum;
import metrics.PekarStaab;
import metrics.Resnik;
import metrics.SimGIC;
import metrics.SimUI;
import metrics.TermMeasure;
import ontology.GeneOntology;
import util.NumberFormatter;
import util.Table2Set;


public class Main 
{
	
	private static String logFile = null;
	private static String goFile = null;
	private static String annotFile = null;
	private static String studyFile = null;
	private static boolean useAllRelations;
	private static boolean structural;
	
	//Logging:
	//- Output stream 
	private static FileOutputStream log;
	 //- Date format
	static SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	//Data Structures:
	//- The Gene Ontology
	private static GeneOntology go;
	//- The set of study gene products
	private static HashSet<String> studySet = null;
	//- The term similarity measure
	private static TermSimilarityMetric termMetric;
	//- The term enum similarity measure
	private static TermMeasure termMeasure;
	//- The gene similarity measure
	private static GeneSimilarityMetric geneMetric;
	//- The gene enum similarity measure
	private static GeneMeasure geneMeasure;
	//Results
	private static Table2Set<Double, HashSet<String>> results = null;


	public static void main(String[] args) 
	{
		//Process the arguments
		processArgs(args);
		//If a log file was specified, start the log
		if(logFile != null)
			startLog();
		//Verify the arguments
		verifyArgs();
		//Parse the enum measures to similarity metrics
		parseTermMeasure();
		parseGeneMeasure();
		//Open and read files
		openOntology();
		openGeneSet();
		computeSimilarity();
		saveResult(0, "similarity_result");
		exit();
	}
	
	private static void computeSimilarity()
	{
		Vector<String> studyVector = new Vector<String>(studySet);
		HashSet<String> tempGeneSet = new HashSet<String>();

		for(int i=0; i<studyVector.size()-1; i++)
		{
			for(int j=i+1; j<studyVector.size()-1; j++)
			{
				String gene1= studyVector.get(i);
				String gene2= studyVector.get(j);
				tempGeneSet.add(gene1);
				tempGeneSet.add(gene2);
				results.add(geneMetric.getGeneSimilarity(gene1, gene2), tempGeneSet);
				tempGeneSet.clear();
			}
		}
	}

	private static void exit()
	{
		if(log != null)
		{
			try{ log.close(); }
			catch (IOException f){ /*Do nothing*/ }
		}
		System.exit(0);
	}
	
	private static void exitError()
	{
		System.err.println("Type 'java -jar GOSemanticSimilarity.jar -h' for details on how to run the program.");
		System.exit(1);		
	}
	
	private static void exitHelp()
	{
		System.out.println("GOSemanticSimilarity analyses a set of gene products for GO term and gene product similarity\n");
		System.out.println("Usage: 'java -jar GOSemanticSimilarity.jar OPTIONS'\n");
		System.out.println("Options:");
		System.out.println("-g, --go FILE_PATH\tPath to the Gene Ontology OBO or OWL file");
		System.out.println("-a, --annotation FILE_PATH\tPath to the tabular annotation file (GAF, BLAST2GO or 2-column table format");
		System.out.println("-s, --study FILE_PATH\tPath to the file listing the study set gene products");
		System.out.println("-st --structural\tCompute structural IC");
		System.out.println("[-rel, --use_all_relations\tInfer annotations through 'part_of' and other non-hierarchical relations]");
		System.out.println("[-res, --result FILE_PATH\tPath to the output similarity result file]");
		System.out.println("-tsm, --term_semantic_similarity_measure\tMetric used to analyse semantic similarity between ontology terms");
		System.out.println("-gsm, --gene_semantic_similarity_measure\tMetric used to analyse semantic similarity between genes or gene products");
		System.exit(0);		
	}
	
	public static GeneOntology getGO()
	{
		return go;
	}
	
	public HashSet<String> getStudySet()
	{
		return studySet;
	}

	private static void openOntology()
	{
		try
		{
			System.out.println(df.format(new Date()) + " - Reading Gene Ontology and annotations");
			go = new GeneOntology(goFile, annotFile, useAllRelations);
			System.out.println(df.format(new Date()) + " - Finished");
		}
		
		catch(IOException e)
		{
			System.err.println(df.format(new Date()) + " - Error: could not read annotation set '" + annotFile + "'!");
			e.printStackTrace();
			try{ log.close(); }
			catch (IOException f){ /*Do nothing*/ }
			System.exit(1);
		}
		catch(OWLOntologyCreationException o)
		{
			System.err.println(df.format(new Date()) + " - Error: could not read Gene Ontology file '" + goFile + "'!");
			o.printStackTrace();
			try{ log.close(); }
			catch (IOException f){ /*Do nothing*/ }
			System.exit(1);
		}	
	}
	
	/**
	 * Opens a gene product set file, which is expected to be a plain text file
	 * containing one or more columns (separated by one of: space, tab, comma, or
	 * semicolon) with the gene product identifier listed in the first column 
	 * @param file: the path to the input gene product file
	 */
	private static void openGeneSet()
	{

		studySet = new HashSet<String>();
		System.out.println(df.format(new Date()) + " - Reading study set from '" + studyFile + "'");

		try
		{
			BufferedReader in = new BufferedReader(new FileReader(studyFile));
			String line;
			String notFound = "";
			int count = 0;
			while((line = in.readLine()) != null)
			{	
				String[] word = line.split("[ \t,;]");
				if(word[0].length() > 0)
				{
					if(go.contains(word[0]))
						studySet.add(word[0]);
					else
					{
						notFound += word[0] + ",";
						count++;
						if(count%15==0)
							notFound += "\n";
					}
				}
			}
			in.close();
			if(notFound.length() > 0)
				System.out.println("Warning: the following gene products were not listed in the " +
						"annotation file and were ignored:\n" + notFound.substring(0, notFound.length()-1));

		}
		catch(IOException e)
		{
			System.err.println("Error: could not read gene product set '" + studyFile + "'!");
			e.printStackTrace();
			try{ log.close(); }
			catch (IOException f){ /*Do nothing*/ }
			System.exit(1);
		}
		System.out.println(df.format(new Date()) + " - Read " + studySet.size() + " genes");
	}
 
	private static void parseTermMeasure()
	{
		if (termMeasure == null)
			return;
			
		if (termMeasure.equals(TermMeasure.RESNIK))
			termMetric = new Resnik(structural);
			
		else if (termMeasure.equals(TermMeasure.LIN))
			termMetric = new Lin(structural);
			
		else if (termMeasure.equals(TermMeasure.JIANG_CONRATH))
			termMetric = new JiangConrath(structural);
	
		else if (termMeasure.equals(TermMeasure.PEKAR_STAAB))
			termMetric = new PekarStaab();		
	}
	
	private static void parseGeneMeasure()
	{
		if (geneMeasure == null)
			return;
		
		if (geneMeasure.equals(GeneMeasure.SIM_UI))
			geneMetric = new SimUI();
		
		else if (geneMeasure.equals(GeneMeasure.SIM_GIC))
			geneMetric = new SimGIC(structural);
		
		else if (geneMeasure.equals(GeneMeasure.COSIM))
			geneMetric = new CoSim(structural);
		
		else if (geneMeasure.equals(GeneMeasure.MAXIMUM))
			geneMetric = new Maximum(structural, termMetric);
		
		else if (geneMeasure.equals(GeneMeasure.BEST_MATCH_AVERAGE))
			geneMetric = new BestMatchAverage(termMetric);
	}
	
	private static void processArgs(String[] args)
	{
		//Process the arguments
		for(int i = 0; i < args.length; i++)
		{
			if((args[i].equalsIgnoreCase("-l") || args[i].equalsIgnoreCase("--log")) &&
					i < args.length-1)
			{
				logFile = args[++i];
			}
			else if((args[i].equalsIgnoreCase("-g") || args[i].equalsIgnoreCase("--go")) &&
					i < args.length-1)
			{
				goFile = args[++i];
			}
			else if((args[i].equalsIgnoreCase("-a") || args[i].equalsIgnoreCase("--annotation")) &&
					i < args.length-1)
			{
				annotFile = args[++i];
			}
			else if((args[i].equalsIgnoreCase("-s") || args[i].equalsIgnoreCase("--study")) &&
					i < args.length-1)
			{
				studyFile = args[++i];
			}
			
			else if((args[i].equalsIgnoreCase("-r") || args[i].equalsIgnoreCase("--use_all_relations")))
			{
				useAllRelations = true;
			}
			else if((args[i].equalsIgnoreCase("-st") || args[i].equalsIgnoreCase("--structural")))
			{
				structural = true;
			}
			else if((args[i].equalsIgnoreCase("-tsm") || args[i].equalsIgnoreCase("--term semantic similarity measure")))
			{
				termMeasure = TermMeasure.parse(args[++i]);
			}
			else if((args[i].equalsIgnoreCase("-gsm") || args[i].equalsIgnoreCase("--gene semantic similarity measure")))
			{
				geneMeasure = GeneMeasure.parse(args[++i]);
			}
			else if(args[i].equalsIgnoreCase("-h") || args[i].equalsIgnoreCase("--help"))
			{
				exitHelp();
			}
		}
	}
	
	public static void saveResult(int index, String file)
	{
		try
		{
			System.out.println(df.format(new Date()) + " - Saving result file '" + file + "'");
			PrintWriter out = new PrintWriter(new FileWriter(file));

			//First write the header
			out.print("Gene1/Gene2/Similarity");

			//Then write the term information (in descending similarity score order)
			for(Double score : results.keySet())
			{
				out.print(results.get(score) + "\t");
				out.print(NumberFormatter.formatScore(score) + "\t");
			}
			out.close();
			System.out.println(df.format(new Date()) + " - Finished");
		}
		catch(IOException e)
		{
			System.err.println("Error: could not write result file '" + file + "'!");
			e.printStackTrace();
			try{ log.close(); }
			catch (IOException f){ /*Do nothing*/ }
			System.exit(1);
		}
	}
	
	public static void startLog()
	{
		try
		{
			//Initialize the log
			log = new FileOutputStream(logFile);
			//Redirect stdOut and stdErr to the log file
	      	System.setOut(new PrintStream(log, true));
	       	System.setErr(new PrintStream(log, true));		
		}
		catch(IOException e)
		{
			System.out.println(df.format(new Date()) + " - Warning: could not initiate log file!");
		}
	}
	
	public static boolean structural()
	{
		return structural;
	}
	
	public static boolean useAllRelations()
	{
		return useAllRelations;
	}

	//Checks that all mandatory parameters were entered so that the program can proceed
	private static void verifyArgs()
	{
		if(goFile == null)
		{
			System.err.println("Error: you must specify an input ontology file.");
			exitError();
		}
		if(annotFile == null)
		{
			System.err.println("Error: you must specify an input annotation file.");
			exitError();
		}
		if(studyFile == null)
		{
			System.err.println("Error: you must specify an input study-set file.");
			exitError();
		}
		if(termMeasure == null && (geneMeasure == GeneMeasure.BEST_MATCH_AVERAGE || geneMeasure == GeneMeasure.MAXIMUM))
		{
			System.err.println("Error: you must specify a term similarity metric.");
			exitError();
		}
	}
}
