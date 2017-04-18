//args 0 is the outline text file
//args 1 is the folder to put the workbook pages in
//args 2 is the folder the quizzes are in
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
	
	public static void main(String[] args) throws IOException
	{
		Scanner fromTextFile = new Scanner(new File(args[0]+".txt"));
		
		//flags for open tags
		boolean openUnit=false;
		boolean openModule=false;
		boolean openPage=false;
		
		File[] dirQuizzes = new File(args[2]).listFiles();
		
		String hold;
		Pattern pattern;
		Matcher matcher;
		String id;
		String title;
		String checkAs;
		PrintWriter toCourseFile=null;
		while(fromTextFile.hasNext())
		{
			hold = fromTextFile.nextLine();
			
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
								"\t<body>\n"+
								"\t\t<p>(This space intentionally left blank.)</p>\n");
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
				
				for(int i=0; i<dirQuizzes.length; i++)
				{
					//if the name of the file contains the way the file was referenced in the outline
					if(dirQuizzes[i].getName().contains(checkAs.subSequence(0, checkAs.length()))) //contains is apparently only for char sequences
					{
						if(dirQuizzes[i].getName().matches("[\\S\\s]*?\\d\\d[\\S\\s]*") && !checkAs.matches("[\\S\\s]*?\\d\\d[\\S\\s]*")) //if the quiz name has a 2 digit number and the ref doesn't, don't put that quiz in (this is to stop Ch 1 from pulling in the Ch 10 quiz, etc)
							continue;
						
						toCourseFile.println("\t\t<activity idref=\""+dirQuizzes[i].getName().split(".xml")[0]+"\" purpose = \"quiz\"/>");
					}
				}
				continue;
			}
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