package sonumina.b4oweb;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.mortbay.jetty.security.B64Code;

import sonumina.b4oweb.server.core.B4OCore;
import sonumina.b4oweb.server.core.ItemResultEntry;

public class B4OCoreTest
{
	@Test
	public void test()
	{
		int i;
		List<ItemResultEntry> resultList = B4OCore.score(Arrays.asList(0,1));
		for (i=0;i<resultList.size();i++)
		{
			int id = resultList.get(i).getItemId();
			System.out.println(id + " " + B4OCore.getItemName(id) + " " + resultList.get(i).getScore());
			if (i>10) break;
		}
	}
}
