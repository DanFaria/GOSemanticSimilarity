package metrics;

public enum TermMeasure 
{
	RESNIK ("Resnik"),
	LIN ("Lin"),
	JIANG_CONRATH("Jiang Conrath"),
	PEKAR_STAAB ("Pekar Staab");
	
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
			if(s.equals(m.label))
			return m;	
		}
		
		return null;		
	}
	
	
	public String toString()
	{
		return label;
	}
	
}

