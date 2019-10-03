//args[0] is the directory with the files whose names need changing

import java.io.*;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class ChangePageNames
{
	public static String destinationDirectory = "namechangedconvertedpages";
	
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
	
	public static void makeNameChangedFile(File inputFile) throws IOException
	{
		String newFilename = removeVowelsNotFirst(inputFile.getName());
		PrintWriter toAFile = new PrintWriter(new File(destinationDirectory+"/"+newFilename)); //do I need to add .xml? No, already there
		
		Scanner fromAFile = new Scanner(inputFile);
		while(fromAFile.hasNext())
		{
			toAFile.println(fromAFile.nextLine());
		}
		
		fromAFile.close();
		toAFile.close();
	}
}