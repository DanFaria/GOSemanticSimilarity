package metrics;

import main.Main;

public class Lin implements TermSimilarityMetric 
{
	private boolean structural;

	public Lin(boolean structural)
	{
		this.structural = structural;
	}

	public double getTermSimilarity(String term1, String term2)
	{
		if(term1.equals(term2))
			return 1.0;
		else
			return 2*Main.getGO().getInfoContent(Main.getGO().getMICA(term1,term2), structural) /
				Main.getGO().getInfoContent(term1,structural) + Main.getGO().getInfoContent(term2,structural);
	}

}
