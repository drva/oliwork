//args 0 is the input file


import java.io.*;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.awt.Image;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

public class Predict_SelectPrinciple_Explain__Convert
{
	public static String idBase = "predict_selectprinciple_explain";
	public static String imageFolder = "images";
	public static String shuffle = "false";
	public static int pointsPerQ = 1;
	public static int maxIdLength = 50; //currently decided at random
	
	//flags for open tags
	public static boolean flagOpenSection=false;
	public static boolean flagOpenQuestion=false;
	public static boolean flagOpenBody=false;
	public static boolean flagOpenChoices=false;
	
	public static String correctAnswer=""; //is up here so it can be reset from methods
	
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
		String choiceId="";
		boolean isCorrectAnswer=false;
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
				openSection(toXMLFile, xmlifyContent(matcher.group("settitle")), matcher.group("setnum"));
				
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
 				toXMLFile.println("\t\t\t\t<p>"+xmlifyContent(hold)+"</p>");
 				
 				continue;
 			}
 			
 			//choices are currently unmarked, so they are 'anything else' (except blank lines)
 			if(!hold.equals(""))
 			{
 				if(flagOpenBody)
 					closeBody(toXMLFile);
 				if(!flagOpenChoices)
 					openChoices(toXMLFile);
 				
 				//identify the correct answer (which is marked by *)
 				if(hold.charAt(0)=='*')
 				{
 					isCorrectAnswer=true;
 					hold = hold.substring(1, hold.length()); //now trim the * off
 				}
 				
 				//create id out of the choice
 				choiceId=xmlifyTitleId(hold.substring(0, Math.min(maxIdLength, hold.length())).toLowerCase().replaceAll("\\s+","_"));
 				
 				toXMLFile.println("\t\t\t\t<choice value=\""+choiceId+"\">"+xmlifyContent(hold)+"</choice>");
 				
 				if(isCorrectAnswer)
 				{
 					correctAnswer = choiceId;
 					isCorrectAnswer=false;
 				}
 			}	
		}
		
		if(flagOpenQuestion)
			closeQuestion(toXMLFile);
			
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
		toXMLFile.println("\t\t<content><p><em style=\"bold\">"+title+"</em></p></content>");
		
		//adds in images for the section, assuming they go directly after the section opener and are saved in the format iSECTIONNUM
		File image = new File(imageFolder+"/i"+id+".png");
		
		//trying to get the width to work right - images should be made *smaller* if needed but not *bigger*
		//https://stackoverflow.com/questions/672916/how-to-get-image-height-and-width-using-java
		BufferedImage bimg = ImageIO.read(image);
		int width = bimg.getWidth();
 		if(image.exists())
 		{
 			toXMLFile.println("\t\t<content><image src=\"../webcontent/"+"i"+id+".png\" width=\""+Integer.toString(Math.min(width, 650))+"\"/></content>");
 		}
	}
	
	public static void closeSection(PrintWriter toXMLFile) throws IOException
	{
		if(flagOpenQuestion)
			closeQuestion(toXMLFile);
		flagOpenSection = false;
	}
	
	public static void openQuestion(PrintWriter toXMLFile, String id, String bodyContent) throws IOException
	{
		flagOpenQuestion = true;
		flagOpenBody = true;
		
		toXMLFile.println("\t\t<question id=\""+id+"\">");
		toXMLFile.println("\t\t\t<body>");
		toXMLFile.println("\t\t\t\t<p>"+bodyContent+"</p>");
	}
	
	//also calls close choices
	public static void closeQuestion(PrintWriter toXMLFile) throws IOException
	{
		closeChoices(toXMLFile);
		
		toXMLFile.println("\t\t</question>");
		flagOpenQuestion = false;
	}
	
	//body gets openeded within openQuestion, and so doesn't have its own method
	public static void closeBody(PrintWriter toXMLFile) throws IOException
	{
		flagOpenBody = false;
		toXMLFile.println("\t\t\t</body>");
	}
	
	public static void openChoices(PrintWriter toXMLFile) throws IOException
	{
		flagOpenChoices = true;
		
		toXMLFile.println("\t\t\t<multiple_choice select=\"single\" shuffle=\""+shuffle+"\">");
	}
	
	//also handles 'part'/responses etc
	public static void closeChoices(PrintWriter toXMLFile) throws IOException
	{
		toXMLFile.println("\t\t\t</multiple_choice>");
		flagOpenChoices = false;
		
		toXMLFile.println("\t\t\t<part>");
		if(!correctAnswer.equals("")) //"" happens when no correct answer was given
		{
			toXMLFile.println("\t\t\t\t<response match=\""+correctAnswer+"\" score=\""+Integer.toString(pointsPerQ)+"\">\n"+
                			"\t\t\t\t\t<feedback>Correct!</feedback>\n"+
           					"\t\t\t\t</response>");
           	correctAnswer=""; //once it's used reset it so it doesn't end up carrying over to the next one if the next one has none given
        }
        toXMLFile.println("\t\t\t\t<response match=\"*\" score=\"0\">\n"+
                		"\t\t\t\t\t<feedback>Incorrect.</feedback>\n"+
           				"\t\t\t\t</response>\n"+
        				"\t\t\t</part>");

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