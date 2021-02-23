//args[0] should be where the srt's are
//args[1] should be the destination directory

import java.io.*;
import java.util.Scanner;

public class ConvertSRTs
{		
	public static String destinationDirectory="";
	public static String srtFileCheck="[\\s\\S]+\\.srt";
	
	public static void main(String[] args)
	{
		destinationDirectory = args[1];
		
		File[] directoryList = new File(args[0]).listFiles(); //https://stackoverflow.com/questions/4917326/how-to-iterate-over-the-files-of-a-certain-directory-in-java
		//send specifically the srt files to be converted
		for(int i=0; i<directoryList.length; i++)
		{
			if(directoryList[i].getName().matches(srtFileCheck))
				convertSRT(directoryList[i]);
		}
	}
	
	public static void convertSRT(File srtFile)
	{
		//get the file id part
		String fileID = srtFile.getName().split("static\\/|\\.srt")[srtFile.getName().split("static\\/|\\.srt").length-1];
		
		System.out.println(fileID);
	}
	
}