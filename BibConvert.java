//args 0 is the input file name

import java.io.*;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.regex.Matcher; 
import java.util.HashMap;

public class BibConvert
{
	//http://stackoverflow.com/questions/27498106/regular-expression-named-capturing-groups-support-in-java-7
	public static String regExArticle = "(?<authors>[\\s\\S]+?)\\s+\\((?<year>\\d\\d\\d\\d)\\)[\\.\\,]?\\s(?<title>[\\s\\S]+?)\\.\\s(?<journal>[\\s\\S]+?)[\\.\\,]\\s(?<volume>[\\d\\(\\)\\s]+)[\\,:;]\\s(?<pages>[p\\d\\-\\s–]+)\\.?"; //notes: the two dashes in pages are apparently different
	public static String regExArticleInBook = "(?<authors>[\\s\\S]+?)\\s+\\((?<year>\\d\\d\\d\\d)\\)[\\.\\,:]?\\s(?<title>[\\s\\S]+?)\\.\\sIn:?\\s(?<editors>[\\s\\S]+?)\\s\\(Eds?\\.\\)[\\,\\.:]\\s(?<booktitle>[\\s\\S]+?)(\\.|(\\s\\((?<pages>[p\\d\\-\\s–\\.]+)\\)\\.))\\s(?<address>[\\s\\S]+?):\\s(?<publisher>[\\s\\S]+?)\\.?\\s?";
	//Kohler, D. J., Brenner, L., & Griffin, D. (2002). The calibration of expert judgment: Heuristics and biases beyond the laboratory. In T. Gilovich, D. Griffin, & D. Kahneman (Eds.), Heuristics and biases: The psychology of intuitive judgment (pp. 686- 715). New York, NY: Cambridge University Press. 
	public static String regExBook2 = "(?<authors>[\\s\\S]+?)\\s+\\((?<year>\\d\\d\\d\\d)\\)[\\.\\,]?\\s(?<title>[\\s\\S]+?)\\.\\s(?<address>[\\s\\S]+?):\\s(?<publisher>[\\s\\S]+?)";
	//Dobelli, R. (2015). The Art of Thinking Clearly. New York, NY: HarperCollins
	public static String regExBook1 = "(?<authors>[\\s\\S]+?)\\s+\\((?<year>\\d\\d\\d\\d)\\)[\\.\\,]?\\s(?<title>[\\s\\S]+?)\\.\\s(?<publisher>[\\s\\S]+?)\\,\\s(?<address>[\\s\\S]+?)";
	//Kahneman, D. (2011). Thinking, Fast and Slow. Penguin Group, London
	//^this is currently overmatching the 'article in book'. Also to the one with the dx.doi webaddress but that seems fairly acceptable
	public static void main(String[] args) throws IOException
	{
		
		Scanner fromTextFile = new Scanner(new File(args[0]+".txt"));
		PrintWriter toXMLFile = new PrintWriter(args[0]+".xml");
		
		//making it a page so I can check it out in OLI
		toXMLFile.print("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"+
								"<!DOCTYPE workbook_page PUBLIC \"-//Carnegie Mellon University//DTD Workbook Page 3.8//EN\" \"http://oli.web.cmu.edu/dtd/oli_workbook_page_3_8.dtd\">\n"+
								"<?xml-stylesheet type=\"text/css\" href=\"http://oli.web.cmu.edu/authoring/oxy-author/oli_workbook_page_3_7.css\"?>\n"+
								"<workbook_page\n"+ 
								"\tid=\""+ncName(args[0])+"\"\n"+
								"\txmlns:bib=\"http://bibtexml.sf.net/\">\n"+
								"\t<head>\n"+
								"\t\t<title>"+args[0]+"</title>\n"+
								"\t</head>\n"+
								"\t<body>\n"+
								"\t\t<p>(deliberately left blank)</p>\n"+
								"\t</body>\n"+
								"\t<bib:file>\n");
		
		HashMap<String, Integer> authorsForId = new HashMap<String, Integer>();
		String hold="";
		Pattern pattern;
		Matcher matcher;
		String protoId;
		String id;
		int countCites =0;
		int countHandled=0;
		//go through the text file
		while(fromTextFile.hasNext())
		{
			hold = fromTextFile.nextLine();
			countCites++;
			
			if(hold.contains("@@@")) //currently using @@@ to split off stuff like 'example adapted from' that I'm currently ignoring
				hold=hold.split("@@@")[1];
				
			//articles
			if(hold.matches(regExArticle))
			{
				countHandled++;
				
				pattern = Pattern.compile(regExArticle);
				matcher = pattern.matcher(hold);
				matcher.matches();
				
				//at the moment using the first author's last name, with numbering using the hashmap to keep track to avoid repetitions
				protoId = matcher.group("authors").split(",")[0]; 
				id = makeEntryId(protoId, authorsForId);
				
				toXMLFile.println("\t\t<bib:entry id=\""+id+"\">");
				toXMLFile.println("\t\t\t<bib:article>");
				toXMLFile.println("\t\t\t\t<bib:author>"+xmlifyContent(matcher.group("authors"))+"</bib:author>");
				toXMLFile.println("\t\t\t\t<bib:title>"+xmlifyContent(matcher.group("title"))+"</bib:title>");
				toXMLFile.println("\t\t\t\t<bib:journal>"+xmlifyContent(matcher.group("journal"))+"</bib:journal>");
				toXMLFile.println("\t\t\t\t<bib:year>"+xmlifyContent(matcher.group("year"))+"</bib:year>");
				toXMLFile.println("\t\t\t\t<bib:volume>"+xmlifyContent(matcher.group("volume"))+"</bib:volume>");
				toXMLFile.println("\t\t\t\t<bib:pages>"+xmlifyContent(matcher.group("pages"))+"</bib:pages>");
				toXMLFile.println("\t\t\t</bib:article>");
				toXMLFile.println("\t\t</bib:entry>");
				
				continue;
			}
			
			//articles in book (before book to avoid book overmatching)
			if(hold.matches(regExArticleInBook))
			{
				countHandled++;
				
				pattern = Pattern.compile(regExArticleInBook);
				matcher = pattern.matcher(hold);
				matcher.matches();
				
				//at the moment using the first author's last name, with numbering using the hashmap to keep track to avoid repetitions
				protoId = matcher.group("authors").split(",")[0]; 
				id = makeEntryId(protoId, authorsForId);
				
				toXMLFile.println("\t\t<bib:entry id=\""+id+"\">");
				toXMLFile.println("\t\t\t<bib:inbook>");
				toXMLFile.println("\t\t\t\t<bib:author>"+xmlifyContent(matcher.group("authors"))+"</bib:author>");
				toXMLFile.println("\t\t\t\t<bib:title>"+xmlifyContent(matcher.group("title"))+"</bib:title>");
				toXMLFile.println("\t\t\t\t<bib:booktitle>"+xmlifyContent(matcher.group("booktitle"))+"</bib:booktitle>");
				toXMLFile.println("\t\t\t\t<bib:year>"+xmlifyContent(matcher.group("year"))+"</bib:year>");
				toXMLFile.println("\t\t\t\t<bib:editor>"+xmlifyContent(matcher.group("editors"))+"</bib:editor>");
				if(matcher.group("pages")!=null)
					toXMLFile.println("\t\t\t\t<bib:pages>"+xmlifyContent(matcher.group("pages"))+"</bib:pages>");
				toXMLFile.println("\t\t\t\t<bib:publisher>"+xmlifyContent(matcher.group("publisher"))+"</bib:publisher>");
				toXMLFile.println("\t\t\t\t<bib:address>"+xmlifyContent(matcher.group("address"))+"</bib:address>");
				toXMLFile.println("\t\t\t</bib:inbook>");
				toXMLFile.println("\t\t</bib:entry>");
				
				continue;
			}
			
			//books
			if(hold.matches(regExBook2) || hold.matches(regExBook1))
			{
				countHandled++;
				
				if(hold.matches(regExBook2)) //this is first to deal with some overmatching
					pattern = Pattern.compile(regExBook2);
				else
					pattern = Pattern.compile(regExBook1);
				matcher = pattern.matcher(hold);
				matcher.matches();
				
				//at the moment using the first author's last name, with numbering using the hashmap to keep track to avoid repetitions
				protoId = matcher.group("authors").split(",")[0]; 
				id = makeEntryId(protoId, authorsForId);
				
				toXMLFile.println("\t\t<bib:entry id=\""+id+"\">");
				toXMLFile.println("\t\t\t<bib:book>");
				if(matcher.group("authors").matches("[\\s\\S]+? \\(ed\\)")) //if it caught an editor in the authors space
					toXMLFile.println("\t\t\t\t<bib:editor>"+xmlifyContent(matcher.group("authors"))+"</bib:editor>");
				else
					toXMLFile.println("\t\t\t\t<bib:author>"+xmlifyContent(matcher.group("authors"))+"</bib:author>");
				toXMLFile.println("\t\t\t\t<bib:title>"+xmlifyContent(matcher.group("title"))+"</bib:title>");
				toXMLFile.println("\t\t\t\t<bib:publisher>"+xmlifyContent(matcher.group("publisher"))+"</bib:publisher>");
				toXMLFile.println("\t\t\t\t<bib:year>"+xmlifyContent(matcher.group("year"))+"</bib:year>");
				toXMLFile.println("\t\t\t\t<bib:address>"+xmlifyContent(matcher.group("address"))+"</bib:address>");
				toXMLFile.println("\t\t\t</bib:book>");
				toXMLFile.println("\t\t</bib:entry>");
				
				continue;
			}
			
			System.out.println(hold);
		}
		
		toXMLFile.println("\t</bib:file>\n"+
							"</workbook_page>");
		toXMLFile.close();
		
		System.out.println(countHandled+" citations handled out of "+countCites+". "+(countCites-countHandled)+" not handled.");
	}
	
