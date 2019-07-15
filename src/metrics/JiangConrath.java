package metrics;

import main.Main;

public class JiangConrath implements TermSimilarityMetric
{
	public double getTermSimilarity(String term1, String term2) 
	{
		if(term1.equals(term2))
			return 1.0;
		else
		{
			double ic1 = Main.getGO().getInfoContent(term1);
			double ic2 = Main.getGO().getInfoContent(term2);
			double mica = Main.getGO().getInfoContent(Main.getGO().getMICA(term1,term2));

			if (ic1 == 0 && ic2 == 0 && mica == 0)
			{
				return 0.0;
			}
			
			return 1-(ic1 + ic2-2*mica);
		}
	}
}

