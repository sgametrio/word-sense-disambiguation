/**
 * Main class. Implements all the functions needed to perform the disambiguation
 */

package com.sgametrio.wsd;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Duration;
import java.time.Instant;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import edu.mit.jwi.item.IWord;
import edu.stanford.nlp.trees.Tree;
import evaluation.InputInstance;
import evaluation.InputSentence;

import com.sgametrio.wsd.KelpAdapter;

public class MyExecutor {
	
	//used for lemmatization
	private StanfordUtilsAdapter stanfordAdapter = null;  
	private WordnetAdapter wordnet = null;
	//saving params
	private String fileNameSentences = "sentences";
	private final Object fileLock = new Object();
	private final Object graphLock = new Object();
	//execution params
	
	
	
	//CONSTRUCTOR
	public MyExecutor(){	
		//this.stanfordAdapter = new StanfordUtilsAdapter();
		this.wordnet = new WordnetAdapter();
		//this.kelp = new KelpAdapter();	
	}
	
	public void closeDictionary() {
		this.wordnet.closeDict();
	}
	/**
	 * Calls all the functions needed to perform the disambiguation
	 * @param input a map having word POS as keys and an array containing
	 * the lemma, the word as it was written in the sentence, the index of the word in the sentence and
	 * the params of the word given in the evaluation framework
	 */
	public void performDisambiguation(InputSentence input){
		Instant before = Instant.now();
		ArrayList<InputInstance> selectedInstances = this.mySelectPos(input.instances);

		input.instances.clear();
		input.instances.addAll(selectedInstances);
		//create the graph sentence		
		MyGraph graph = this.createDisambiguationGraph(input);
				
		if(Globals.saveGml){
			graph.saveToGML(Globals.gmlPath + Globals.fileName + graph.getSentenceId() + ".gml");
		}
		// Use centrality to disambiguate senses in a word
		if (!Globals.runSolver) {
			this.printCentralityDisambiguation(graph, false);
		} else {
			if (graph.getNodes().size() == 0) {
				// Graph has no vertexes, nothing to disambiguate, continue;
				graph.log(Globals.logInfo, "[NO WORDS] don't have words to disambiguate.");
			} else if (graph.getNodesIndexByClusters().size() == 1) {
				graph.log(Globals.logInfo, "[1 CLUSTER] has only 1 cluster, for now prints most common sense");
				Map<Integer, MyVertex> disambiguationMap = new HashMap<Integer, MyVertex>();
				MyVertex v = graph.getDisambiguationNodes().get(0);
				disambiguationMap.put(v.getSentenceIndex(), v);
				String log = this.printMapToFile(disambiguationMap, Globals.fileName, Globals.evaluation, false);
				if (log.length() > 0)
					graph.log(Globals.logStatistics, log);
			} else {
				// Run solver to disambiguate senses
				Instant beforeTSP = Instant.now();
				graph.saveToGTSP(Globals.tspSolverPathToGTSPLIB, Globals.fileName+graph.getSentenceId());
				this.setTSPSolver(graph.getSentenceId());
				if(this.runSolver(graph)) {
					Instant afterTSP = Instant.now();
					Duration dTSP = Duration.between(beforeTSP, afterTSP);
					graph.log(Globals.logStatistics, "[TIME][TSP] " + dTSP.toString());
					this.generateOutputFile(graph);
				}
			}
			
		}
		Instant after = Instant.now();
		Duration time = Duration.between(before, after);
		graph.log(Globals.logStatistics, "[FINISHED] " + time);
		System.out.println("[GRAPH " + graph.getSentenceId() + "][FINISHED] Time: " + time);
		graph.logOnFile();
	}
	
	
	private void printCentralityDisambiguation(MyGraph graph, boolean sentences) {
		// Map<Sentence Index, Senso corretto> 
		// Contiene i sensi disambiguati
		Map<Integer, MyVertex> disambiguationMap = new HashMap<Integer, MyVertex>();
		// Readable results
		for (MyVertex v : graph.getNodes()) {
			int sentenceIndex = v.getSentenceIndex();
			if (sentenceIndex < 0)
				continue;
			// KPP centrality for now
			float vCentrality = v.getCentrality();
			// per ogni sentence index devo scegliere uno e un solo nodo
			// Se la map contiene un nodo, lo sostituisco solo se ha maggiore centralità
			if (disambiguationMap.containsKey(sentenceIndex)) {
				if (disambiguationMap.get(sentenceIndex).getCentrality() < vCentrality) {
					disambiguationMap.put(sentenceIndex, v);
				}
			} else {
				disambiguationMap.put(sentenceIndex, v);
			}
		}
		// Print results
		String log = this.printMapToFile(disambiguationMap, Globals.fileNameCentrality, Globals.evaluation, sentences);
		graph.log(Globals.logStatistics, log);
		// results in .key format
	}


