package sonumina.boqa.server;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class B4OCoreTest
{
	@Test
	public void test() throws InterruptedException, IOException
	{
		int i;

		BOQACore c = new BOQACore("../boqa/data/human-phenotype-ontology.obo.gz","../boqa/data/phenotype_annotation.omim.gz");

		List<ItemResultEntry> resultList = c.score(Arrays.asList(0,1));
		for (i=0;i<resultList.size();i++)
		{
			int id = resultList.get(i).getItemId();
			System.out.println(id + " " + c.getItemName(id) + " " + resultList.get(i).getScore());
			if (i>10) break;
		}
	}
}