	public static String ncName(String fixCharacters) //taken from OutlineConvert
	{
		//http://stackoverflow.com/questions/1631396/what-is-an-xsncname-type-and-when-should-it-be-used
		//"The practical restrictions of NCName are that it cannot contain several symbol characters like :, @, $, %, &, /, +, ,, ;, whitespace characters or different parenthesis. Furthermore an NCName cannot begin with a number, dot or minus character although they can appear later in an NCName."
		if(fixCharacters.substring(0,1).matches("[0-9\\.\\-]"))
			fixCharacters = "_"+fixCharacters;
		
		fixCharacters = fixCharacters.replaceAll("[:@\\$%&\\/\\+,;\\s\\(\\)\\[\\]\\{\\}]", "");
		fixCharacters = fixCharacters.replaceAll("[’\\?]",""); //not sure if ncname but xml didn't like it
		
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
	
	public static String makeEntryId(String protoId, HashMap<String, Integer> authorsForId)
	{
		protoId = ncName(protoId);
		
		if(authorsForId.get(protoId) != null) //if we've already used this author
		{
			authorsForId.put(protoId, authorsForId.get(protoId)+1); //update the map
			return protoId + "_" +Integer.toString(authorsForId.get(protoId));
		}
		else //if this is our first time using this author
		{
			authorsForId.put(protoId, 1); 
			return protoId + "_1";
		}
	}
}