package edu.stanford.mobisocial.dungbeetle.util;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public abstract class StringSearchAndReplacer{

    private final Pattern pattern;

    public StringSearchAndReplacer(String p){
        pattern = Pattern.compile(p);
    }

    public String apply(String input){
        Matcher m = pattern.matcher(input);  
        StringBuffer sb = new StringBuffer();  
        while (m.find()){  
            m.appendReplacement(sb, "");  
            sb.append(replace(m));  
        }  
        m.appendTail(sb);  
        return sb.toString();
    }

    abstract protected String replace(Matcher m);

}