	public void createDir(String path) {
		// if the directory does not exist, create it
		// substring to avoid /
		File resDir = new File(path.substring(0, path.length()-1));
		if (!resDir.exists()) {
			try{
		    	resDir.mkdir();
		    } 
		    catch(SecurityException e){
		    	System.err.print(Thread.currentThread().getStackTrace()[1].getMethodName()+" threw: ");
				System.err.println(e);
		    }        
		}
	}

	private String printMapToFile(Map<Integer, MyVertex> disambiguationMap, String fileName, boolean evaluation, boolean sentences) {
		// print Map ordered by key
		synchronized (fileLock) {
			String log = "[SENTENCE TERMS]\n";
			String fileContent = "";
			try {
				PrintWriter keyFileWriter;
				if (sentences) {
					keyFileWriter = new PrintWriter(new FileWriter(Globals.resultsPath + fileName + this.fileNameSentences +Globals.resultsFileName, true));
				} else {
					keyFileWriter = new PrintWriter(new FileWriter(Globals.resultsPath + fileName +Globals.resultsFileName, true));
				}
				//sort word by their position in the text
				SortedSet<Integer> keys = new TreeSet<>(disambiguationMap.keySet());
				for (Integer key : keys) { 
					MyVertex v = disambiguationMap.get(key);
					if(v.getSentenceTermId() != null){
						log += v.getSentenceTermId() + " " + v.getGlossKey() + " " + v.getCentrality() + "\n";
						fileContent += v.getSentenceTermId()+" "+v.getGlossKey()+"\n";
					}
				}
				keyFileWriter.write(fileContent);
				keyFileWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return log;
		}
	}

	/**
	 * Eliminate the words having POS not specified in posTags variable
	 * @param oldMap: the map containing all the word of the sentence divided by POS
	 * @return a map containing only the word having the "interesting" POS
	 */
	private ArrayList<InputInstance> mySelectPos(ArrayList<InputInstance> old){
		
		ArrayList<String> posTags = new ArrayList<String>();
		String[] wordnetTags = {"NOUN", "VERB", "ADJ", "ADV"};
		for (String tag : wordnetTags) {
			posTags.add(tag);
		}
		
		//select only the words belonging to the specified ("interesting") POS
		ArrayList<InputInstance> instances = new ArrayList<InputInstance>();
		for (InputInstance i : old) {
			if (posTags.contains(i.pos)) {
				instances.add(i);
			}
		}
		
		return instances;
		
	}
	
	
	/**
	 * Set configuration parameters of solver by:
	 * * copy runGLKH helper script to runGLKH(graph id here)
	 * * dynamically update configuration file
	 * Change param of TSPSolver main file to set input and output files
	 */
	private void setTSPSolver(String id){
		
		BufferedReader oldFileReader = null;
		BufferedWriter newFileWriter = null;
		String mySolverName = Globals.tspSolverFileName+id;
		
		try {
			
			oldFileReader = new BufferedReader(new FileReader(Globals.tspSolverPathFileName));
			newFileWriter = new BufferedWriter(new FileWriter(Globals.tspSolverHomeDir+mySolverName));
			
			//reads from the old file and write on the new one
			String line;
			while ((line = oldFileReader.readLine()) != null) {
				if (line.contains("par=TMP")){
					line = "par=TMP/" + id + ".pid$$.par";
				}
				if (line.contains("PROBLEM_FILE")){
					line = "echo \"PROBLEM_FILE = " + Globals.GTSPLIBDirectory + Globals.fileName+id+".gtsp\" > $par";
				}
				if (line.contains("OUTPUT_TOUR_FILE")){
					line = "echo \"OUTPUT_TOUR_FILE = " + Globals.GTOURSDirectory + Globals.fileName+id+".tour\" >> $par";
				}
				if (line.contains("RUNS")) {
					line = "echo \"RUNS = " + Globals.runs + "\" >> $par";
				}
				if (line.contains("PI_FILES")) {
					//line = "echo \"PI_FILE = PI_FILES/" + Globals.fileName + "_" + id + ".pi\" >> $par";
					line = "";
				}
				if (line.contains("MAX_TRIALS")) {
					line = "";
				}
				
				newFileWriter.write(line+"\n");
			}
			
			oldFileReader.close();
			newFileWriter.close();
		
		}catch(IOException e){
			System.err.print(Thread.currentThread().getStackTrace()[1].getMethodName()+" threw: ");
			System.err.println(e);
		}
		
	}
	

	/**
	 * Runs the tspSolver script. Results are saved in G-TOURS folder.
	 * @return true if the solver completed its task, false otherwise
	 */
	public boolean runSolver(MyGraph graph){
		String id = graph.getSentenceId();
		File solver = new File(Globals.tspSolverPathFileName+id);
		if (solver.exists()) {
			try{
				//Set permissions
				// TODO: Dynamic check Operating System
				Set<PosixFilePermission> perms = new HashSet();
				perms.add(PosixFilePermission.OWNER_READ);
				perms.add(PosixFilePermission.OWNER_WRITE);

				Files.setPosixFilePermissions(solver.toPath(), perms);
				solver.setExecutable(true);
				solver.setReadable(true);
				solver.setWritable(true);
				//command to execute the solver
				ProcessBuilder process = new ProcessBuilder("./" + Globals.tspSolverFileName+id);
				process.directory(new File(Globals.tspSolverHomeDir));

				Process p = process.start();
				//get the error stream
				InputStream is = p.getErrorStream();
				String errorStream = this.getProcessOutput(is);
				String output = this.getProcessOutput(p.getInputStream());

				if (errorStream.length() > 1)
					graph.log(Globals.logWarning, errorStream);
				//if verbose mode is on, prints tsp solver output and errors
				if(Globals.solverVerbosity){
					graph.log(Globals.logInfo, output);
				}
				
				return true;
				
			}catch (IOException e) {
				System.err.print(Thread.currentThread().getStackTrace()[1].getMethodName()+" threw: ");
				System.err.println(e);
				
			}
        } else {
            System.out.println("Solver file does not exist");
        }
		return false;
		
	}

	
	/**
	 * Prints the output of tspSolver script
	 * @param inputStream
	 */
	private String getProcessOutput(InputStream inputStream) {
		
		StringBuilder sb = new StringBuilder();
		BufferedReader br = null;

		try {
			br = new BufferedReader(new InputStreamReader(inputStream));
			String line = null;
			
			while ((line = br.readLine()) != null) {
				sb.append(line + System.getProperty("line.separator"));
			}
			br.close();
		}catch(IOException e){ 
			System.err.print(Thread.currentThread().getStackTrace()[1].getMethodName()+" threw: ");
			System.err.println(e);
		}
		return sb.toString();	
	}
	
	/**
	 * Given an array of words to disambiguate and their POS, create a graph representing
	 * the sentence by: 
	 * * create a node for every sense of every word (in WordNet)
	 * * create edges between every couple of nodes and weigh them by syntactic similarity algorithm
	 * * if necessary, create nodes and edges useful to get a better disambiguation
	 * * compute centrality on nodes and distribute it on edges
	 * @param instances
	 * @return a graph representing the sentence
	 */
	private MyGraph createDisambiguationGraph(InputSentence sentence){
		MyGraph graph, centralityGraph = null;
		synchronized (graphLock) {
			graph = new MyGraph();
			centralityGraph = new MyGraph();
		}		
		graph.setSentence(sentence.sentence);
		graph.setSentenceId(sentence.sentenceId);
		centralityGraph.setSentence(sentence.sentence);
		centralityGraph.setSentenceId(sentence.sentenceId);
		// for every instance, I have to find all senses
		for (InputInstance instance : sentence.instances) {
			this.myCreateNodes(graph, instance);
			this.myCreateNodes(centralityGraph, instance);
		}
		this.myCreateEdges(graph);
		Instant beforeDFS = Instant.now();
		this.createNodesByDFS(centralityGraph, Globals.nodesDepth);
		Instant afterDFS = Instant.now();
		Duration dDFS = Duration.between(beforeDFS, afterDFS);
		graph.log(Globals.logStatistics, "[TIME][DFS] " + dDFS.toString());
		if (Globals.centrality) {
			Instant beforeC = Instant.now();
			this.computeVertexCentrality(centralityGraph);
			Instant afterC = Instant.now();
			Duration dC = Duration.between(beforeC, afterC);
			graph.log(Globals.logStatistics, "[TIME][CENTRALITY] " + dC.toString());
			graph.log(Globals.logStatistics, "[SIZE][NODES] " + centralityGraph.getNodes().size());
			graph.log(Globals.logStatistics, "[SIZE][EDGES] " + centralityGraph.getEdgesSize());
			// Distribute centrality on edges
			this.copyCentrality(centralityGraph.getNodes(), graph.getNodes());
			this.distributeCentralityOnEdges(graph);
		}
		if(Globals.saveGml){
			centralityGraph.saveToGML(Globals.gmlPath + Globals.fileName + graph.getSentenceId() + "_centrality.gml");
		}
		if (Globals.runSolver) {
			return graph;
		} else {
			return centralityGraph;
		}
	}

	private void copyCentrality(ArrayList<MyVertex> from, ArrayList<MyVertex> to) {
		// nodes present in `from` ArrayList are present in `to` too
		for (int i = 0; i < to.size(); i++) {
			to.get(i).setCentrality(from.get(i).getCentrality());
		}
	}
	
	/**
	 * Add nodes and edges to the graph by DFSing WordNet graph 
	 * @param graph
	 * @param nodesDepth
	 */
	private void createNodesByDFS(MyGraph graph, int nodesDepth) {
		ArrayList<MyVertex> nodes = graph.getNodes();
		Map<IWord, MyVertex> wordMap = new ConcurrentHashMap<IWord, MyVertex>();
		Map<IWord, MyVertex> originalWordMap = new ConcurrentHashMap<IWord, MyVertex>();
		// Create a map to better lookup words
		for (MyVertex node : nodes) {
			if (!wordMap.containsKey(node.getWord())) {
				wordMap.put(node.getWord(), node);
				originalWordMap.put(node.getWord(), node);
			}
		}
		
		for (MyVertex node : originalWordMap.values()) {
			Stack<IWord> path = new Stack<IWord>();
			this.computeDFS(graph, nodesDepth, originalWordMap, wordMap, path, node, node.getWord());
		}		
	}

	/**
	 * Execute DFS on WordNet graph, adding edges and vertexes if find path from start to another node in the graph
	 * @param graph
	 * @param depth
	 * @param wordMap
	 * @param path
	 * @param start
	 */
	private void computeDFS(MyGraph graph, int depth, Map<IWord, MyVertex> originalWordMap, Map<IWord, MyVertex> wordMap, Stack<IWord> path, MyVertex start, IWord current) {
		if (depth == 0)
			return;
		for (IWord w : wordnet.getAllRelatedWords(current)) {
			// Do not create edges between words that disambiguate the same sentence word (index)
			// TODO: migliorare questo if
			if (originalWordMap.containsKey(w)) {
				MyVertex last = wordMap.get(w);
				if (!current.equals(w) && last.getSentenceIndex() != start.getSentenceIndex()) {
					//System.out.println(" graph " + graph.getSentenceId() + " Found path between " + start.getWord().toString() + " SI: " + start.getSentenceIndex() + " and " + last.getWord().toString() + " SI: " + last.getSentenceIndex());
					MyVertex v = null;
					//System.out.println("Intermediate nodes:");
					for (IWord w1 : path) {
						//System.out.println(w1.toString());
						// Do not create again nodes representing a word already in the map
						if (wordMap.containsKey(w1)) {
							v = wordMap.get(w1);
						} else {
							v = new MyVertex(w1);
							graph.addNode(v);
							wordMap.put(w1, v);
						}
						// If the edge does not exist create one
						if (graph.distance(v, last) == -1 && !v.equals(last)) {
							graph.addEdge(v, last, computeEdgeWeight(v, last));
						}
						last = v;
					}
					if (graph.distance(start, last) == -1 && !start.equals(last)) {
						graph.addEdge(start, last, computeEdgeWeight(start, last));
					}
				}	
			} else {
				path.push(w);
				this.computeDFS(graph, depth-1, originalWordMap, wordMap, path, start, w);
				path.pop();
			}
		}
		
	}

	/**
	 * Set edge weight between every node in the graph as the centrality mean between these nodes
	 * @param graph
	 */
	private void distributeCentralityOnEdges (MyGraph graph) {
		ArrayList<MyVertex> vertexes = graph.getDisambiguationNodes();
		for (MyVertex v : vertexes) {
			for (MyEdge e : v.getEdges()) {
				float mean = graph.computeMeanCentrality(v, e.getDest());
				if (mean == 0) {
					//graph.log(Globals.logInfo, " mean 0 on an edge");
					// Ghost edges
					v.removeEdge(e);
				} else {
					e.setWeight(mean*e.getWeight());
				}
			}
		}	
	}

	/**
	 * Create an edge between every node in the graph and weigh this edge
	 * @param graph
	 */
	private void myCreateEdges(MyGraph graph) {
		//Edge creation
		ArrayList<MyVertex> vertices = graph.getNodes();
		int size = vertices.size();
		for(int i = 0; i < size; i++){
			// Start from i+1 cause it's undirected and it will create the other edges itself
			for(int j = i+1; j < size; j++){
				//doesn't create edges between vertexes representing the same word 
				if( vertices.get(i).getSentenceIndex() != vertices.get(j).getSentenceIndex()){
					if ( ! ( vertices.get(i).getGlossKey().equalsIgnoreCase(vertices.get(j).getGlossKey()))){
						// Compute similarity here
						float edgeWeight = this.computeEdgeWeight(vertices.get(i), vertices.get(j));
						graph.addEdge(vertices.get(i), vertices.get(j), edgeWeight);
					}
				}
			}
		}
		
	}


	private void myCreateNodes(MyGraph graph, InputInstance input) {
		//for all the WordNet glosses of that word and its lemma
		for(IWord word : this.wordnet.getWordsList(input.lemma, input.pos)) {
			String gloss = word.getSynset().getGloss().split("\"")[0];
			MyVertex v = new MyVertex(input, word, "", gloss);
			graph.addNode(v);
		}		
	}


	private void computeVertexCentrality(MyGraph graph) {
		switch (Globals.computeCentrality) {
			case Globals.kppBellmanFordCentrality: 
				this.computeKppBellmanFordCentrality(graph);
				break;
			case Globals.kppCentrality:
				this.computeKppSingleEdgeCentrality(graph);
				break;
			case Globals.pageRankCentrality:
				this.computeIterativePageRankCentrality(graph);
				break;
			case Globals.inDegreeCentrality:
				this.computeInDegreeCentrality(graph);
				break;
			default:
				this.computeKppSingleEdgeCentrality(graph);
				break;
		}
			
	}

	private void computeInDegreeCentrality(MyGraph graph) {
		// Weight vertexes by kpp centrality
		ArrayList<MyVertex> vertexes = graph.getNodes();
		int size = vertexes.size();
		for (int i = 0; i < size; i++) {
			MyVertex node = vertexes.get(i);
			node.setCentrality((float) node.getEdges().size() / size );
		}
	}

	/**
	 * Compute KPP centrality on every nodes present in the graph.
	 * KPP is computed as sum of the distance divided by nodes cardinality.
	 * @param graph
	 */
	private void computeKppSingleEdgeCentrality(MyGraph graph) {
		// Weight vertexes by kpp centrality
		ArrayList<MyVertex> vertexes = graph.getNodes();
		int size = vertexes.size();
		for (int i = 0; i < size; i++) {
			float kppCentrality = 0, distance = 0;
			for (int j = 0; j < size; j++) {
				if (i != j) {
					// TODO: distance (Dijkstra) instead of single edge weight
					float distPath = graph.distance(vertexes.get(i), vertexes.get(j));
					if (distPath > 0)
						distance += (float) 1 / distPath;
				}
			}
			if (size > 1)
				kppCentrality = distance / (size - 1);
			vertexes.get(i).setCentrality(kppCentrality);			
		}
	}
	
	private void computeKppBellmanFordCentrality(MyGraph graph) {
		// Weight vertexes by kpp centrality (BellmanFord distance)
		ArrayList<MyVertex> vertexes = graph.getDisambiguationNodes();
		int size = vertexes.size();
		for (MyVertex node : vertexes) {
			float kppCentrality = 0;
			Map<MyVertex, Float> paths = graph.BellmanFord(node);
			for (MyVertex dest : paths.keySet()) {
				float distance = paths.get(dest);
				// avoid self and cluster nodes
				if (distance > 0.0 && distance != (float)Integer.MAX_VALUE) {
					//graph.log(Globals.logWarning, "[BF] " + node.getId() + "-" + dest.getId() + " = " + distance);
					kppCentrality += (float) 1 / distance;
				}
			}
			// Do normalization
			if (size > 1)
				kppCentrality = kppCentrality / (size - 1);
			node.setCentrality(kppCentrality);			
		}
	}
	
	/**
	 * Compute page rank on graph nodes iteratively:
	 * PR(i, t+1) = sum_(i,j)inE_(PR(j, t)/OutDegree(j)
	 * @param graph
	 */
	private void computeRealIterativePageRankCentrality(MyGraph graph) {
		ArrayList<MyVertex> nodes = graph.getNodes();
		// random-surfer: close to 0 -> more random, close to 1 -> less
		float alpha = Globals.dampingFactor;
		int size = nodes.size();
		// Set initial page rank centrality
		for (MyVertex node : nodes) {
			node.setCentrality(round((float) 1 / size));
		}
		// Compute now iteratively until convergence
		boolean convergence = false;
		float[] sums = new float[size];
		int cycle = 0;
		while (convergence == false && cycle < 5) {
			convergence = true;
			int j = 0;
			for (MyVertex node : nodes) {
				float oldPageRank = node.getCentrality();
				float sum = 0;
				for (MyEdge e : node.getEdges()) {
					// Avoid ghost edges
					if (e.getWeight() == -1)
						continue;
					sum += e.getDest().getCentrality() / e.getDest().getOutDegree();
					// random-surfer adjustment
					sum *= alpha;
					sum += (1-alpha)/size;
				}
				//System.out.println("node " + node.getId() + "  " + sums[j] + " --> " + sum);
				sums[j] = sum;
				j++;
			}
			// Update centralities and verify if convergence has been found 
			for (int i = 0; i < nodes.size(); i++) {
				MyVertex node = nodes.get(i);
				float pagerank = round(node.getCentrality());
				float sum = round(sums[i]);
				if (pagerank == sum)
					continue;
				node.setCentrality(sum);
				//System.out.println(sum + "  " + pagerank);
				convergence = false;
			}
			cycle++;
		}
	}
	private float round(float f) {
		return (float) Math.round(f * Globals.precision) / Globals.precision;
	}

	/**
	 * Compute page rank on graph nodes iteratively:
	 * PR(i, t+1) = sum_(i,j)inE_(PR(j, t)/OutDegree(j)
	 * @param graph
	 */
	private void computeIterativePageRankCentrality(MyGraph graph) {
		ArrayList<MyVertex> nodes = graph.getNodes();
		// random-surfer: close to 0 -> more random, close to 1 -> less
		float alpha = Globals.dampingFactor;
		int size = nodes.size();
		// Set initial page rank centrality
		for (MyVertex node : nodes) {
			node.setCentrality((float) 1 / size);
		}
		// Compute now iteratively until convergence
		boolean convergence = false;
		while (convergence == false) {
			convergence = true;
			for (MyVertex node : nodes) {
				float oldPageRank = node.getCentrality();
				float sum = 0, pagerank = 0;
				for (MyEdge e : node.getEdges()) {
					// Avoid ghost edges
					if (e.getWeight() == -1)
						continue;
					sum += e.getDest().getCentrality() / e.getDest().getOutDegree();
					// random-surfer adjustment
					sum *= alpha;
					sum += (1-alpha)/size;
				}
				// pagerank a bit false because I have to update the centralities after the iteration on nodes
				node.setCentrality(sum);
				pagerank = node.getCentrality();
				if (pagerank != oldPageRank) {
					convergence = false;
					//System.out.println(oldPageRank + " -> " + pagerank);
				}
			}
		}
	}

	// TODO: sistemare bene fino a convergenza
	private void computePageRankVertexCentrality(MyGraph graph, float alpha) {
		// Weight vertexes by page rank centrality
		ArrayList<MyVertex> vertices = graph.getNodes();
		int nVertici = vertices.size();
		for (int i = 0; i < nVertici; i++) {
			float pageRankCentrality = pageRank(graph, vertices.get(i), nVertici, alpha, 5);
			vertices.get(i).setCentrality(pageRankCentrality);
		}
	}
	
	private float pageRank(MyGraph graph, MyVertex v, int nVertici, float alpha, int step) {
		if (step == 0)
			return 1 / nVertici;
		
		float pagerank = 0;
		pagerank = (1 - alpha)/nVertici;
		float sum = 0;
		for (MyEdge target : v.getEdges()) {
			sum += pageRank(graph, target.getDest(), nVertici, alpha, step-1) / target.getDest().getOutDegree();
		}
		return pagerank + alpha * sum;
	}
	
	/**
	 * Compute edge weight between v1 and v2, based on whatever measure 
	 * @param v1
	 * @param v2
	 * @return
	 */
	public float computeEdgeWeight(MyVertex v1, MyVertex v2) {
		return 1;
		/*if (v1.getTreeGlossRepr() != null && v2.getTreeGlossRepr() != null) {
			return this.kelp.computeTreeSimilarity(v1.getTreeGlossRepr(), v2.getTreeGlossRepr(), Globals.treeKernelType);
		} else {
			System.out.println("Edge weight can't be computed");
			return 0;
		}*/
	}
	
	/**
	 * Take the solution given by the tsp solver and create an output file that specifies the sense chosen
	 * for each word of the sentence
	 * @param graph
	 */
	private void generateOutputFile(MyGraph graph){
		
		PrintWriter log = null;
		try {
			//log used to write which sentences were composed only by one word and have not been disambiguated
			log = new PrintWriter(new FileWriter("log.txt", true));
			
			//open reader for the tspSolver output file
			BufferedReader tourFileReader = new BufferedReader(
					new FileReader(Globals.tspSolverPathToGTOURS+Globals.fileName+graph.getSentenceId()+".tour"));
			
			Map<Integer, MyVertex> disambiguationMap = new HashMap<Integer, MyVertex>();
			String line;
			boolean read = false;
			
			//read the tspSolverOutput file
			while((line = tourFileReader.readLine()) != null){
				if(line.equals("-1")){
					read = false;
				}
				if(read){
					MyVertex v = graph.getNodeByIndex(Integer.parseInt(line.trim())-1);//-1 because solver ids starts from 1, our vertices ids starts from 0
					disambiguationMap.put(v.getSentenceIndex(), v);
				}
				if(line.equalsIgnoreCase("TOUR_SECTION")){
					read = true;
				}
				if(line.contains("Length = ")){
					// Check that length is positive, otherwise we have a problem
					int length = Integer.parseInt(line.split(" ")[4]);
					if (length <= 0) {
						graph.log(Globals.logWarning, "[WARNING] Tour has a negative length of: " + length);
					}
				}
			}
			
			tourFileReader.close();
			//open writer to write results
			graph.log(Globals.logStatistics, this.printMapToFile(disambiguationMap, Globals.fileName, true, false));
			
		} catch (FileNotFoundException e) {
			//output file of tsp solver not created, the graph size wasn't >1
			log.write(Globals.fileName+graph.getId()+"\n");
			log.close();
			System.err.print(Thread.currentThread().getStackTrace()[1].getMethodName()+" threw: ");
			System.err.println(e);
		} catch (IOException e){
			System.err.print(Thread.currentThread().getStackTrace()[1].getMethodName()+" threw: ");
			System.err.println(e);
		}
	}
}
