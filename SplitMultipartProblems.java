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
	public static String putSingletsHere = "ProblemsSinglePartToConvert";
	public static String putMultiplesHere = "ProblemsMultiPartToConvert";
	public static String problemPartBeginRegex="<(?<tagname>[^\\s\\/]+?response)[\\S\\s]*?>";
	public static String problemPartEndRegex="<\\/[^>]+?response>";
	public static String rootProblemBeginRegex="<problem[\\S\\s]*?>";
	public static Pattern pattern;
	public static Matcher matcher;
	public static HashSet<String> problemTypes;
	public static String numericTag = "numericalresponse";
	public static String fillInBlankTag = "optionresponse";
	
	public static void main(String[] args) throws IOException
	{
		destinationDirectory = args[1];
		//making folders for the singlets and the multiples
		File makeDirs = new File(destinationDirectory+"/"+putSingletsHere); //https://www.tutorialspoint.com/how-to-create-a-new-directory-by-using-file-object-in-java
		makeDirs.mkdir();
		makeDirs = new File(destinationDirectory+"/"+putMultiplesHere);
		makeDirs.mkdir();
		
		//want this program to output the kind of problems this problem collection has, and using the set for that.
		problemTypes = new HashSet<String>();
		
		File[] directoryList = new File(args[0]).listFiles(); //https://stackoverflow.com/questions/4917326/how-to-iterate-over-the-files-of-a-certain-directory-in-java
		//go through all the problem files
		for(int i=0; i<directoryList.length; i++)
		{
			processProblemFile(directoryList[i]);
		}
		
		System.out.println("Question types present:");
		problemTypes.forEach((String name) -> {
            System.out.println(name);
        }); //https://zetcode.com/java/foreach/
	
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
		//numerics and optionresponses/fill in the blanks with multiple parts are a different sort of case and will get processed with singlets. So need to catch them.
		//if I run into multipart text response will need to add here.
		//(note, if something is supposed to be a multipart numeric and then have other question types it'll be still taken as a general multipart. Leaving it that way, hopefully any issues will be back-up handled by content tag wrapping now present in the xslt.) 
		boolean allNumeric = true;
		boolean allOption = true;
		
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
			
			//this this is not a numeric then they're not all numerics; if it's not an optionresponse then they're not all that.
			if(!matcher.group("tagname").equals(numericTag))
				allNumeric = false;
			if(!matcher.group("tagname").equals(fillInBlankTag))
				allOption = false;
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
		else if(numbegins==1) //so this is not a multipart problem, so just copy file over. 
		{
			PrintWriter toAFile = new PrintWriter(new File(destinationDirectory+"/"+putSingletsHere+"/"+filename));

			toAFile.print(fileText);
			toAFile.close();
		}
		else if(numbegins>1 && (allNumeric || allOption)) //All-numerics and all-optionresponses also go with singlets.
		{
			PrintWriter toAFile = new PrintWriter(new File(destinationDirectory+"/"+putSingletsHere+"/"+filename));
			toAFile.print(fileText);
			toAFile.close();
			//want to note all-numerics and all-optionresponses
			if(allNumeric)
				System.out.println("Multi-part numeric: " +filename);
			if(allOption)
				System.out.println("Multi-part fill in the blank: " +filename);
		}
		else if(numbegins>1) //this is a multipart, so we want to use q tags to separate the multiple parts
		{
			//dividing the last part off from the others
			String fileTextP1 = fileText.substring(0, startOfLastBegin);
			String fileTextP2 = fileText.substring(startOfLastBegin);
			
			//want an opening q tag after the opening root problem tag
			pattern = Pattern.compile(rootProblemBeginRegex);
			matcher = pattern.matcher(fileTextP1);
			fileTextP1 = matcher.replaceFirst("$0\n<q>");
			
			//for all parts except the last one, we want to close off the previous q and open a new one
			pattern = Pattern.compile(problemPartEndRegex);
			matcher = pattern.matcher(fileTextP1);
			fileTextP1 = matcher.replaceAll("$0\n</q>\n<q>");
			
			//for the last part, we just want a closing q tag
			matcher = pattern.matcher(fileTextP2);
			fileTextP2 = matcher.replaceAll("$0\n</q>");
			
			//write the results into the output file
			PrintWriter toAFile = new PrintWriter(new File(destinationDirectory+"/"+putMultiplesHere+"/"+filename));
			toAFile.print(fileTextP1+fileTextP2);
			toAFile.close();
		}
		else
		{
			System.out.println("!No question tags: "+filename);
		}
		
		
		fromAFile.close();					
	}
}