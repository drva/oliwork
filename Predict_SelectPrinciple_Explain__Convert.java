//args 0 is the input file
//arg 1 is the optional 'directory' file that maps question sets to pages they should go on
//if it is not given everything is put in the same page


import java.io.*;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.awt.Image;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.HashMap;

public class Predict_SelectPrinciple_Explain__Convert
{
	public static PrintWriter toXMLFile = null;
	
	public static boolean multiFile = false;
	public static HashMap<Integer, String[]> pagesMapping = null;
	public static HashMap<Integer, PrintWriter> pagesMappingPW = null;
	
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
		Pattern pattern;
 		Matcher matcher;
 		String hold = "";
 		
 		if(args.length>=2) //set the flag for having a directory file
 			multiFile = true;
		
		if(!multiFile) //so, no directory file was given, just the input file
		{
			//so we only need one printwriter/one xml file
			toXMLFile= new PrintWriter(idBase+".xml");
			openFormativeAssessment(idBase);
		}
		else //we have a directory file and need to do things with the mapping
		{
			idBase = "pspe_"; //make the base shorter since it'll be concatenated
			
			pagesMapping = new HashMap<Integer, String[]>();
			pagesMappingPW = new HashMap<Integer, PrintWriter>();
			String regExMapLine = "(?<title>[\\s\\S]+?)\\s*\\((?<num1>\\d+)(&(?<num2>\\d+))?\\)";
			String[] holdArray = null; 
			PrintWriter holdPW = null;
			//process the directory
			Scanner fromMapping = new Scanner(new File(args[1]+".txt"));
			while(fromMapping.hasNext())
			{
				hold = fromMapping.nextLine().trim(); //leading and trailing whitespace
				if(!hold.matches(regExMapLine)) //skip non-matching lines
				{
					continue;
				}	
					 
				pattern = Pattern.compile(regExMapLine);
				matcher = pattern.matcher(hold);
				matcher.matches();
				
				if(matcher.group("num2")==null) //so, there is only one question set for this entry
				{
					holdArray = new String[2];
					//map from the question set number to an array to hold things
					pagesMapping.put(Integer.parseInt(matcher.group("num1")), holdArray);
					//the first thing is the 'title'
					holdArray[0] = matcher.group("title");
					//the second thing is the 'id form' of the title
					holdArray[1] = ncName(matcher.group("title").toLowerCase().replaceAll("\\s+", "_").replaceAll("\\.", ""));
					//the printwriter goes in its own hashmap since it's not a string
					pagesMappingPW.put(Integer.parseInt(matcher.group("num1")), new PrintWriter(idBase+holdArray[1]+".xml"));
				}
				else //there are two questions sets for this entry (that need to go in the same file)
				{	
					//the question sets need to map to the *same* array and printwriter
					//the array has an extra entry to be a flag later
					holdArray = new String[3];
					//map from the question set number to an array to hold things
					pagesMapping.put(Integer.parseInt(matcher.group("num1")), holdArray);
					pagesMapping.put(Integer.parseInt(matcher.group("num2")), holdArray);
					//the first thing is the 'title'
					holdArray[0] = matcher.group("title");
					//the second thing is the 'id form' of the title
					holdArray[1] = ncName(matcher.group("title").toLowerCase().replaceAll("\\s+", "_").replaceAll("\\.", ""));
					//the third thing will be the flag
					//the printwriter goes in its own hashmap since it's not a string
					holdPW = new PrintWriter(idBase+holdArray[1]+".xml");
					pagesMappingPW.put(Integer.parseInt(matcher.group("num1")), holdPW);
					pagesMappingPW.put(Integer.parseInt(matcher.group("num2")), holdPW);
				}
				//regardless, begin each xml file
				toXMLFile = pagesMappingPW.get(Integer.parseInt(matcher.group("num1"))); //it's fine to just use num1 since it there's a num2 it's the same printwriter
				openFormativeAssessment(idBase+holdArray[1]); 
			}
		}
 		
 		String regExBeginQuestionSet = "(?<setnum>\\d+)\\.\\s*(?<settitle>[\\s\\S]+)";
 		String regExBeginQuestion = "(?<qId>Q\\d+(?<qdesignation1>[a-z]))\\s*\\((?<qdesignation2>[\\s\\S]+?)\\):\\s*(?<question>[\\s\\S]+)";
 		String regExBodyCont = "(Assume:|\\d\\))[\\s\\S]*"; //this is not a good way of doing this. However, atm neither body-continuation nor choices are identified in any way. But, all body-continuation is either 'Assume:' or starts with '1)'. So using that.
 		
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
					closeSection();
				openSection(xmlifyContent(matcher.group("settitle")), matcher.group("setnum"));
				
