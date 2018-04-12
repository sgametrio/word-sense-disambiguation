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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import additional.ClosenessCentrality;
import dk.aaue.sna.alg.centrality.EigenvectorCentrality;

import org.jgrapht.alg.scoring.PageRank;
import org.jgrapht.graph.DefaultWeightedEdge;

import edu.mit.jwi.item.IWord;
import evaluation.InputInstance;
import evaluation.InputSentence;

public class JExecutor {
	private WordnetAdapter wordnet = null;
	//saving params
	private final Object fileLock = new Object();
	private final Object graphLock = new Object();
	//execution params
	
	
	
	//CONSTRUCTOR
	public JExecutor(){
		this.wordnet = new WordnetAdapter();
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
		JGraph dGraph = null;
		JGraph cGraph = null;
		synchronized(this.graphLock) {
			dGraph = new JGraph(input.sentence, input.sentenceId);
			cGraph = new JGraph(input.sentence, input.sentenceId);
		}
		ArrayList<JNode> senses = this.disambiguateInstances(input.instances);
		// Add disambiguation nodes to both graphs
		for (JNode n : senses) {
			dGraph.addVertex(n);
			cGraph.addVertex(n);
		}
		// Add auxiliary nodes to compute centrality
		this.addDFSNodes(cGraph);
		// Use centrality to disambiguate senses
		Map<JNode, Double> scores = this.computeCentrality(cGraph);
		// Distribute centralities on edges
		this.createEdgesByCentrality(dGraph);
		cGraph.exportCustomGml(Globals.gmlPath + Globals.fileName + dGraph.getSentenceId() + "_centrality.gml");
		if (!Globals.runSolver) {
			Map<Integer, JNode> map = this.centralityDisambiguation(scores);
			cGraph.log(Globals.logStatistics, this.printMapToFile(map, Globals.fileName));
		} else {
			if (dGraph.vertexSet().size() == 0) {
				// Graph has no vertexes, nothing to disambiguate, continue;
				dGraph.log(Globals.logInfo, "[NO WORDS] don't have words to disambiguate.");
			} else if (dGraph.getClusters().size() == 1) {
				dGraph.log(Globals.logInfo, "[1 CLUSTER] has only 1 cluster, for now prints most common sense");
				Map<Integer, JNode> disambiguationMap = new HashMap<Integer, JNode>();
				JNode v = dGraph.getFirstVertex();
				disambiguationMap.put(v.getSentenceIndex(), v);
				dGraph.log(Globals.logStatistics, this.printMapToFile(disambiguationMap, Globals.fileName));
			} else {
				// Run solver to disambiguate senses
				Instant beforeTSP = Instant.now();
				dGraph.saveToGTSP(Globals.tspSolverPathToGTSPLIB, Globals.fileName+dGraph.getSentenceId());
				this.setTSPSolver(dGraph.getSentenceId());
				if(this.runSolver(dGraph)) {
					Instant afterTSP = Instant.now();
					Duration dTSP = Duration.between(beforeTSP, afterTSP);
					dGraph.log(Globals.logStatistics, "[TIME][TSP] " + dTSP.toString());
					this.generateOutputFile(dGraph);
				}
			}
			
		}
		dGraph.exportCustomGml(Globals.gmlPath + Globals.fileName + dGraph.getSentenceId() + ".gml");
		Instant after = Instant.now();
		Duration time = Duration.between(before, after);
		dGraph.log(Globals.logStatistics, "[FINISHED] " + time);
		System.out.println("[GRAPH " + dGraph.getSentenceId() + "][FINISHED] Time: " + time);
		if (Globals.runSolver) {
			dGraph.logOnFile();
		} else {
			cGraph.logOnFile();
		}
		
		
	}
	
	
	private ArrayList<JNode> disambiguateInstances(ArrayList<InputInstance> instances) {
		ArrayList<JNode> all = new ArrayList<JNode>();
		for (InputInstance i : instances) {
			all.addAll(this.disambiguateSingleInstance(i));
		}
		return all;
	}

	private ArrayList<JNode> disambiguateSingleInstance(InputInstance input) {
		ArrayList<JNode> senses = new ArrayList<JNode>();
		for(IWord word : this.wordnet.getWordsList(input.lemma, input.pos)) {
			JNode v = new JNode(word, input);
			senses.add(v);
		}
		return senses;
	}

