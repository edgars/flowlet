package org.jboss.flowlet.rest.framework;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class TextFileUtil {
	
	/**
     * Read the file into a String.
     * @param file - the file to be read
     * @return String with the content of the file
     * @throws IOException - when we can't read the file
     */
    public static String readTextFile(String file) throws IOException 
    { 
    	File rFile = new File(file);
        StringBuffer sb = new StringBuffer(1024);
        BufferedReader reader = new BufferedReader(new FileReader(rFile.getPath()));
        
        char[] chars = new char[1];
        try{
        while( (reader.read(chars)) > -1){
            sb.append(String.valueOf(chars)); 
            chars = new char[1];
        }}finally{
        reader.close();
        }
        return sb.toString();
    }

}
