//args[0] is the directory with the files whose names need changing

import java.io.*;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class ChangePageNames
{
	public static String destinationDirectory = "namechangedAgainconvertedpages";
	
	public static void main(String[] args) throws IOException
	{
		File[] directoryList = new File(args[0]).listFiles(); //https://stackoverflow.com/questions/4917326/how-to-iterate-over-the-files-of-a-certain-directory-in-java
		for(int i=0; i<directoryList.length; i++)
		{
			makeNameChangedFile(directoryList[i]);
		}
	}
	
	public static String removeVowelsNotFirst(String toFix)
	{
		//I was thinking I couldn't remove u's because u marks the unit-name part of the filename, but no, I can just leave the first character alone and remove other u's
		String fixed="";
		char hold;
		fixed = fixed + toFix.charAt(0);
		for(int i=1; i<toFix.length(); i++)
		{
			hold = toFix.charAt(i);
			if(hold!='a' && hold!='e' && hold!='i' && hold!='o' && hold!='u')
				fixed=fixed+hold;
		}
		return fixed;
	}
	
	//changes the name of the file to the new version. Looks through file for more filenames and changes those too
	public static void makeNameChangedFile(File inputFile) throws IOException
	{		
		String newFilename = removeVowelsNotFirst(truncateUnitsModules(inputFile.getName()));
		PrintWriter toAFile = new PrintWriter(new File(destinationDirectory+"/"+newFilename)); //do I need to add .xml? No, already there
		
		String hasFilenameRegex = "(?<pre>[\\s\\S]*?\")(?<filename>u-\\S+?m-\\S+?p-\\S+?)(?<post>\"[\\s\\S]*)"; 
		String LORegex = "(?<pre>[\\s\\S]*?\")(?<filename>u-\\S+?m-\\S+?p-\\S+?)(?<islo>LO_[0-9]+)(?<post>\"[\\s\\S]*)"; 

		Scanner fromAFile = new Scanner(inputFile);
		String hold="";
		while(fromAFile.hasNext())
		{
			hold=fromAFile.nextLine();
			if(hold.matches(hasFilenameRegex) && !hold.matches(LORegex)) //not currently adjusting LO filenames, so excluding them
			{
				System.out.println(hold); //help me look out for it catching incorrect things
				
				Pattern pattern = Pattern.compile(hasFilenameRegex);
				Matcher matcher = pattern.matcher(hold);
				matcher.matches();
				
				toAFile.println(matcher.group("pre")+removeVowelsNotFirst(truncateUnitsModules(matcher.group("filename")))+matcher.group("post"));
			}
			else
				toAFile.println(hold);
		}
		
		fromAFile.close();
		toAFile.close();
	}
	
	//taking a filename made out of u-[unitname]_m-[modulename]_p-[pagename], truncates the former two to their first 'word'
	public static String truncateUnitsModules(String toFix)
	{
		String nameRegex = "u-(?<unitname>\\S+?)-m-(?<modulename>\\S+?)(?<pagesection>-p-\\S+)";
		Pattern pattern = Pattern.compile(nameRegex);
		Matcher matcher = pattern.matcher(toFix);
		Boolean doMatch = matcher.matches();
		
		if(!doMatch) //if the string given isn't in the right for, just give it back.
			return toFix;
		
		String truncatedUnitName = matcher.group("unitname").split("_")[0]; //note, these will produce an undesirable effect if the unitname/modulename currently starts with an underscore for some reason. 
		String truncatedModuleName = matcher.group("modulename").split("_")[0];
		
		//note I am currently *not* checking if this causes uniqueness problems or doing anything about this
		return "u-"+truncatedUnitName+"-m-"+truncatedModuleName+matcher.group("pagesection");
	}
}