package metrics;

import java.util.HashMap;
import java.util.Set;
import java.util.Vector;
import main.Main;
import ontology.GOType;

public class SimGIC implements TermSimilarityMetric, GeneSimilarityMetric, GeneSet
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
			double intersection = 0.0;
			double union = 0.0;

			//computes intersection IC
			for (String i: set1)
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

	public double getSetSimilarity(Vector<String> geneSet1, Vector<String> geneSet2)
	{
		HashMap<String, Integer> count1 = Main.getGO().termCountMap(geneSet1, Main.getType());

		if(geneSet1.equals(geneSet2))
			return 1.0;
	
		HashMap<String, Integer> count2 = Main.getGO().termCountMap(geneSet2, Main.getType());
		double intersection = 0.0;
		double union = 0.0;

		for (String s: count1.keySet())
		{
			if (count2.containsKey(s))
			{
				intersection += Math.min(count1.get(s),count2.get(s))* Main.getGO().getInfoContent(s);
				union += Math.max(count1.get(s),count2.get(s))* Main.getGO().getInfoContent(s);
				count2.remove(s);
			}
			else union += count1.get(s)* Main.getGO().getInfoContent(s);
		}
		for (String s: count2.keySet())
		{
			union += count2.get(s)* Main.getGO().getInfoContent(s);
		}
		return intersection/union;
	}
}