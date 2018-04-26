# Word Sense Disambiguation

Use vertex centrality measures to disambiguate senses of a word.

## Thoughts
* Seems that adding both synset and related synsets' (all possible relationships) words is better
	* maybe it's useful to go deeper (more than one edge) (this seems the correct way)
		* depth 3-4 seems the best in terms of results/computation time 

* Maybe working on examples inside words could give us better results

## Possible future work
* Tries:
	* Don't create edges between distant words in a sentence, maybe a single sentence is composed by multiple, separate, logical sentences
	* Work on all relationships or weight differently the relationships by words
	* Work on SemCor3
	* Separate results based on NOUNS, VERBS ADJ etc..
	* Try with GLNS solver in fast mode and compare
	* 

* // Move all filesystem resources stream to getResourceAsAStream

* // Use indipendent dictionary (add Adapter for BabelNet, for example)
