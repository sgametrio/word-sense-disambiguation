# Word Sense Disambiguation

Use vertex centrality measures combined with sintactic similarity computed distances between them to disambiguate senses of a word.

## Possible further ways
* Additional nodes based on relations between senses (Done, but how?)
* (check) do not make duplicate edges, if it connects two sense of two different words then create node and connect every vertex to it
* Extend scorer to create a .json file with more statistics to improve logic
	* Completely wrong disambiguations
	* Stats for POS
	* 
* Use indipendent dictionary (add Adapter for BabelNet, for example)
* (!!!!) Refactor graph structure
