package edu.stanford.mobisocial.dungbeetle.feed.iface;

public class FeedView {
    final String mName;
    final Class<?> mClassName;

    public String getName() {
        return mName;
    }

	public Class<?> getClassName() {
	    return mClassName;
	}

	public FeedView(String name, Class<?> className) {
	    mName = name;
	    mClassName = className;
	}
}
