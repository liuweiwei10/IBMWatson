Weiwei Liu
CS583
Watson Project

    Usage: sh test.sh [-abcdef] "folder name" "question file"
		for example:sh test.sh wiki question.txt
		options:
		    -a:  use BM25 instead of vector space
	 	    -b:  use standard analyzer instead of english analyzer
 		    -c:  don't include category of the question in the query"
		    -d:  selecting words to form query
		    -e:  index the wikipedia pages directly without removing useless information
		    -f:  don't apply the reranking improvement" 

Note: 
     -The wikipedia pages folder is not included, please copy it to the code folder.
     -For the best configuration, please run with just the folder name and question filename, without any other option.

Thanks for use.

