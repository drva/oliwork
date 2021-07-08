//args[0] is the directory of the course you want to do this for

import java.io.*;
import java.util.Scanner;
import java.util.HashSet;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class StanfordListTags
{
	public static Pattern pattern;
	public static Matcher matcher;
	public static String tagRegex="<(?<tagname>[^\\s\\/>]+)[\\S\\s]*?>";
	
	public static void main(String[] args) throws IOException
	{
		//hash set will be used to keep track of the tags encountered
		HashSet<String> contentTags = new HashSet<String>();
		HashSet<String> problemTags = new HashSet<String>();
		
		//get the lists of files to process
		File[] contentFiles = new File(args[0]+"/html").listFiles(); //https://stackoverflow.com/questions/4917326/how-to-iterate-over-the-files-of-a-certain-directory-in-java
		File[] problemFiles = new File(args[0]+"/problem").listFiles();
		
		//send them to process.
		for(int i=0; i<contentFiles.length; i++)
		{
			processFile(contentFiles[i], contentTags); //by passing the hashset I can make one method and use it for both content and problems
		}
		for(int i=0; i<problemFiles.length; i++)
		{
			processFile(problemFiles[i], problemTags); //by passing the hashset I can make one method and use it for both content and problems
		}
		
		System.out.println("## Content tags:");
		contentTags.forEach((String name) -> {
            System.out.println(name);
        }); //https://zetcode.com/java/foreach/ originally used in SplitMultipartProblems
        System.out.println();
        System.out.println("## Problem tags:");
		problemTags.forEach((String name) -> {
            System.out.println(name);
        });
	}
	
	public static void processFile(File inputFile, HashSet<String> tagSet) throws IOException
	{
		Scanner fromAFile = new Scanner(inputFile);
		String hold="";
		
		//go through the file looking for tags.
		while(fromAFile.hasNext())
		{
			hold=fromAFile.nextLine();
			pattern = Pattern.compile(tagRegex);
			matcher = pattern.matcher(hold);
						
			while(matcher.find())
			{
				tagSet.add(matcher.group("tagname"));
			}
		}
 
 		fromAFile.close();	
	}
}