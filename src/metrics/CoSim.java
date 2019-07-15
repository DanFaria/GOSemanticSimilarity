package metrics;

import java.util.HashMap;
import java.util.Set;
import java.util.Vector;
import main.Main;
import ontology.GOType;

public class CoSim implements TermSimilarityMetric, GeneSimilarityMetric, GeneSet
{
	HashMap<String, Integer> count1 = new HashMap<String, Integer>();
	HashMap<String, Integer> count2 = new HashMap<String, Integer>();
	
	public double getTermSimilarity(String term1, String term2)
	{
				if(term1.equals(term2))
			return 1;
		else
		{
			double intersection = 0;
			double union = 0;
			
			if (Main.getGO().getCommonAncestors(term1,term2).isEmpty())
				return 0.0;
			for (String i: Main.getGO().getCommonAncestors(term1,term2))
				intersection += Math.pow(Main.getGO().getInfoContent(i), 2.0);
			for (String i: Main.getGO().getCombinedAncestors(term1,term2))
				union += Math.pow(Main.getGO().getInfoContent(i), 2.0);
		
			return intersection / union;
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

			for (String i: set1)
			{
				double IC = Math.pow(Main.getGO().getInfoContent(i), 2.0);
				
				if(set2.contains(i))
					intersection += IC;

				else
					union += IC;
			}

			for (String i: set2)
			{
				union += Math.pow(Main.getGO().getInfoContent(i), 2.0);
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
			double IC = Main.getGO().getInfoContent(s);
			if (count2.containsKey(s))
			{
				intersection += Math.pow(Math.min(count1.get(s),count2.get(s))* IC, 2.0);
				union += Math.pow(Math.max(count1.get(s),count2.get(s))* IC, 2.0);
				count2.remove(s);
			}
			else union += Math.pow(count1.get(s)* IC, 2.0);
		}
		for (String s: count2.keySet())
		{
			union += Math.pow(count2.get(s)* Main.getGO().getInfoContent(s),2.0);
		}
		return intersection / union;
	}
}