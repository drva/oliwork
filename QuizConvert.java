//first argument is name of file. Spaces will be retained in title, converted to underscores in file name and id. Underscores will become spaces in title. xml characters are deleted
//alternative, first argument is name of directory
//second optional argument is max_attempts
//third optional argument is points per question
//fourth optional argument is whether to shuffle answers
//fifth optional argument is the folder with the images

//@@content@@ is bold (I made it __ but then realized that was used in fill in blanks)
//^^^content^^^ is superscript

//images that are anything other than 'there is one image and it is the last thing in body' need to be preprocessed in the text file
//format example: INSERTIMAGE: image5.05a.png CAPTION: SCREEN A where caption is optional
//^edit: images should now include the file extension to allow different ones

import java.io.*;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class QuizConvert
{
	public static int maxAttemptsDefault = 2;
	public static int pointsPerQDefault = 1;
	public static String shuffleDefault = "true";
	public static String imageFolderDefault ="quizimages";
	
	public static void main(String[] args) throws IOException
	{
		//if a max attempts is provided, use it, otherwise use the default
		String maxAttempts = Integer.toString(maxAttemptsDefault);
		if(args.length>1)
			maxAttempts = args[1];
		//likewise for points per q
		String pointsPerQ = Integer.toString(pointsPerQDefault);
		if(args.length>2)
			pointsPerQ = args[2];
		//and shuffling
		String shuffle = shuffleDefault;
		if(args.length>3 && (args[3].equals("true")||args[3].equals("false"))) //also check validity
			shuffle = args[3];
		//and quiz images folder
		String imageFolder = imageFolderDefault;
		if(args.length>4)
			imageFolder= args[4];
		
		File file = new File(args[0]+".txt");
		
		//if it's a file, process it
		if(file.exists())
			processFile(file, maxAttempts, pointsPerQ, shuffle, imageFolder);
		//otherwise figure it's a directory
		else
		{
			File[] fileList = new File(args[0]).listFiles(); //directories don't end in .txt
			for(int i=0; i<fileList.length; i++)
			{
				processFile(fileList[i], maxAttempts, pointsPerQ, shuffle, imageFolder);
			}
		}
	}
	
	public static void processFile(File inputFile, String maxAttempts, String pointsPerQ, String shuffle, String imageFolder) throws IOException
	{
		String inputFileName = inputFile.getName().substring(0, inputFile.getName().lastIndexOf("."));
		//xml characters
 		inputFileName = inputFileName.replaceAll("&", "and"); 
 		inputFileName = inputFileName.replaceAll("<", "");
 		inputFileName = inputFileName.replaceAll(">", "");
 		inputFileName = inputFileName.replaceAll("'", "");
 		inputFileName = inputFileName.replaceAll("\"", "");
		
		//if the input filename has spaces (in which case it would have been submitted in quotes to be a command line argument) replace them with underscores to use as output filename and id
		//also take care of some other undesired characters
		String fileId = idChars(inputFileName.replaceAll("\\s", "_"));
		
		Scanner fromTextFile = new Scanner(inputFile);
		PrintWriter toXMLFile= new PrintWriter(fileId+".xml");
			
		//underscore in input filename become spaces in the title of the output file
		String title = inputFileName.replaceAll("_", " ");
		
		//begin the xml file
		toXMLFile.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"+
							"<!DOCTYPE assessment\n"+
  							"PUBLIC \"-//Carnegie Mellon University//DTD Assessment 2.4//EN\" \"http://oli.web.cmu.edu/dtd/oli_assessment_2_4.dtd\">\n"+
							"<assessment id=\""+ fileId +"\" max_attempts=\""+maxAttempts+"\">\n"+
 							"\t<title>"+title+"</title>");
 							
 							
 		//a flag for having opened a question tag
 		boolean openQuestion=false;
 		//a flag for having opened an input tag
 		boolean openInputs=false;
 		//a flag for having an opened body tag
 		boolean openBody=false;
 		//a flag for having an opened choice tag
 		boolean openChoice=false;
 		
 		String rightAnswerId="";
 		String questionId="";
 		
 		String hold="";
 		Pattern idLine = Pattern.compile("[0-9]+\\.\\s+([a-zA-Z0-9_\\-\\.]+)\\s*"); //first whitespace has + incase of typos
 		Pattern answerIncorrect = Pattern.compile("([a-z])\\.\\s*([\\s\\S]+)"); //whitespace has a * in case of typos
 		Pattern answerCorrect = Pattern.compile("\\*([a-z])\\.\\s*([\\s\\S]+)");
 		Pattern body = Pattern.compile("\\S+[\\s\\S]*"); //will cause an issue if a body line starts with whitespace...
 		Pattern prepImage = Pattern.compile("INSERTIMAGE: (\\S+)");
 		Pattern prepImageAndCap = Pattern.compile("INSERTIMAGE: (\\S+) CAPTION: ([\\s\\S]+)");
 		Matcher matcher;
 		//processing the file
 		while(fromTextFile.hasNext())
 		{
 			hold = fromTextFile.nextLine();
 			//xml characters
 			hold = hold.replaceAll("&", "&amp;"); //goes first so it doesn't overwrite the others replacements after
 			hold = hold.replaceAll("<", "&lt;");
 			hold = hold.replaceAll(">", "&gt;");
 			hold = hold.replaceAll("'", "&apos;");
 			hold = hold.replaceAll("\"", "&quot;");
 			
 			//preprocessed images
 			matcher=prepImageAndCap.matcher(hold);
 			if(matcher.matches())
 			{
 				toXMLFile.println("\t\t\t<image src=\"../webcontent/"+matcher.group(1)+"\">");
 				toXMLFile.println("\t\t\t\t<caption>"+matcher.group(2)+"</caption>");
 				toXMLFile.println("\t\t\t</image>");
 				continue;
 			}
 			matcher=prepImage.matcher(hold);
 			if(matcher.matches())
 			{
 				toXMLFile.println("\t\t\t<image src=\"../webcontent/"+matcher.group(1)+"\"/>");
 				continue;
 			}
 			
 			//line with the question id (with handling of xml character things)
 			matcher=idLine.matcher(xmlifyId(hold));
 			if(matcher.matches())
 			{
 				//if our previous question's inputs are still open, close them, then handle the response part
 				if(openInputs)
 				{
					closeInputsAndDoResponse(openChoice, toXMLFile, rightAnswerId, pointsPerQ);
 					openInputs=false;
 					openChoice=false;
 				}
 				
 				//if our previous question is still open, close it
 				if(openQuestion)
 				{
 					closeQuestion(toXMLFile);
    				openQuestion=false;
 				}
 				questionId = matcher.group(1);
 				toXMLFile.println("\t<multiple_choice id=\""+"q"+matcher.group(1)+"\">"); //the q is there to make it an NCName
 				//open body tag
 				toXMLFile.println("\t\t<body>"); 
 				openBody=true;
 				openQuestion=true;
 				continue;
 			}
 			
 			//correct answer (for lack of a good way to do id's, I'm just using the letters)
 			matcher=answerCorrect.matcher(hold);
 			if(matcher.matches())
 			{
 				if(!openInputs) //if we haven't already opened the inputs tag, close body (including image handling) and open it
 				{
 					closeBody(toXMLFile, imageFolder, questionId);
 					openBody=false;
 					
 					toXMLFile.println("\t\t<input shuffle=\""+shuffle+"\">");
 					openInputs=true;
 				}
 				//if we have an open choice, close it
 				if(openChoice)
 				{
 					toXMLFile.println("\t\t\t</choice>");
 					openChoice=false;
 				}	
 				
 				rightAnswerId=matcher.group(1);
 					
 				toXMLFile.println("\t\t\t<choice value=\""+matcher.group(1)+"\">\n"+
 									"\t\t\t\t"+matcher.group(2));
 				openChoice=true;
 				continue;
 			}
 			
 			//incorrectAnswer
 			matcher=answerIncorrect.matcher(hold);
 			if(matcher.matches())
 			{
 				if(!openInputs) //if we haven't already opened the inputs tag, close body (including image handling) and open it
 				{
 					closeBody(toXMLFile, imageFolder, questionId);
 					openBody=false;
 					
 					toXMLFile.println("\t\t<input shuffle=\""+shuffle+"\">");
 					openInputs=true;
 				}
 				//if we have an open choice, close it
 				if(openChoice)
 				{
 					toXMLFile.println("\t\t\t</choice>");
 					openChoice=false;
 				}	
 					
 				toXMLFile.println("\t\t\t<choice value=\""+matcher.group(1)+"\">\n"+
 									"\t\t\t\t"+matcher.group(2));
 				openChoice=true;
 				continue;
 			}
 			
 			if(hold.matches("\\s+")) //space lines
 				continue;
 			
 			//body line (is last to be default)
 			//this now handles body lines and non-first-line choice lines
 			matcher=body.matcher(hold);
 			if(matcher.matches())
 			{
 				//the replaceAll handles bold and superscript
 				toXMLFile.println("\t\t\t\t<p>"+matcher.group().replaceAll("@@([\\s\\S]+)@@","<em style=\"bold\">$1</em>").replaceAll("\\^\\^\\^([\\s\\S]+)\\^\\^\\^","<sup>$1</sup>")+"</p>");
 				
 				//to enable multiple body lines, image processing and closing the body tag has been moved to inputs
 				continue;
 			}		
 		}
 		
 		//if we have open inputs or open question, take care of that
 		if(openInputs)
 		{
			closeInputsAndDoResponse(openChoice, toXMLFile, rightAnswerId, pointsPerQ);
 			openInputs=false;
 			openChoice=false;
 		}
 		if(openQuestion)
 		{
 			closeQuestion(toXMLFile);
    		openQuestion=false;
 		}					
 							
 		//end the xml file
 		toXMLFile.println("</assessment>");
		
		fromTextFile.close();
		toXMLFile.close();
	}
	
	public static void closeBody(PrintWriter toXMLFile, String imageFolder, String questionId)
	{
		//at the moment this converter can add in images in the following circumstances:
 		//-there is one image which goes directly after the question body and is followed immediately by the multiple choices
 		//-the images are saved with the format imageQUESTIONID.png, where QUESTIONID is the question id from the word file (as in 1.10, etc)
 		File image = new File(imageFolder+"/image"+questionId+".png");
 		if(image.exists())
 		{
 			toXMLFile.println("\t\t\t<image src=\"../webcontent/"+"image"+questionId+".png\"/>");
 		}
 		toXMLFile.println("\t\t</body>");
	}
	
	public static void closeQuestion(PrintWriter toXMLFile)  throws IOException
	{
		toXMLFile.println("\t</multiple_choice>\n");
	}
	
	public static void closeInputsAndDoResponse(boolean openChoice, PrintWriter toXMLFile, String rightAnswerId, String pointsPerQ) throws IOException
	{
		if(openChoice)
		{
			toXMLFile.println("\t\t\t</choice>");
		}
		toXMLFile.println("\t\t</input>");
		toXMLFile.println("\t\t<part>\n"+
        					"\t\t\t<response match=\""+rightAnswerId+"\" score=\""+pointsPerQ+"\">\n"+
          					"\t\t\t\t<feedback>Correct!</feedback>\n"+
        					"\t\t\t</response>\n"+
        					"\t\t\t<response match=\"*\">\n"+
          					"\t\t\t\t<feedback>Incorrect.</feedback>\n"+
        					"\t\t\t</response>\n"+
        					"\t\t</part>");
	}
	
	//ids need a different way of dealing with the xml characters
	public static String xmlifyId(String fixCharacters)
	{
		//xml characters
 		fixCharacters = fixCharacters.replaceAll("&amp;", "and"); 
 		fixCharacters = fixCharacters.replaceAll("&lt;", "");
 		fixCharacters = fixCharacters.replaceAll("&gt;", "");
 		fixCharacters = fixCharacters.replaceAll("&apos;", "");
 		fixCharacters = fixCharacters.replaceAll("&quot;", "");
 		
 		return fixCharacters;
	}
	
	//this is mostly borrowed from ncName in OutlineConvert, but modified
	public static String idChars(String fixCharacters)
	{
		//http://stackoverflow.com/questions/1631396/what-is-an-xsncname-type-and-when-should-it-be-used
		//"The practical restrictions of NCName are that it cannot contain several symbol characters like :, @, $, %, &, /, +, ,, ;, whitespace characters or different parenthesis. Furthermore an NCName cannot begin with a number, dot or minus character although they can appear later in an NCName."
		if(fixCharacters.length()>0 && fixCharacters.substring(0,1).matches("[0-9\\.\\-]")) //the first part allows it to deal with empty strings without throwing exceptions
			fixCharacters = "_"+fixCharacters;
		
		fixCharacters = fixCharacters.replaceAll("&", "and"); 
		fixCharacters = fixCharacters.replaceAll("[:@\\$%\\/\\+,;\\s\\(\\)\\[\\]\\{\\}]", "");
		fixCharacters = fixCharacters.replaceAll("[’\\?]",""); //not sure if ncname but xml didn't like it
		fixCharacters = fixCharacters.replaceAll("\\.","_"); //raphael had said to avoid .'s
		
		return fixCharacters;
	}
}