package edu.stanford.mobisocial.dungbeetle.google;


public class Constants {

	public static final String CONSUMER_KEY 	= "anonymous";
	public static final String CONSUMER_SECRET 	= "anonymous";

	public static final String SCOPE 			= "https://www.googleapis.com/auth/webfinger";

	public static final String REQUEST_URL 		= "https://www.google.com/accounts/OAuthGetRequestToken";
	public static final String ACCESS_URL 		= "https://www.google.com/accounts/OAuthGetAccessToken";  
	public static final String AUTHORIZE_URL 	= "https://www.google.com/accounts/OAuthAuthorizeToken";
	
	public static final String RESOURCE_URL 		= "https://www.googleapis.com/webfinger/v1/properties";
//	public static final String WEB_FINGER_BASE_URL = "http://www.google.com/s2/webfinger/?q=YOUR_EMAIL_ADDRESS@gmail.com";

	
	public static final String ENCODING 		= "UTF-8";
	
	public static final String	OAUTH_CALLBACK_SCHEME	= "x-oauthflow";
	public static final String	OAUTH_CALLBACK_HOST		= "callback";
	public static final String	OAUTH_CALLBACK_URL		= OAUTH_CALLBACK_SCHEME + "://" + OAUTH_CALLBACK_HOST;

}
