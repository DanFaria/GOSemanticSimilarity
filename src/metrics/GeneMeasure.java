package metrics;

public enum GeneMeasure 
{
	SIM_UI ("simUI"),
	SIM_GIC ("simGIC"),
	COSIM ("CoSim"),
	MAXIMUM ("Maximum"),
	BEST_MATCH_AVERAGE ("BestMatchAverage");
	
	private String label;

	//Constructors
	private GeneMeasure(String label)
	{	
		this.label = label;
	}
	
	//Methods
	public static GeneMeasure parse(String s)
	{
		for(GeneMeasure m : GeneMeasure.values())
		{
			if(s.equalsIgnoreCase(m.label))
			return m;	
		}
		
		return null;		
	}
	
	public String toString()
	{
		return label;
	}
	
}

