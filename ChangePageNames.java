//args[0] is the directory with the files whose names need changing

import java.io.*;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class ChangePageNames
{
	public static void main(String[] args)
	{
		File[] directoryList = new File(args[0]).listFiles(); //https://stackoverflow.com/questions/4917326/how-to-iterate-over-the-files-of-a-certain-directory-in-java
		for(int i=0; i<directoryList.length; i++)
		{
			System.out.println(directoryList[i].getName());
		}
	}
}