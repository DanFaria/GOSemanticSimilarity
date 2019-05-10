package metrics;

import java.util.Set;

import main.Main;

public class SimGIC implements TermSimilarityMetric, GeneSimilarityMetric 
{
	public double getTermSimilarity(String term1, String term2)
	{
		if(term1.equals(term2))
			return 1;
		else
		{
			double intersectIC = 0;
			double unionIC = 0;
			
			for (String i: Main.getGO().getCommonAncestors(term1,term2))
				intersectIC += Main.getGO().getInfoContent(i);
		
			for (String i: Main.getGO().getCombinedAncestors(term1,term2))
				unionIC += Main.getGO().getInfoContent(i);
			
			return intersectIC*1.0 / unionIC;
		}
	}

	
	public double getGeneSimilarity(String gene1, String gene2) 
	{
		if(gene1.equals(gene2))
			return 1;
		else
		{
			Set<String> set2 = Main.getGO().geneTerms.get(gene2);
			double intersection = 0.0;
			double union = 0.0;
			
			//computes intersection IC
			for (String i: Main.getGO().geneTerms.get(gene1))
			{	
				double IC = Main.getGO().getInfoContent(i);
				
				if(set2.contains(i))
					intersection += IC;
			
				else
					union += IC;		
			}
			if(intersection == 0.0)
				return 0.0;
			//computes union IC
			for (String i: set2)
			{
				union += Main.getGO().getInfoContent(i);
			}
			return intersection / union;
		}
	}	
}