//args 0 is the input file name

//@@@ in text file sets off things at beginning of citations to be ignored (like 'Source: ')
//some info on the bib xml format: http://cnx.org/contents/LFmTVwBw@8/CNXML-Reference-Extensions

import java.io.*;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.regex.Matcher; 
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Arrays;

public class BibConvert
{
	//http://stackoverflow.com/questions/27498106/regular-expression-named-capturing-groups-support-in-java-7
	public static String regExArticle = "(?<authors>[\\s\\S]+?)\\s+\\((?<year>\\d\\d\\d\\d)\\)[\\.\\,]?\\s(?<title>[\\s\\S]+?)\\.\\s(?<journal>[\\s\\S]+?)[\\.\\,]\\s(?<volume>[\\d\\(\\)\\s]+)[\\,:;]\\s(?<pages>[p\\d\\-\\s–]+)\\.?"; //notes: the two dashes in pages are apparently different
	public static String regExArticleInBook1 = "(?<authors>[\\s\\S]+?)\\s+\\((?<year>\\d\\d\\d\\d)\\)[\\.\\,:]?\\s(?<title>[\\s\\S]+?)\\.\\s[Ii]n:?\\s(?<editors>[\\s\\S]+?)\\s\\(Eds?\\.\\)[\\,\\.:]\\s(?<booktitle>[\\s\\S]+?)(\\.|(\\s\\((?<pages>[p\\d\\-\\s–\\.]+)\\)\\.))\\s(?<address>[\\s\\S]+?):\\s(?<publisher>[\\s\\S]+?)\\.?\\s?";
	//Kohler, D. J., Brenner, L., & Griffin, D. (2002). The calibration of expert judgment: Heuristics and biases beyond the laboratory. In T. Gilovich, D. Griffin, & D. Kahneman (Eds.), Heuristics and biases: The psychology of intuitive judgment (pp. 686- 715). New York, NY: Cambridge University Press. 
	public static String regExArticleInBook2 = "(?<authors>[\\s\\S]+?)\\s+\\((?<year>\\d\\d\\d\\d)\\)[\\.\\,:]?\\s(?<title>[\\s\\S]+?)\\.\\s[Ii]n:?\\s(?<editors>[\\s\\S]+?)\\s\\(Eds?\\.\\)[\\,\\.:]\\s(?<booktitle>[\\s\\S]+?)(\\.|(\\s\\((?<pages>[p\\d\\-\\s–\\.]+)\\)\\.))\\s(?<publisher>[\\s\\S]+?)\\,\\s(?<address>[\\s\\S]+?)\\.?\\s?";
	public static String regExBook2 = "(?<authors>[\\s\\S]+?)\\s+\\((?<year>\\d\\d\\d\\d)\\)[\\.\\,]?\\s(?<title>[\\s\\S]+?)\\.\\s(?<address>[\\s\\S]+?):\\s(?<publisher>[\\s\\S]+?)";
	//Dobelli, R. (2015). The Art of Thinking Clearly. New York, NY: HarperCollins
	public static String regExBook1 = "(?<authors>[\\s\\S]+?)\\s+\\((?<year>\\d\\d\\d\\d)\\)[\\.\\,]?\\s(?<title>[\\s\\S]+?)\\.\\s(?<publisher>[\\s\\S]+?)\\,\\s(?<address>[\\s\\S]+?)";
	//Kahneman, D. (2011). Thinking, Fast and Slow. Penguin Group, London
	//{has been fixed with addition of article in book handling}->^this is currently overmatching the 'article in book'. 
	//Also to the one with the dx.doi webaddress but that seems fairly acceptable
	public static String regExAuthorYearTitleHow = "(?<authors>[\\s\\S]+?)\\s+\\((?<year>\\d\\d\\d\\d)\\)[\\.\\,]?\\s(?<title>[\\s\\S]+?)\\.\\s(?<moreinfo>[\\s\\S]+?)\\.?";
	
