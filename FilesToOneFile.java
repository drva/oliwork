//args[0] is the directory with the files 
//args[1] will go in the name of the output file and be its root tag

import java.io.*;
import java.util.Scanner;

public class FilesToOneFile
{
	public static void main(String[] args) throws IOException
	{
		String whatsThis = args[1];
		
		File[] directoryList = new File(args[0]).listFiles(); //https://stackoverflow.com/questions/4917326/how-to-iterate-over-the-files-of-a-certain-directory-in-java
		Scanner fromAFile;
		PrintWriter toOneFile = new PrintWriter(new File("all"+whatsThis+".xml"));
		toOneFile.println("<"+whatsThis+">");
		
		//go through the files and copy them into our one file
		for(int i=0; i<directoryList.length; i++)
		{
			fromAFile = new Scanner(directoryList[i]);
			while(fromAFile.hasNext())
				toOneFile.println(fromAFile.nextLine());
			fromAFile.close();
		}
		
		toOneFile.println("</"+whatsThis+">");
		toOneFile.close();
	}
}