package sonumina.wordnet;

import static org.junit.Assert.*;

import org.junit.Test;

public class WordNetParserTest
{
	@Test
	public void testWordnetParser()
	{
		WordNetParser.parserWordnet("../b4o/data.noun");
	}
}
