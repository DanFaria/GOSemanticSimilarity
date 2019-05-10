package metrics;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import main.Main;

public class BestMatchAverage implements GeneSimilarityMetric 
{
	private TermSimilarityMetric measure;
	
	public BestMatchAverage(TermSimilarityMetric measure)
	{
		this.measure = measure;
	}
	
	public double getGeneSimilarity(String gene1, String gene2) 
	{
		//Similarity between a gene and itself is 1 by default
		if(gene1.equals(gene2)) 
			return 1;
		//Get the terms of the two genes
		Set<String> terms1 = Main.getGO().getNonRedundantTerms(gene1);
		Set<String> terms2 = Main.getGO().getNonRedundantTerms(gene2);
		//Initialize the scores
		double score1 = 0.0;
		double score2 = 0.0;
		
		//Identify all common terms and set them as default best matches
		Set<String> temp = new HashSet<String>(terms1);
		temp.retainAll(terms2);
		
		if(temp.size() == terms1.size() && temp.size() == terms2.size())
			return 1.0;

		HashMap<String,Double> gene1Scores = new HashMap<String,Double>();
		HashMap<String,Double> gene2Scores = new HashMap<String,Double>();


		//Compute the maximum similarity between the genes' terms
		for(String t1: terms1)
		{	
			if(temp.contains(t1))
				gene1Scores.put(t1, 1.0);
			for(String t2: terms2)
			{
				if(temp.contains(t2))
				{
					gene2Scores.put(t2, 1.0);
					if(temp.contains(t1))
						continue;
				}
				double sim = measure.getTermSimilarity(t1, t2);
				if(!gene1Scores.containsKey(t1) || sim > gene1Scores.get(t1))
					gene1Scores.put(t1, sim);
				if(!gene2Scores.containsKey(t2) || sim > gene1Scores.get(t1))
					gene2Scores.put(t2, sim);
			}
		}
		
		for( String term: gene1Scores.keySet())
		{
			score1 += gene1Scores.get(term);
		}
		
		for( String term: gene2Scores.keySet())
		{
			score2 += gene2Scores.get(term);
		}
		
		score1 /= terms1.size();
		score2 /= terms2.size();
		
		return (score1+score2)/2;
		
		
	}	

}