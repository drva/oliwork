//args 0 is the input file


import java.io.*;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class Predict_SelectPrinciple_Explain__Convert
{
	public static String idBase = "predict_selectprinciple_explain";
	
	//flags for open tags
	public static boolean flagOpenSection=false;
	public static boolean flagOpenQuestion=false;
	public static boolean flagOpenBody=false;
	
	public static void main(String[] args) throws IOException
	{
		Scanner fromTextFile = new Scanner(new File(args[0]+".txt"));
		PrintWriter toXMLFile= new PrintWriter(idBase+".xml");
		
		openFormativeAssessment(idBase, toXMLFile);
 		
 		String regExBeginQuestionSet = "(?<setnum>\\d+)\\.\\s*(?<settitle>[\\s\\S]+)";
 		String regExBeginQuestion = "(?<qId>Q\\d+(?<qdesignation1>[a-z]))\\s*\\((?<qdesignation2>[\\s\\S]+?)\\):\\s*(?<question>[\\s\\S]+)";
 		String regExBodyCont = "(Assume:|\\d\\))[\\s\\S]*"; //this is not a good way of doing this. However, atm neither body-continuation nor choices are identified in any way. But, all body-continuation is either 'Assume:' or starts with '1)'. So using that.
 		Pattern pattern;
 		Matcher matcher;
 		
		String hold="";
		//go through the text file
		while(fromTextFile.hasNext())
		{
			hold = fromTextFile.nextLine();
			hold = hold.trim(); //leading and trailing whitespace
 			
 			//question sets (become sections)
 			if(hold.matches(regExBeginQuestionSet))
 			{
 				pattern = Pattern.compile(regExBeginQuestionSet);
				matcher = pattern.matcher(hold);
				matcher.matches();
				
				if(flagOpenSection)
					closeSection(toXMLFile);
				openSection(toXMLFile, xmlifyContent(matcher.group("settitle")), "sec"+matcher.group("setnum"));
				
				continue;
 			}
 			
 			//question opening lines
 			if(hold.matches(regExBeginQuestion))
 			{
 				pattern = Pattern.compile(regExBeginQuestion);
				matcher = pattern.matcher(hold);
				matcher.matches();
				
				if(flagOpenQuestion)
					closeQuestion(toXMLFile);
				
				openQuestion(toXMLFile, matcher.group("qId"), xmlifyContent("Part "+matcher.group("qdesignation1")+", "+matcher.group("qdesignation2")+": "+matcher.group("question")));
				
				continue;
 			}
 			
 			//question body continuation on another line
 			if(hold.matches(regExBodyCont))
 			{
 				toXMLFile.println("\t\t\t\t"+xmlifyContent(hold));
 			}
 			
 			
		}
		
		if(flagOpenSection)
			closeSection(toXMLFile);
		closeFormativeAssessment(toXMLFile);
		toXMLFile.close();		
	}
	
	public static void openFormativeAssessment(String id, PrintWriter toXMLFile) throws IOException
	{
		toXMLFile.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"+
					"<!DOCTYPE assessment\n"+
  					"PUBLIC \"-//Carnegie Mellon University//DTD Inline Assessment 1.0//EN\" \"http://oli.web.cmu.edu/dtd/oli_inline_assessment_1_0.dtd\">\n"+
					"<?xml-stylesheet type=\"text/css\" href=\"http://oli.web.cmu.edu/authoring/oxy-author/oli_inline_assessment_1_1.css\"?>\n"+
					"<assessment id=\""+id+"\">\n"+
					"\t<title>Check your work</title>");
	}
	
	public static void closeFormativeAssessment(PrintWriter toXMLFile) throws IOException
	{
		toXMLFile.println("</assessment>");
	}
	
	public static void openSection(PrintWriter toXMLFile, String title, String id) throws IOException
	{
		flagOpenSection = true;
		toXMLFile.println("\t<section id=\""+id+"\">");
		toXMLFile.println("\t\t<title>"+title+"</title>");
	}
	
	public static void closeSection(PrintWriter toXMLFile) throws IOException
	{
		flagOpenSection = false;
		toXMLFile.println("\t</section>");
	}
	
	public static void openQuestion(PrintWriter toXMLFile, String id, String bodyContent) throws IOException
	{
		flagOpenQuestion = true;
		flagOpenBody = true;
		
		toXMLFile.println("\t\t<question id=\""+id+"\">");
		toXMLFile.println("\t\t\t<body>");
		toXMLFile.println("\t\t\t\t"+bodyContent);
	}
	
	public static void closeQuestion(PrintWriter toXMLFile) throws IOException
	{
		flagOpenQuestion = false;
		toXMLFile.println("\t\t</question>");
	}
	
	public static String xmlifyContent(String fixCharacters) //from OutlineConvert
	{
		if(fixCharacters == null) //since I pass in a matcher group that is null for part of link handling 
			return "";
		
		fixCharacters = fixCharacters.replaceAll("&", "&amp;"); //goes first so it doesn't overwrite the others replacements after
 		fixCharacters = fixCharacters.replaceAll("<", "&lt;");
 		fixCharacters = fixCharacters.replaceAll(">", "&gt;");
 		fixCharacters = fixCharacters.replaceAll("'", "&apos;");
 		fixCharacters = fixCharacters.replaceAll("\"", "&quot;");
 		
 		return fixCharacters;
	}
}