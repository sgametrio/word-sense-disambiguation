# Word Sense Disambiguation

Use vertex centrality measures combined with sintactic similarity computed distances between them to disambiguate senses of a word.

## Problems
* Some sentences (see senseval3.d000.s081 for example) does not disambiguate

		No instance to disambiguate

## Thoughts
* Same synset nodes or related synsets, how and why?
* Seems that adding both synset and related synsets' words is better
	* maybe it's useful to go deeper (more than one edge) (this seems the correct way)

## Possible future work
* Make this work under windows (FFFFFFFFFFFFFFFFFFFFFFF)
* Focus on performance (reduce WordNet accesses, for example)
	* Set completing time in debugging mode
	* Even on final report, include timings
* Make some tests to instantly identify:		
	* centrality 0.0
	* disambiguated sense isn't the one with max centrality
	* 
* Extend scorer to create a .json file with more statistics to improve logic
	* Completely wrong disambiguations
	* Stats for POS
	* ...
	* Make .json readable by external program that can create graphs and visual information (plotly.js or chart.js for example)
* Use indipendent dictionary (add Adapter for BabelNet, for example)
* (!!!!) Refactor graph structure
* TODO: Make this CI/CD and schedule evaluation every night on latest commit
