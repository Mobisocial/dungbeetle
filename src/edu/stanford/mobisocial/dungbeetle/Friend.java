package edu.stanford.mobisocial.dungbeetle;

public class Friend {
    
    private String userName, id, photoId;
    
    public Friend(String userName) {
    	this.userName = userName;
    }
    
    public Friend(String userName, String id, String photoId) {
    	this.userName = userName;
    	this.id = id;
    	this.photoId = photoId;
    }
    
    public String getUserName() {
    	return userName;
    }
    public void setUserName(String userName) {
    	this.userName = userName;
    }
    
    public String getId() {
    	return id;
    }
}