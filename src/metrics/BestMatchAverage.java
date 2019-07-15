package metrics;

import java.util.HashMap;
import java.util.Set;
import java.util.Vector;

import main.Main;
import ontology.GOType;

public class BestMatchAverage implements GeneSimilarityMetric, GeneSet
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
		Set<String> terms1;
		Set<String> terms2;
		GOType t = Main.getType();
		if(t == null)
		{
			terms1 = Main.getGO().getAnnotationsGene(gene1);
			terms2 = Main.getGO().getAnnotationsGene(gene2);
		}
		else
		{
			terms1 = Main.getGO().getAnnotationsGene(gene1,t);
			terms2 = Main.getGO().getAnnotationsGene(gene2,t);
		}

		//Initialize the scores
		double score1 = 0.0;
		double score2 = 0.0;
		//Identify all common terms and set them as default best matches
		HashMap<String,Double> gene1Scores = new HashMap<String,Double>();
		HashMap<String,Double> gene2Scores = new HashMap<String,Double>();

		//Compute the maximum similarity between the genes' terms
		for(String t1: terms1)
		{	
			for(String t2: terms2)
			{
				double sim = measure.getTermSimilarity(t1, t2);

				if(!gene1Scores.containsKey(t1) || sim > gene1Scores.get(t1))
					gene1Scores.put(t1, sim);
				if(!gene2Scores.containsKey(t2) || sim > gene2Scores.get(t2))
					gene2Scores.put(t2, sim);
			}
		}

		for(String term: gene1Scores.keySet())
			score1 += gene1Scores.get(term);

		for(String term: gene2Scores.keySet())
			score2 += gene2Scores.get(term);	

		score1 /= terms1.size();
		score2 /= terms2.size();


		return (score1+score2)/2;
	}

	public double  getSetSimilarity(Vector<String> geneSet1, Vector<String> geneSet2) 
	{
		HashMap<String, Integer> count1 = Main.getGO().termCountMap(geneSet1, Main.getType());

		if(geneSet1.equals(geneSet2))
			return 1.0;

		HashMap<String, Integer> count2 = Main.getGO().termCountMap(geneSet2, Main.getType());
		double maxScore = 0.0;
		String bestMatch = null;
		double score1 = 0.0;
		double score2 = 0.0;

		for (String t1: count1.keySet())
		{
			if (count2.containsKey(t1))
				score1 += 1.0 * Math.min(count1.get(t1), count2.get(t1))/ count1.get(t1);
			else 
			{
				for( String t2: count2.keySet())
				{
					double sim = measure.getTermSimilarity(t1, t2);
					if(sim > maxScore)
					{
						maxScore = sim;
						bestMatch = t2;
					}
				}
			}
			score1 += maxScore * Math.min(count1.get(bestMatch), count2.get(bestMatch)) / count1.get(t1);
			maxScore = 0.0;
			bestMatch = null;
		}
		for (String t2: count2.keySet())
		{
			if (count1.containsKey(t2))
				score2 += 1.0 * Math.min(count1.get(t2), count2.get(t2))/ count2.get(t2);
			else 
			{
				for( String t1: count1.keySet())
				{
					double sim = measure.getTermSimilarity(t1, t2);
					if(sim > maxScore)
					{
						maxScore = sim;
						bestMatch = t1;
					}
				}
			}
			score2 += maxScore * Math.min(count1.get(bestMatch), count2.get(bestMatch)) / count1.get(t2);
			maxScore = 0.0;
			bestMatch = null;
		}
		return (score1+score2)/2;
	}	

}