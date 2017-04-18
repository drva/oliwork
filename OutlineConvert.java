//args 0 is the outline text file
//system.out outputs the org file text

import java.io.*;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class OutlineConvert
{
	public static String regExUnit = "Unit \\d+:: ([\\s\\S]+?)\\s*";
	public static String regExModule = "Module \\d+/ ([\\s\\S]+?)\\s*";
	
	public static void main(String[] args) throws IOException
	{
		Scanner fromTextFile = new Scanner(new File(args[0]+".txt"));
		
		//flags for open tags
		boolean openUnit=false;
		boolean openModule=false;
		boolean openPage=false;
		
		String hold;
		Pattern pattern;
		Matcher matcher;
		String id;
		String title;
		while(fromTextFile.hasNext())
		{
			hold = fromTextFile.nextLine();
			
			//units
			if(hold.matches(regExUnit))
			{
				//close tags if needed
				if(openPage)
				{
					closePage();
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
			}
			
			//modules (which atm are also pages)
			if(hold.matches(regExModule))
			{
				//close tags if needed
				if(openPage)
				{
					closePage();
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
				
				System.out.println("\t\t\t\t<module id=\""+id+"\">");
				System.out.println("\t\t\t\t\t<title>"+title+"</title>");
				openModule = true;
				
				//page
				System.out.println("\t\t\t\t<item>\n"+
									"\t\t\t\t\t<resourceref idref=\""+id+"\"/>\n"+
									"\t\t\t\t</item>");
				openPage=true;
			}
		}
		
		//close tags if needed
		if(openPage)
		{
			closePage();
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
	
	public static void closePage()
	{
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