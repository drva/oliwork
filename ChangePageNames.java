//args[0] is the directory with the files whose names need changing
//args[1] is hash if you want to do the promoting-uniqueness hashing and anything else if you don't.
//args[2] is the destination

import java.io.*;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class ChangePageNames
{
	public static String destinationDirectory = "";
	public static boolean hashYN = false;
	
	public static void main(String[] args) throws IOException
	{
		if(args[1].equals("hash"))
			hashYN = true;
		destinationDirectory = args[2];
		
		File[] directoryList = new File(args[0]).listFiles(); //https://stackoverflow.com/questions/4917326/how-to-iterate-over-the-files-of-a-certain-directory-in-java
		for(int i=0; i<directoryList.length; i++)
		{
			makeNameChangedFile(directoryList[i]);
		}
	}
	
	public static String removeVowelsNotFirst(String toFix)
	{
		//a bit of a support for roman numerals having been used - will also catch other ii and iii's but those don't come up *too* often I think. Will still render iv and vi as v but those *do* come up often otherwise so making the tradeoff.
		toFix = toFix.replaceAll("iii|III","3");
		toFix = toFix.replaceAll("ii|II","2");
		
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
		//to promote uniqueness (though still doesn't guarantee), we can optionally add a number derived from hashing to the page names (and we likewise do this down below if we do it here).
		String numCode ="";
		if(hashYN)
			numCode = Integer.toString(Math.abs(inputFile.getName().split(".xml")[0].hashCode() % 1000)); //splitting here so it can match the in-file versions
			
		String newFilename = removeVowelsNotFirst(truncateUnitsModules(inputFile.getName())).split(".xml")[0] + numCode +".xml"; //need to split off and re-add the .xml so that the numcode if applicable goes in front of it
		//checking if the filename we want has already been used https://howtodoinjava.com/java/io/how-to-check-if-file-exists-in-java/
		File tempCheckFile = new File(destinationDirectory+"/"+newFilename);
		if(tempCheckFile.exists())
			System.out.println("!DUPLICATE " + inputFile.getName());
		
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
				
				if(hashYN)
					numCode = Integer.toString(Math.abs(matcher.group("filename").split(".xml")[0].hashCode() % 1000));
				toAFile.println(matcher.group("pre")+removeVowelsNotFirst(truncateUnitsModules(matcher.group("filename"))).split(".xml")[0]+numCode+matcher.group("post")); //doing the .xml split here too. It should be needed, but it's really important this match the filename version, including if by some chance there's a .xml in the middle of the string somewhere. This being here is why the split is done last; otherwise if there was a .xml in the middle a split version might not fit the filename structure.
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
		
		//String truncatedUnitName = matcher.group("unitname").split("_")[0]; //note, these will produce an undesirable effect if the unitname/modulename currently starts with an underscore for some reason. 
		//String truncatedModuleName = matcher.group("modulename").split("_")[0];
		
		//above noted problem with something already beginning with underscore happened, adjusting code to work better with this.
		String truncatedUnitName = wordFromUnderscoreString(matcher.group("unitname"));
		String truncatedModuleName = wordFromUnderscoreString(matcher.group("modulename"));
		
		
		//note I am currently *not* checking if this causes uniqueness problems or doing anything about this
		return "u-"+truncatedUnitName+"-m-"+truncatedModuleName+matcher.group("pagesection");
	}
	
	//job-doing function - taking a string of 'words' separated by underscores and returning the first non-empty one (for truncating unit and module names)
	public static String wordFromUnderscoreString(String toFix)
	{
		//split on underscore, look through resulting array and return first non blank. If for some reason they're all blank go ahead and return blank.
		String[] wordArray = toFix.split("_");
		for(int i=0; i<wordArray.length; i++)
			if(!wordArray[i].equals(""))
				return wordArray[i];
		
		return "";
	}
}