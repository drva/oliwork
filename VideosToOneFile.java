//args[0] is the directory with the video files

import java.io.*;
import java.util.Scanner;

public class VideosToOneFile
{
	public static void main(String[] args) throws IOException
	{
		File[] directoryList = new File(args[0]).listFiles(); //https://stackoverflow.com/questions/4917326/how-to-iterate-over-the-files-of-a-certain-directory-in-java
		Scanner fromAFile;
		PrintWriter toOneFile = new PrintWriter(new File("allVideos.xml"));
		toOneFile.println("<videos>");
		
		//go through the files and copy them into our one file
		for(int i=0; i<directoryList.length; i++)
		{
			fromAFile = new Scanner(directoryList[i]);
			while(fromAFile.hasNext())
				toOneFile.println(fromAFile.nextLine());
			fromAFile.close();
		}
		
		toOneFile.println("</videos>");
		toOneFile.close();
	}
}