	//for partial processing
	public static String regExBeginningForPartial = "(?<pre>[\\s\\S]*?)";
	public static String regExEndForPartial = "(?<post>[\\s\\S]*)";
	public static String regExYearForPartial = "(?<year>\\d\\d\\d\\d)";
	public static String regExMonthForPartial = "(?<month>(\\d{1,2}\\s)?((January|February|March|April|May|June|July|August|September|October|November|December)|((Jan|Feb|Mar|Apr|Jun|Jul|Aug|Sep|Sept|Oct|Nov|Dec).?))(\\s\\d{1,2})?)"; //day is included with month since there's no other space for it
	public static String regExTitleForPartial = "(?<title>[\"“”][\\s\\S]+?[\"”])"; //for typos purposes I am permitting a close quote beginning a quote
	public static String regExURLForPartial = "(?<url>[\\S]*?(http|://|www.)[\\S]+)";
	
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
		ArrayList<String> notHandled = new ArrayList<String>();
		String authorYearTitleHow="";
		String partialProcessing="";
		String hold="";
		String hold2="";
		String temp="";
		String partialInProgress="";
		Pattern pattern;
		Matcher matcher;
		String protoId;
		String id;
		int countCites =0;
		int countHandled=0;
		int countPartial=0;
		//go through the text file
		while(fromTextFile.hasNext())
		{
			hold = fromTextFile.nextLine();
			hold = hold.trim(); //leading and trailing whitespace
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
			if(hold.matches(regExArticleInBook1) || hold.matches(regExArticleInBook2))
			{
				countHandled++;
				
				if(hold.matches(regExArticleInBook1)) 
					pattern = Pattern.compile(regExArticleInBook1);
				else
					pattern = Pattern.compile(regExArticleInBook2);
				matcher = pattern.matcher(hold);
				matcher.matches();
				
				//at the moment using the first author's last name, with numbering using the hashmap to keep track to avoid repetitions
				protoId = matcher.group("authors").split(",")[0]; 
				id = makeEntryId(protoId, authorsForId);
				
				toXMLFile.println("\t\t<bib:entry id=\""+id+"\">");
				toXMLFile.println("\t\t\t<bib:incollection>");
				toXMLFile.println("\t\t\t\t<bib:author>"+xmlifyContent(matcher.group("authors"))+"</bib:author>");
				toXMLFile.println("\t\t\t\t<bib:title>"+xmlifyContent(matcher.group("title"))+"</bib:title>");
				toXMLFile.println("\t\t\t\t<bib:booktitle>"+xmlifyContent(matcher.group("booktitle"))+"</bib:booktitle>");
				toXMLFile.println("\t\t\t\t<bib:publisher>"+xmlifyContent(matcher.group("publisher"))+"</bib:publisher>");
				toXMLFile.println("\t\t\t\t<bib:year>"+xmlifyContent(matcher.group("year"))+"</bib:year>");
				toXMLFile.println("\t\t\t\t<bib:editor>"+xmlifyContent(matcher.group("editors"))+"</bib:editor>");
				if(matcher.group("pages")!=null)
					toXMLFile.println("\t\t\t\t<bib:pages>"+xmlifyContent(matcher.group("pages"))+"</bib:pages>");
				toXMLFile.println("\t\t\t\t<bib:address>"+xmlifyContent(matcher.group("address"))+"</bib:address>");
				toXMLFile.println("\t\t\t</bib:incollection>");
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
			
			//author-year-title-howpublished
			if(hold.matches(regExAuthorYearTitleHow))
			{
				countPartial++;
				
				pattern = Pattern.compile(regExAuthorYearTitleHow);
				matcher = pattern.matcher(hold);
				matcher.matches();
				
				//at the moment using the first author's last name, with numbering using the hashmap to keep track to avoid repetitions
				protoId = matcher.group("authors").split(",")[0]; 
				id = makeEntryId(protoId, authorsForId);
				
				//these are written into a string to be put in the file all together later
				authorYearTitleHow = authorYearTitleHow +"\t\t<bib:entry id=\""+id+"\">\n";
				//was going to code as articles atm because most of the ones in this format in the current file seem to be (not all), but that turns out to require a journal item.
				authorYearTitleHow = authorYearTitleHow +"\t\t\t<bib:misc>\n";
				authorYearTitleHow = authorYearTitleHow +"\t\t\t\t<bib:author>"+xmlifyContent(matcher.group("authors"))+"</bib:author>\n";
				authorYearTitleHow = authorYearTitleHow +"\t\t\t\t<bib:title>"+xmlifyContent(matcher.group("title"))+"</bib:title>\n";
				authorYearTitleHow = authorYearTitleHow +"\t\t\t\t<bib:howpublished>"+xmlifyContent(matcher.group("moreinfo"))+"</bib:howpublished>\n";
				authorYearTitleHow = authorYearTitleHow +"\t\t\t\t<bib:year>"+xmlifyContent(matcher.group("year"))+"</bib:year>\n";
				authorYearTitleHow = authorYearTitleHow +"\t\t\t</bib:misc>\n";
				authorYearTitleHow = authorYearTitleHow +"\t\t</bib:entry>\n";
				
				continue;
			}
			
			//partial processing
				//note that all of these will only grab the first instance of the looked for pattern, but this is desired since there can only be one instance of each tag
			hold2 = hold; //need it in a different string since I might modify it
			if(hold2.matches(regExForLineMatch(regExTitleForPartial)))//matches matches against the whole line, so the linematch function handles that
			{
				pattern = Pattern.compile(regExForLineMatch(regExTitleForPartial));
				matcher = pattern.matcher(hold2);
				matcher.matches();
				
				hold2 = matcher.group("pre")+matcher.group("post"); //take out the target, turn the rest back into a string
				partialInProgress = partialInProgress+"\t\t\t\t<bib:title>"+xmlifyContent(matcher.group("title"))+"</bib:title>\n";
			}
			if(hold2.matches(regExForLineMatch(regExURLForPartial)))//matches matches against the whole line, so the linematch function handles that
			{
				pattern = Pattern.compile(regExForLineMatch(regExURLForPartial));
				matcher = pattern.matcher(hold2);
				matcher.matches();
				
				hold2 = matcher.group("pre")+matcher.group("post"); //take out the target, turn the rest back into a string
				partialInProgress = partialInProgress+"\t\t\t\t<bib:howpublished>"+xmlifyContent(matcher.group("url"))+"</bib:howpublished>\n"; //putting url in howpublished since note will be occupied
			}
				//split is because I don't want this to grab retrieved-on dates
				//I need to print month first because xml rules, but grab year first because otherwise month can overmatch something like March 2012
			if(hold2.split("[Rr]etrieved on")[0].matches(regExForLineMatch(regExYearForPartial)))//matches matches against the whole line, so the linematch function handles that
			{
				pattern = Pattern.compile(regExForLineMatch(regExYearForPartial));
				matcher = pattern.matcher(hold2);
				matcher.matches();
				
				hold2 = matcher.group("pre")+matcher.group("post"); //take out the target, turn the rest back into a string
				temp = "\t\t\t\t<bib:year>"+xmlifyContent(matcher.group("year"))+"</bib:year>\n";
			}
			if(hold2.split("[Rr]etrieved on")[0].matches(regExForLineMatch(regExMonthForPartial)))//matches matches against the whole line, so the linematch function handles that
			{
				pattern = Pattern.compile(regExForLineMatch(regExMonthForPartial));
				matcher = pattern.matcher(hold2);
				matcher.matches();
				
				hold2 = matcher.group("pre")+matcher.group("post"); //take out the target, turn the rest back into a string
				partialInProgress = partialInProgress+"\t\t\t\t<bib:month>"+xmlifyContent(matcher.group("month"))+"</bib:month>\n";
				
				partialInProgress = partialInProgress+temp; //if there was not year temp is just empty string
				temp=""; //reset it
			}
			if(!hold2.equals(hold)) //so, some partial processing was done
			{
				countPartial++;
				
				partialInProgress = partialInProgress+"\t\t\t\t<bib:note>"+xmlifyContent(hold2)+"</bib:note>\n";
				
				protoId = forMiscProtoId(hold2);
				id = makeEntryId(protoId, authorsForId);
				
				//these are written into a string to be put in the file all together later
				partialProcessing = partialProcessing +"\t\t<bib:entry id=\""+id+"\">\n";
				partialProcessing = partialProcessing +"\t\t\t<bib:misc>\n";
				partialProcessing = partialProcessing + partialInProgress;
				partialProcessing = partialProcessing +"\t\t\t</bib:misc>\n";
				partialProcessing = partialProcessing +"\t\t</bib:entry>\n";
				
				partialInProgress = ""; //clear it for next time
				
				continue;
			}
			
			notHandled.add(hold);
			System.out.println(hold);
		}
		
		toXMLFile.println();
		
		toXMLFile.println(authorYearTitleHow);
		
		toXMLFile.println(partialProcessing);
		
		//put anything not handled in a misc entry as a note
		for(int i=0; i<notHandled.size(); i++)
		{
			protoId = forMiscProtoId(notHandled.get(i));
				
			id = makeEntryId(protoId, authorsForId);
			
			toXMLFile.println("\t\t<bib:entry id=\""+id+"\">");
			toXMLFile.println("\t\t\t<bib:misc>");
			toXMLFile.println("\t\t\t\t<bib:note>"+xmlifyContent(notHandled.get(i))+"</bib:note>");
			toXMLFile.println("\t\t\t</bib:misc>");
			toXMLFile.println("\t\t</bib:entry>");
		}
		
		toXMLFile.println("\t</bib:file>\n"+
							"</workbook_page>");
		toXMLFile.close();
		
		System.out.println(countHandled+" citations handled, "+countPartial+" citations partially handled out of "+countCites+". "+(countCites-countHandled-countPartial)+" not handled.");
	}
	
