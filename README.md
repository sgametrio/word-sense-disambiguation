# Word Sense Disambiguation

Use vertex centrality measures to disambiguate senses of a word.

## Problems
When there are multiple optimal path don't know which one is chosen, maybe we can tell him which node it has to take (lower id -> more common sense)

## Thoughts
* Seems that adding both synset and related synsets' (all possible relationships) words is better
	* maybe it's useful to go deeper (more than one edge) (this seems the correct way)
		* depth 3-4 seems the best in terms of results/computation time 

* Maybe working on examples inside words could give us better results
* Don't create edges between distant words in a sentence, maybe a single sentence is composed by multiple, separate, logical sentences

## Possible future work
* Don't create edges between distant words in a sentence, maybe a single sentence is composed by multiple, separate, logical sentences

* Move all filesystem resources stream to getResourceAsAStream

* Use indipendent dictionary (add Adapter for BabelNet, for example)
