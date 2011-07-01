package sonumina.b4oweb;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import sonumina.b4oweb.server.B4OCore;
import sonumina.b4oweb.server.ItemResultEntry;

public class B4OCoreTest
{
	@Test
	public void test()
	{
		B4OCore.score(Arrays.asList(0,1,2,3));
	}
}
