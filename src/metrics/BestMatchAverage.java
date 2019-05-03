package metrics;

import java.util.HashSet;
import java.util.Set;

import main.Main;

public class BestMatchAverage implements GeneSimilarityMetric 
{
	private TermSimilarityMetric measure;
	private double[][] similarityMatrix;
	private double[][] tempMatrix;
	private int max;
	private int total;
	private int[] rowCover;
	private int[] columnCover;
	private int[][] assignments;
	private Set<Integer> markedRow;
	private Set<Integer> markedColumn;
	private boolean done;
	private double similarityScore;

	public BestMatchAverage(TermSimilarityMetric measure)
	{
		this.measure = measure;
	}
	
	public double getGeneSimilarity(String gene1, String gene2) 
	{
		if(gene1.equals(gene2)) 
			return 1;

		Set<String> terms1 = Main.getGO().getNonRedundantTerms(gene1);
		Set<String> terms2 = Main.getGO().getNonRedundantTerms(gene2);
		total = Math.max(terms1.size(), terms2.size());
		//Remove terms that are shared by the two genes
		//(they should always be assigned by the Hungarian
		//algorithm, so there is no point including them
		//and their similarity is always 1)
		Set<String> temp = new HashSet<String>(terms1);
		terms1.removeAll(terms2);
		terms2.removeAll(temp);
		max = Math.max(terms1.size(), terms2.size());
		similarityScore = total - max;

		//If all terms in one gene are present in the other
		//we can compute the similarity directly
		if(Math.min(terms1.size(), terms2.size()) == 0)
			return similarityScore * 1.0 / total;

		//Otherwise we proceed with the similarity computations and
		//the Hungarian algorithm
		similarityMatrix = new double[max][max];
		tempMatrix = new double[max][max];

		int count1 = 0; 
		int count2;
		double maxrow;

		//Compute the similarity matrix between the genes' terms
		//and do the first step of the Hungarian algorithm to find
		//the maximum weighted bipartite mapping: find and subtract
		//the maximum value of each row
		for(String t1: terms1)
		{	
			maxrow = 0.0;
			count2 = 0;

			for(String t2: terms2)
			{
				similarityMatrix[count1][count2] = measure.getTermSimilarity(t1, t2);
				tempMatrix[count1][count2] = similarityMatrix[count1][count2];

				if(similarityMatrix[count1][count2] > maxrow)
					maxrow = similarityMatrix[count1][count2];
				count2++;
			}
			//Subtract the maximum of each row and replace it with the absolute value
			//to enable the traditional implementation of the Hungarian algorithm
			for (int i=0; i<max; i++)
				tempMatrix[count1][i] = Math.abs(tempMatrix[count1][i]-maxrow); 

			count1++;
		}
		//Check assignment and return score if complete
		assignment();
		if(done)
			return scoreAssignment();
		//Otherwise subtract column maximums as per the rows above
		subtractColumns();
		//Check again the assignment
		assignment();
		//While not complete, cover zeros, create new ones, and check assignment
		while(!done)
		{
			createZeros();
			assignment();
		}
		return scoreAssignment();
	}
	
	//Find and subtract the minimum of each column
	private void subtractColumns()
	{
		for(int i=0; i<max; i++) //columns
		{
			double mincolumn=1.1;
			for(int j=0; j<max; j++) //rows
			{
				if(mincolumn > tempMatrix[j][i])
					mincolumn = tempMatrix[j][i];
			}
			for(int j=0; j<max; j++)
				tempMatrix[j][i] -= mincolumn;
		}
	}
	
	
	//And check if you have a complete assignment
	private void assignment()
	{
		rowCover = new int[max];
		columnCover = new int[max];
		assignments = new int[max][max];
		done = false;
		int assignmentCount = 0;
		for(int i=0; i<max; i++) //rows
		{
			for(int j=0; j<max; j++) //columns
			{
				if(tempMatrix[i][j]==0 && rowCover[i]==0 && columnCover[j]==0)
				{
					rowCover[i]= 1;
					columnCover[j] = 1;
					assignments[i][j] = 1;
					assignmentCount++;
				}
			}
		}
		//Check if we have a trivial assignment
		done = assignmentCount == max;
		if(done)
			return;
	
		//Otherwise cover all zeros with the minimum number of lines
		markedRow = new HashSet<Integer>();
		markedColumn = new HashSet<Integer>();
		for(int i=0; i<max; i++)
		{
			//Mark all rows having no assignments
			if(rowCover[i] == 0)
			{
				markedRow.add(i);

				//Mark all (unmarked) columns having zeros in newly marked row(s)
				for(int j=0; j<max; j++)
				{
					if(tempMatrix[i][j]==0)
						markedColumn.add(j);

					//Mark all rows having assignments in newly marked columns
					for(int l=0; l<max; l++)
					{
						if(assignments[l][j]==1)
							markedRow.add(l);
					}
				}
			}
		}
		//Check if we can make a complete assignment
		done = markedColumn.size() - markedRow.size() == 0;
		if(done)
		{
			
		}
	}
	
	// Create additional zeros: find the minimum uncovered number, subtract it 
	// to the uncovered entries and add it to every element covered by two lines.
	private void createZeros()
	{
		double min = 1.1;

		for(int i=0; i<max; i++) 
		{
			if(markedRow.contains(i))
				continue;

			for(int j=0; j<max; j++) 
			{
				if(markedColumn.contains(j))
					continue;		
				if(tempMatrix[i][j]< min)
					min = tempMatrix[i][j];
			}
		}	

		for(int i=0; i<max; i++) 
		{
			for(int j=0; j<max; j++) 
			{
				if(markedRow.contains(i) && markedColumn.contains(j))
					tempMatrix[i][j] += min;		
				else if(!markedRow.contains(i) && !markedColumn.contains(j))
					tempMatrix[i][j] -= min;
			}
		}
	}

	//Compute the similarity score of the otimal assignment (best match average)
	private double scoreAssignment()
	{			
		for(int i=0; i<max; i++) 
		{
			for(int j=0; j<max; j++) 
			{
				if(assignments[i][j] == 1)
					similarityScore =+ similarityMatrix[i][j];
			}
		}
		return similarityScore / total;
	}
}




