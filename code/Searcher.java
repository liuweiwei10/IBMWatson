import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

public class Searcher {

	public Searcher(IndexReader reader, Query query, String queryStr, String category, String answer) {
		try {
			int hitsPerPage = 10;
			IndexSearcher searcher = new IndexSearcher(reader);
			if (Configuration.isBM25) {
				Similarity similarity = new BM25Similarity();
				searcher.setSimilarity(similarity);
			}
			TopScoreDocCollector collector = TopScoreDocCollector.create(hitsPerPage);

			searcher.search(query, collector);

			ScoreDoc[] hits = collector.topDocs().scoreDocs;
//		    System.out.println("before reranking:");
//			printHitsScores(hits,searcher);
//            hits = rerankHits(hits,  searcher, category);
//            System.out.println("after reranking:");       	
//			printHitsScores(hits,searcher);
			
			
			// display results
			if (hits.length > 0) {
				boolean result = false;
				Document candidateDoc;
				if (!Configuration.isImprovement) {
					// get the top 1 hit
					candidateDoc = searcher.doc(hits[0].doc);

				} else {
					// re-select the candidate according to their doc names
					candidateDoc = searcher.doc(hits[0].doc);
					for (int j = 0; j < hits.length; j++) {
						candidateDoc = searcher.doc(hits[j].doc);
						String[] docName = removeStopWords(candidateDoc.get("name"));
						int tokenCount = docName.length;
						int intersectionCount = 0;
						for (int k = 0; k < tokenCount; k++) {
							if (queryStr.contains(docName[k])) {
								intersectionCount++;
							}
						}
						if (intersectionCount > 0) {// tokenCount *
													// RERANK_THRESH
							//System.out.println("excluding " + candidateDoc.get("name") + ": " + intersectionCount
								//	+ " same tokens, exclude it..");
						} else {
							break;
						}
					}
				}

				Document hit0Doc = searcher.doc(hits[0].doc);
				if (answer.contains(candidateDoc.get("name"))) {
					result = true;
					if (!candidateDoc.get("name").equals(hit0Doc.get("name"))) {
						//System.out.println("exclude: success!");
					}
				}
				if (answer.contains(hit0Doc.get("name")) && !result) {
					//System.out.println("exclude: fails!");
				}
				if (result) {
					StatisticHolder.correctCount++;
				}

				System.out.println("result:" + result + "\t" + candidateDoc.get("name") + "\t" + answer +"\n\n");

				// check if the top 10 hits contain the correct answer
				for (int j = 0; j < hits.length; j++) {
					Document doc = searcher.doc(hits[j].doc);
					//System.out.println("hit no." + (j + 1) + ":" + doc.get("name"));
					if (answer.contains(doc.get("name"))) {
						StatisticHolder.top10CorrectCount++;
						// System.out.println("hit no." + (j + 1) + " is correct
						// the answer");
						break;
					}
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void printHitsScores(ScoreDoc[] hits, IndexSearcher searcher) {
		try {
			for (int j = 0; j < hits.length; j++) {
				Document doc;

				doc = searcher.doc(hits[j].doc);

				System.out.println("hit no." + (j + 1) + ":" + doc.get("name") + ", score" + hits[j].score);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private ScoreDoc[] rerankHits(ScoreDoc[] hits, IndexSearcher searcher, String category) {

		ScoreDoc[] newHits = hits;
		try {
			// Build a temporary index for the top hits
			Analyzer analyzer;
			if (Configuration.isEnglishAnalyzer) {
				CharArraySet stopWords = EnglishAnalyzer.getDefaultStopSet();
				analyzer = new EnglishAnalyzer(stopWords);
			} else {
				analyzer = new StandardAnalyzer();

			}
			Directory tempIndex = new RAMDirectory();
			IndexWriterConfig config = new IndexWriterConfig(analyzer);
			IndexWriter w = new IndexWriter(tempIndex, config);

			for (int i = 0; i < hits.length; i++) {
				Document doc = searcher.doc(hits[i].doc);
				addDoc(w, doc.get("category"), doc.get("name"));
			}
			w.close();

			// use the category as the query: trying to match the category of
			// the question to the category of the wiki page
			String categoryStr = "";
			if (category != null && !category.equals("")) {
				String tokens[] = removeStopWords(category);
				for (String t : tokens) {
					categoryStr += t + " ";
					System.out.println("category token:" + t);
				}
			}
			if (!categoryStr.equals("")) {
				// query
				System.out.println("category query:" + categoryStr);
				Query q = new QueryParser("category", analyzer).parse(QueryParser.escape(categoryStr));

				// re-search
				int hitsPerPage = 10;
				IndexReader reader = DirectoryReader.open(tempIndex);
				IndexSearcher searcher2 = new IndexSearcher(reader);
				TopScoreDocCollector collector = TopScoreDocCollector.create(hitsPerPage);
				System.out.println("category query:" + categoryStr);
				searcher2.search(q, collector);
				ScoreDoc[] tempHits = collector.topDocs().scoreDocs;
				System.out.println("temp Hits:");
				printHitsScores(tempHits,searcher2);
				for (int i = 0; i < hits.length; i++)
					for (int j = 0; j < tempHits.length; j++) {
						Document doc1 = searcher.doc(hits[i].doc);
						Document doc2 = searcher2.doc(tempHits[j].doc);
						String name1 = doc1.get("name");
						String name2 = doc2.get("name");

						if (name1.equals(name2)) {
							newHits[i].score += tempHits[j].score;
							break;
						}

					}
				newHits = sortHits(newHits, searcher);
				reader.close();

			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return newHits;
	}

	private  ScoreDoc[] sortHits(ScoreDoc[] hits, IndexSearcher searcher) {	

		  int lenD = hits.length;
		  ScoreDoc tempDoc;
		  for(int i = 0;i<lenD;i++){
		    for(int j = (lenD-1);j>=(i+1);j--){
		      if(hits[j].score > hits[j-1].score){
		    	  tempDoc = hits[j];
		          hits[j]= hits[j-1];
		          hits[j-1]=tempDoc;
		      }
		    }
		  }
		  return hits;
		
	}
	
	private String[] removeStopWords(String text) throws Exception {

		CharArraySet stopWords = EnglishAnalyzer.getDefaultStopSet();
		Analyzer analyzer = new EnglishAnalyzer(stopWords);

		TokenStream tokenStream = analyzer.tokenStream(null, new StringReader(text));
		List<String> list = new ArrayList<String>();
		CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
		tokenStream.reset();
		while (tokenStream.incrementToken()) {
			String term = charTermAttribute.toString();
			list.add(term);
		}
		String[] tokens = new String[list.size()];
		tokens = list.toArray(tokens);
		analyzer.close();
		return tokens;
	}

	/***
	 * add doc
	 * 
	 * @param w
	 * @param text
	 * @param name
	 * @throws IOException
	 */
	private void addDoc(IndexWriter w, String category, String name) throws IOException {
		Document doc = new Document();

		doc.add(new TextField("category", category, Field.Store.YES));

		// use a string field for name because we don't want it tokenized
		doc.add(new StringField("name", name, Field.Store.YES));

		w.addDocument(doc);
	}
}
