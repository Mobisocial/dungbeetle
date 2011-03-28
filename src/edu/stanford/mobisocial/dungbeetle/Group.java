package edu.stanford.mobisocial.dungbeetle;

import java.util.ArrayList;

public class Group {
    
    private String groupName;
    private ArrayList<Friend> groupMembers;
    
    public Group(String groupName)
    {
    	this.groupName = groupName;
    	groupMembers = new ArrayList<Friend>();
    }
    public String getGroupName() {
    	return groupName;
    }
    
    public void setGroupName(String groupName) {
    	this.groupName = groupName;
    }
    
    public void addMember(Friend friend)
    {
    	groupMembers.add(friend);
    }
    
    public ArrayList<Friend> getGroupMembers()
    {
    	return groupMembers;
    }
    
    public int getSize()
    {
    	return groupMembers.size();
    }
    
}