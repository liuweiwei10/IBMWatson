import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;

public class QueryGenerator {
	private static int QUERY_TERM_MAX; // if the query has more than
										// QUERY_TERM_MAX words, than pick the
										// most informative QUERY_TERM_MAX as
										// keywords
    private String queryString="";
    private Query query = null; 
	
    public QueryGenerator(String clue, String category, IndexReader reader, Analyzer analyzer) {
		if(Configuration.isSelectingKeywords){
			QUERY_TERM_MAX = 10;			
		} else {
			QUERY_TERM_MAX = 100; //with setting the max number of terms to 100, no word will be excluded from the query			
		}
		
		String queryStr = "";
		try {
            if(Configuration.isCategoryIncludedInQuery) {
    			queryStr = clue + " " + category.toLowerCase();
            } else {
            	queryStr = clue;
            }
			//System.out.println("query before optimization:" + queryStr);

			//remove stop words
			String tokens[] = removeStopWords(queryStr);
			//System.out.print("tokenized query:");
			queryStr ="";

			for(String s : tokens) {
				queryStr += s + " ";
				//System.out.print(s+" ");
			}

			if (tokens.length > QUERY_TERM_MAX) {
				// get the most informative terms to construct the query
				queryStr = "";
				Map<String, Long> tfMap = new HashMap<String, Long>();
				for (int j = 0; j < tokens.length; j++) {
					Term term = new Term("text", tokens[j]);

					tfMap.put(tokens[j], reader.totalTermFreq(term));

					//System.out.println("term:" + tokens[j] + ", freq:" + reader.totalTermFreq(term));
				}

				// get most informative terms
				ValueComparator bvc = new ValueComparator(tfMap);
				@SuppressWarnings("unchecked")
				TreeMap<String, Long> sortedMap = new TreeMap<String, Long>(bvc);
				sortedMap.putAll(tfMap);

				int j = 0;
                String topTerms[] =new String[QUERY_TERM_MAX];
				for (Entry<String, Long> entry : sortedMap.entrySet()) {
					if (j < QUERY_TERM_MAX) {
						//System.out.println(words[entry.getKey()] + ":" + entry.getValue());
						topTerms[j] = entry.getKey();
						j++;
					}
				}
				List<String> list = Arrays.asList(topTerms);
				for(String token: tokens) {
					if(list.contains(token)) {
						queryStr += token + " ";
					}
				}
			}
			//MultiFieldQueryParser parser  = new MultiFieldQueryParser(new String[]{"text", "category"}, analyzer);
			//query= parser.parse(MultiFieldQueryParser.escape(queryStr));
			queryString = queryStr;
			query = new QueryParser("text", analyzer).parse(QueryParser.escape(queryStr));

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
	
	public Query getQuery() {
		return query;
	}
	
	public String getQueryString(){
		return queryString;
	}

}
