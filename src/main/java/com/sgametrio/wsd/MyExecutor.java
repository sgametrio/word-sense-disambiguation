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
import java.time.Instant;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.*;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.jgrapht.alg.flow.PushRelabelMFImpl.VertexExtension;

import java.util.Set;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import edu.mit.jwi.item.IWord;
import edu.mit.jwi.item.IWordID;
import edu.mit.jwi.item.Pointer;
import edu.stanford.nlp.trees.Tree;
import evaluation.InputInstance;
import evaluation.InputSentence;

import com.sgametrio.wsd.KelpAdapter;

public class MyExecutor {
	
	//used for lemmatization
	private StanfordUtilsAdapter stanfordAdapter = null;  
	private WordnetAdapter wordnet = null;
	private KelpAdapter kelp = null;
	
	//saving params
	private String fileNameSentences = "sentences";
	private final Object fileLock = new Object();
	private final Object graphLock = new Object();
	//execution params
	
	
	
	private int trial = 1;
	
	//CONSTRUCTOR
	public MyExecutor(){	
		this.stanfordAdapter = new StanfordUtilsAdapter();
		this.wordnet = new WordnetAdapter();
		this.kelp = new KelpAdapter();	
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
		ArrayList<InputInstance> selectedInstances = this.mySelectPos(input.instances);

		input.instances.clear();
		input.instances.addAll(selectedInstances);
		//create the graph sentence		
		MyGraph graph = this.createDisambiguationGraph(input);
				
		if(Globals.saveGml){
			graph.saveToGML(Globals.gmlPath + Globals.fileName + graph.getId() + ".gml");
		}
		// Use centrality to disambiguate senses in a word
		if (!Globals.runSolver) {
			this.printCentralityDisambiguation(graph, false);
		} else {
			if (graph.getNodes().size() == 0) {
				// Graph has no vertexes, nothing to disambiguate, continue;
				System.out.println("Graph " + graph.getId() + " doesn't have words to disambiguate..");
			} else if (graph.getNodesIndexByClusters().size() == 1) {
				System.out.println("Graph " + graph.getId() + " has only 1 cluster, for now prints most common sense");
				Map<Integer, MyVertex> disambiguationMap = new HashMap<Integer, MyVertex>();
				MyVertex v = graph.getDisambiguationNodes().get(0);
				disambiguationMap.put(v.getSentenceIndex(), v);
				this.printMapToFile(disambiguationMap, Globals.fileName, Globals.evaluation, false);
				
			} else {
				// Write on file if something is weird (clusters with zero centrality)
				if (Globals.graphVerbosity) {
					if (graph.printUsefulInformation(Globals.graphsInfoPath + Globals.fileName + "_" + graph.getId() + "_" + graph.getSentenceId() + ".txt")) {
						// If something has been written save GML too
						graph.saveToGML(Globals.gmlPath + Globals.fileName + graph.getSentenceId() + ".gml");
					}
				}
				// Run solver to disambiguate senses
				if(graph.saveToGTSP(Globals.tspSolverPathToGTSPLIB, Globals.fileName+graph.getId())){
					this.setTSPSolver(graph.getId());
					if(this.runSolver(graph.getId())){
						this.generateOutputFile(graph);
					}
				} else {
					System.err.println("graph " + graph.getId() + " saveToGTSP returned false");
					graph.saveToGML(Globals.gmlPath + Globals.fileName+graph.getId() + ".gml");
				}
			}
			
		}
		System.out.println("Graph finished: " + graph.getId() + "_" + graph.getSentenceId());
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
			double vCentrality = v.getCentrality();
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
		this.printMapToFile(disambiguationMap, Globals.fileNameCentrality, Globals.evaluation, sentences);
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

	private void printMapToFile(Map<Integer, MyVertex> disambiguationMap, String fileName, boolean evaluation, boolean sentences) {
		// print Map ordered by key
		synchronized (fileLock) {
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
				   if(evaluation) { //evaluation mode output format
						if(v.getSentenceTermId() != null){
							System.out.println("Centrality " + v.getCentrality());
							if (v.getCentrality() == 0) {
								System.out.println("Disambiguated sense with centrality 0");
							}
							keyFileWriter.write(v.getSentenceTermId()+" "+v.getGlossKey()+"\n");
						}
					} else { //sentence wsd output format
						keyFileWriter.write("Disambiguated \""+v.getWord()+"\" as \n\t\t ("+v.getGlossKey()+") \""+this.wordnet.getGloss(v.getWord().getID())+"\"\n");
					}
				}
				keyFileWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
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
	private void setTSPSolver(int id){
		
		BufferedReader oldFileReader = null;
		BufferedWriter newFileWriter = null;
		String mySolverName = Globals.tspSolverFileName+id;
		
		try {
			
			oldFileReader = new BufferedReader(new FileReader(Globals.tspSolverPathFileName));
			newFileWriter = new BufferedWriter(new FileWriter(Globals.tspSolverHomeDir+mySolverName));
			
			//reads from the old file and write on the new one
			String line;
			while ((line = oldFileReader.readLine()) != null) {
				//System.out.println(line);
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
	public boolean runSolver(int id){
		
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

				System.err.print(errorStream);
				//if verbose mode is on, prints tsp solver output and errors
				if(Globals.verbose){
					System.out.println(output);
					System.out.println("____________________________________");
				}
				
				//if the error is a segmentation fault error, it can be solved running again the script
				//it tries again for a max of 100 times
				/*if(errorStream.contains("Segmentation fault")){
					if(this.trial<=100){
						System.out.println("Retrying computation. Trial #"+this.trial);
						this.trial++;
						runSolver();
					}					
				}else if((errorStream.contains("not greater than"))||errorStream.contains("dimension < 3")){
					System.out.println("The graph does not have enough node to run tsp-solver.");
					return false;
				}*/
				this.trial = 1;				
				
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
		MyGraph graph = null;
		synchronized (graphLock) {
			graph = new MyGraph();
		}		
		graph.setSentence(sentence.sentence);
		graph.setSentenceId(sentence.sentenceId);
		// for every instance, I have to find all senses
		for (InputInstance instance : sentence.instances) {
			this.myCreateNodes(graph, instance);
		}
		//this.myCreateEdges(graph);
		this.createNodesByDFS(graph, Globals.nodesDepth);
		if (Globals.centrality) {
			// Add support nodes to compute different centrality
			//this.addSupportNodes(graph, Globals.nodesDepth);
			this.computeVertexCentrality(graph);
			// Distribute centrality on edges
			this.distributeCentralityOnEdges(graph);
		}
		return graph;
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
							graph.addEdge(v, last, depth);
						}
						last = v;
					}
					if (graph.distance(start, last) == -1 && !start.equals(last)) {
						graph.addEdge(start, last, depth);
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
				double mean = graph.computeMeanCentrality(v, e.getDest());
				if (mean == 0)
					System.out.println("Graph " + graph.getSentenceId() + " with mean 0 on an edge");
				else
					e.setWeight(mean*e.getWeight());
			}
		}	
	}

	/**
	 * Create an edge between every node in the graph and weigh this edge as the syntactic similarity
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
					// Do not create edges between distant words (Let's say 10 words distance)
					//if (Math.abs(vertices.get(i).getSentenceIndex() - vertices.get(j).getSentenceIndex()) < 10) {
						// Se due sensi disambiguano due sentence index diversi e hanno la stessa gloss key allora ha senso computare
						if ( ! ( vertices.get(i).getGlossKey().equalsIgnoreCase(vertices.get(j).getGlossKey()))){

							// Compute similarity here
							double edgeWeight = this.computeEdgeWeight(vertices.get(i), vertices.get(j));
							
							graph.addEdge(vertices.get(i), vertices.get(j), edgeWeight);
						}
					//}
				}
			}
		}
		
	}


	private void myCreateNodes(MyGraph graph, InputInstance input) {
		//for all the WordNet glosses of that word and its lemma
		for(IWord word : this.wordnet.getWordsList(input.lemma, input.pos)) {
			String gloss = word.getSynset().getGloss().split("\"")[0];
			//compute dependency trees for the gloss
			ArrayList<Tree> treeRepresentations = stanfordAdapter.computeDependencyTree(gloss);
			if(treeRepresentations.size()>1){
				//if the gloss is composed by multiple sentences, there could be more dependency tree
				//in this case a message is given and only the first tree is considered
				System.out.println("More than one tree representation for a gloss of \""+input.lemma+"\" ("+gloss+") computed. Took the first one.");
			}
			MyVertex v = new MyVertex(input, word, treeRepresentations.get(0).toString(), gloss);
			graph.addNode(v);
		}		
	}


	private void computeVertexCentrality(MyGraph supportGraph) {
		switch (Globals.computeCentrality) {
			case Globals.allCentrality: 
				this.computeKppVertexCentrality(supportGraph);
				//this.computePageRankVertexCentrality(supportGraph, 0.5);
				//this.computeInDegVertexCentrality(supportGraph);
				break;
			case Globals.kppCentrality:
				this.computeKppVertexCentrality(supportGraph);
				break;
			case Globals.pageRankCentrality:
				//this.computePageRankVertexCentrality(supportGraph, 0.5);
				break;
			case Globals.inDegreeCentrality:
				//this.computeInDegVertexCentrality(supportGraph);
				break;
			default:
				this.computeKppVertexCentrality(supportGraph);
				break;
		}
			
	}

	/**
	 * For every vertex in the graph we search related senses in wordnet.
	 * If we find common related senses between different vertexes with different sentence indexes,
	 * we add this word to the graph so we can compute centrality based also on these support nodes
	 * @param graph
	 * @param depth
	 */
	private void addSupportNodes(MyGraph graph, int depth) {
		if (depth == 0) return;
		Map<IWord, ArrayList<MyVertex>> relatedWords = new HashMap<IWord, ArrayList<MyVertex>>();
		ArrayList<MyVertex> vertexes = graph.getNodes();
		for (MyVertex v : vertexes) {
			// Prendo tutte le word dal synset, e dai synsets correlati
			ArrayList<IWord> vRelatedWords = v.getRelatedWords();
			// Avoid searching two times the same word
			if (vRelatedWords == null) {
				vRelatedWords = this.wordnet.getSynonymsAndRelatedWords(v.getWord().getID());
				v.setRelatedWords(vRelatedWords);
			}
			
			for (IWord word : vRelatedWords) {
				// Se la parola esiste già nel mio grafo non devo aggiungerla
				if (graph.containsWord(word))
					continue;
				
				ArrayList<MyVertex> tempVertexes;
				if (relatedWords.containsKey(word)) {
					tempVertexes = relatedWords.get(word);
				} else {
					tempVertexes = new ArrayList<MyVertex>();
				}
				tempVertexes.add(v);
				relatedWords.put(word, tempVertexes);
			}
		}
		// Per ogni parola correlata ho tutti i vertici che l'hanno in comune
		// Se ha almeno 2 vertici in comune allora controllo se esiste già nel mio grafo
		// Se non esiste creo il vertice e computo la relazione fra tutti i vertici che ha
		for (IWord w : relatedWords.keySet()) {
			ArrayList<MyVertex> possibleVertexes = relatedWords.get(w);
			// Se una parola è collegata ad almeno due sensi che disambiguano due parole diverse allora
			ArrayList<Integer> differentIndexes = new ArrayList<Integer>();
			for (MyVertex vv : possibleVertexes) {
				if (!differentIndexes.contains(vv.getSentenceIndex())) {
					differentIndexes.add(vv.getSentenceIndex());
				}
			}
			if (differentIndexes.size() < 2) {
				continue;
			}
			String gloss = w.getSynset().getGloss().split("\"")[0];
			
			//compute dependency trees for the gloss
			ArrayList<Tree> treeRepresentations = stanfordAdapter.computeDependencyTree(gloss);
			// Più di un vertice correlato, creo il nodo e gli collego i vertici
		
			MyVertex temp = new MyVertex(w, treeRepresentations.get(0).toString());
			graph.addNode(temp);
			// Collego tutti i vertici a quel nodo
			for (MyVertex v : possibleVertexes) {
				double weight = this.computeEdgeWeight(v, temp);
				graph.addEdge(v, temp, weight);
			}
		}
		this.addSupportNodes(graph, depth-1);
	}
	
	/**
	 * Compute KPP centrality on every nodes present in the graph.
	 * KPP is computed as sum of the distance divided by nodes cardinality.
	 * Edge weight here is node similarity (higher is more similar, so, the node, is more central).
	 * So I do not reverse edge weight like the original formula says (because the original formula counts on node distance)
	 * @param graph
	 */
	private void computeKppVertexCentrality(MyGraph graph) {
		// Weight vertexes by kpp centrality
		ArrayList<MyVertex> vertexes = graph.getNodes();
		int size = vertexes.size();
		for (int i = 0; i < size; i++) {
			double kppCentrality = 0;
			double distance = 0;
			for (int j = 0; j < size; j++) {
				if (i != j) {
					double distPath = graph.distance(vertexes.get(i), vertexes.get(j));
					// Check if path exists (or edge)
					if (distPath > 0)
						distance += distPath;
				}
			}
			if (size > 1)
				kppCentrality = distance / (size - 1);
			vertexes.get(i).setCentrality(kppCentrality);			
		}
	}
	
	// TODO: sistemare bene
	private void computePageRankVertexCentrality(MyGraph graph, double alpha) {
		// Weight vertexes by page rank centrality
		ArrayList<MyVertex> vertices = graph.getNodes();
		int nVertici = vertices.size();
		for (int i = 0; i < nVertici; i++) {
			double pageRankCentrality = pageRank(graph, vertices.get(i), nVertici, alpha, 5);
			vertices.get(i).setCentrality(pageRankCentrality);
			//System.out.println("Vertice " + vertices.get(i).getId() + " pagerank " + vertices.get(i).getPageRankCentrality());
		}
	}
	
	private double pageRank(MyGraph graph, MyVertex v, int nVertici, double alpha, int step) {
		if (step == 0)
			return 1 / nVertici;
		
		double pagerank = 0;
		pagerank = (1 - alpha)/nVertici;
		double sum = 0;
		for (MyEdge target : v.getEdges()) {
			sum += pageRank(graph, target.getDest(), nVertici, alpha, step-1) / target.getDest().getOutDegree();
		}
		return pagerank + alpha * sum;
	}
	
	public double computeEdgeWeight(MyVertex v1, MyVertex v2) {
		if (v1.getTreeGlossRepr() != null && v2.getTreeGlossRepr() != null) {
			return this.kelp.computeTreeSimilarity(v1.getTreeGlossRepr(), v2.getTreeGlossRepr(), Globals.treeKernelType);
		} else {
			System.out.println("Edge weight can't be computed");
			return 0;
		}
		
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
					new FileReader(Globals.tspSolverPathToGTOURS+Globals.fileName+graph.getId()+".tour"));
			
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
						System.out.println("[WARNING] Tour has a negative length of: " + length + " on sentence " + graph.getSentenceId());
					}
				}
			}
			
			tourFileReader.close();
			//open writer to write results
			this.printMapToFile(disambiguationMap, Globals.fileName, true, false);
			
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
