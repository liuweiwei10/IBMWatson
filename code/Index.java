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
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;

public class Index {
	private static String uselessTexts[] = { "==See also==", "==References==", "==External links==",
			"==Further reading==", "==Notes==", "==Bibliography==", "#REDIRECT", "===", "==" };
	private Directory index;
	int n = 1;
//
//	public static void main(String[] args) throws IOException {
//		new Index("wiki");
//	}

	/***
	 * constructor : build index for the wikipeida files in folder
	 * 
	 * @param folder
	 */
	public Index(String folder) {
		try {
			Analyzer analyzer;
			if (Configuration.isEnglishAnalyzer) {
				CharArraySet stopWords = EnglishAnalyzer.getDefaultStopSet();
				analyzer = new EnglishAnalyzer(stopWords);

			} else {
				analyzer = new StandardAnalyzer();

			}
			File f = new File("index");
			if(f.exists() && f.isDirectory()) {
				deleteDirectory(f);
			}
			
			index = FSDirectory.open(Paths.get("./index/"));

			IndexWriterConfig config = new IndexWriterConfig(analyzer);
			IndexWriter w = new IndexWriter(index, config);
			Similarity similarity = new BM25Similarity();
			if (Configuration.isBM25) {
				config.setSimilarity(similarity);
			}
			int n=0;
			File[] files = new File(folder + "/").listFiles();
			for (File file : files) {
				if (!file.isDirectory()) {
					System.out.println(n);
					n++;
					BufferedReader br = new BufferedReader(new FileReader(file));
					try {
						String line;
						String curDocName = null;
						String curCategoryText = "";
						String curDocText = "";
						// process the input file line by line and build
						// inverted index.
						while ((line = br.readLine()) != null) {
							if (line.startsWith("[[") && line.endsWith("]]")) { 
								// a new wikipedia page begins at this line
							
								//System.out.println(line);
					
								// added the previous doc to index
								if (curDocName != null && curDocText != null) {
									if (Configuration.isRemovingUselessInfoFromIndex) {
										curDocText = processWikiPage(curDocText);

									}
									//System.out.println("name:\n"+curDocName+"\nText:\n"+curDocText+"\nCategory:\n"+curCategoryText);
									addDoc(w, curDocText, curCategoryText, curDocName);

								}
								
								// extract the name of the new wikipedia page
								curDocName = line.substring(2, line.length() - 2);

								// reset the text to empty
								curDocText = "";
							} else {
								// it's still content of the previous wikipedia
								// page, add the current line to the text
								if (Configuration.isRemovingUselessInfoFromIndex) {
									if (line.startsWith("CATEGORIES:")) {
										curCategoryText = line.substring(11);		
										curDocText += curCategoryText +" ";
									} else {
										curDocText += line;
									}
								} else {
									curDocText += line;
								}
							}
						}
						if (curDocName != null && curDocText != null) {
							// add the last wikipedia page into index							
							addDoc(w, curDocText, curCategoryText, curDocName);
						}
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
			}

			System.out.println("indexing...Done!\n\n");
			w.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * get index
	 * 
	 * @return
	 */
	public Directory getIndex() {
		return index;
	}

	/***
	 * add doc
	 * 
	 * @param w
	 * @param text
	 * @param name
	 * @throws IOException
	 */
	private void addDoc(IndexWriter w, String content, String category, String name) throws IOException {
		Document doc = new Document();

		doc.add(new TextField("text", content, Field.Store.YES));
		doc.add(new TextField("category", category, Field.Store.YES));

		// use a string field for name because we don't want it tokenized
		doc.add(new StringField("name", name, Field.Store.YES));

		w.addDocument(doc);
	}

	/**
	 * process the wiki page to get rid of some useless texts
	 * 
	 * @param text
	 * @return
	 */
	private String processWikiPage(String text) {
		String processedText = text;
		//System.out.println("before processing:\n" + text);
		// remove "[tpl]....[/tpl]"
		processedText = removeTpls(processedText);
		// remove other useless texts
		processedText = removeOtherUselessTexts(processedText);
		//System.out.println("after processing:\n" + processedText);
		return processedText;
	}

	/***
	 * remove [tpl]...[/tpl]
	 * 
	 * @param text
	 * @return
	 */
	private String removeTpls(String text) {
		String processedText = text;	
		while (processedText.contains("[tpl]") && text.contains("[/tpl]")) {
			int index1 = processedText.indexOf("[tpl]");
			int index2 = processedText.indexOf("[/tpl]");
			if(index2 > index1) {
			String str1 = processedText.substring(0, index1); 
		    String str2 = processedText.substring(index2 + 6);
		    processedText = str1 + " "+ str2;
			} else {
				processedText = processedText.replaceFirst("\\[/tpl\\]", " ");
			}
		}
		return processedText;
	}

	/**
	 * remove other useless texts
	 * 
	 * @param text
	 * @return
	 */
	private String removeOtherUselessTexts(String text) {
		String processedText = text;
		for (int i = 0; i < uselessTexts.length; i++) {
			processedText = processedText.replaceAll(uselessTexts[i], " ");
		}

		return processedText;
	}

	/***
	 * print index
	 * 
	 * @throws IOException
	 */
	public void printIndex() throws IOException {
		IndexReader reader = DirectoryReader.open(index);
		int num = reader.numDocs();
		for (int i = 0; i < num; i++) {
			Document d = reader.document(i);
			System.out.println(d.getField("name").stringValue());
			System.out.println(d.getField("text").stringValue() + "\n\n");

		}
		reader.close();
	}
	
	public boolean deleteDirectory(File directory) {
	    if(directory.exists()){
	        File[] files = directory.listFiles();
	        if(null!=files){
	            for(int i=0; i<files.length; i++) {
	                if(files[i].isDirectory()) {
	                    deleteDirectory(files[i]);
	                }
	                else {
	                    files[i].delete();
	                }
	            }
	        }
	    }
	    return(directory.delete());
	}

}
