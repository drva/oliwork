//args[0] is the Stanford course files directory, 
//args[1] is the rest of the address of the first file to process

import java.io.*;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class StanfordConvertXML
{	
	//so that I don't have to move this file into the Stanford course files directory, this will hopefully help the program find files
	public static String directoryPrefix;
	
	//will put the newly made workbook pages into a directory (eta: note, apparently I need to make this directory before running, the program won't do it it will error out)
	public static String directoryForPages = "pages";
	
	public static Pattern pattern;
	public static Matcher matcher;
	
	public static String regexNameLine = "<\\S+? display_name=\"(?<name>[\\s\\S]+?)\"[\\s\\S]*>";
	//by having begin and end I can use the same regexs for all the file types
	public static String regexLinkBegin = "<";
	public static String regexLinkEnd = " url_name=\"(?<fileId>[0-9a-z]+)\"/>";
	
	public static PrintWriter toAFile;

	public static void main(String[] args) throws IOException
	{
		directoryPrefix = args[0];
		
		//at the moment this is taking in a *unit* ('chapter') and going from there
		chapter(directoryPrefix+"/"+args[1]);
	
	}
	
	//chapters are units
	public static void chapter(String filename) throws IOException
	{
		String thisOne = "chapter";
		String nextOneDown = "sequential";
		String oliName = "unit";
		
		Scanner fromTextFile = new Scanner(new File(filename));
		
		String hold="";
		while(fromTextFile.hasNext())
		{
			hold = fromTextFile.nextLine().trim(); //also get rid of leading and trailing whitespace
			
			//if it's the name
			if(hold.matches(regexNameLine))
			{
				pattern = Pattern.compile(regexNameLine);
				matcher = pattern.matcher(hold);
				matcher.matches();
				
				//is there a way to do in java the thing I just learned in Python where you use (in python) a multiplication sign to print the same thing multiple times? That would be good for the tabs, so I could have a number input and have it tab that many. Better for 'and the next layer gets one more' and for synchronizing to end tags and in general less hardcoded.
				System.out.println("\t<"+oliName+" id=\""+makeId(matcher.group("name"))+"\">\n"+
									"\t\t<title>"+xmlifyTitleId(matcher.group("name"))+"</title>");
				
				continue;
			}
			
			//if it's a link
			if(hold.matches(regexLinkBegin+nextOneDown+regexLinkEnd))
			{
				pattern = Pattern.compile(regexLinkBegin+nextOneDown+regexLinkEnd);
				matcher = pattern.matcher(hold);
				matcher.matches();
				
				sequential(directoryPrefix+"/"+nextOneDown+"/"+matcher.group("fileId")+".xml");
				
				continue;
			}
						
			//if it's the ending tag (we can ignore it)
			if(hold.matches("</"+thisOne+">"))
			{
				continue;
			}
			
			//anything else
			System.out.println("!!!"+thisOne+": "+hold); 
			
		}
		System.out.println("\t</"+oliName+">");
		
		fromTextFile.close();
	}
	
	//sequentials are modules
	public static void sequential(String filename) throws IOException
	{
		String thisOne = "sequential";
		String nextOneDown = "vertical";
		String oliName = "module";
		
		Scanner fromTextFile = new Scanner(new File(filename));
		
		String hold="";
		while(fromTextFile.hasNext())
		{
			hold = fromTextFile.nextLine().trim(); //also get rid of leading and trailing whitespace
			
			//if it's the name
			if(hold.matches(regexNameLine))
			{
				pattern = Pattern.compile(regexNameLine);
				matcher = pattern.matcher(hold);
				matcher.matches();
				
				System.out.println("\t\t<"+oliName+" id=\""+makeId(matcher.group("name"))+"\">\n"+
									"\t\t\t<title>"+xmlifyTitleId(matcher.group("name"))+"</title>");
			

				continue;
			}
			
			//if it's a link
			if(hold.matches(regexLinkBegin+nextOneDown+regexLinkEnd))
			{
				pattern = Pattern.compile(regexLinkBegin+nextOneDown+regexLinkEnd);
				matcher = pattern.matcher(hold);
				matcher.matches();
				
				vertical(directoryPrefix+"/"+nextOneDown+"/"+matcher.group("fileId")+".xml");
			
				continue;
			}
						
			//if it's the ending tag (we can ignore it)
			if(hold.matches("</"+thisOne+">"))
			{
				continue;
			}
			
			//anything else
			System.out.println("!!!"+thisOne+": "+hold);
			
		}
		System.out.println("\t\t</"+oliName+">");
		
		fromTextFile.close();
	}
	
	//verticals are pages
	public static void vertical(String filename) throws IOException
	{
		String thisOne = "vertical";
		String nextOneDown = "html";
		String oliName = "page";
		
		Scanner fromTextFile = new Scanner(new File(filename));
		
		String hold="";
		while(fromTextFile.hasNext())
		{
			hold = fromTextFile.nextLine().trim(); //also get rid of leading and trailing whitespace
			
			//if it's the name
			if(hold.matches(regexNameLine))
			{
				pattern = Pattern.compile(regexNameLine);
				matcher = pattern.matcher(hold);
				matcher.matches();
				
				//saving them since I need them more than once in this one
				String pageID = makeId(matcher.group("name"));
				String pageTitle = xmlifyTitleId(matcher.group("name"));
				
				System.out.println("\t\t\t<item>\n"+
									"\t\t\t\t<resourceref idref=\""+pageID+"\"/>\n"+
									"\t\t\t</item>");
				
				//making a workbook page
				toAFile = new PrintWriter(new File(directoryForPages+"/"+pageID+".xml"));
					//file header
				toAFile.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"+
								"<!DOCTYPE workbook_page\n"+
								"\tPUBLIC \"-//Carnegie Mellon University//DTD Workbook Page 3.8//EN\"\n"+
     							"\t\"http://oli.web.cmu.edu/dtd/oli_workbook_page_3_8.dtd\">\n"+
								"<workbook_page id=\""+pageID+"\">");
					//head and opening body
				toAFile.println("\t<head>\n"+
								"\t\t<title>"+pageTitle+"</title>\n"+
								"\t</head>\n"+
								"\t<body>");
					//so there's a problem where learning objective stuff goes in the head and I don't have those yet to put there. Will handle that later.

				
				continue;
			}
			
			//if it's a link
			if(hold.matches(regexLinkBegin+nextOneDown+regexLinkEnd))
			{
				pattern = Pattern.compile(regexLinkBegin+nextOneDown+regexLinkEnd);
				matcher = pattern.matcher(hold);
				matcher.matches();
				
				//this one doesn't have the .xml ending like the others since htmls have both an xml and an html file
				html(directoryPrefix+"/"+nextOneDown+"/"+matcher.group("fileId"));
			
				continue;
			}
			
			//if it's the ending tag (this time can't ignore)
			if(hold.matches("</"+thisOne+">"))
			{
				//close off the workbook page
				toAFile.println("\t</body>\n"+
								"</workbook_page>");
				
				continue;
			}
			
			//anything else
				//here I am putting it in a codeblock as I think that should hold it without causing problems in the xml
			toAFile.println("<codeblock sytax=\"xml\">"+"\n<!-- !!!"+thisOne+"--->\n"+hold+"\n</codeblock>"); 
		}
		
		toAFile.close(); //closing it here instead of at the end tag so if there's like a blank line after the end tag for some reason or something it won't break
		fromTextFile.close();
	}
	
	public static void html(String filename) throws IOException
	{
		String xmlfile = filename+".xml";
		String htmlfile = filename+".html";
		
		Scanner fromHTML = new Scanner(new File(htmlfile));
		Scanner fromXML = new Scanner(new File(xmlfile));
		
		//copy the xml files into our files as comments to preserve them as notes. Can keep the <>s because it's in a comment
		toAFile.println("<!--The .xml file paired with the source html file read:");
		while(fromXML.hasNext())
		{
			toAFile.println(fromXML.nextLine());
		}
		toAFile.println("-->");
		
		fromXML.close();
		
		//process the html file
			//each html file is a section
		toAFile.println("\t\t<section>");
		while(fromHTML.hasNext())
		{
			toAFile.println(xmlifyContent(fromHTML.nextLine()));
		}
		toAFile.println("\t\t</section>");
		
		fromHTML.close();
	}
	
	public static String makeId(String fixCharacters)
	{
		return ncName(xmlifyTitleId(fixCharacters.toLowerCase().replaceAll("\\s+", "_")));
	}
	
	public static String xmlifyTitleId(String fixCharacters) //from OutlineConvert
	{
		//xml characters
 		fixCharacters = fixCharacters.replaceAll("&", "and"); 
 		fixCharacters = fixCharacters.replaceAll("<", "");
 		fixCharacters = fixCharacters.replaceAll(">", "");
 		fixCharacters = fixCharacters.replaceAll("'", "");
 		fixCharacters = fixCharacters.replaceAll("\"", "");
 		
 		return fixCharacters;
	}
	
	public static String ncName(String fixCharacters) //from OutlineConvert
	{
		//http://stackoverflow.com/questions/1631396/what-is-an-xsncname-type-and-when-should-it-be-used
		//"The practical restrictions of NCName are that it cannot contain several symbol characters like :, @, $, %, &, /, +, ,, ;, whitespace characters or different parenthesis. Furthermore an NCName cannot begin with a number, dot or minus character although they can appear later in an NCName."
		if(fixCharacters.substring(0,1).matches("[0-9\\.\\-]"))
			fixCharacters = "_"+fixCharacters;
		
		fixCharacters = fixCharacters.replaceAll("[:@\\$%&\\/\\+,;\\s\\(\\)\\[\\]\\{\\}]", "");
		fixCharacters = fixCharacters.replaceAll("[’\\?]",""); //not sure if ncname but xml didn't like it
		
		return fixCharacters;
	}
	
	public static String xmlifyContent(String fixCharacters) //from LOConvert, modified
	{
		fixCharacters = fixCharacters.replaceAll("&", "&amp;"); //goes first so it doesn't overwrite the others replacements after
 		//currently leaving these out so they don't mess up tags (also just found out from googling that ' and " don't even need to be escaped in text (neither does > actually))
 		//fixCharacters = fixCharacters.replaceAll("<", "&lt;");
 		//fixCharacters = fixCharacters.replaceAll(">", "&gt;");
 		//fixCharacters = fixCharacters.replaceAll("'", "&apos;");
 		//fixCharacters = fixCharacters.replaceAll("\"", "&quot;");
 		
 		return fixCharacters;
	}
}