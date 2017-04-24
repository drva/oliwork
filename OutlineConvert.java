//args 0 is the outline text file
//args 1 is the folder to put the workbook pages in
//args 2 is the folder the quizzes are in
//args 3 is the folder the slides are in
//args 4 is the folder the papers are in
//system.out outputs the org file text

import java.io.*;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class OutlineConvert
{
	public static String regExUnit = "Unit \\d+:: ([\\s\\S]+?)\\s*";
	public static String regExModule = "Module \\d+/ ([\\s\\S]+?)\\s*";
	public static String regExQuiz = "([\\s\\S]+?) quiz[\\s]*";
	public static String regExSlides = "([\\s\\S]+? slides) \\[([\\s\\S]+?)\\]";
	public static String regExPapers = "([\\s\\S]+?): ([\\s\\S]+?) \\[([\\s\\S]+?)…?\\]";
	
	public static void main(String[] args) throws IOException
	{
		Scanner fromTextFile = new Scanner(new File(args[0]+".txt"));
		
		//flags for open tags
		boolean openUnit=false;
		boolean openModule=false;
		boolean openPage=false;
		
		File[] dirQuizzes = new File(args[2]).listFiles();
		File[] dirSlides = new File(args[3]).listFiles();
		File[] dirPapers = new File(args[4]).listFiles();
		
		String hold;
		Pattern pattern;
		Matcher matcher;
		String id;
		String title;
		String checkAs;
		Boolean couldntHandle=false;
		PrintWriter toCourseFile=null;
		while(fromTextFile.hasNext())
		{
			hold = fromTextFile.nextLine();
			
			//blank lines and space lines
			if(hold.equals("") || hold.matches("[\\s]+"))
				continue;
			
			//units
			if(hold.matches(regExUnit))
			{
				//close tags if needed
				if(openPage)
				{
					closePage(toCourseFile);
					openPage=false;
				}
				if(openModule)
				{
					closeModule();
					openModule=false;
				}
				if(openUnit)
				{
					closeUnit();
					openUnit=false;
				}
				
				pattern = Pattern.compile(regExUnit);
				matcher = pattern.matcher(xmlifyTitleId(hold));
				matcher.matches();
				
				title=matcher.group(1);
				id = ncName(matcher.group(1).replaceAll("\\s","_"));
				
				System.out.println("\t\t\t<unit id=\""+id+"\">");
				System.out.println("\t\t\t\t<title>"+title+"</title>");
				openUnit = true;
				continue;
			}
			
			//modules (which atm are also pages)
			if(hold.matches(regExModule))
			{
				//close tags if needed
				if(openPage)
				{
					closePage(toCourseFile);
					openPage=false;
				}
				if(openModule)
				{
					closeModule();
					openModule=false;
				}
				
				pattern = Pattern.compile(regExModule);
				matcher = pattern.matcher(xmlifyTitleId(hold));
				matcher.matches();
				
				title=matcher.group(1);
				id = ncName(matcher.group(1).replaceAll("\\s","_"));
				
				if(id.length()>56) //I'm having an error and wondering if it's due to length
					id=id.substring(0,57);
				
				System.out.println("\t\t\t\t<module id=\""+id+"\">");
				System.out.println("\t\t\t\t\t<title>"+title+"</title>");
				openModule = true;
				
				//page
				//the org file text
				System.out.println("\t\t\t\t<item>\n"+
									"\t\t\t\t\t<resourceref idref=\""+id+"\"/>\n"+
									"\t\t\t\t</item>");
									
				//the workbook page
				toCourseFile = new PrintWriter(args[1]+"/"+id+".xml");
			
				toCourseFile.print("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"+
								"<!DOCTYPE workbook_page PUBLIC \"-//Carnegie Mellon University//DTD Workbook Page 3.7//EN\" \"http://oli.web.cmu.edu/dtd/oli_workbook_page_3_7.dtd\">\n"+
								"<?xml-stylesheet type=\"text/css\" href=\"http://oli.web.cmu.edu/authoring/oxy-author/oli_workbook_page_3_7.css\"?>\n"+
								"<workbook_page id=\""+id+"\">\n"+
								"\t<head>\n"+
								"\t\t<title>"+title+"</title>\n"+
								"\t</head>\n"+
								"\t<body>\n");
				openPage=true;
				continue;
			}
			
			//quizzes
			if(hold.matches(regExQuiz))
			{
				pattern = Pattern.compile(regExQuiz);
				matcher = pattern.matcher(hold);
				matcher.matches();
				
				checkAs = matcher.group(1).replaceAll("\\s","_");
				checkAs = checkAs.replaceAll("&","and");
				
				for(int i=0; i<dirQuizzes.length; i++)
				{
					//if the file is .xml and the name of the file contains the way the file was referenced in the outline
					if(dirQuizzes[i].getName().matches("[\\s\\S]+\\.xml") && dirQuizzes[i].getName().contains(checkAs.subSequence(0, checkAs.length()))) //contains is apparently only for char sequences
					{
						if(dirQuizzes[i].getName().matches("[\\D]*?\\d\\d[\\D]*") && !checkAs.matches("[\\S\\s]*?\\d\\d[\\S\\s]*")) //if the quiz name has a 2 digit number and the ref doesn't, don't put that quiz in (this is to stop Ch 1 from pulling in the Ch 10 quiz, etc) {modified to avoid issues with 2013 in Data discovery}
							continue;
						
						toCourseFile.println("\t\t<activity idref=\""+dirQuizzes[i].getName().split(".xml")[0]+"\" purpose = \"quiz\"/>");
					}
				}
				continue;
			}
			
			//slides
			if(hold.matches(regExSlides))
			{
				pattern = Pattern.compile(regExSlides);
				matcher = pattern.matcher(hold);
				matcher.matches();
				
				checkAs = matcher.group(2);
				
				//matching to the file name, or the first part thereof (for the L#'s), or the file name without extension
				for(int i=0; i<dirSlides.length; i++)
				{
					if(checkAs.equals(dirSlides[i].getName()) || checkAs.equals(dirSlides[i].getName().split(".pptx")[0])|| checkAs.equals(dirSlides[i].getName().split("-")[0]))
					{
						toCourseFile.println("\t\t<p><link href=\"../webcontent/slides/"+dirSlides[i].getName()+"\">"+xmlifyContent(matcher.group(1))+"</link></p>");
					}
				}
				continue;
			}
			
			//papers
			if(hold.matches(regExPapers))
			{
				pattern = Pattern.compile(regExPapers);
				matcher = pattern.matcher(hold);
				matcher.matches();
				
				checkAs = matcher.group(3);
				
				couldntHandle=true; //for papers the converter can't match up atm
				//if filename matches ref, or filename without.pdf matches ref, or as much of filename as ref has matches ref
				for(int i=0; i<dirPapers.length; i++)
				{
					if(checkAs.equals(dirPapers[i].getName()) || checkAs.equals(dirPapers[i].getName().split(".pdf")[0])|| checkAs.equals(dirPapers[i].getName().split(".pdf")[0].substring(0,Math.min(dirPapers[i].getName().split(".pdf")[0].length(),checkAs.length())))) //min is in there to avoid index out of bounds when it checks other filenames
					{
						toCourseFile.println("\t\t<p>"+xmlifyContent(matcher.group(1))+": <link href=\"../webcontent/papers/"+dirPapers[i].getName()+"\">"+xmlifyContent(matcher.group(2))+"</link></p>");
						couldntHandle=false;
					}
				}
				if(couldntHandle)
				{
					toCourseFile.println("\t\t<p>!!"+xmlifyContent(hold)+"</p>");
					couldntHandle=false;
				}
				continue;
			}
			
			toCourseFile.println("\t\t<p>"+xmlifyContent(hold)+"</p>");
		}
		
		//close tags if needed
		if(openPage)
		{
			closePage(toCourseFile);
			openPage=false;
		}
		if(openModule)
		{
			closeModule();
			openModule=false;
		}
		if(openUnit)
		{
			closeUnit();
			openUnit=false;
		}		
	}
	
	public static String xmlifyContent(String fixCharacters)
	{
		fixCharacters = fixCharacters.replaceAll("&", "&amp;"); //goes first so it doesn't overwrite the others replacements after
 		fixCharacters = fixCharacters.replaceAll("<", "&lt;");
 		fixCharacters = fixCharacters.replaceAll(">", "&gt;");
 		fixCharacters = fixCharacters.replaceAll("'", "&apos;");
 		fixCharacters = fixCharacters.replaceAll("\"", "&quot;");
 		
 		return fixCharacters;
	}
	
	public static String xmlifyTitleId(String fixCharacters)
	{
		//xml characters
 		fixCharacters = fixCharacters.replaceAll("&", "and"); 
 		fixCharacters = fixCharacters.replaceAll("<", "");
 		fixCharacters = fixCharacters.replaceAll(">", "");
 		fixCharacters = fixCharacters.replaceAll("'", "");
 		fixCharacters = fixCharacters.replaceAll("\"", "");
 		
 		return fixCharacters;
	}
	
	public static String ncName(String fixCharacters)
	{
		//http://stackoverflow.com/questions/1631396/what-is-an-xsncname-type-and-when-should-it-be-used
		//"The practical restrictions of NCName are that it cannot contain several symbol characters like :, @, $, %, &, /, +, ,, ;, whitespace characters or different parenthesis. Furthermore an NCName cannot begin with a number, dot or minus character although they can appear later in an NCName."
		if(fixCharacters.substring(0,1).matches("[0-9\\.\\-]"))
			fixCharacters = "_"+fixCharacters;
		
		fixCharacters = fixCharacters.replaceAll("[:@\\$%&\\/\\+,;\\s\\(\\)\\[\\]\\{\\}]", "");
		fixCharacters = fixCharacters.replaceAll("[’\\?]",""); //not sure if ncname but xml didn't like it
		
		return fixCharacters;
	}
	
	public static void closePage(PrintWriter toCourseFile)
	{
		toCourseFile.println("\t</body>\n"+
								"</workbook_page>");
		toCourseFile.close();
	}
	
	public static void closeModule()
	{
		System.out.println("\t\t\t\t</module>");
	}
	
	public static void closeUnit()
	{
		System.out.println("\t\t\t</unit>");
	}
}