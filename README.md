# Word Sense Disambiguation

Use vertex centrality measures combined with sintactic similarity computed distances between them to disambiguate senses of a word.

## Possible future work
* Additional nodes based on relations between senses (Done, but how?)
* Make this work under windows (FFFFFFFFFFFFFFFFFFFFFFF)
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
