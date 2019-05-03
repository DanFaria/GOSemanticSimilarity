package metrics;

import java.util.HashSet;

import main.Main;

public class PekarStaab implements TermSimilarityMetric 
{
	
	public double getTermSimilarity(String term1, String term2) 
	{	
		if(term1.equals(term2))
			return 1.0;
		
		String root1 = Main.getGO().getRoot(Main.getGO().getType(term1));
		String root2 = Main.getGO().getRoot(Main.getGO().getType(term1));
		boolean useAllRelations = Main.useAllRelations();
		
		if(!useAllRelations && !root1.equals(root2))
			return 0.0;		
		
		String lowestCommonAncestor = null;
		int lowestDistance = 0;
		
		HashSet<String> commonAncestors = Main.getGO().getCommonAncestors(term1,term2);
		
		if(commonAncestors.contains(term1))
		{
			lowestDistance = Main.getGO().getMaxDistance(term2, term1);
			lowestCommonAncestor = term1;
		}
		else if(commonAncestors.contains(term2))
		{
			lowestDistance = Main.getGO().getMaxDistance(term1, term2);
			lowestCommonAncestor = term2;
		}
		else
		{
			for(String i: commonAncestors)
			{

				int distance = Main.getGO().getMaxDistance(term1,i) + Main.getGO().getMaxDistance(term2,i);

				if (lowestDistance < distance)
				{
					lowestDistance = distance;
					lowestCommonAncestor = i;
				}
			}
		}
		String root = Main.getGO().getRoot(Main.getGO().getType(lowestCommonAncestor));
		int rootDistance = Main.getGO().getMaxDistance(lowestCommonAncestor, root);
		if(useAllRelations)
			rootDistance++;
		return  rootDistance * 1.0 / (rootDistance + lowestDistance);
	}

}
