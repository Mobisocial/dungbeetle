package edu.stanford.mobisocial.dungbeetle;

public class Contact implements Comparable
{
	public String name;
	public String id;
	
	public Contact(String iId, String iName)
	{
		name = iName;
		id = iId;
	}
	

	@Override
	public int compareTo(Object another) {
		// TODO Auto-generated method stub
		return name.compareTo(((Contact)another).name);
	}
}