				continue;
 			}
 			
 			//question opening lines
 			if(hold.matches(regExBeginQuestion))
 			{
 				pattern = Pattern.compile(regExBeginQuestion);
				matcher = pattern.matcher(hold);
				matcher.matches();
				
				if(flagOpenQuestion)
					closeQuestion();
				
				openQuestion(matcher.group("qId"), xmlifyContent("Part "+matcher.group("qdesignation1")+", "+matcher.group("qdesignation2")+": "+matcher.group("question")));
				
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
 					closeBody();
 				if(!flagOpenChoices)
 					openChoices();
 				
 				//identify the correct answer (which is marked by *)
 				if(hold.charAt(0)=='*')
 				{
 					isCorrectAnswer=true;
 					//hold = hold.substring(1, hold.length()); //now trim the * off
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
		
		if(!multiFile) //end the single file
		{
			if(flagOpenQuestion)
				closeQuestion();
			if(flagOpenSection)
				closeSection();
			closeFormativeAssessment();
		
			toXMLFile.close();
		}
		else //end each file
			//https://stackoverflow.com/questions/1066589/iterate-through-a-hashmap
			for(PrintWriter value : pagesMappingPW.values()) 
			{
				//I *think* doing this for-each like this should be fine (like, there shouldn't be some random file where this *isn't* open)
				toXMLFile = value;
				
				if(flagOpenQuestion)
					closeQuestion();
				if(flagOpenSection)
					closeSection();
				closeFormativeAssessment();
		
				value.close();
			}		
	}
	
	public static void openFormativeAssessment(String id) throws IOException
	{
		toXMLFile.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"+
					"<!DOCTYPE assessment\n"+
  					"PUBLIC \"-//Carnegie Mellon University//DTD Inline Assessment 1.0//EN\" \"http://oli.web.cmu.edu/dtd/oli_inline_assessment_1_0.dtd\">\n"+
					"<?xml-stylesheet type=\"text/css\" href=\"http://oli.web.cmu.edu/authoring/oxy-author/oli_inline_assessment_1_1.css\"?>\n"+
					"<assessment id=\""+id+"\">\n"+
					"\t<title>Check your work</title>");
	}
	
	public static void closeFormativeAssessment() throws IOException
	{
		toXMLFile.println("</assessment>");
	}
	
	public static void openSection(String title, String id) throws IOException
	{
		if(multiFile) //if we're working with multiple files we need to set the right PrintWriter for this section
		{
			toXMLFile = pagesMappingPW.get(Integer.parseInt(id));
		}
		
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
	
	public static void closeSection() throws IOException
	{
		if(flagOpenQuestion)
			closeQuestion();
		flagOpenSection = false;
	}
	
	public static void openQuestion(String id, String bodyContent) throws IOException
	{
		flagOpenQuestion = true;
		flagOpenBody = true;
		
		toXMLFile.println("\t\t<question id=\""+id+"\">");
		toXMLFile.println("\t\t\t<body>");
		toXMLFile.println("\t\t\t\t<p>"+bodyContent+"</p>");
	}
	
	//also calls close choices
	public static void closeQuestion() throws IOException
	{
		closeChoices();
		
		toXMLFile.println("\t\t</question>");
		flagOpenQuestion = false;
	}
	
	//body gets openeded within openQuestion, and so doesn't have its own method
	public static void closeBody() throws IOException
	{
		flagOpenBody = false;
		toXMLFile.println("\t\t\t</body>");
	}
	
	public static void openChoices() throws IOException
	{
		flagOpenChoices = true;
		
		toXMLFile.println("\t\t\t<multiple_choice select=\"single\" shuffle=\""+shuffle+"\">");
	}
	
	//also handles 'part'/responses etc
	public static void closeChoices() throws IOException
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
	
	public static String ncName(String fixCharacters) //taken from BinConvert
	{
		//http://stackoverflow.com/questions/1631396/what-is-an-xsncname-type-and-when-should-it-be-used
		//"The practical restrictions of NCName are that it cannot contain several symbol characters like :, @, $, %, &, /, +, ,, ;, whitespace characters or different parenthesis. Furthermore an NCName cannot begin with a number, dot or minus character although they can appear later in an NCName."
		if(fixCharacters.substring(0,1).matches("[0-9\\.\\-]"))
			fixCharacters = "_"+fixCharacters;
		
		fixCharacters = fixCharacters.replaceAll("[:@\\$%&\\/\\+,;\\s\\(\\)\\[\\]\\{\\}]", "");
		fixCharacters = fixCharacters.replaceAll("[’\\?]",""); //not sure if ncname but xml didn't like it
		fixCharacters = fixCharacters.replaceAll("[\"\']",""); //xml didn't like either
		
		return fixCharacters;
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