package metrics;

import main.Main;

public class Lin implements TermSimilarityMetric 
{
	public double getTermSimilarity(String term1, String term2)
	{
		if(term1.equals(term2))
			return 1.0;
		else
			return 2*Main.getGO().getInfoContent(Main.getGO().getMICA(term1,term2)) /
				Main.getGO().getInfoContent(term1) + Main.getGO().getInfoContent(term2);
	}

}
