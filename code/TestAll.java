import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class TestAll {
	private static String questionsFileName = "questions.txt";
	private static String folderName ="wiki";

	public static void main(String[] args) throws IOException {
		if(args.length < 2) {
			usage();
			return;
		}else {
			//the last arg is the question file;
			questionsFileName = args[args.length-1];
			
			//the second last arg is the question file;
			folderName = args[args.length-2];
			for(int i=0; i<args.length-2; i++) {
				if(args[i].equals("-a")) {
					Configuration.isBM25 = true;
				}else if(args[i].equals("-b")) {
					Configuration.isEnglishAnalyzer = false;					
				}else if(args[i].equals("-c")) {
					Configuration.isCategoryIncludedInQuery = false;					
				}else if(args[i].equals("-d")) {
					Configuration.isSelectingKeywords = true;					
				}else if(args[i].equals("-e")) {
					Configuration.isRemovingUselessInfoFromIndex = false;					
				}else if(args[i].equals("-f")) {
					Configuration.isImprovement = false;					
				}else{	
					System.out.println("unrecognizable argument ..");
					usage();
					return;

				}
			}			
		}
		printConfigurations();		
		//check folder existence
		File f1 = new File(folderName);
		if (!f1.exists() || !f1.isDirectory()) {
		   System.out.println("folder:" +folderName + " not found!!");
		   return;
		}		
		//check question existence
		File f2 = new File(questionsFileName);
		if (!f2.exists()) {
		   System.out.println("question file:" +questionsFileName + " not found!!");
		   return;
		}		
		
		 //Directory index = FSDirectory.open(Paths.get("./index/"));
		System.out.println("indexing...");
		 Index myIndex = new Index(folderName);
		 Directory index = myIndex.getIndex();
		
		 
		// printIndex(index);
		Analyzer analyzer;
		if (Configuration.isEnglishAnalyzer) {
			CharArraySet stopWords = EnglishAnalyzer.getDefaultStopSet();
			analyzer = new EnglishAnalyzer(stopWords);
		} else {
			analyzer = new StandardAnalyzer();
		}

		BufferedReader br = null;

		int totalCount = 0;

		try {
			String line;
			br = new BufferedReader(new FileReader(questionsFileName));
			String category = null;
			String clue = null;
			String answer = null;
			int i = 0; // to identify whether the current line indicates a
						// category/clue/answer;

			// process the input file line by line and build inverted index.
			while ((line = br.readLine()) != null) {
				if (!line.trim().equals("")) {
					if (i == 0) {// category
						category = line;
					} else if (i == 1) {// clue
						clue = line;
					} else if (i == 2) {// answer
						answer = line;
					} else {
						System.out.println("error in questions file!");
						return;
					}
					i++;
				} else {					
					if (i != 3) {
						System.out.println("error in questions file!");
						return;
					}
					if (category != null && clue != null && answer != null) {
						System.out.println("Category:" + category);
						System.out.println("Clue:" + clue);
						IndexReader reader = DirectoryReader.open(index);
						totalCount++;

						// generate query from clue and category
						QueryGenerator qg = new QueryGenerator(clue, category, reader, analyzer);
						Query query = qg.getQuery();
						String queryStr = qg.getQueryString();
						//System.out.println("query:" + query.toString());

						// search
						new Searcher(reader, query, queryStr, category, answer);

						// for (int j = 0; j < hits.length; ++j) {
						// int docId = hits[j].doc;
						// Document d = searcher.doc(docId);
						// System.out.println((j + 1) + ". " + d.get("name") +
						// "\t" + hits[j].score);
						//
						// }

						reader.close();
					}
					i = 0; // reset i;
				}
			}
			System.out.println("Perfomance:" + StatisticHolder.correctCount + " out of " + totalCount + " are correct!");
			//System.out.println("top 10 accuracy:" + StatisticHolder.top10CorrectCount + " out of " + totalCount + " are correct!");

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null)
					br.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}

	public static void printIndex(Directory index) throws IOException {
		IndexReader reader = DirectoryReader.open(index);
		int num = reader.numDocs();
		for (int i = 0; i < num; i++) {
			Document d = reader.document(i);
			System.out.println(d.getField("name").stringValue());
			System.out.println(d.getField("text").stringValue() + "\n\n");

		}
		reader.close();
	}
	
	public static void usage() {
		System.out.println();
		System.out.println("Usage: \n    sh test.sh [-abcdef] \"folder name\" question file");
		System.out.println("       for example:sh test.sh wiki question.txt");
		System.out.println("       options: ");
		System.out.println("           -a:  use BM25 instead of vector space");
		System.out.println("           -b:  use standard analyzer instead of english analyzer");
		System.out.println("           -c:  don't include category of the question in the query" );
		System.out.println("           -d:  selecting words to form query" );
		System.out.println("           -e:  index the wikipedia pages directly without removing usless information" );
		System.out.println("           -f:  don't apply the reranking improvement" );
	}
	
	public static void printConfigurations() {
		System.out.println("Configurations:\n");
		System.out.println("isBM25:" + Configuration.isBM25);
		System.out.println("isEnglishAnalyzer:" + Configuration.isEnglishAnalyzer);
		System.out.println("isCategoryIncludedInQuery:" + Configuration.isCategoryIncludedInQuery);
		System.out.println("isSelectingKeywords:" + Configuration.isSelectingKeywords);
		System.out.println("isRemovingUselessInfoFromIndex:" + Configuration.isRemovingUselessInfoFromIndex);
		System.out.println("isImprovement:" + Configuration.isImprovement);
		System.out.println("\n\n");

	}

}
