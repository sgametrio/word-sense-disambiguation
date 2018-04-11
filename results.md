### Most Common Sense (credo)
##### Fasulli (calcolavo centralità su grafo senza archi -> centralità 0, quindi prendeva il primo senso, sempre)
* Senseval3: 66.2
* Senseval2: 66.8
* All: 65.2

### KPP centrality with auxiliary nodes (both synset and related synset) commit:a61a3671d58a4f
##### depth=0
* Senseval2: 43.4
* Senseval3: 38.6
* Semeval2007: 25.9
* Semeval2013: 40.7
* Semeval2015: 49.1
* All: 41.3

* MiniSenseval3: 30.8

##### depth=1
* MiniSenseval3: 28.2
* Senseval3: 39.7
* All: 39.4

##### depth=3
* MiniSenseval3: 33.3
* Senseval2: 44.5
* Senseval3: 40.7
* Semeval2007: 28.1
* Semeval2013: 43.3
* Semeval2015: 51.3
* All: 43.2

### similarity tree-based E-GTSP (Marco)
* MiniSenseval3: 30.0
* Semeval2007: 26.6
* Semeval2013: 41.2
* Semeval2015: 48.6
* Senseval3: 38.2
* All: 41.3

### Distribute centrality on edges (commit:4e1025842602099e2feadc8bb7ac8eb2923155ba)
#### weight = mean*weight
* All: 41.5

#### mean instead of weight
* All: 41.2

### kpp (no BellmanFord) centrality graph -> connected graph -> tsp (commit: dad6bd0e07789786fc6)
##### depth DFS = 2
* All: 43.4

##### depth DFS = 3 
* All: 46.2 -> 1H19M

##### depth DFS = 4
* All: 46.9 -> 3H19M
* Mini: 41.0

### kpp centrality graph -> disambiguate by centrality
##### depth DFS = 4
* All: 51.1

### pagerank centrality graph -> disambiguate by centrality
##### depth DFS = 4
* Mini: 46.2
* All: 50.8-50.9 -> 30M

### pagerank centrality graph -> connected graph -> tsp
##### depth DFS = 2
* All: 43.3

##### depth DFS = 3
* All: 46.0 -> 45M

##### depth DFS = 4
* Mini: 43.6
* All: 47.3-47.5 -> 3H16M -> 1H19M

