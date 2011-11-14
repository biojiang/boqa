package sonumina.wordnet;

import static org.junit.Assert.*;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.Test;

public class WordNetParserTest
{
	@Test
	public void testWordnetParser()
	{
		try {
			WordNetParser.parserWordnet("../b4o/data.noun");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
