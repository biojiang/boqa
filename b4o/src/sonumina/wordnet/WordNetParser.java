package sonumina.wordnet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.logging.Logger;

import ontologizer.go.ParentTermID;
import ontologizer.go.Term;
import ontologizer.go.TermContainer;
import ontologizer.go.TermID;
import ontologizer.go.TermRelation;
import sonumina.math.graph.DirectedGraph;
import sonumina.math.graph.Edge;

public class WordNetParser
{
	private static Logger logger = Logger.getLogger(WordNetParser.class.getCanonicalName());

	static class Pointer extends Edge<WordNetTerm>
	{
		public Pointer(WordNetTerm source, WordNetTerm dest)
		{
			super(source, dest);
		}

		

/*				
			  (define-wordnet-pointer-symbol "!" :noun :antonym)
			  (define-wordnet-pointer-symbol "@" :noun :hypernym)
			  (define-wordnet-pointer-symbol "~" :noun :hyponym)
			  (define-wordnet-pointer-symbol "#m" :noun :member-meronym)
			  (define-wordnet-pointer-symbol "#s" :noun :substance-meronym)
			  (define-wordnet-pointer-symbol "#p" :noun :part-meronym)
			  (define-wordnet-pointer-symbol "%m" :noun :member-holonym)
			  (define-wordnet-pointer-symbol "%s" :noun :substance-holonym)
			  (define-wordnet-pointer-symbol "%p" :noun :part-holonym)
			  (define-wordnet-pointer-symbol "=" :noun :attribute)*/

		/**
		 * @see http://wordnet.princeton.edu/man/wninput.5WN.html
		 */
		String symbol; 
		String synsetOffset;
		String pos;
		String st;
	}

	static class WordNetTerm
	{
		public String id;
		public String type;

		public ArrayList<String> words;
		public String glos;
		
		public boolean equals(WordNetTerm obj)
		{
			return id.equals(obj.id);
		}
		
		@Override
		public int hashCode()
		{
			return id.hashCode();
		}
	}
	
	/**
	 * Parses the word net file.
	 * 
	 * @param fileName
	 * @return
	 * @throws IOException
	 */
	public static TermContainer parserWordnet(String fileName) throws IOException
	{
		BufferedReader in = new BufferedReader(new FileReader(new File(fileName)));

		/** Maps a unique id to a word net term */
		HashMap<String,WordNetTerm> wordNetMap = new HashMap<String,WordNetTerm>();
		
		/** Arrangement as graph */
		DirectedGraph<WordNetTerm> wordNetGraph = new DirectedGraph<WordNetParser.WordNetTerm>();

		String line;
		
		while ((line = in.readLine()) != null)
		{
			/* Ignore empty lines and comments */
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

			WordNetTerm source = wordNetMap.get(id);
			if (source == null)
			{
				source = new WordNetTerm();
				source.id = id;
			}
			source.type = "n";
			source.words = words;

			if (!wordNetMap.containsKey(id))
			{
				wordNetMap.put(id, source);
				wordNetGraph.addVertex(source);
			}
			
			int pCnt = Integer.parseInt(entries[wCur]);
			int pCur = wCur + 1;

//			if (source.id.equals("09917593"))
//				System.out.println(pCnt + " ");
			for (int i=0;i<pCnt;i++)
			{
				String pointerSymb = entries[pCur];
				String synsetOffset = entries[pCur+1];
				String pos = entries[pCur+2];
				String st = entries[pCur+3]; /* source/target */

				if (pos.equals("n") && (pointerSymb.equals("@") || pointerSymb.equals("@i") || pointerSymb.equals("~")))// || pointerSymb.equals("%p")))
				{
//					if (source.id.equals("09917593"))
//						System.out.print(synsetOffset + "(" + pointerSymb + ")" + " ");

					WordNetTerm dest = wordNetMap.get(synsetOffset);
					if (dest == null)
					{
						dest = new WordNetTerm();
						dest.id = synsetOffset;
					}

					if (!wordNetMap.containsKey(synsetOffset))
					{
						wordNetMap.put(synsetOffset, dest);
						wordNetGraph.addVertex(dest);
					}

					Pointer e;
					if (pointerSymb.equals("~"))
					{
						e = new Pointer(source, dest);
					} else
					{
						e = new Pointer(dest, source);
					}

					if (!wordNetGraph.hasEdge(e.getSource(), e.getDest()))
						wordNetGraph.addEdge(e);

				}
				pCur += 4;
			}

//			if (source.id.equals("09917593"))
//				System.out.println();

			String glos = null;
			
			if (entries[pCur].equals("|"))
			{
				int pos = line.indexOf('|');
				if (pos > 0 && line.length() > pos + 2)
					glos = line.substring(pos + 2);
			}
			source.glos = glos;
		}

		/* Now construct the terms */
		int numRelations = 0;
		HashSet<Term> terms = new HashSet<Term>();
		for (WordNetTerm t : wordNetGraph)
		{
			ArrayList<ParentTermID> parentList = new ArrayList<ParentTermID>();
			
			Iterator<WordNetTerm> iter = wordNetGraph.getParentNodes(t);
			if (iter != null)
			{
				while (iter.hasNext())
				{
					WordNetTerm p = iter.next();
					ParentTermID pt = new ParentTermID(new TermID("WNO:" + p.id), TermRelation.IS_A);
					parentList.add(pt);
					numRelations++;
				}
			}
			
			ParentTermID [] parents = new ParentTermID[parentList.size()];
			parentList.toArray(parents);
			
			Term nt = new Term("WNO:" + t.id,t.words.get(0),parents);
			nt.setDefinition(t.glos);
			terms.add(nt);
		}

		logger.info("Contains " + terms.size() + " terms with " + numRelations + " relations");

		return new TermContainer(terms, "OBO","Unknown");
	}
}
