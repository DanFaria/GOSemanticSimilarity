package metrics;

import main.Main;

public class Maximum implements GeneSimilarityMetric 
{
	private TermSimilarityMetric measure;
	
	public Maximum(TermSimilarityMetric measure)
	{
		this.measure = measure;
	}
	
	public double getGeneSimilarity(String gene1, String gene2) 
	{
		if(gene1.equals(gene2))
			return 1;
		else
		{
			double max= 0;

			for (String t1: Main.getGO().getNonRedundantTerms(gene1))
			{				
				for (String t2:  Main.getGO().getNonRedundantTerms(gene2))
				{
					double score = measure.getTermSimilarity(t1, t2);
					if (max > score)
					{
						max = score;
					}
				}

			}
			return max;
		}
	}

}
