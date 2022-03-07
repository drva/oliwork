//args[0] is directory with files to make placeholder versions of 
//args[1] is destination directory

import java.io.*;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class MakePlaceholderPages
{
	public static String destinationDirectory;
	
	public static void main(String[] args) throws IOException
	{
		//source and destination
		File[] fileList = new File(args[0]).listFiles(); //from ConvertSRTs, there noted: https://stackoverflow.com/questions/4917326/how-to-iterate-over-the-files-of-a-certain-directory-in-java
		destinationDirectory = args[1];
		
		//go through the files I want to make placeholders of, and make one for each
		for(int i=0; i<fileList.length; i++)
			makePlaceholderPage(fileList[i]);
		
		
	
	}
	
	public static void makePlaceholderPage(File input) throws IOException
	{
		Scanner fromFile = new Scanner(input);
		String fileName = input.getName().split("/")[input.getName().split("/").length-1]; //this should include the .xml, but not the directories
		String fileID = fileName.substring(0, fileName.length()-4); //this should not include the .xml
		
		PrintWriter toPlaceholderFile = new PrintWriter(new File(destinationDirectory+"/"+fileName)); //placeholder page has same filename as the original
		//workbook page header. Title is the page part of the file id - this should be sufficiently clear but also differentiated from the actual page name, for easier recognition in the outline
		toPlaceholderFile.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"+
								"<!DOCTYPE workbook_page\n"+
								"\tPUBLIC \"-//Carnegie Mellon University//DTD Workbook Page 3.8//EN\"\n"+
     							"\t\"http://oli.web.cmu.edu/dtd/oli_workbook_page_3_8.dtd\">\n"+
								"<workbook_page xmlns:wb=\"http://oli.web.cmu.edu/activity/workbook/\" id=\""+fileID+"\">");
					//head and opening body
		toPlaceholderFile.println("\t<head>\n"+
								"\t\t<title>"+fileID.split("-p-")[fileID.split("-p-").length-1]+"</title>\n"+
								"\t</head>\n"+
								"\t<body>");
								
		toPlaceholderFile.println("\t\t<p>This is a placeholder for a page currently in the process of conversion.</p>");						
		
		//putting assessment links if any into the page
		String assessmentLink="&lt;problem\\surl_name=\"(?<aid>[a-z0-9]+)\"/&gt;";
		Pattern pattern = Pattern.compile(assessmentLink);
		Matcher matcher;
		String hold = "";
		
		while(fromFile.hasNext())
		{
			hold=fromFile.nextLine();
			if(hold.trim().matches(assessmentLink))
			{
				matcher = pattern.matcher(hold.trim());
				matcher.matches();
				toPlaceholderFile.println("\t\t<wb:inline idref=\"a_"+matcher.group("aid")+"\"/>");
			}
		}	
								
		//closing off
		fromFile.close();
		toPlaceholderFile.println("\t</body>\n"+
									"</workbook_page>");
		toPlaceholderFile.close();
	} 
}