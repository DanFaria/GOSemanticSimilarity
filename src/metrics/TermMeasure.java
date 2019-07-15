package metrics;

public enum TermMeasure 
{
	RESNIK ("Resnik"),
	LIN ("Lin"),
	JIANG_CONRATH("JiangConrath"),
	COSIM ("CoSim"),
	SIMUI ("SimUI"),
	SIMGIC ("SimGIC"),
	PEKAR_STAAB ("PekarStaab");
	
	private String label;

	//Constructors
	private TermMeasure(String label)
	{	
		this.label = label;
	}
	
	//Methods
	public static TermMeasure parse(String s)
	{
		for(TermMeasure m : TermMeasure.values())
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