	/**
	 * From scores to disambiguation
	 * @param scores
	 * @return map<cluster, sense> which has disambiguated one sense per cluster
	 */
	private Map<Integer, JNode> centralityDisambiguation(Map<JNode, Double> scores) {
		Map<Integer, JNode> disambiguationMap = new HashMap<Integer, JNode>();
		// Readable results
		for (JNode v : scores.keySet()) {
			int sentenceIndex = v.getSentenceIndex();
			if (sentenceIndex < 0)
				continue;
			double centrality = scores.get(v);
			// per ogni sentence index devo scegliere uno e un solo nodo
			// Se la map contiene un nodo, lo sostituisco solo se ha maggiore centralità
			if (disambiguationMap.containsKey(sentenceIndex)) {
				if (scores.get(disambiguationMap.get(sentenceIndex)) < centrality) {
					disambiguationMap.replace(sentenceIndex, v);
				}
			} else {
				disambiguationMap.put(sentenceIndex, v);
			}
		}
		return disambiguationMap;
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

	private String printMapToFile(Map<Integer, JNode> disambiguationMap, String fileName) {
		// print Map ordered by key
		String log = "[SENTENCE TERMS]\n";
		synchronized (fileLock) {
			String fileContent = "";
			try {
				PrintWriter keyFileWriter;
				keyFileWriter = new PrintWriter(new FileWriter(Globals.resultsPath + fileName +Globals.resultsExt, true));
				//sort word by their position in the text
				SortedSet<Integer> keys = new TreeSet<>(disambiguationMap.keySet());
				for (Integer key : keys) { 
					JNode v = disambiguationMap.get(key);
					if(v.getTermId() != null){
						log += v.getTermId() + " " + v.getSenseKey() + " " + v.getCentrality() + "\n";
						fileContent += v.getTermId()+" "+v.getSenseKey()+"\n";
					}
				}
				keyFileWriter.write(fileContent);
				keyFileWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return log;
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
	public boolean runSolver(JGraph graph){
		String id = graph.getSentenceId();
		File solver = new File(Globals.tspSolverPathFileName+id);
		if (solver.exists()) {
			try{
				//Set permissions
				// TODO: Dynamic check Operating System
				Set<PosixFilePermission> perms = new HashSet<>();
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
            graph.log(Globals.logSevere, "Solver file does not exist");
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
	 * Add nodes and edges to the graph by DFSing WordNet graph 
	 * @param centralityGraph
	 */
	private void addDFSNodes(JGraph centralityGraph) {
		int nodesDepth = Globals.nodesDepth;
		Set<JNode> nodes = centralityGraph.vertexSet();
		Map<IWord, JNode> wordMap = new ConcurrentHashMap<IWord, JNode>();
		Map<IWord, JNode> originalWordMap = new ConcurrentHashMap<IWord, JNode>();
		// Create a map to better lookup words
		for (JNode node : nodes) {
			if (!wordMap.containsKey(node.getWord())) {
				wordMap.put(node.getWord(), node);
				originalWordMap.put(node.getWord(), node);
			}
		}
		
		for (JNode node : originalWordMap.values()) {
			Stack<IWord> path = new Stack<IWord>();
			this.computeDFS(centralityGraph, nodesDepth, originalWordMap, wordMap, path, node, node.getWord());
		}		
	}

	/**
	 * Execute DFS on WordNet graph, adding edges and vertexes if find path from start to another node in the graph
	 * @param centralityGraph
	 * @param depth
	 * @param wordMap
	 * @param path
	 * @param node
	 */
	private void computeDFS(JGraph centralityGraph, int depth, Map<IWord, JNode> originalWordMap, Map<IWord, JNode> wordMap, Stack<IWord> path, JNode node, IWord current) {
		if (depth == 0)
			return;
		for (IWord w : wordnet.getAllRelatedWords(current)) {
			// Do not create edges between words that disambiguate the same sentence word (index)
			if (originalWordMap.containsKey(w)) {
				JNode last = wordMap.get(w);
				if (!current.equals(w) && last.getSentenceIndex() != node.getSentenceIndex()) {
					JNode v = null;
					for (IWord w1 : path) {
						// Do not create again nodes representing a word already in the map
						if (wordMap.containsKey(w1)) {
							v = wordMap.get(w1);
						} else {
							v = new JNode(w1);
							centralityGraph.addVertex(v);
							wordMap.put(w1, v);
						}
						if (!v.equals(last)) {
							DefaultWeightedEdge edge = new DefaultWeightedEdge();
							centralityGraph.addEdge(v, last, edge);
						}
						last = v;
					}
					if (!node.equals(last)) {
						DefaultWeightedEdge edge = new DefaultWeightedEdge();
						centralityGraph.addEdge(node, last, edge);
					}
				}	
			} else {
				path.push(w);
				this.computeDFS(centralityGraph, depth-1, originalWordMap, wordMap, path, node, w);
				path.pop();
			}
		}
		
	}

	/**
	 * Set edge weight between every node in the graph as the centrality mean between these nodes
	 * @param graph
	 */
	private void createEdgesByCentrality (JGraph graph) {
		Set<JNode> nodes = graph.vertexSet();
		for (JNode n1 : nodes) {
			for (JNode n2 : nodes) {
				if (n1.equals(n2))
					continue;
				if (n1.getSentenceIndex() != n2.getSentenceIndex()) {
					// Insert distance instead of weight here by 1 / weight (weight is in [0,1]
					// Multiply by precision
					double mean = JNode.mean(n1, n2);
					if (mean == 0.0)
						continue;
					// Globals.precision let me lose only little info (decimal values)
					double distance = Math.round(Globals.precision * (1 - mean));
					DefaultWeightedEdge e = graph.addEdge(n1, n2);
					// Edge already exists
					if (e == null)
						continue;
					graph.setEdgeWeight(e, distance);
				}
			}
		}	
	}

	/**
	 * Create an edge between every node in the graph and weigh this edge
	 * @param graph
	 */
	private void addAllEdgesBetweenClusters(JGraph graph) {
		//Edge creation
		Set<JNode> nodes = graph.vertexSet();
		for (JNode node : nodes) {
			for (JNode node2 : nodes) {
				// Do not create edge between same cluster node
				if (node.getSentenceIndex() != node2.getSentenceIndex()) {
					// addEdge doesn't create duplicates, default weight 1
					graph.addEdge(node, node2);
				}
			}
		}
	}

	private Map<JNode, Double> computeCentrality(JGraph centralityGraph) {
		switch (Globals.computeCentrality) {
			case Globals.kppBellmanFordCentrality: 
				//this.computeKppBellmanFordCentrality(centralityGraph);
			case Globals.kppCentrality:
				//this.computeKppSingleEdgeCentrality(centralityGraph);
			case Globals.pageRankCentrality:
				return this.computeIterativePageRankCentrality(centralityGraph);
			case Globals.inDegreeCentrality:
				//this.computeInDegreeCentrality(centralityGraph);
			case Globals.closenessCentrality:
				return this.computeClosenessCentrality(centralityGraph);
			case Globals.eigenvectorCentrality:
				return this.computeEigenvectorCentrality(centralityGraph);
			default:
				//this.computeKppSingleEdgeCentrality(centralityGraph);
			return null;
		}
			
	}

	private Map<JNode, Double> computeIterativePageRankCentrality(JGraph graph) {
		PageRank<JNode, DefaultWeightedEdge> cc = new PageRank<JNode, DefaultWeightedEdge>(graph);
		Map<JNode, Double> scores = cc.getScores();
		this.assignScores(graph, scores);
		return scores;
	}
	
	private Map<JNode, Double> computeEigenvectorCentrality(JGraph graph) {
		EigenvectorCentrality<JNode, DefaultWeightedEdge> ec = new EigenvectorCentrality<JNode, DefaultWeightedEdge>(graph);
		Map<JNode, Double> scores = ec.calculate().getRaw();
		this.assignScores(graph, scores);
		return scores;
	}

	/**
	 * Seems to not work (give 0.0 to all)
	 * @param graph
	 * @return
	 */
	private Map<JNode, Double> computeClosenessCentrality(JGraph graph) {
		ClosenessCentrality<JNode, DefaultWeightedEdge> cc = new ClosenessCentrality<JNode, DefaultWeightedEdge>(graph);
		cc.compute();
		Map<JNode, Double> scores = cc.getScores();
		this.assignScores(graph, scores);
		return scores;
	}

	private void assignScores(JGraph graph, Map<JNode, Double> scores) {
		for (Entry<JNode, Double> entry : scores.entrySet()) {
			entry.getKey().setCentrality(entry.getValue());
		}
	}

	/**
	 * Take the solution given by the tsp solver and create an output file that specifies the sense chosen
	 * for each word of the sentence
	 * @param graph
	 */
	private void generateOutputFile(JGraph graph){
		synchronized (fileLock) {
			try {
				//open reader for the tspSolver output file
				BufferedReader tourFileReader = new BufferedReader(
						new FileReader(Globals.tspSolverPathToGTOURS+Globals.fileName+graph.getSentenceId()+".tour"));
				
				Map<Integer, JNode> disambiguationMap = new HashMap<Integer, JNode>();
				String line;
				boolean read = false;
				
				//read the tspSolverOutput file
				ArrayList<JNode> array = graph.getVertexArray();
				while((line = tourFileReader.readLine()) != null){
					if(line.equals("-1")) {
						read = false;
					}
					if(read){
						int index = Integer.parseInt(line.trim())-1;
						JNode v = array.get(index);
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
				graph.log(Globals.logStatistics, this.printMapToFile(disambiguationMap, Globals.fileName));

			} catch (IOException e){
				System.err.print(Thread.currentThread().getStackTrace()[1].getMethodName()+" threw: ");
				System.err.println(e);
			}
		}
		
	}
}
