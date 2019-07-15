package main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import java.util.Vector;

import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import graph.HeatChart;
import metrics.BestMatchAverage;
import metrics.CoSim;
import metrics.GeneMeasure;
import metrics.GeneSet;
import metrics.GeneSimilarityMetric;
import metrics.JiangConrath;
import metrics.Lin;
import metrics.Maximum;
import metrics.PekarStaab;
import metrics.Resnik;
import metrics.SimGIC;
import metrics.SimUI;
import metrics.TermMeasure;
import metrics.TermSimilarityMetric;
import ontology.GOType;
import ontology.GeneOntology;
import util.NumberFormatter;


public class Main 
{
	private static String logFile = null;
	private static String goFile = null;
	private static String annotFile = null;
	private static String studyFile1 = null;
	private static String studyFile2 = null;
	private static String resultTableFile = "results";
	private static boolean useAllRelations = false;
	private static boolean structural = false;
	private static boolean listOfPairs = false;
	private static boolean compareSets = false;
	private static GOType type = null;

	//Logging:
	//- Output stream .geneTerms.get
	private static FileOutputStream log;
	//- Date format
	static SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	//Data Structures:
	//- The Gene Ontology
	private static GeneOntology go;
	//- The set of study gene products
	private static Vector<String> studySet1 = null;
	//- The set of study gene products
	private static Vector<String> studySet2 = null;
	//- The term similarity measure
	private static TermSimilarityMetric termMetric;
	//- The term enum similarity measure
	private static TermMeasure termMeasure = null;
	//- The gene similarity measure
	private static GeneSimilarityMetric geneMetric;
	//- The gene enum similarity measure
	private static GeneMeasure geneMeasure = null;
	private static GeneSet geneSet;
	//Results
	private static Vector<String> gene1 = new Vector<String>();
	private static Vector<String> gene2 = new Vector<String>();
	private static Vector<Double> score = new Vector<Double>();

	public static void main(String[] args) 
	{
		//Process the arguments
		processArgs(args);
		//If a log file was specified, start the log
		if(logFile != null)
			startLog();
		//Verify the arguments
		verifyArgs();
		System.out.println("logFile:" + logFile);
		System.out.println("goFile:" + goFile);
		System.out.println("annotFile:" + annotFile);
		System.out.println("studyFile1:" + studyFile1);
		System.out.println("studyFile2:" + studyFile2);
		System.out.println("useAllRelations:" + useAllRelations);
		System.out.println("structural:" + structural);
		System.out.println("List of pairs:" + listOfPairs);
		System.out.println("Compare sets:" + compareSets);
		System.out.println("term Measure:" + termMeasure);
		System.out.println("gene Measure:" + geneMeasure);
		System.out.println("GO type:" + type);
		//Open and read files
		openOntology();
		parseTermMeasure();
		if (compareSets)
		{
			parseGeneSet();
			try 
			{
				studySet1 = openGeneSet(studyFile1);
				studySet2 = openGeneSet(studyFile2);
			} 
			catch (FileNotFoundException e) 
			{
				System.err.println("Error: could not find the study set file!");
				e.printStackTrace();
			}
			computeSimilarity();
			saveResult(0, resultTableFile);
			exit();
		}	
		else
		{
			parseGeneMeasure();
			if (listOfPairs)
				openGenePairsSet(studyFile1);
			else
			{
				try 
				{ 
					studySet1 = openGeneSet(studyFile1);
				} 
				catch (FileNotFoundException e)
				{ 
					System.err.println("Error: could not find the study set file!");
					e.printStackTrace();
				}
			}
			computeSimilarity();
			saveResult(0, resultTableFile);
			//HeatMap
			getHeatChart();
			exit();
		}
	}

