//args[0] should be where the problems are
//args[1] should be the destination directory

import java.io.*;
import java.util.Scanner;
import java.util.HashSet;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class SplitMultipartProblems
{
	public static String destinationDirectory="";
	public static String problemPartBeginRegex="<(?<tagname>[^\\s\\/]+?response)[\\S\\s]*?>";
	public static String problemPartEndRegex="<\\/[^>]+?response>";
	public static Pattern pattern;
	public static Matcher matcher;
	public static HashSet<String> problemTypes;
	
	public static void main(String[] args) throws IOException
	{
		destinationDirectory = args[1];
		//want this program to output the kind of problems this problem collection has, and using a set for that.
		problemTypes = new HashSet();
		
		File[] directoryList = new File(args[0]).listFiles(); //https://stackoverflow.com/questions/4917326/how-to-iterate-over-the-files-of-a-certain-directory-in-java
		//go through all the problem files
		for(int i=0; i<directoryList.length; i++)
		{
			processProblemFile(directoryList[i]);
		}
	
	}
	
	public static void processProblemFile(File inputFile) throws IOException
	{
		String filename = inputFile.getName().split("/")[inputFile.getName().split("/").length-1];
		String fileText=""; //since I'm going to need to find and replace in it a specific way, reading the whole file into a string.
		Scanner fromAFile = new Scanner(inputFile);
		boolean multipart=false;
		int numbegins = 0;
		int numends = 0;
		int startOfLastBegin = 0;
		
		while(fromAFile.hasNext())
			fileText=fileText+fromAFile.nextLine()+"\n";
			
		pattern = Pattern.compile(problemPartBeginRegex);
		matcher = pattern.matcher(fileText);
		
		//go through the file text, count problem type opening tags, add their names to the set, track the start of the last one
		while(matcher.find())
		{
			numbegins++;
			startOfLastBegin = matcher.start(); //this should end with it being the start of the last opening tag
			problemTypes.add(matcher.group("tagname"));
		}
		
		pattern = Pattern.compile(problemPartEndRegex);
		matcher = pattern.matcher(fileText);
		
		//go through the file text, count problem type closing tags
		while(matcher.find())
		{
			numends++;
		}
		
		if(numends != numbegins)
		{
			System.out.println("!Tag number mismatch: "+filename);
		}					
	}
}