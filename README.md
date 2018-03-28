# Word Sense Disambiguation

Use vertex centrality measures combined with syntactic similarity computed distances between them to disambiguate senses of a word.

## Problems
* Some sentences (see senseval3.d000.s081 for example) does not disambiguate

		No instance to disambiguate

## Thoughts
* Seems that adding both synset and related synsets' (all possible relationships) words is better
	* maybe it's useful to go deeper (more than one edge) (this seems the correct way)
		* depth 3 seems the best in terms of results/computation time 

* Maybe working on examples inside words could give us better results


## Possible future work
* Don't create edges between distant words in a sentence, maybe a single sentence is composed by multiple, separate, logical sentences

* Move all filesystem resources stream to getResourceAsAStream
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
