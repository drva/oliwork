//args[0] is the directory of the course you want to do this for
//args[1] is redact for leaving out tags I've processed; all (or anything else) for leaving them 
//args[2] is nc for leaving out things inside comments, anything else for keeping them

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
	public static Boolean redact=false;
	public static String tagRegex="<(?<tagname>[^\\s\\/>]+)[\\S\\s]*?>";
	public static String xblockRegex="(?<toprint>[\\S\\s]*?xblock[\\S]*)[\\s]*[\\S\\s]*"; //toprint should thus contain everything before the line says xblock, and then anything through the next whitespace after
	public static ArrayList<String> xblockSearch = new ArrayList<String>(); //an ArrayList for xblock search stuff
	public static String processedTags="ProcessedTags"; //directory where lists of these are, currently hardcoding.
	public static String oneLineCommentRegex="<!--[\\S\\s]*?-->";
	public static String beginCommentRegex="<!--";
	public static String endCommentRegex="-->";
	public static Boolean removeComments=false;
	
	public static void main(String[] args) throws IOException
	{
		if(args[1].equals("redact"))
			redact=true;
		if(args[2].equals("nc"))
			removeComments=true;	
		
		
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
		
		//if want to leave out tags I've already processed
		if(redact)
		{	
			//ArrayLists will hold the list of tags to leave out. 
			ArrayList<String> contentProcessed = new ArrayList<String>();
			ArrayList<String> problemProcessed = new ArrayList<String>();
			ArrayList<String> courseProcessed = new ArrayList<String>();
			ArrayList<String> chapterProcessed = new ArrayList<String>();
			ArrayList<String> seqProcessed = new ArrayList<String>();
			ArrayList<String> verticalProcessed = new ArrayList<String>();
			
			//get the list of tags to leave out from their files.
			getProcessedTags(contentProcessed, "content");
			getProcessedTags(problemProcessed, "problem");
			getProcessedTags(courseProcessed, "course");
			getProcessedTags(chapterProcessed, "chapter");
			getProcessedTags(seqProcessed, "sequential");
			getProcessedTags(verticalProcessed, "vertical");
			
			//for testing the process before I actually write the code to read this in from files.
			/*contentProcessed.add("p");
			problemProcessed.add("numericalresponse");
			problemProcessed.add("demandhint");
			courseProcessed.add("chapter");
			seqProcessed.add("vertical");*/
			
			redactProcessed(contentTags, contentProcessed);
			redactProcessed(problemTags, problemProcessed);
			redactProcessed(courseTags, courseProcessed);
			redactProcessed(chapterTags, chapterProcessed);
			redactProcessed(seqTags, seqProcessed);
			redactProcessed(verticalTags, verticalProcessed);
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
		Boolean insideCommentIgnore=false;
		
		//go through the file looking for tags and xblock stuff.
		while(fromAFile.hasNext())
		{
			hold=fromAFile.nextLine();
			
			//if we're leaving out things inside comments, take care of that.
			if(removeComments)
			{
				//if there are comments contained entirely in this line, get rid of that part of the line, keeping everything else (I was worried this would cause problems with something like <tag<!--comment-->attribute> but actually that's not allowed)
				pattern=Pattern.compile(oneLineCommentRegex);
				matcher=pattern.matcher(hold);
				while(matcher.find())
				{
					//System.out.println("comment: "+hold); this and two lines below used for testing
					hold=hold.substring(0,matcher.start())+hold.substring(matcher.end());
					//System.out.println("removed: "+hold);	
				}
				
			}
			
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
	
	public static void redactProcessed(HashSet<String> tagSet, ArrayList<String> toRedact)
	{
		//tags that are in the toRedact list get removed from the taglist set.
		for(int i=0; i<toRedact.size(); i++)
			tagSet.remove(toRedact.get(i));
	}
	
	public static void getProcessedTags(ArrayList<String> tags, String filename) throws IOException
	{
		//get the file
		Scanner fromAFile = new Scanner(new File(processedTags+"/"+filename+".txt"));
		
		String hold="";
		while(fromAFile.hasNext())
		{
			hold=fromAFile.nextLine();
			tags.add(hold.split("\\s")[0]); //since tags can't have whitespace, splitting off anything after whitespace so the files can have notes
		}
	
		fromAFile.close();
	}
}