	public static String ncName(String fixCharacters) //taken from OutlineConvert
	{
		//http://stackoverflow.com/questions/1631396/what-is-an-xsncname-type-and-when-should-it-be-used
		//"The practical restrictions of NCName are that it cannot contain several symbol characters like :, @, $, %, &, /, +, ,, ;, whitespace characters or different parenthesis. Furthermore an NCName cannot begin with a number, dot or minus character although they can appear later in an NCName."
		if(fixCharacters.substring(0,1).matches("[0-9\\.\\-]"))
			fixCharacters = "_"+fixCharacters;
		
		fixCharacters = fixCharacters.replaceAll("[:@\\$%&\\/\\+,;\\s\\(\\)\\[\\]\\{\\}]", "");
		fixCharacters = fixCharacters.replaceAll("[’\\?]",""); //not sure if ncname but xml didn't like it
		fixCharacters = fixCharacters.replaceAll("[\"\']",""); //added here, xml didn't like either
		
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
		protoId = protoId.replaceAll("\\.", "");
		
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
	
	public static String forMiscProtoId(String line)
	{
		String[] forMiscId = line.split("[\\.\\,;:\\?…\\s]+");
		//it looks like if one of the things to split at is first the first array element might be empty string. So, move them all down one
		while(forMiscId[0].equals(""))
		{
			for(int i=0; i<forMiscId.length-1; i++)
			{
				forMiscId[i] = forMiscId[i+1];
			}
			forMiscId = Arrays.copyOf(forMiscId, forMiscId.length-1);
		}
		String protoId;
			
		//if the first two 'words' for start with a capital letter (so, possibly the author), use them as the proto id
		//otherwise, use the first non-beginning word that does start with one
		//otherwise use the last word
		protoId = forMiscId[forMiscId.length-1];
		if(forMiscId[0].substring(0,1).matches("[A-Z]") && forMiscId.length>1 && forMiscId[1].substring(0,1).matches("[A-Z]"))
			protoId = forMiscId[0] + forMiscId[1];
		else
			for(int j=1; j<forMiscId.length; j++) //the first word often just starts with a capital letter even if not important, so unless it met the prior condition, we skip that one
			{
				if(forMiscId[j].substring(0,1).matches("[A-Z]"))
				{
					protoId = forMiscId[j];
					break;
				}
			}
		
		return protoId;	
	}
	
	public static String restringArray(String[] arrayToRestring)
	{
		String toReturn = "";
		for(int i = 0; i<arrayToRestring.length; i++)
		{
			toReturn = toReturn+arrayToRestring[i];
		}
		
		return toReturn;
	}
	
	public static String regExForLineMatch(String regex)
	{
		return regExBeginningForPartial+regex+regExEndForPartial;
	}
}