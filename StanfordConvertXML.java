//args[0] is the Stanford course files directory, 
//args[1] is the rest of the address of the first file to process 
//args[2] is chapter to do chapter by chapter and course to do course.

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
	
	//it turns out there is another format LOs can have (aside from being all in one html in a div), as in the philanthropy course, where they are distributed among html files, one per. Variables for helping handle that (since they're as noted distributed among multiple htmls and can't be handled all one)
	public static boolean distributedLOsOpen = false;
	public static int numDistLO = 1;
	
	//it turns out some course, for instance the philanthropy course, put more things after a reference section. We can't allow that, so new processing to save it for the end
	public static String holdReferences = "";
	public static boolean referencesInHold = false;

	public static void main(String[] args) throws IOException
	{
		//making an xml file to match the new page names I make to their filenames bc need it for links
		lookupTable = new PrintWriter(new File("pagesTable.xml"));
		lookupTable.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"+
							"<pages>");
		
		directoryPrefix = args[0];
		
		//can do a whole course or chapter by chapter
		//if there's a second arg telling you a chapter, do that chapter
		if(args[2].equals("chapter"))
			chapter(directoryPrefix+"/"+args[1]);
//WILL WANT TO ADJUST THIS TO TAKE IN COURSE.XML AND GET THE FILENAME THERE (I THINK IT'S THERE)
		else if(args[2].equals("course"))
			course(directoryPrefix+"/"+args[1]);
		else
			System.out.println("Please enter course to do a whole course and chapter to do a chapter");
		
		lookupTable.println("</pages>");
		lookupTable.close();
	}
	
	public static void course(String filename) throws IOException
	{
		String thisOne = "course";
		String nextOneDown = "chapter";
		//String oliName = 
		
		//needs to be up here to be passed on later
		//String unitid = "";
		
		String regexCourseNameLine = "\\s*<"+thisOne+"\\s+[\\s\\S]*?\\s+display_name=\"(?<name>[\\s\\S]+?)\"[\\s\\S]*>"; //different from others bc display_name does not immediately follow the tag name
		String regexwikislug = "\\s*<wiki\\s+slug=\"[\\s\\S]+?\"\\s*\\/>"; //seems to be a thing course files have. (regex101 says / needs to be escaped. Don't think I escaped it in other regex, but will try to abide).
		
		Scanner fromTextFile = new Scanner(new File(filename));
		
		String hold="";
		while(fromTextFile.hasNext())
		{
			hold = fromTextFile.nextLine().trim(); //also get rid of leading and trailing whitespace
			
			//if it's the name
			if(hold.matches(regexCourseNameLine))
			{
				pattern = Pattern.compile(regexNameLine);
				matcher = pattern.matcher(hold);
				matcher.matches();
				
				//currently not processing any of the other attributes etc here
				//GOING TO WANT TO MAKE THE ORG FILE HERE
				
				continue;
			}
			
			//if it's a link
			if(hold.matches(regexLinkBegin+nextOneDown+regexLinkEnd))
			{
				pattern = Pattern.compile(regexLinkBegin+nextOneDown+regexLinkEnd);
				matcher = pattern.matcher(hold);
				matcher.matches();
				
				chapter(directoryPrefix+"/"+nextOneDown+"/"+matcher.group("fileId")+".xml"); 
				
				continue;
			}
						
			//if it's the ending tag (we can ignore it)
			if(hold.matches("</"+thisOne+">"))
			{
				//GOING TO WANT TO CLOSE ORG FILE HERE (OR BELOW)
				continue;
			}
			
			
			
			//anything else
			System.out.println("!!!"+thisOne+": "+hold); 
			
		}
		
		fromTextFile.close();
	}
	
	//chapters are units
	public static void chapter(String filename) throws IOException
	{
		String thisOne = "chapter";
		String nextOneDown = "sequential";
		String oliName = "unit";
		
		//needs to be up here to be passed on later
		String unitid = "";
		
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
				
				unitid=makeId(matcher.group("name"));
				continue;
			}
			
			//if it's a link
			if(hold.matches(regexLinkBegin+nextOneDown+regexLinkEnd))
			{
				pattern = Pattern.compile(regexLinkBegin+nextOneDown+regexLinkEnd);
				matcher = pattern.matcher(hold);
				matcher.matches();
				
				sequential(directoryPrefix+"/"+nextOneDown+"/"+matcher.group("fileId")+".xml", unitid); //now also handing on the unit id so it can be used in page filenames
				
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
	public static void sequential(String filename, String unitid) throws IOException
	{
		String thisOne = "sequential";
		String nextOneDown = "vertical";
		String oliName = "module";
		
		//needs to be up here to be passed on later
		String moduleid = "";	
		
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
				
				System.out.println("\t\t<"+oliName+" id=\""+"u-"+unitid+"-m-"+makeId(matcher.group("name"))+"\">\n"+
									"\t\t\t<title>"+xmlifyTitleId(matcher.group("name"))+"</title>");
			
				moduleid = makeId(matcher.group("name"));
				continue;
			}
			
			//if it's a link
			if(hold.matches(regexLinkBegin+nextOneDown+regexLinkEnd))
			{
				pattern = Pattern.compile(regexLinkBegin+nextOneDown+regexLinkEnd);
				matcher = pattern.matcher(hold);
				matcher.matches();
				
				vertical(directoryPrefix+"/"+nextOneDown+"/"+matcher.group("fileId")+".xml", unitid, moduleid); //now also handing on the unit id and module id so it can be used in page filenames
			
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
	public static void vertical(String filename, String unitid, String moduleid) throws IOException
	{
		String thisOne = "vertical";
		String nextOneDown = "html";
		String oliName = "page";
		
		//moving this up here because I need it to pass to the html handler for LO reasons
		//page id being modified to have unit and module id in it to avoid name overlaps
		String pageID = "u-"+unitid+"-m-"+moduleid+"-p-";
		
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
				pageID = pageID+makeId(matcher.group("name"));
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
				String filenum = filename.split("[./]")[filename.split("[./]").length-2]; //I only want the number id not the rest of the file address
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
					//this was originally here when References were being handled differently. I am leaving it in case something else comes up where I need to close off body early
				if(!bodyClosedEarly)
					toAFile.println("\t</body>");
				else
					bodyClosedEarly=false;
				
				//it turns out some course, for instance the philanthropy course, put more things after a reference section. We can't allow that, so new processing to save it for the end
					//this is the end, so here we write it in and clear our variables 
				if(referencesInHold)
				{
					toAFile.println(holdReferences);
					holdReferences = "";
					referencesInHold = false;
				}	
				toAFile.println("</workbook_page>");
				
				continue;
			}
			
			//anything else
				//here I am putting it in a codeblock as I think that should hold it without causing problems in the xml
			toAFile.println("<codeblock syntax=\"xml\">"+"\n<!-- !!!"+thisOne+"-->\n"+hold.replaceAll("<", "&lt;").replaceAll(">","&gt;")+"\n</codeblock>"); //apparently codeblocks can't actually contain tags, need to do the replacing of <>s 
		}
		
		toAFile.close(); //closing it here instead of at the end tag so if there's like a blank line after the end tag for some reason or something it won't break
		fromTextFile.close();
		
		//it turns out there is another format LOs can have (aside from being all in one html in a div), as in the philanthropy course, where they are distributed among html files, one per. Closing stuff relevant to that off, if there was any, so can start fresh on next page etc
		if(distributedLOsOpen)
		{
			distributedLOsOpen = false;
			numDistLO = 1;
			toLOFile.println("</objectives>");
			toLOFile.close();
		}
				
	}
	
	public static void html(String filename, String pageID) throws IOException
	{
		String xmlfile = filename+".xml";
		String htmlfile = filename+".html";
		
		String XMLContent = "";
		
		Scanner fromHTML = new Scanner(new File(htmlfile));
		Scanner fromXML = new Scanner(new File(xmlfile));
		
		while(fromXML.hasNext())
		{
			XMLContent=XMLContent+fromXML.nextLine();
		}
		fromXML.close();
		//I don't want to copy in the licensings so I check if this is one of those and if it is I don't copy it in
			//Having expanded titles it will notice as such to include 'License' makes it more likely it will catch an actual section that's simply called that. For this reason and in general, it will now mark places where it left out a license section
		if(XMLContent.matches("[\\s\\S]+?display_name\\s*=\\s*\"(Licensing|License)\"[\\s\\S]+?")) //contains() doesn't do regex and I want it to catch spacing variations around the =
		{	
			toAFile.print("<!--A License section was here:");
			toAFile.print(XMLContent);
			toAFile.println("-->");
			return;
		}
		//Reference goes in a <bib:file> which goes outside body so checking for that too
			//it turns out some course, for instance the philanthropy course, put more things after a reference section. We can't allow that, so new processing to save it for the end
		if(XMLContent.matches("[\\s\\S]+?display_name\\s*=\\s*\"References?\"[\\s\\S]+?"))
		{
			//copy everything we're going to put into the reference section into a string to write into the file later
			holdReferences += "<!--The .xml file paired with the source html file read:\n"+XMLContent+"\n-->";
			
			holdReferences +="\n<section>"; //putting the content in a section bc grouping it is needed for later xslt handing and it already is set up to use section as the group
			while(fromHTML.hasNext())
			{
				holdReferences += "\n"+fromHTML.nextLine();
			}
			fromHTML.close();
			holdReferences +="\n</section>";
			
			referencesInHold = true; //flag so we know to write it in later
			
			return; //exit early since the rest of the method is meant for not-this
		}
		//it turns out there is another format LOs can have, as in the philanthropy course, where they are distributed among html files, one per. Handling those
		if(XMLContent.matches("[\\s\\S]+?display_name\\s*=\\s*\"Learning\\s+Objective\"[\\s\\S]+?"))
		{
			if(!distributedLOsOpen) //if this is the first LO of this page we've encountered
			{
				//making an LO page (took format from ELearning course creation files)
				toLOFile = new PrintWriter(new File(directoryForPages+"/LOs_"+pageID+".xml")); //this is already global scope so we'll have access to it across the multiple htmls
				toLOFile.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"+
								"<!DOCTYPE objectives PUBLIC \"-//Carnegie Mellon University//DTD Learning Objectives 2.0//EN\" \"http://oli.web.cmu.edu/dtd/oli_learning_objectives_2_0.dtd\">\n"+
								"<objectives id=\"LOs_"+pageID+"\">\n"+
								"\t<title>LOs</title>");
				distributedLOsOpen = true;
			}
			
			//handling here separately since these don't write to the main file, aren't sections, etc
					//copy the xml files into our files as comments to preserve them as notes. Can keep the <>s because it's in a comment
			toLOFile.println("<!--The .xml file paired with the source html file read:");
			toLOFile.print(XMLContent); //since I have it in a string now
			toLOFile.println("-->");
			
				//process the html file (which contains a learning objective)
			toLOFile.println("\t<objective id=\""+pageID+"_LO_"+numDistLO+"\">"); //open objective tag for this objective
			numDistLO++;
			String hold = "";
			while(fromHTML.hasNext())
			{
				hold = fromHTML.nextLine();
				toLOFile.println(xmlifyContent(hold));
			}
			fromHTML.close();
			toLOFile.println("</objective>"); //close objective tag
			return; //exit early since the rest of the method is meant for not-these-files
		}
		
		//copy the xml files into our files as comments to preserve them as notes. Can keep the <>s because it's in a comment
		toAFile.println("<!--The .xml file paired with the source html file read:");
		toAFile.print(XMLContent); //since I have it in a string now
		toAFile.println("-->");
		
		//process the html file
		String hold="";
		
		String hRegex = "(?<pre>[\\s\\S]*?)(?<header><h(?<num>[1-6])(?<attributes>[\\s\\S]*?)>)(?<post>[\\s\\S]*)"; //lines with headers (I'd originally only run into 3 and up inside html files, but in philanthropy encountered h2's inside html files as well, so modifying)
		boolean openSubsection = false;
		int[] openSubsectionLevels = new int[7]; //for legibility ease, openSubsectionLevels[5] represents h5 and so on (and [0-2] just get ignored since they either don't exist or are dealt with in other ways). (modification: h2 may be dealt with here too now (and h1 just in case))
		
		String loHeadRegex = "[\\s\\S]*?<h2[\\s\\S]*?>Learning Objectives?</h2>[\\s\\S]*"; //finding learning objective sections
		boolean expectLOs = false;
		String loRegex="\\s*<li>(?<lo>[\\s\\S]+?)(</li>|<li>)\\s*"; //a few LO's turn out to 'terminate' with another <li> tag instead of closing tag. Going to try to have this script recognize those too, may walk back if this causes problems
		int numLO=1;
		
			//each html file is a section
		toAFile.println("\t\t<section>");
		while(fromHTML.hasNext())
		{
			hold = fromHTML.nextLine();
			
			//handling headers (note, in the chapter I have I only found (below h2s) h3s and h4, but better be prepared for h5s and h6s too in case they do come up anywhere.
			if(hold.matches(hRegex) && !hold.matches(loHeadRegex)) //since hRegex now looks for h2s as well, and the lo header is an h2, excluding it
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
			closeSubsections(1, openSubsectionLevels); //end of file means close any sections I have open, and h3 is the highest level I handle in this way (modification: now potentially h2s, which I've seen, and h1, in case, might be handled this way)
			openSubsection=false;
		}
		toAFile.println("\t\t</section>");
		
		fromHTML.close();
	}
	
	public static String makeId(String fixCharacters)
	{
		return ncName(xmlifyTitleId(fixCharacters.toLowerCase().replaceAll("\\s+", "_")));
	}
	
	public static String xmlifyTitleId(String fixCharacters) //from OutlineConvert, modified
	{
		//at least one of the edX files has a name where & is already represented as &amp;, which is therefore producing not what is wanted
		fixCharacters = fixCharacters.replaceAll("&amp;", "and"); 
		
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
		fixCharacters = fixCharacters.replaceAll("—","-"); //em-dash. adding in StanfordConvertXML. xml didn't like. 
		
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
		
		//while I'm here will also fix the <o:p> problem (there are empty <o:p> tags and they cause a namespace problem. Apparently this is just a microsoft thing https://stackoverflow.com/questions/7808968/what-do-op-elements-do-anyway/7809422)
 		fixCharacters = fixCharacters.replaceAll("<o:p>", "<p>");
 		fixCharacters = fixCharacters.replaceAll("</o:p>", "</p>");
 		
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