package metrics;

import java.util.HashSet;

import main.Main;

public class SimUI implements TermSimilarityMetric, GeneSimilarityMetric 
{
	public double getTermSimilarity(String term1, String term2)
	{
		if(term1.equals(term2))
			return 1;
		else
			return Main.getGO().getCommonAncestors(term1,term2).size() * 1.0 / Main.getGO().getCombinedAncestors(term1,term2).size();		
	}
	
	public double getGeneSimilarity(String gene1, String gene2) 
	{
		if(gene1.equals(gene2))
			return 1;
		else
		{
			HashSet<String> commonTerms	= new HashSet<String>(Main.getGO().getCommonTerms(gene1,gene2));

			return commonTerms.size() * 1.0 / (Main.getGO().geneTerms.get(gene1).size()+ Main.getGO().geneTerms.get(gene2).size()- commonTerms.size());
		}
	}

}
