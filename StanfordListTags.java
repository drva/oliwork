//args[0] is the directory of the course you want to do this for

import java.io.*;
import java.util.Scanner;
import java.util.HashSet;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.ArrayList;

public class StanfordListTags
{
	public static Pattern pattern;
	public static Matcher matcher;
	public static String tagRegex="<(?<tagname>[^\\s\\/>]+)[\\S\\s]*?>";
	public static String xblockRegex="(?<toprint>[\\S\\s]*?xblock[\\S]*)[\\s]*[\\S\\s]*"; //toprint should thus contain everything before the line says xblock, and then anything through the next whitespace after
	public static ArrayList<String> xblockSearch = new ArrayList<String>(); //an ArrayList for xblock search stuff
	
	public static void main(String[] args) throws IOException
	{
		//hash set will be used to keep track of the tags encountered
		HashSet<String> contentTags = new HashSet<String>();
		HashSet<String> problemTags = new HashSet<String>();
		
		HashSet<String> courseTags = new HashSet<String>();
		HashSet<String> chapterTags = new HashSet<String>();
		HashSet<String> seqTags = new HashSet<String>();
		HashSet<String> verticalTags = new HashSet<String>();
		
		
		//get the lists of files to process
		File[] contentFiles = new File(args[0]+"/html").listFiles(); //https://stackoverflow.com/questions/4917326/how-to-iterate-over-the-files-of-a-certain-directory-in-java
		File[] problemFiles = new File(args[0]+"/problem").listFiles();
		
		File[] courseFiles = new File(args[0]+"/course").listFiles();
		File[] chapterFiles = new File(args[0]+"/chapter").listFiles();
		File[] seqFiles = new File(args[0]+"/sequential").listFiles();
		File[] verticalFiles = new File(args[0]+"/vertical").listFiles();
		
		//send them to process.
		for(int i=0; i<contentFiles.length; i++)
		{
			processFile(contentFiles[i], contentTags); //by passing the hashset I can make one method and use it for both content and problems
		}
		for(int i=0; i<problemFiles.length; i++)
		{
			processFile(problemFiles[i], problemTags); //by passing the hashset I can make one method and use it for both content and problems
		}
		
		for(int i=0; i<courseFiles.length; i++)
		{
			processFile(courseFiles[i], courseTags); 
		}
		for(int i=0; i<chapterFiles.length; i++)
		{
			processFile(chapterFiles[i], chapterTags); 
		}
		for(int i=0; i<seqFiles.length; i++)
		{
			processFile(seqFiles[i], seqTags); 
		}
		for(int i=0; i<verticalFiles.length; i++)
		{
			processFile(verticalFiles[i], verticalTags); 
		}
		
		
		
		System.out.println("## Higher structure tags:");
		System.out.println("### -course-");
		courseTags.forEach((String name) -> {
            System.out.println(name);
        });
		System.out.println("### -chapter-");
		chapterTags.forEach((String name) -> {
            System.out.println(name);
        });
		System.out.println("### -sequential-");
		seqTags.forEach((String name) -> {
            System.out.println(name);
        });
		System.out.println("### -vertical-");
		verticalTags.forEach((String name) -> {
            System.out.println(name);
        });
		
		System.out.println();
		System.out.println("## Content tags:");
		contentTags.forEach((String name) -> {
            System.out.println(name);
        }); //https://zetcode.com/java/foreach/ originally used in SplitMultipartProblems
        System.out.println();
        System.out.println("## Problem tags:");
		problemTags.forEach((String name) -> {
            System.out.println(name);
        });
        
        System.out.println();
        System.out.println("## xblock potential");
        for(int i=0; i<xblockSearch.size(); i++)
        	System.out.println(xblockSearch.get(i));
	}
	
	public static void processFile(File inputFile, HashSet<String> tagSet) throws IOException
	{
		Scanner fromAFile = new Scanner(inputFile);
		String hold="";
		
		//go through the file looking for tags and xblock stuff.
		while(fromAFile.hasNext())
		{
			hold=fromAFile.nextLine();
			pattern = Pattern.compile(tagRegex);
			matcher = pattern.matcher(hold);
						
			while(matcher.find())
			{
				tagSet.add(matcher.group("tagname"));
			}
			
			//at the moment for xblock just searching one per line
			if(hold.matches(xblockRegex))
			{
				pattern=Pattern.compile(xblockRegex);
				matcher = pattern.matcher(hold);
				matcher.matches();
				xblockSearch.add(matcher.group("toprint"));
			}
		}
 
 		fromAFile.close();	
	}
}