//args[0] is the Stanford course files directory, 
//args[1] is the rest of the address of the first file to process

import java.io.*;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class StanfordConvert
{	
	//so that I don't have to move this file into the Stanford course files directory, this will hopefully help the program find files
	public static String directoryPrefix;
	
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
		
		toAFile = new PrintWriter(new File("content.html")); //I want to stick all the course content in a file together (in order) so I can look at it/go through it easier
		//since content appears to be basically in html, doing an html file rn so I can copy it in
		toAFile.println("<html>\n"+
						"<head><title>content</title></head>\n"+
						"<body>");
		
		//at the moment this is taking in a *unit* ('chapter') and going from there
		chapter(directoryPrefix+"/"+args[1]);
		
		toAFile.println("</body>\n"+
						"</html>");
		toAFile.close();
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
				toAFile.println("<h1><b>"+oliName+": " + matcher.group("name")+"</b></h1>");
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
			toAFile.println("<textarea cols=\"100\">!!!"+thisOne+": "+hold+"</textarea>"); 
			//for these looked through list of html tags tried ins which is inserted which like not quite it but. But that got rid of the stuff after !!!vertical, which I figured out by looking at the source of it was because there's tags like <problem> and stuff. Tried pre but it still did that. (Did know I could also fix it by replace and all). But I'd been going to use pre for when I copied those files in, thinking it might show the tags fine, and looks like not and I want them and don't really want to conver to &lg and all. So then I thought of text area, and, works, yay!
			
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
			
				toAFile.println("<h1>"+oliName+": " + matcher.group("name")+"</h1>");
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
			toAFile.println("<textarea cols=\"100\">!!!"+thisOne+": "+hold+"</textarea>");
			
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
				
				System.out.println("\t\t\t<item>\n"+
									"\t\t\t\t<resourceref idref=\""+makeId(matcher.group("name"))+"\"/>\n"+
									"\t\t\t</item>");
				toAFile.println("<hr/>\n<h1><i>"+oliName+": " + matcher.group("name")+"</i></h1>");
				continue;
			}
			
			//if it's a link
			if(hold.matches(regexLinkBegin+nextOneDown+regexLinkEnd))
			{
				pattern = Pattern.compile(regexLinkBegin+nextOneDown+regexLinkEnd);
				matcher = pattern.matcher(hold);
				matcher.matches();
				
				html(directoryPrefix+"/"+nextOneDown+"/"+matcher.group("fileId"));
			
				continue;
			}
			
			//if it's the ending tag (we can ignore it)
			if(hold.matches("</"+thisOne+">"))
			{
				continue;
			}
			
			//anything else
			toAFile.println("<textarea cols=\"100\">!!!"+thisOne+": "+hold+"</textarea>"); 
		}
		
		fromTextFile.close();
	}
	
	public static void html(String filename) throws IOException
	{
		String xmlfile = filename+".xml";
		String htmlfile = filename+".html";
		
		Scanner fromHTML = new Scanner(new File(htmlfile));
		Scanner fromXML = new Scanner(new File(xmlfile));
		
		//copy the xml files into our file, within pre tags, so I can check their contents. Fix the <>'s so it goes through alright
		toAFile.println("<pre>");
		while(fromXML.hasNext())
		{
			toAFile.println(fromXML.nextLine().replaceAll("<", "&lt;").replaceAll(">","&gt;"));
		}
		toAFile.println("</pre>");
		
		fromXML.close();
		
		//copy the html files into our file
		while(fromHTML.hasNext())
		{
			toAFile.println(fromHTML.nextLine());
		}
		
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
}