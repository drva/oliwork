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
	public static PrintWriter toLOFile;
	public static PrintWriter lookupTable;
	
	public static boolean bodyClosedEarly=false;

	public static void main(String[] args) throws IOException
	{
		//making an xml file to match the new page names I make to their filenames bc need it for links
		lookupTable = new PrintWriter(new File("pagesTable.xml"));
		lookupTable.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"+
							"<pages>");
		
		directoryPrefix = args[0];
		
		//at the moment this is taking in a *unit* ('chapter') and going from there
		chapter(directoryPrefix+"/"+args[1]);
		
		lookupTable.println("</pages>");
		lookupTable.close();
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
		
		//moving this up here because I need it to pass to the html handler for LO reasons
		String pageID = "unknown";
		
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
				pageID = makeId(matcher.group("name"));
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
					//{now handled in the xslt} so there's a problem where learning objective stuff goes in the head and I don't have those yet to put there. Will handle that later.
				
				//logging to the file of name-file correspondences
				String filenum = filename.split("[./]")[2]; //I only want the number id not the rest of the file address
				lookupTable.println("<page filename=\""+filenum+"\"><id pageid=\""+pageID+"\"/></page>");
				
				continue;
			}
			
			//if it's a link
			if(hold.matches(regexLinkBegin+nextOneDown+regexLinkEnd))
			{
				pattern = Pattern.compile(regexLinkBegin+nextOneDown+regexLinkEnd);
				matcher = pattern.matcher(hold);
				matcher.matches();
				
				//this one doesn't have the .xml ending like the others since htmls have both an xml and an html file
				html(directoryPrefix+"/"+nextOneDown+"/"+matcher.group("fileId"), pageID);
			
				continue;
			}
			
			//if it's the ending tag (this time can't ignore)
			if(hold.matches("</"+thisOne+">"))
			{
				//close off the workbook page
					//I now may have closed the body tag earlier if there was a Reference section, so checking for that
				if(!bodyClosedEarly)
					toAFile.println("\t</body>");
				else
					bodyClosedEarly=false;
				toAFile.println("</workbook_page>");
				
				continue;
			}
			
			//anything else
				//here I am putting it in a codeblock as I think that should hold it without causing problems in the xml
			toAFile.println("<codeblock syntax=\"xml\">"+"\n<!-- !!!"+thisOne+"-->\n"+hold.replaceAll("<", "&lt;").replaceAll(">","&gt;")+"\n</codeblock>"); //apparently codeblocks can't actually contain tags, need to do the replacing of <>s 
		}
		
		toAFile.close(); //closing it here instead of at the end tag so if there's like a blank line after the end tag for some reason or something it won't break
		fromTextFile.close();
	}
	
	public static void html(String filename, String pageID) throws IOException
	{
		String xmlfile = filename+".xml";
		String htmlfile = filename+".html";
		
		String XMLContent = "";
		
		Scanner fromHTML = new Scanner(new File(htmlfile));
		Scanner fromXML = new Scanner(new File(xmlfile));
		
		//I don't want to copy in the licensings so I check if this is one of those and if it is I don't copy it in
		while(fromXML.hasNext())
		{
			XMLContent=XMLContent+fromXML.nextLine();
		}
		if(XMLContent.matches("[\\s\\S]+?display_name\\s*=\\s*\"Licensing\"[\\s\\S]+?")) //contains() doesn't do regex and I want it to catch spacing variations around the =
			return;
		//Reference goes in a <bib:file> which goes outside body so checking for that too
		if(XMLContent.matches("[\\s\\S]+?display_name\\s*=\\s*\"Reference\"[\\s\\S]+?"))
		{
			toAFile.println("\t</body>");
			bodyClosedEarly = true;
		}
		
		//copy the xml files into our files as comments to preserve them as notes. Can keep the <>s because it's in a comment
		toAFile.println("<!--The .xml file paired with the source html file read:");
		toAFile.print(XMLContent); //since I have it in a string now
		toAFile.println("-->");
		
		fromXML.close();
		
		//process the html file
		String hold="";
		
		String hRegex = "(?<pre>[\\s\\S]*?)(?<header><h(?<num>[3-6])>)(?<post>[\\s\\S]*)"; //lines with headers
		boolean openSubsection = false;
		int[] openSubsectionLevels = new int[7]; //for legibility ease, openSubsectionLevels[5] represents h5 and so on (and [0-2] just get ignored since they either don't exist or are dealt with in other ways).
		
		String loHeadRegex = "[\\s\\S]*?<h2[\\s\\S]*?>Learning Objectives?</h2>[\\s\\S]*"; //finding learning objective sections
		boolean expectLOs = false;
		String loRegex="\\s*<li>(?<lo>[\\s\\S]+?)</li>\\s*";
		int numLO=1;
		
			//each html file is a section
		toAFile.println("\t\t<section>");
		while(fromHTML.hasNext())
		{
			hold = fromHTML.nextLine();
			//handling headers (note, in the chapter I have I only found (below h2s) h3s and h4, but better be prepared for h5s and h6s too in case they do come up anywhere.
			if(hold.matches(hRegex))
			{
				pattern = Pattern.compile(hRegex);
				matcher = pattern.matcher(hold);
				matcher.matches();
				
				//so I'm translating headers into sections, but sections need closing tags, so I need to put those in at the right times. The right times are 'I hit a new header of equal or higher level' (this) (where 3 is higher level than 4 etc) or 'end of file'.
				if(openSubsection)
				{
					closeSubsections(Integer.parseInt(matcher.group("num")), openSubsectionLevels);
					openSubsection=false;
				}
				
				if(!matcher.group("pre").equals("")) //if there was something on the line before the header I want to put a newline after it before inserting the section tag, but if there wasn't anything I don't want a blank newline. 
					toAFile.println(xmlifyContent(matcher.group("pre")));
				toAFile.println(printTabs(Integer.parseInt(matcher.group("num")))+"<section>\n"+matcher.group("header")+xmlifyContent(matcher.group("post")));
				
				openSubsection=true;
				openSubsectionLevels[Integer.parseInt(matcher.group("num"))]=1;
				
				continue;
			}
			
			//handling learning objectives. Atm I am putting LO's in their own file file, changing the actual LO text to a standard message in the workbook file, and leaving the rest of the header alone to deal with in the xslt
			if(hold.matches(loHeadRegex))
			{
				expectLOs=true;
				
				//making an LO page (had put this in vertical() because I needed the page id, but now I pass that in so moving it to here) (took format from ELearning course creation files)
				toLOFile = new PrintWriter(new File(directoryForPages+"/LOs_"+pageID+".xml"));
				toLOFile.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"+
								"<!DOCTYPE objectives PUBLIC \"-//Carnegie Mellon University//DTD Learning Objectives 2.0//EN\" \"http://oli.web.cmu.edu/dtd/oli_learning_objectives_2_0.dtd\">\n"+
								"<objectives id=\"LOs_"+pageID+"\">\n"+
								"\t<title>LOs</title>");
			}
			if(expectLOs && hold.matches(loRegex)) //the LOs here are just list items so I need to flag when I'm expecting them (after an LO header)
			{
				pattern = Pattern.compile(loRegex);
				matcher = pattern.matcher(hold);
				matcher.matches();
				
				toLOFile.println("\t<objective id=\""+pageID+"_LO_"+numLO+"\">"+xmlifyContent(matcher.group("lo"))+"</objective>");
				numLO++;
				
				toAFile.println("<li>LO WAS HERE</li>"); //replacing with a standard thing for easier removal in xslt
				
				continue;
			}
			if(expectLOs && hold.matches("[\\s\\S]*?</div>[\\s\\S]*"))
			{
				//done with all the LOs so stop expecting them and close their file.
				expectLOs=false;
				toLOFile.println("</objectives>");
				toLOFile.close();
			}
			
			toAFile.println(xmlifyContent(hold));
		}
		//so I'm translating headers into sections, but sections need closing tags, so I need to put those in at the right times. The right times are 'I hit a new header of equal or higher level' (where 3 is higher level than 4 etc) or 'end of file' (this).
		if(openSubsection)
		{
			closeSubsections(3, openSubsectionLevels); //end of file means close any sections I have open, and h3 is the highest level I handle in this way
			openSubsection=false;
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
		fixCharacters = fixCharacters.replaceAll("[!]",""); //adding in StanfordConvertXML. not sure if ncname but xml didn't like it
		
		return fixCharacters;
	}
	
	public static String xmlifyContent(String fixCharacters) //from LOConvert, modified
	{
				
//this is actually causing problems for what-were-nbsps and stuff; will want to fix		
		fixCharacters = fixCharacters.replaceAll("&", "&amp;"); //goes first so it doesn't overwrite the others replacements after
 		//currently leaving these out so they don't mess up tags (also just found out from googling that ' and " don't even need to be escaped in text (neither does > actually))
 		//fixCharacters = fixCharacters.replaceAll("<", "&lt;");
 		//fixCharacters = fixCharacters.replaceAll(">", "&gt;");
 		//fixCharacters = fixCharacters.replaceAll("'", "&apos;");
 		//fixCharacters = fixCharacters.replaceAll("\"", "&quot;");
 		
 		//special characters (https://stackoverflow.com/questions/9625602/how-to-display-nbsp-in-xml-output need to be unicode)
		//nbsp, mdash, rsquo, ndash, ldquo, rdquo, macr
		//taking into account already converted ampersand
		fixCharacters = fixCharacters.replaceAll("&amp;nbsp;", "&#xA0;");
		fixCharacters = fixCharacters.replaceAll("&amp;mdash;", "&#x2014;");
		fixCharacters = fixCharacters.replaceAll("&amp;rsquo;", "&#x2019;");
		fixCharacters = fixCharacters.replaceAll("&amp;ndash;", "&#x2013;");
		fixCharacters = fixCharacters.replaceAll("&amp;ldquo;", "&#x201C;");
		fixCharacters = fixCharacters.replaceAll("&amp;rdquo;", "&#x201D;");
		fixCharacters = fixCharacters.replaceAll("&amp;macr;", "&#xAF;");
 		
 		return fixCharacters;
	}

	public static void closeSubsections(int level, int[] openSubsectionLevels)
	{
		//so if I hit a new h6 and I had a previous section headed by an h6 open, I want to close the old one. But if I had a previous section headed by an h4 say open, I don't want to close it - it can keep going with this new h6 section inside it. But if I hit a new h4, I do also want to close any h5 or h6 sections I had open too.
		for(int i=openSubsectionLevels.length-1; i>=level; i--)
		{
			//a 1 there marks that I actually did open a section, so it needs to be closed
			if(openSubsectionLevels[i]==1)
			{
				toAFile.println(printTabs(i)+"</section>");
				openSubsectionLevels[i]=0; 	//checked and yes this will change the array up in the original method too https://stackoverflow.com/questions/21653048/changing-array-in-method-changes-array-outside
			}
		}
	} 
	
	public static String printTabs(int numtabs)
	{
		//checked: no you can't do string multiplication in java, this suggests a method https://stackoverflow.com/questions/2255500/can-i-multiply-strings-in-java-to-repeat-sequences, I'm just doing a loop
		String toReturn="";
		for(int i=0; i<numtabs; i++)
			toReturn=toReturn+"\t";
		
		return toReturn;	
	}
}