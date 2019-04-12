package metrics;

import java.util.Set;

import main.Main;

public class SimGIC implements TermSimilarityMetric, GeneSimilarityMetric 
{
	private boolean structural;

	public SimGIC (boolean structural)
	{
		this.structural = structural;
	}

	public double getTermSimilarity(String term1, String term2)
	{
		if(term1.equals(term2))
			return 1;
		else
		{
			double intersectIC = 0;
			double unionIC = 0;
			
			for (String i: Main.getGO().getCommonAncestors(term1,term2))
				intersectIC += Main.getGO().getInfoContent(i, structural);
		
			for (String i: Main.getGO().getCombinedAncestors(term1,term2))
				unionIC += Main.getGO().getInfoContent(i, structural);
			
			return intersectIC / unionIC;
		}
	}

	
	public double getGeneSimilarity(String gene1, String gene2) 
	{
		if(gene1.equals(gene2))
			return 1;
		else
		{
			Set<String> set2 = Main.getGO().geneTerms.get(gene2);
			
			double intersection = 0;
			double union = 0;

			for (String i: Main.getGO().geneTerms.get(gene1))
			{
				double IC = Main.getGO().getInfoContent(i, structural);
				
				if(set2.contains(i))
					intersection += IC;
			
				else
					union += IC;
			}

			for (String i: set2)
			{
				union += Main.getGO().getInfoContent(i, structural);
			}
			
			return intersection / union;
		}
	}
	
	
}
