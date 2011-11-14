package sonumina.wordnet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import ontologizer.go.Term;
import ontologizer.go.TermContainer;

public class WordNetParser
{
	public static TermContainer parserWordnet(String fileName) throws IOException
	{
		BufferedReader in = new BufferedReader(new FileReader(new File(fileName)));
		
		String line;
		
		while ((line = in.readLine()) != null)
		{
			if (line.length() == 0) continue;
			if (!Character.isDigit(line.charAt(0))) continue;

			String [] entries = line.split(" ");

			String id = entries[0];
			
			int wCnt = Integer.parseInt(entries[3],16);
			ArrayList<String> words = new ArrayList<String>();
			int wCur = 4;
			for (int i=0;i<wCnt;i++)
			{
				String word = entries[wCur];
				String lexId = entries[wCur+1];
				words.add(word);
				wCur+=2;
			}
			
			int pCnt = Integer.parseInt(entries[wCur]);
			int pCur = wCur + 1;
			for (int i=0;i<pCnt;i++)
			{
				String pointerSymb = entries[pCur];

				String synsetOffset = entries[pCur+1];
				String pos = entries[pCur+2];
				String st = entries[pCur+3];
				pCur += 4;
			}

			String glos = null;
			
			if (entries[pCur].equals("|"))
			{
				int pos = line.indexOf('|');
				if (pos > 0 && line.length() > pos + 2)
					glos = line.substring(pos + 2);
			}


			System.out.println(id + "  " + words.get(0) + " " + pCnt + " " + entries[pCur] + " " + glos);
		}

		

		return null;
	}
}
