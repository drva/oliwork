//args[0] is list of assessment ids to make files with. args[1] is destination directory

import java.io.*;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.ArrayList;

public class MakePlaceholderActivities
{
	public static String destinationDirectory;
	public static String placeholderActivityTemplate = "PlaceholderActivityTemplate.xml";
	
	public static void main(String[] args) throws IOException
	{
		destinationDirectory = args[1];
		
		Scanner getAssessmentIds = new Scanner(new File(args[0]));
		ArrayList<String> assessmentIds = new ArrayList<String>();
		
		//get the list of assessment ids to make files for
		while(getAssessmentIds.hasNext())
			assessmentIds.add(getAssessmentIds.nextLine());
		
		getAssessmentIds.close();
		
		Scanner getTemplate = new Scanner(new File(placeholderActivityTemplate));
		String template="";
		
		//read the template into a string
		while(getTemplate.hasNext())
			template = template+getTemplate.nextLine()+"\n";
			
		getTemplate.close();
			
		PrintWriter makePlaceholders;
		
		//make a placeholder activity for each id
		for(int i=0; i<assessmentIds.size(); i++)
		{
			//make and name file
			makePlaceholders = new PrintWriter(new File(destinationDirectory+"/a_"+assessmentIds.get(i)+".xml"));
			//put in contents, putting in the right ids. making question id different than I do in the real ones since it doesn't need to be the same
			makePlaceholders.print(template.replaceAll("ASSESSMENT_ID_HERE", "a_"+assessmentIds.get(i)).replaceAll("QUESTION_ID_HERE", "x_"+assessmentIds.get(i)));
			makePlaceholders.close();
		}
	
	}
}