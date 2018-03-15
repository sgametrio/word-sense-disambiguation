# Word Sense Disambiguation

Use vertex centrality measures combined with sintactic similarity computed distances between them to disambiguate senses of a word.

## Problems
* Some sentences (see senseval3.d000.s081 for example) does not disambiguate

		No instance to disambiguate

## Possible future work
* Additional nodes based on relations between senses (Done, but how?)
* Make this work under windows (FFFFFFFFFFFFFFFFFFFFFFF)
* Focus on performance (reduce WordNet accesses, for example)
* Make some tests to instantly identify:		
	* centrality 0.0
	* disambiguated sense isn't the one with max centrality
	* 
* Extend scorer to create a .json file with more statistics to improve logic
	* Completely wrong disambiguations
	* Stats for POS
	* ...
	* Make .json readable by external program that can create graphs and visual information
* Use indipendent dictionary (add Adapter for BabelNet, for example)
* (!!!!) Refactor graph structure