	private static void computeSimilarity()
	{
		if (listOfPairs)
		{
			for(int i=0; i<studySet1.size(); i++)
			{
				String g1=studySet1.get(i);
				String g2=studySet2.get(i);
				double sc = geneMetric.getGeneSimilarity(g1, g2);
				gene1.add(g1);
				gene2.add(g2);
				score.add(sc);
			}
		}
		else if (compareSets)
		{
			score.add(geneSet.getSetSimilarity(studySet1, studySet2));
			gene1.addAll(getLowestSharedTerms(studySet1, studySet2));
		}

		else
		{
			for(int i=0; i<studySet1.size()-1; i++)
			{
				String g1= studySet1.get(i);
				for(int j=i+1; j<studySet1.size(); j++)
				{
					String g2= studySet1.get(j);
					double sc = geneMetric.getGeneSimilarity(g1, g2);
					gene1.add(g1);
					gene2.add(g2);
					score.add(sc);
				}
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
		System.err.println("Type 'java -jar GSS.jar -h' for details on how to run the program.");
		System.exit(1);		
	}

	private static void exitHelp()
	{
		System.out.println("GOSemanticSimilarity analyses a set of gene products for GO term and gene product similarity\n");
		System.out.println("Usage: 'java -jar GOSemanticSimilarity.jar OPTIONS'\n");
		System.out.println("Options:");
		System.out.println("-g, --go FILE_PATH\tPath to the Gene Ontology OBO or OWL file");
		System.out.println("-a, --annotation FILE_PATH\tPath to the tabular annotation file (GAF, BLAST2GO or 2-column table format");
		System.out.println("-s1, --study1 FILE_PATH\tPath to the fisrt file listing the study set gene products");
		System.out.println("-s2, --study2 FILE_PATH\tPath to the second file listing the study set gene products");
		System.out.println("-t, --type ['molecular_function'/'biological_process'/'cellular_component']");
		System.out.println("-st --structural Compute structural IC");
		System.out.println("-rel, --use_all_relations Infer annotations through 'part_of' and other non-hierarchical relations]");
		System.out.println("-lp, --list_of_pairs The study set is a list of pairs of genes");
		System.out.println("-set, --set Compare two study sets");
		System.out.println("-res, --result\tFILE_PATH\tPath to the output similarity result file]");
		System.out.println("-tsm, --term_metric\tMetric used to analyse semantic similarity between ontology terms");
		System.out.println("-gsm, --gene_metric\tMetric used to analyse semantic similarity between genes or gene products");
		System.exit(0);		
	}

	public static GeneOntology getGO()
	{
		return go;
	}

	public static void getHeatChart()
	{
		System.out.println(df.format(new Date()) + " - Preparing the Heat Chart");
		File resultGraphFile = new File("HeatChart.png");
		int size = studySet1.size();

		if(listOfPairs)
		{
			double[][] matrix = new double[size][1];
			// Fill the similarity matrix based on score vector previously computed
			for(int i=0; i < size; i++)
			{
				matrix[i][0] = score.elementAt(i);
			}

			HeatChart hc = new HeatChart(matrix);
			//Set axis values
			String[] yAxis = new String[size];
			String[] xAxis = new String[1];
			for(int i=0; i< size; i++)
				yAxis[i] = studySet1.elementAt(i) + " x " + studySet2.elementAt(i);
			xAxis[0] = " ";
			hc.setYValues(yAxis);
			hc.setYAxisLabel(null);
			hc.setXValues(xAxis);
			hc.setXAxisLabel(null);

			try {
				hc.saveToFile(resultGraphFile);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		else
		{
			//Set similarity values
			double[][] similarityMatrix = getSimilarityMatrix();
			HeatChart hc = new HeatChart(similarityMatrix);
			//Set axis values
			String[] axis = studySet1.toArray(new String[size]);
			hc.setXValues(axis);
			hc.setYValues(axis);
			try {
				hc.saveToFile(resultGraphFile);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		System.out.println(df.format(new Date()) + " - Finished!");
	}

	private static Set<String> getLowestSharedTerms(Vector<String> geneSet1, Vector<String> geneSet2)
	{
		Set<String> terms = Main.getGO().termSet(geneSet1, type);
		terms.retainAll(Main.getGO().termSet(geneSet2, type));
		return Main.getGO().getNonRedundantTerms(terms);
	}

	private static double[][] getSimilarityMatrix() 
	{
		int matrixSize= studySet1.size();

		double [][] similarityMatrix = new double[matrixSize][matrixSize];
		int count = 0;

		// Fill the similarity matrix based on score vector previously computed
		for(int i=0; i < matrixSize-1; i++)
		{
			for(int j=i+1; j < matrixSize; j++)
			{
				//The similarity between a gene and itself is 1
				if(j == i+1)
					similarityMatrix[i][j-1] = 1;
				similarityMatrix[i][j] = score.elementAt(count+j-1);
			}
			count += matrixSize - (i+2);
		}

		//Fill the last entry of the matrix (not covered by the for cycle)
		similarityMatrix[matrixSize-1][matrixSize-1] = 1;

		// Fill the upper half of the square matrix  
		for(int i=0; i < matrixSize; i++)
		{
			for(int j=0; j < matrixSize; j++)
				similarityMatrix[j][i] = similarityMatrix[i][j];
		}

		return similarityMatrix;
	}

	public static GOType getType()
	{
		return type;
	}

	private static void openOntology()
	{
		try
		{
			System.out.println(df.format(new Date()) + " - Reading Gene Ontology and annotations");
			go = new GeneOntology(goFile, annotFile, useAllRelations, structural);
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
	 * Opens a set file containing pairs of gene products, which is expected to be a plain text file
	 * in which the elements of the pair are separated by one of: space, tab, comma, or
	 * semicolon) with the gene products identifier listed in the first and second collumns.
	 * @param file: the path to the input gene product file
	 */
	private static void openGenePairsSet(String studyFile)
	{
		System.out.println(df.format(new Date()) + " - Reading study set from '" + studyFile + "'");
		studySet1 = new Vector<String>();
		studySet2 = new Vector<String>();
		try
		{
			BufferedReader in = new BufferedReader(new FileReader(studyFile));
			String line;
			String notFound = "";
			String noPair = "";
			int count = 0;
			while((line = in.readLine()) != null)
			{	
				String[] word = line.split("[ \t,;]");
				// If one doesn't have a pair
				if (word.length<2)
					noPair += word[0] + ",";
				//check if both genes are in the annotation file
				//if not, add to not found 
				//if one gene is and the other is not, add to no pair list
				else if(!go.contains(word[0]))
				{
					notFound += word[0] + ",";
					count++;
					if(count%15==0)
						notFound += "\n";

					if(!go.contains(word[1]))
					{
						notFound += word[1] + ",";
						count++;
					}
					else
						noPair += word[1]+ ",";	
				}
				else if(!go.contains(word[1]))
				{
					notFound += word[1] + ",";
					count++;
					if(count%15==0)
						notFound += "\n";
					noPair += word[0]+ ",";
				}
				//if both genes are in the annotation file, save them
				else
				{
					studySet1.add(word[0]);
					studySet2.add(word[1]);
				}
			}
			in.close();
			if(notFound.length() > 0)
				System.out.println("Warning: the following gene products were not listed in the annotation file and were ignored:\n" 
						+ notFound.substring(0,notFound.length()-1));
			if(noPair.length() > 0)
				System.out.println("Warning: the following gene products did not have a pair and were ignored:\n" 
						+ noPair.substring(0,noPair.length()-1));
		}
		catch(IOException e)
		{
			System.err.println("Error: could not read gene product set '" + studyFile1 + "'!");
			e.printStackTrace();
			try{ log.close(); }
			catch (IOException f){ /*Do nothing*/ }
			System.exit(1);
		}

		System.out.println(df.format(new Date()) + " - Read " + studySet1.size() + " pairs of genes");
	}
	/**
	 *Opens a gene product set file, which is expected to be a plain text file
	 * containing one or more columns (separated by one of: space, tab, comma, or
	 * semicolon) with the gene product identifier listed in the first column or a list
	 * of pairs of genes (separated by one of: space, tab, comma, or semicolon).
	 * @param file: the path to the input gene product file
	 */
	private static Vector<String> openGeneSet(String studyFile) throws FileNotFoundException
	{
		System.out.println(df.format(new Date()) + " - Reading study set from '" + studyFile + "'");
		Vector<String> studySet = new Vector<String>();
		try
		{
			BufferedReader in = new BufferedReader(new FileReader(studyFile));
			String line;
			String notFound = "";
			int count = 0;
			while((line = in.readLine()) != null)
			{	
				String[] word = line.split("[ \t,;]");
				if(go.contains(word[0]))
				{
					studySet.add(word[0]);
				}
				else
				{
					notFound += word[0] + ",";
					count++;
					if(count%15==0)
						notFound += "\n";
				}
			}
			in.close();
			if(notFound.length() > 0)
				System.out.println("Warning: the following gene products were not listed in the annotation file and were ignored:\n" 
						+ notFound.substring(0,notFound.length()-1));
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
		return studySet;
	}
	private static void parseTermMeasure()
	{
		if (termMeasure == null)
			return;
		if (termMeasure.equals(TermMeasure.RESNIK))
			termMetric = new Resnik();
		else if (termMeasure.equals(TermMeasure.LIN))
			termMetric = new Lin();
		else if (termMeasure.equals(TermMeasure.JIANG_CONRATH))
			termMetric = new JiangConrath();
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
			geneMetric = new SimGIC();
		else if (geneMeasure.equals(GeneMeasure.COSIM))
			geneMetric = new CoSim();
		else if (geneMeasure.equals(GeneMeasure.MAXIMUM))
			geneMetric = new Maximum(termMetric);
		else if (geneMeasure.equals(GeneMeasure.BEST_MATCH_AVERAGE))
			geneMetric = new BestMatchAverage(termMetric);
	}
	private static void parseGeneSet()
	{
		if (geneMeasure == null)
			return;
		if (geneMeasure.equals(GeneMeasure.SIM_UI))
			geneSet = new SimUI();
		else if (geneMeasure.equals(GeneMeasure.SIM_GIC))
			geneSet = new SimGIC();
		else if (geneMeasure.equals(GeneMeasure.COSIM))
			geneSet = new CoSim();
		else if (geneMeasure.equals(GeneMeasure.BEST_MATCH_AVERAGE))
			geneSet = new BestMatchAverage(termMetric);
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
			else if((args[i].equalsIgnoreCase("-s1") || args[i].equalsIgnoreCase("--study1")) &&
					i < args.length-1)
			{
				studyFile1 = args[++i];
			}
			else if((args[i].equalsIgnoreCase("-s2") || args[i].equalsIgnoreCase("--study2")) &&
					i < args.length-1)
			{
				studyFile2 = args[++i];
			}
			else if((args[i].equalsIgnoreCase("-t") || args[i].equalsIgnoreCase("--type")) &&
					i < args.length-1)
			{
				type = GOType.parse(args[++i]);
			}
			else if((args[i].equalsIgnoreCase("-res") || args[i].equalsIgnoreCase("--results")) &&
					i < args.length-1)
			{
				resultTableFile = args[++i];
			}

			else if((args[i].equalsIgnoreCase("-rel") || args[i].equalsIgnoreCase("--use_all_relations")))
			{
				useAllRelations = true;
			}
			else if((args[i].equalsIgnoreCase("-st") || args[i].equalsIgnoreCase("--structural")))
			{
				structural = true;
			}
			else if((args[i].equalsIgnoreCase("-lp") || args[i].equalsIgnoreCase("--list_of_pairs")))
			{
				listOfPairs = true;
			}
			else if((args[i].equalsIgnoreCase("-set") || args[i].equalsIgnoreCase("--set")))
			{
				compareSets = true;
			}
			else if((args[i].equalsIgnoreCase("-tsm") || args[i].equalsIgnoreCase("--term_metric")))
			{
				termMeasure = TermMeasure.parse(args[++i]);
			}
			else if((args[i].equalsIgnoreCase("-gsm") || args[i].equalsIgnoreCase("--gene_metric")))
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
		System.out.println(df.format(new Date()) + " - Saving result file '" + file + "'");

		if (compareSets)
		{
			try
			{
				PrintWriter out = new PrintWriter(new FileWriter(file));
				out.println("The similarity between "+ studyFile1 + " and " + studyFile2 + " is " + score.elementAt(0).toString());
				out.println();
				if(gene1.isEmpty())
					out.println("There are no common GO Terms between the two data sets.");
				else
				{
					out.println("The common GO terms found between the two data sets were ");

					for (String s: gene1)
						out.println("- "+ Main.getGO().getLabel(s));
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
		else
		{
			try
			{
				PrintWriter out = new PrintWriter(new FileWriter(file));
				for(int i=0; i<gene1.size(); i++)
				{
					out.print(gene1.elementAt(i)+ "\t");
					out.print(gene2.elementAt(i)+ "\t");
					out.println(NumberFormatter.formatScore(score.elementAt(i)));
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
		if(studyFile1 == null)
		{
			System.err.println("Error: you must specify an input study-set file.");
			exitError();
		}
		if(geneMeasure == null)
		{
			System.err.println("Error: you must specify gene similarity measure.");
			exitError();
		}
		if(termMeasure == null && (geneMeasure == GeneMeasure.BEST_MATCH_AVERAGE || geneMeasure == GeneMeasure.MAXIMUM))
		{
			System.err.println("Error: you must specify a term similarity metric.");
			exitError();
		}
		if((termMeasure == TermMeasure.COSIM || termMeasure == TermMeasure.SIMGIC || termMeasure == TermMeasure.SIMUI)
				&& (geneMeasure == GeneMeasure.BEST_MATCH_AVERAGE))
		{
			System.out.println("Error: Best match average gene similarity metric can only be implemented with the following term similarity metrics: Resnik, Lin, Jiang Conrath, PekarStaab");
			exitError();
		}
		if((termMeasure == TermMeasure.COSIM || termMeasure == TermMeasure.SIMGIC || termMeasure == TermMeasure.SIMUI)
				&& (geneMeasure == GeneMeasure.MAXIMUM))
		{
			System.out.println("Error: Maximum gene similarity metric can only be implemented with the following term similarity metrics: Resnik, Lin, Jiang Conrath, PekarStaab");
			exitError();
		}
	}
}
