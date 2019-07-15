package metrics;

import java.util.Set;

import main.Main;
import ontology.GOType;

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
			double max= 0.0;
			Set<String> set1;
			Set<String> set2;
			GOType t = Main.getType();
			if(t == null)
			{
				set1 = Main.getGO().getAnnotationsGene(gene1);
				set2 = Main.getGO().getAnnotationsGene(gene2);
			}
			else
			{
				set1 = Main.getGO().getAnnotationsGene(gene1,t);
				set2 = Main.getGO().getAnnotationsGene(gene2,t);
			}

			for (String t1: set1)
			{				
				for (String t2:  set2)
				{
					double score = measure.getTermSimilarity(t1, t2);

					if (max < score)
						max = score;
				}
			}
			return max;
		}
	}

}
