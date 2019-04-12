package metrics;

import main.Main;

public class Resnik implements TermSimilarityMetric
{
	private boolean structural;
	
	public Resnik(boolean structural)
	{
		this.structural = structural;
	}
	
	public double getTermSimilarity(String term1, String term2)
	{
		if(term1.equals(term2))
			return 1.0;
		else 
			return Main.getGO().getInfoContent(Main.getGO().getMICA(term1,term2), structural);
	}
	
}
