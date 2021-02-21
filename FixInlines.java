//args[0] is the directory with the files to go through. args[1] is the destination directory

import java.io.*;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class FixInlines
{
	public static String destinationDirectory;
	
	public static void main(String[] args) throws IOException
	{
		File[] directoryList = new File(args[0]).listFiles(); //https://stackoverflow.com/questions/4917326/how-to-iterate-over-the-files-of-a-certain-directory-in-java
		destinationDirectory = args[1];
		for(int i=0; i<directoryList.length; i++)
		{
			fixInlinesInFile(directoryList[i]);
		}
	}
	
	/*we want to find things that should be assessment links and change them to our proper assessment links,
	find submit and compares that are just in the files and move them to their own files.
	At the moment both those things are wrapped in codeblocks.*/
	public static void fixInlinesInFile(File inputFile) throws IOException
	{		
		PrintWriter toAFile = new PrintWriter(new File(destinationDirectory+"/"+inputFile.getName()));
		
		String codeblockBeginRegex = "\\s*<codeblock syntax=\"xml\">\\s*"; 
		String codeblockEndRegex = "\\s*<\\/codeblock>\\s*";
		String problemRegex = "\\s*&lt;problem url_name=\"(?<id>[a-z0-9]+?)\"\\/&gt;\\s*";
		String sandcBeginRegex = "\\s*&lt;submit-and-compare url_name=\"(?<id>[a-z0-9]+?)\"[\\s\\S]*";
		
		String holdOther = "";
		Boolean codeblockFlag = false; //marks being in the process of handling a codeblock
		Boolean openedSandC = false;
		
		PrintWriter sAndCFile = null; //to avoid compilation errors about not having been initialized
		Pattern pattern;
		Matcher matcher;

		Scanner fromAFile = new Scanner(inputFile);
		String hold="";
		while(fromAFile.hasNext())
		{
			hold=fromAFile.nextLine();
			//if I catch the opening of a codeblock, that means what comes next might be an assessment
			if(hold.matches(codeblockBeginRegex))
			{
				//there are other things I put in codeblocks aside from assessments, and if this is one of those I'll want to leave it as is, so saving how it should begin
				holdOther = hold;
				hold = fromAFile.nextLine();
				if(hold.matches("<!-- !!!vertical-->"))
				{
					holdOther = holdOther+"\n"+hold; 
					codeblockFlag = true;
				}
				else //this means this wasn't the kind of codeblock I'm interested in; put the lines in the new file regularly and don't work on it further
				{
					toAFile.println(holdOther);
					toAFile.println(hold);
				}		
			}
			else if(openedSandC)
			{
				if(hold.matches(codeblockEndRegex)) //if we get to the endmark, we close everything off
				{
					openedSandC = false; //end of the submit and compare
					sAndCFile.close();
					codeblockFlag = false; //end of working in codeblock
				}
				else //otherwise copy the s-and-c stuff into its file
					sAndCFile.println(hold.replaceAll("&lt;","<").replaceAll("&gt;",">"));
			}
			else if(hold.matches(codeblockEndRegex))
			{	
				if(codeblockFlag)
					codeblockFlag = false; //end of working in codeblock
				/*note: I think as written this program will currently end up stripping out anything that goes opening codeblock/vertical mark/closing codeblock, but that seems fine since those wouldn't be useful. (I don't think any exist, but)*/			
				else //if we weren't working on either an s-and-c (above) or an assessment, then this is the end of some other kind of codeblock and we keep it
					toAFile.println(hold);
			}
			else if(codeblockFlag)
			{
				if(hold.matches(problemRegex)) //make our format of assessment link
				{
					pattern = Pattern.compile(problemRegex);
					matcher = pattern.matcher(hold);
					matcher.matches();
					
					toAFile.println("<wb:inline idref=\""+"a_"+matcher.group("id")+"\"/>"); //not currently adding a purpose. Using the a_ in front of old id as id system I started in philanthropy
					System.out.println(matcher.group("id")+" - "+inputFile.getName()); //getting a list of the activities to process them
				}
				else if(hold.matches(sandcBeginRegex)) //mark beginning a submit and compare (since they're multiline); make new file for it; start writing to it
				{
					openedSandC = true;
					pattern = Pattern.compile(sandcBeginRegex);
					matcher = pattern.matcher(hold);
					matcher.matches();
					
					sAndCFile = new PrintWriter(new File(destinationDirectory+"/"+"a_"+matcher.group("id")+".xml")); //might want to change directory later
					
					sAndCFile.println(hold.replaceAll("&lt;","<").replaceAll("&gt;",">")); //note that I am *not* changing the id in here atm
					
					toAFile.println("<wb:inline idref=\""+"a_"+matcher.group("id")+"\"/> <!--submit and compare-->"); //not currently adding a purpose. Using the a_ in front of old id as id system I started in philanthropy

				}
				else //this is neither a linked assessment nor a submit and compare, so put back the codeblock beginning and go on as for any other line
				{
					toAFile.println(holdOther);
					toAFile.println(hold);
					codeblockFlag = false;
				}
			}
			//for everything else we're just copying the lines of the file over
			else
				toAFile.println(hold);
		}
		
		fromAFile.close();
		toAFile.close();
	}	
}