//first argument is name of file. Spaces will be retained in title, converted to underscores in file name and id. Underscores will become spaces in title
//alternative, first argument is name of directory
//second optional argument is max_attempts
//third optional argument is points per question
//fourth optional argument is whether to shuffle answers
//fifth optional argument is the folder with the images

import java.io.*;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class QuizConvert
{
	public static int maxAttemptsDefault = 2;
	public static int pointsPerQDefault = 3;
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
		
		//if the input filename has spaces (in which case it would have been submitted in quotes to be a command line argument) replace them with underscores to use as output filename and id
		String fileId = inputFileName.replaceAll("\\s", "_");
		
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
 		
 		String rightAnswerId="";
 		String questionId="";
 		
 		String hold="";
 		Pattern idLine = Pattern.compile("[0-9]+\\.\\s+([0-9\\.]+)\\s*"); //first whitespace has + incase of typos
 		Pattern answerIncorrect = Pattern.compile("([a-z])\\.\\s*([\\s\\S]+)"); //whitespace has a * in case of typos
 		Pattern answerCorrect = Pattern.compile("\\*([a-z])\\.\\s*([\\s\\S]+)");
 		Pattern body = Pattern.compile("\\S+[\\s\\S]*"); //will cause an issue if a body line starts with whitespace...
 		Matcher matcher;
 		//processing the file
 		while(fromTextFile.hasNext())
 		{
 			hold = fromTextFile.nextLine();
 			
 			//line with the question id
 			matcher=idLine.matcher(hold);
 			if(matcher.matches())
 			{
 				//if our previous question's inputs are still open, close them, then handle the response part
 				if(openInputs)
 				{
					closeInputsAndDoResponse(toXMLFile, rightAnswerId, pointsPerQ);
 					openInputs=false;
 				}
 				
 				//if our previous question is still open, close it
 				if(openQuestion)
 				{
 					closeQuestion(toXMLFile);
    				openQuestion=false;
 				}
 				questionId = matcher.group(1);
 				toXMLFile.println("\t<multiple_choice id=\""+"q"+matcher.group(1)+"\">"); //the q is there to make it an NCName
 				openQuestion=true;
 				continue;
 			}
 			
 			//correct answer (for lack of a good way to do id's, I'm just using the letters)
 			matcher=answerCorrect.matcher(hold);
 			if(matcher.matches())
 			{
 				if(!openInputs) //if we haven't already opened the inputs tag, open it
 				{
 					toXMLFile.println("\t\t<input shuffle=\""+shuffle+"\">");
 					openInputs=true;
 				}
 				
 				rightAnswerId=matcher.group(1);
 					
 				toXMLFile.println("\t\t\t<choice value=\""+matcher.group(1)+"\">"+matcher.group(2)+"</choice>");
 				continue;
 			}
 			
 			//incorrectAnswer
 			matcher=answerIncorrect.matcher(hold);
 			if(matcher.matches())
 			{
 				if(!openInputs) //if we haven't already opened the inputs tag, open it
 				{
 					toXMLFile.println("\t\t<input shuffle=\""+shuffle+"\">");
 					openInputs=true;
 				}
 					
 				toXMLFile.println("\t\t\t<choice value=\""+matcher.group(1)+"\">"+matcher.group(2)+"</choice>");
 				continue;
 			}
 			
 			//body line (goes last because it'll overapply otherwise) (right now needs there to only be one)
 			matcher=body.matcher(hold);
 			if(matcher.matches())
 			{
 				toXMLFile.println("\t\t<body>"+matcher.group());
 				
 				//at the moment this converter can add in images in the following circumstances:
 				//-there is one image which goes directly after the question body and is followed immediately by the multiple choices
 				//-the images are saved with the format imageQUESTIONID.png, where QUESTIONID is the question id from the word file (as in 1.10, etc)
 				File image = new File(imageFolder+"/image"+questionId+".png");
 				if(image.exists())
 				{
 					toXMLFile.println("\t\t\t<image src=\"../webcontent/"+"image"+questionId+".png\"/>");
 				}
 				toXMLFile.println("\t\t</body>");
 				continue;
 			}		
 		}
 		
 		//if we have open inputs or open question, take care of that
 		if(openInputs)
 		{
			closeInputsAndDoResponse(toXMLFile, rightAnswerId, pointsPerQ);
 			openInputs=false;
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
	
	public static void closeQuestion(PrintWriter toXMLFile)  throws IOException
	{
		toXMLFile.println("\t</multiple_choice>\n");
	}
	
	public static void closeInputsAndDoResponse(PrintWriter toXMLFile, String rightAnswerId, String pointsPerQ) throws IOException
	{
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
}