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
		
		toAFile = new PrintWriter(new File("content.txt")); //I want to stick all the course content in a file together (in order) so I can look at it/go through it easier
		
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
				
				System.out.println("\t\t\t<item>\n"+
									"\t\t\t\t<resourceref idref=\""+makeId(matcher.group("name"))+"\"/>\n"+
									"\t\t\t</item>");
				continue;
			}
			
			//if it's a link
			if(hold.matches(regexLinkBegin+nextOneDown+regexLinkEnd))
			{
				pattern = Pattern.compile(regexLinkBegin+nextOneDown+regexLinkEnd);
				matcher = pattern.matcher(hold);
				matcher.matches();
				
				html(directoryPrefix+"/"+nextOneDown+"/"+matcher.group("fileId")+".xml");
			
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
		
		fromTextFile.close();
	}
	
	public static void html(String filename) throws IOException
	{
		System.out.println(filename);
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