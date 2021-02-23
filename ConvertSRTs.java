//args[0] should be where the srt's are
//args[1] should be the destination directory

import java.io.*;
import java.util.Scanner;

public class ConvertSRTs
{		
	public static String destinationDirectory="";
	public static String srtFileCheck="[\\s\\S]+\\.srt";
	public static String hasAnyLanguageLetter="[\\S\\s]*?\\p{L}[\\S\\s]*"; //https://stackoverflow.com/questions/3617797/regex-to-match-only-letters?rq=1
	public static String transcriptPrefix = "transcript";
	
	public static void main(String[] args) throws IOException
	{
		destinationDirectory = args[1];
		
		File[] directoryList = new File(args[0]).listFiles(); //https://stackoverflow.com/questions/4917326/how-to-iterate-over-the-files-of-a-certain-directory-in-java
		//send specifically the srt files to be converted
		for(int i=0; i<directoryList.length; i++)
		{
			if(directoryList[i].getName().matches(srtFileCheck))
				convertSRT(directoryList[i]);
		}
	}
	
	public static void convertSRT(File srtFile) throws IOException
	{
		//get the file id part
		String fileID = srtFile.getName().split("static\\/|\\.srt")[srtFile.getName().split("static\\/|\\.srt").length-1];
		String pageID = transcriptPrefix+fileID;
		
		String fileText = "";
		String title = fileID;
		boolean titleGotten = false;
		String hold="";
		
		Scanner fromSRTFile = new Scanner(srtFile);
		//because I want a meaningful title and my best candidate is taking it from the file, reading the file into a holding string rather than directly into destination file.
		while(fromSRTFile.hasNext())
		{
			hold = fromSRTFile.nextLine();
			fileText = fileText+xmlifyContent(hold)+"\n";
			//we want the title to be the first content-containing line of the transcript, which I'm currently trying to identify by 'has at least one letter'. If this never happens the title will be the file id.
			if(!titleGotten)
			{
				if(hold.matches(hasAnyLanguageLetter))
				{
					title=xmlifyContent(hold)+"...";
					titleGotten=true;
				}
			}						
		}
		
		//making a workbook page
			//making the our-version file id etc will need to get a make NC name addition if that becomes a problem
		PrintWriter toConvFile = new PrintWriter(new File(destinationDirectory+"/"+pageID+".xml"));
			//file header
		toConvFile.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"+
								"<!DOCTYPE workbook_page\n"+
								"\tPUBLIC \"-//Carnegie Mellon University//DTD Workbook Page 3.8//EN\"\n"+
     							"\t\"http://oli.web.cmu.edu/dtd/oli_workbook_page_3_8.dtd\">\n"+
								"<workbook_page id=\""+pageID+"\">");
			//head and opening body
		toConvFile.println("\t<head>\n"+
								"\t\t<title>"+title+"</title>\n"+
								"\t</head>\n"+
								"\t<body>");
			//putting in a codeblock of text format since that matches the right format
		toConvFile.println("\t\t<codeblock syntax=\"text\" number=\"false\">");
		toConvFile.print(fileText);
			//closing everything
		toConvFile.print("\t\t</codeblock>\n"+
							"\t</body>\n"+
							"</workbook_page>");
			
		fromSRTFile.close();
		toConvFile.close();
	}
	
	//this might cause a problem with special characters? Not sure how that works in srt files...
	public static String xmlifyContent(String fixCharacters) //from StanfordConvertXML, more modified
	{
		fixCharacters = fixCharacters.replaceAll("&", "&amp;"); //goes first so it doesn't overwrite the others replacements after
 		//' and " don't need to be escaped in text (neither does > actually))
 		fixCharacters = fixCharacters.replaceAll("<", "&lt;");
 		
 		
 		return fixCharacters;
	}
	
}