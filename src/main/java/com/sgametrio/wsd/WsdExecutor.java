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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import edu.mit.jwi.item.IWord;
import edu.mit.jwi.item.IWordID;
import edu.stanford.nlp.trees.Tree;

import com.sgametrio.wsd.KelpAdapter;

public class WsdExecutor {
	
	//used for lemmatization
	private StanfordUtilsAdapter stanfordAdapter = null;  
	private WordnetAdapter wordnet = null;
	private KelpAdapter kelp = null;
	
	//saving params
	private String fileName = "senseval3_subTrees";
	private  int progrSaveName = 1;
	private String fileNameCentrality = "centrality";
	private String fileNameSentences = "sentences";
	private  int progrSaveNameCentrality = 1;
	
	//execution params
	private String treeKernelType = "subTree"; //subTree, subsetTree, partialTree, smoothedPartialTree
	private boolean saveExamples = false;
	private boolean saveGml = false;
	private boolean runSolver = false;
	private boolean evaluation = false;
	private boolean centrality = false;
	public boolean verbose = false;
	
	private int trial = 1;
	
	//CONSTRUCTOR
	public WsdExecutor(){	
		this.stanfordAdapter = new StanfordUtilsAdapter();
		this.wordnet = new WordnetAdapter();
		this.kelp = new KelpAdapter();	
	}
	
	
	/**
	 * Calls all the functions needed to perform the disambiguation when param is a sentence
	 * @param sentence
	 * @param centrality 
	 */
	//@overload
	public void performDisambiguation(String sentence, Boolean centrality){
		
		//compute POS and Lemmas of the given sentence
		HashMap<String, ArrayList<String[]>> pos_lemmaWordIndexParams = 
				this.stanfordAdapter.calculateLemmasAndPOS(sentence);
		this.performDisambiguation(pos_lemmaWordIndexParams, centrality, true);
	}
	
	
	/**
	 * Calls all the functions needed to perform the disambiguation
	 * @param pos_lemmaWordIndexParamsMAP a map having word POS as keys and an array containing
	 * the lemma, the word as it was written in the sentence, the index of the word in the sentence and
	 * the params of the word given in the evaluation framework
	 */
	public void performDisambiguation(HashMap<String, ArrayList<String[]>> pos_lemmaWordIndexParamsMAP, boolean centrality, boolean sentences){
		HashMap<String, ArrayList<String[]>> selectedPos = this.selectPos(pos_lemmaWordIndexParamsMAP);
		//create the graph
		WsdGraph graph = this.createDisambiguationGraph(selectedPos, centrality);
		
		//save gml (optional)
		if(this.saveGml){
			if (sentences) {
				graph.saveToGML(Globals.gmlPath, this.fileNameSentences+this.progrSaveName);
			} else {
				graph.saveToGML(Globals.gmlPath, this.fileName+this.progrSaveName);
			}
		}
		
		this.createResultsDir();
		// Use centrality to disambiguate senses in a word
		if (centrality) {
			this.printCentralityDisambiguation(graph, sentences);
		} else {
			//save to gtsp (NEEDED). The solver and all dependent methods are invoked only if the graph is not empty
			if(graph.saveToGTSP(Globals.tspSolverPathToGTSPLIB, this.fileName+this.progrSaveName)){
				this.setTSPSolver();
				if(this.getRunSolver()) {
					if(this.runSolver()){
						this.generateOutputFile(graph);
					}
				}	
			}
		}
		
		System.out.println(this.progrSaveName);
		//the following must be the last instruction of this method
		this.progrSaveName++;
		
	}
	
	
	private void createResultsDir() {
		// if the directory  for RESULTS does not exist, create it
		File resDir = new File("RESULTS");
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


	private void printCentralityDisambiguation(WsdGraph graph, boolean sentences) {
		// Map<Sentence Index, Senso corretto> 
		// Contiene i sensi disambiguati
		Map<Integer, WsdVertex> disambiguationMap = new HashMap<Integer, WsdVertex>();
		// Readable results
		for (WsdVertex v : graph.getVerticesList()) {
			int sentenceIndex = v.getSentenceIndex();
			if (sentenceIndex < 0)
				continue;
			// KPP centrality for now
			double vCentrality = v.getKppCentrality();
			// per ogni sentence index devo scegliere uno e un solo nodo
			// Se la map contiene un nodo, lo sostituisco solo se ha maggiore centralità
			if (disambiguationMap.containsKey(sentenceIndex)) {
				if (disambiguationMap.get(sentenceIndex).getKppCentrality() < vCentrality) {
					disambiguationMap.put(sentenceIndex, v);
				}
			} else {
				disambiguationMap.put(sentenceIndex, v);
			}
		}
		// Print results
		//System.out.print("Printing to file.. ");
		this.printMapToFile(disambiguationMap, this.fileNameCentrality, this.evaluation, sentences);
		// results in .key format
	}


	private void printMapToFile(Map<Integer, WsdVertex> disambiguationMap, String fileName, boolean evaluation, boolean sentences) {
		// print Map ordered by key
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
			   WsdVertex v = disambiguationMap.get(key);
			   if(evaluation) { //evaluation mode output format
					if(v.getParams() != null){
						keyFileWriter.write(v.getParams()+" "+v.getGlossKey()+"\n");
					}
				} else { //sentence wsd output format
					keyFileWriter.write("Disambiguated \""+v.getWord()+"\" as \n\t\t ("+v.getGlossKey()+") \""+v.getGloss()+"\"\n");
				}
			}
			keyFileWriter.close();
			this.progrSaveNameCentrality++;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	/**
	 * Eliminate the words having POS not specified in posTags variable
	 * @param oldMap: the map containing all the word of the sentence divided by POS
	 * @return a map containing only the word having the "interesting" POS
	 */
	private HashMap<String, ArrayList<String[]>> selectPos(HashMap<String, ArrayList<String[]>> oldMap){
		
		String[] posTags;
		if(this.evaluation){
			//if evaluation is performed, it considers wordnet tags;
			String[] evaluationPosTags = {"NOUN", "VERB", "ADJ", "ADV"};
			posTags = evaluationPosTags;
		}else{
			//if the disambiguation is performed giving sentences as input, it considers Stanford Parser tags;
			String[] stanfordTags = {"JJ","JJR","JJS","NN","NNS",
					"NNP","NNPS","RB","RBR","RBS",
					"MD", "VB","VBD","VBG","VBN",
					"VBP","VBZ"};
			posTags = stanfordTags;
		}
		
		//select only the words belonging to the specified ("interesting") POS
		HashMap<String, ArrayList<String[]>> selectedPos = new HashMap<String, ArrayList<String[]>>();
		for(String tag : posTags){
			if(oldMap.containsKey(tag)){
				selectedPos.put(tag, oldMap.get(tag));
			}
		}
		
		return selectedPos;
		
	}
	
	
	/**
	 * Set configuration parameters by dynamically update configuration file
	 * Change param of TSPSolver main file to set input and output files
	 */
	private void setTSPSolver(){
		
		BufferedReader oldFileReader = null;
		BufferedWriter newFileWriter = null;
		String tempFileName = "tmp_"+Globals.tspSolverFileName;
		
		try {
			
			oldFileReader = new BufferedReader(new FileReader(Globals.tspSolverPathFileName));
			newFileWriter = new BufferedWriter(new FileWriter(Globals.tspSolverHomeDir+tempFileName));
			
			//reads from the old file and write on the new one
			String line;
			while ((line = oldFileReader.readLine()) != null) {
				//System.out.println(line);
				if (line.contains("PROBLEM_FILE")){
					line = "echo \"PROBLEM_FILE = " + Globals.GTSPLIBDirectory + this.fileName+this.progrSaveName+".gtsp\" > $par";
				}
				if (line.contains("OUTPUT_TOUR_FILE")){
					line = "echo \"OUTPUT_TOUR_FILE = " + Globals.GTOURSDirectory + this.fileName+this.progrSaveName+".tour\" >> $par";
				}
				if (line.contains("RUNS")) {
					line = "echo \"RUNS = " + Globals.runs + "\" >> $par";
				}
				newFileWriter.write(line+"\n");
			}
			
			oldFileReader.close();
			newFileWriter.close();
			
			//delete old file
			File oldFile = new File(Globals.tspSolverPathFileName);
			oldFile.delete();
			
			// And rename tmp file to old file name
			File newFile = new File(Globals.tspSolverHomeDir+tempFileName);
			newFile.renameTo(oldFile);
			
		}catch(IOException e){
			System.err.print(Thread.currentThread().getStackTrace()[1].getMethodName()+" threw: ");
			System.err.println(e);
		}
		
	}
	

	/**
	 * Runs the tspSolver script. Results are saved in G-TOURS folder.
	 * @return true if the solver completed its task, false otherwise
	 */
	public boolean runSolver(){
		
		File solver = new File(Globals.tspSolverPathFileName);
		if (solver.exists()) {
			try{
				//Set permissions
				Set<PosixFilePermission> perms = new HashSet();
				perms.add(PosixFilePermission.OWNER_READ);
				perms.add(PosixFilePermission.OWNER_WRITE);

				Files.setPosixFilePermissions(solver.toPath(), perms);
				solver.setExecutable(true);
				solver.setReadable(true);
				solver.setWritable(true);
				//command to execute the solver
				ProcessBuilder process = new ProcessBuilder("./" + Globals.tspSolverFileName);
				process.directory(new File(Globals.tspSolverHomeDir));

				Process p = process.start();
				//get the error stream
				//InputStream is = p.getErrorStream();
				//String errorStream = this.getProcessOutput(is);
				String output = this.getProcessOutput(p.getInputStream());

				//if verbose mode is on, prints tsp solver output and errors
				if(this.verbose){
					System.out.println(output);
					//System.out.println(errorStream);
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
	 * Create a graph having nodes that stores all the information in a PersonalVertex format and 
	 * edges in the JGraphT weighted graph format where the weight is computed using tree kernels 
	 * @param centrality 
	 * @param pos_lemmaWordIndexParams: a map having POS as key and a list of word (with their lemma
	 * index and evaluation params) having that POS as value
	 */
	private WsdGraph createDisambiguationGraph(Map<String, ArrayList<String[]>> pos_lemmaWordIndexParams, boolean centrality){
		
		WsdGraph graph = new WsdGraph();
		// Support graph on which we compute vertex centrality
		WsdGraph supportGraph = new WsdGraph();
		
		//Initialize the graph creating nodes
		Iterator<Map.Entry<String, ArrayList<String[]>>> it_p_lwip = pos_lemmaWordIndexParams.entrySet().iterator();
		
		//for all the POS-words pairs
		while (it_p_lwip.hasNext()) {
			
		    Map.Entry<String, ArrayList<String[]>> pair_p_lwip = (Map.Entry<String, ArrayList<String[]>>)it_p_lwip.next();
		    String posTag = pair_p_lwip.getKey();
		    
		    //for all the words having a given POS tag
		    for(String[] lemmaWordIndexParams: pair_p_lwip.getValue()){
		    	String searchTerm = lemmaWordIndexParams[0];
		    	String originalWord = lemmaWordIndexParams[1];
		    	String[] lemmaWord = {lemmaWordIndexParams[0],lemmaWordIndexParams[1]};
		    	String params = lemmaWordIndexParams[3];
		    	int sentenceIndex = Integer.parseInt(lemmaWordIndexParams[2]);
		    	this.createNodes(graph, sentenceIndex, searchTerm, originalWord, posTag, lemmaWord, params);
		    	if (centrality) {
		    		// Add nodes on support graph
			    	this.createNodes(supportGraph, sentenceIndex, searchTerm, originalWord, posTag, lemmaWord, params);
		    	}
		    	
		    	//if the lemma is different from the original word, search senses for both of them
		    	if(!searchTerm.equalsIgnoreCase(originalWord)){
		    		this.createNodes(graph, sentenceIndex, originalWord, originalWord, posTag, lemmaWord, params);
		    		if (centrality) {
		    			// Add nodes on support graph
			    		this.createNodes(supportGraph, sentenceIndex, originalWord, originalWord, posTag, lemmaWord, params);
		    		}
		    	}
		    }
		    it_p_lwip.remove();
		    
		}
		
		
		this.createEdges(graph);
		if (centrality) {
			// check if we can compute distances on support nodes
			int depth = 1;
			this.createEdges(supportGraph);
			this.addSupportNodes(supportGraph, depth);
			this.computeVertexCentrality(supportGraph);
			//supportGraph.saveToGML("GML/", "supportGraph");
			this.copyCentrality(supportGraph.getVerticesList(), graph.getVerticesList());
		}
		
		return graph;
	}

	private void computeVertexCentrality(WsdGraph graph) {
		switch (Globals.computeCentrality) {
			case Globals.allCentrality: 
				this.computeKppVertexCentrality(graph);
				this.computePageRankVertexCentrality(graph, 0.5);
				this.computeInDegVertexCentrality(graph);
				break;
			case Globals.kppCentrality:
				this.computeKppVertexCentrality(graph);
				break;
			case Globals.pageRankCentrality:
				this.computePageRankVertexCentrality(graph, 0.5);
				break;
			case Globals.inDegreeCentrality:
				this.computeInDegVertexCentrality(graph);
				break;
			default:
				this.computeKppVertexCentrality(graph);
				break;
		}
			
	}


	private void copyCentrality(ArrayList<WsdVertex> from, ArrayList<WsdVertex> to) {
		// nodes present in `from` ArrayList are present in `to` too
		for (int i = 0; i < to.size(); i++) {
			to.get(i).setCentrality(from.get(i).getCentrality());
			to.get(i).setKppCentrality(from.get(i).getKppCentrality());
			to.get(i).setInDegCentrality(from.get(i).getInDegCentrality());
			to.get(i).setPageRankCentrality(from.get(i).getPageRankCentrality());
		}
	}


	/**
	 * For every vertex in the graph we search related senses in wordnet.
	 * If we find common related senses between different vertexes with different sentence indexes,
	 * we add this word to the graph so we can compute centrality based also on these support nodes
	 * @param graph
	 * @param depth
	 */
	private void addSupportNodes(WsdGraph graph, int depth) {
		Map<IWord, ArrayList<WsdVertex>> relatedWords = new HashMap<IWord, ArrayList<WsdVertex>>();
		ArrayList<WsdVertex> vertexes = graph.getVerticesList();
		for (WsdVertex v : vertexes) {
			// TODO: Con che relazione ha senso cercare le parole?
			ArrayList<IWord> vRelatedWords1 = this.wordnet.getSynsetWords(v.getWordId());
			ArrayList<IWord> vRelatedWords2 = this.wordnet.getRelatedSynsetWords(v.getWordId());
			ArrayList<IWord> vRelatedWords = new ArrayList<IWord>();
			for (IWord word : vRelatedWords1) {
				vRelatedWords.add(word);
			}
			for (IWord word : vRelatedWords2) {
				if (!vRelatedWords.contains(word))
					vRelatedWords.add(word);
			}

			for (IWord word : vRelatedWords) {
				// Se la parola esiste già nel mio grafo non devo aggiungerla
				if (graph.containsWord(word))
					continue;
				
				ArrayList<WsdVertex> tempVertexes;
				if (relatedWords.containsKey(word)) {
					tempVertexes = relatedWords.get(word);
				} else {
					tempVertexes = new ArrayList<WsdVertex>();
				}
				tempVertexes.add(v);
				relatedWords.put(word, tempVertexes);
			}
		}
		// Per ogni parola correlata ho tutti i vertici che l'hanno in comune
		// Se ha almeno 2 vertici in comune allora controllo se esiste già nel mio grafo
		// Se non esiste creo il vertice e computo la relazione fra tutti i vertici che ha
		
		for (IWord w : relatedWords.keySet()) {
			ArrayList<WsdVertex> possibleVertexes = relatedWords.get(w);
			// Se una parola è collegata ad almeno due sensi che disambiguano due parole diverse allora
			ArrayList<Integer> differentIndexes = new ArrayList<Integer>();
			for (WsdVertex vv : possibleVertexes) {
				if (!differentIndexes.contains(vv.getSentenceIndex())) {
					differentIndexes.add(vv.getSentenceIndex());
				}
			}
			if (differentIndexes.size() < 2) {
				continue;
			}
			String[] glossAndSenseKey = this.wordnet.getGloss(w.getID());
			
			String[] glossExamples = glossAndSenseKey[0].split("\""); //separates glosses form examples
			
			//compute dependency trees for the gloss
			ArrayList<Tree> treeRepresentations = stanfordAdapter.computeDependencyTree(glossExamples[0]);
			// Più di un vertice correlato, creo il nodo e ci collego i vertici
			for (WsdVertex vv1 : possibleVertexes) {
				int sentenceIndex1 = vv1.getSentenceIndex();
				for (WsdVertex vv2 : possibleVertexes) {
					if (vv2.getSentenceIndex() != sentenceIndex1) {
						// Creo il nodo, e creo i due archi pesati
						WsdVertex temp = graph.addVertex(w, treeRepresentations.get(0).toString());
						double edgeWeight = this.kelp.computeTreeSimilarity(temp.getTreeGlossRepr(),
								vv1.getTreeGlossRepr(), this.treeKernelType);
						graph.addEdge(temp.getId(), vv1.getId(), edgeWeight);
						edgeWeight = this.kelp.computeTreeSimilarity(temp.getTreeGlossRepr(),
								vv2.getTreeGlossRepr(), this.treeKernelType);
						graph.addEdge(temp.getId(), vv2.getId(), edgeWeight);
					}
				}
			}
		}
	}

	private void computeInDegVertexCentrality(WsdGraph graph) {
		// Weight vertexes by indegree centrality
		ArrayList<WsdVertex> vertices = graph.getVerticesList();
		int nVertici = vertices.size();
		for (int i = 0; i < nVertici; i++) {
			double indegreeCentrality;
			double indegree = graph.inDegreeOf(vertices.get(i));
			indegreeCentrality = indegree / nVertici;
			graph.getVertex(vertices.get(i).getId()).setInDegCentrality(indegreeCentrality);			
		}
		
	}
	
	private void computeKppVertexCentrality(WsdGraph graph) {
		// Weight vertexes by kpp centrality
		ArrayList<WsdVertex> vertices = graph.getVerticesList();
		int nVertici = vertices.size();
		for (int i = 0; i < nVertici; i++) {
			double kppCentrality = 0;
			double distance = 0;
			for (int j = 0; j < nVertici; j++) {
				if (i != j) {
					double distPath = graph.distance(vertices.get(i), vertices.get(j));
					// Check if path exists (or edge)
					if (distPath > 0)
						distance += distPath;
				}
			}
			if (nVertici > 1)
				kppCentrality = distance / (nVertici - 1);
			graph.getVertex(vertices.get(i).getId()).setKppCentrality(kppCentrality);			
		}
	}
	
	private void computePageRankVertexCentrality(WsdGraph graph, double alpha) {
		// Weight vertexes by page rank centrality
		ArrayList<WsdVertex> vertices = graph.getVerticesList();
		int nVertici = vertices.size();
		for (int i = 0; i < nVertici; i++) {
			double pageRankCentrality = pageRank(graph, vertices.get(i), nVertici, alpha, 5);
			vertices.get(i).setPageRankCentrality(pageRankCentrality);
			//System.out.println("Vertice " + vertices.get(i).getId() + " pagerank " + vertices.get(i).getPageRankCentrality());
		}
	}
	
	private double pageRank(WsdGraph graph, WsdVertex v, int nVertici, double alpha, int step) {
		
		if (step == 0)
			return 1 / nVertici;
		
		double pagerank = 0;
		pagerank = (1 - alpha)/nVertici;
		double sum = 0;
		for (WsdVertex target : graph.getVerticesList()) {
			if (graph.getEdge(v, target) != null) {
				// Esiste l'arco
				sum += pageRank(graph, target, nVertici, alpha, step-1) / graph.outDegreeOf(target);
			}
		}
		return pagerank + alpha * sum;
	}
	
	/**
	 * Create and add vertices to the given graph. Vertices represents wordNet glosses for the words of the sentence
	 * @param graph
	 * @param sentenceIndex
	 * @param searchTerm
	 * @param originalWord
	 * @param posTag
	 * @param lemmaWord
	 * @param params
	 */
	private void createNodes(WsdGraph graph, int sentenceIndex, String searchTerm, String originalWord, String posTag, String[] lemmaWord, String params){
    	
		//for all the WordNet glosses of that word and its lemma
		for(IWordID wordId : this.wordnet.getWordsIds(searchTerm, posTag)) {
			String[] glossAndSenseKey = this.wordnet.getGloss(wordId);
			
			String[] glossExamples = glossAndSenseKey[0].split("\""); //separates glosses form examples
			String senseKey = glossAndSenseKey[1];
			//compute dependency trees for the gloss
			ArrayList<Tree> treeRepresentations = stanfordAdapter.computeDependencyTree(glossExamples[0]);
			if(treeRepresentations.size()>1){
				//if the gloss is composed by multiple sentences, there could be more dependency tree
				//in this case a message is given and only the first tree is considered
				System.out.println("More than one tree representation for a gloss of \""+searchTerm+"\" ("+glossExamples[0]+") computed. Took the first one.");
			}
			ArrayList<String> examples = new ArrayList<String>();
			if(this.saveExamples){//store the examples
				
				for(int i = 1; i < glossExamples.length; i++){
					String example = glossExamples[i].trim();
					if(example.length()>1){//remove [;] generated by splitting glosses
						examples.add(example);
					}
				}
			}
			graph.addVertex(sentenceIndex, searchTerm, originalWord, posTag, glossExamples[0], senseKey, params, lemmaWord, examples, wordId, treeRepresentations.get(0).toString());
		}
		
	}
	
	
	/**
	 * Create and add edges to the given graph. The edges weights are calculated how?
	 * @param graph
	 */
	private void createEdges(WsdGraph graph){
		//Edge creation
		ArrayList<WsdVertex> vertices = graph.getVerticesList();
		int nVertici = vertices.size();
		for(int i = 0; i < nVertici; i++){
			
			for(int j = i+1; j< nVertici; j++){
				//doesn't create edges between vertexes representing the same word 
				if( vertices.get(i).getSentenceIndex() != vertices.get(j).getSentenceIndex()){
					if (vertices.get(i).getSentenceIndex() == -1 || vertices.get(j).getSentenceIndex() == -1) {
						// Support nodes involved -> don't compute, set edgeweight
						double edgeWeight = 1;
						graph.addEdge(vertices.get(i).getId(), vertices.get(j).getId(), edgeWeight);
						
					// Se due sensi disambiguano due sentence index diversi e hanno la stessa gloss key allora ha senso computare
					} else if ( ! ( vertices.get(i).getGlossKey().equalsIgnoreCase(vertices.get(j).getGlossKey()))){

						// Compute similarity here
						double edgeWeight = this.kelp.computeTreeSimilarity(vertices.get(i).getTreeGlossRepr(),
								vertices.get(j).getTreeGlossRepr(), this.treeKernelType);
						
						graph.addEdge(vertices.get(i).getId(), vertices.get(j).getId(), edgeWeight);
					}
				}
			}
		}
	}

	
	/**
	 * Take the solution given by the tsp solver and create an output file that specifies the sense chosen
	 * for each word of the sentence
	 * @param graph
	 */
	private void generateOutputFile(WsdGraph graph){
		
		PrintWriter log = null;
		try {
			//log used to write which sentences were composed only by one word and have not been disambiguated
			log = new PrintWriter(new FileWriter("log.txt", true));
			
			//open reader for the tspSolver output file
			BufferedReader tourFileReader = new BufferedReader(
					new FileReader(Globals.tspSolverPathToGTOURS+this.fileName+this.progrSaveName+".tour"));
			//open writer to write results
			PrintWriter keyFileWriter = new PrintWriter(
					new FileWriter(Globals.resultsPath + this.fileName+Globals.resultsFileName, true));
			//sorted map used to order words by their position in the sentence
			TreeMap<Integer, String[]> sortMap = new TreeMap<Integer, String[]>();
			String line;
			boolean read = false;
			
			//read the tspSolverOutput file
			while((line = tourFileReader.readLine()) != null){
				if(line.equals("-1")){
					read = false;
				}
				if(read){
					WsdVertex v = graph.getVertex(Integer.parseInt(line.trim())-1);//-1 because solver ids starts from 1, our vertices ids starts from 0
					String[] wordGloss_KeyGlossParams = {v.getWord(), v.getGlossKey(), v.getGloss(), v.getParams()};
					sortMap.put(v.getSentenceIndex(), wordGloss_KeyGlossParams);
				}
				if(line.equalsIgnoreCase("TOUR_SECTION")){
					read = true;
				}
			}
			
			//sort word by their position in the text
			Iterator<Entry<Integer, String[]>> it = sortMap.entrySet().iterator();
			while(it.hasNext()){
				Map.Entry<Integer, String[]> pair = (Map.Entry<Integer, String[]>)it.next();
				String[] wordGloss_keyGlosParamsElement = pair.getValue();
				
				if(this.evaluation){ //evaluation mode output format
					if(wordGloss_keyGlosParamsElement[3] != null){
						keyFileWriter.write(pair.getValue()[3]+" "+pair.getValue()[1]+"\n");
					}
				}else{ //sentence wsd output format
					keyFileWriter.write("Disambiguated \""+pair.getValue()[0]+"\" as \n\t\t ("+pair.getValue()[1]+") \""+pair.getValue()[2]+"\"\n");
				}
		        it.remove();
			}
			tourFileReader.close();
			keyFileWriter.close();
			
		} catch (FileNotFoundException e) {
			//output file of tsp solver not created, the graph size wasn't >1
			log.write(this.fileName+this.progrSaveName+"\n");
			log.close();
			System.err.print(Thread.currentThread().getStackTrace()[1].getMethodName()+" threw: ");
			System.err.println(e);
		} catch (IOException e){
			System.err.print(Thread.currentThread().getStackTrace()[1].getMethodName()+" threw: ");
			System.err.println(e);
		}
	}
	
	public void enableSaveGml(){
		this.saveGml = true;
	}
	public void enableVerboseMode(){
		this.verbose = true;
	}
	public void disableVerboseMode(){
		this.verbose = false;
	}
	public void enableEvaluationMode(){
		this.evaluation = true;
	}
	public void disableEvaluationMode(){
		this.evaluation = false;
	}
	public void enableSaveExamples(){
		this.saveExamples = true;
	}
	public void enableSolver(){
		this.runSolver = true;
	}
	public void disableSaveExamples(){
		this.saveExamples = false;
	}
	public String getFileName(){
		return this.fileName;
	}
	
	public boolean getRunSolver(){
		return this.runSolver;
	}
	
	public void setFileName(String newFileName){
		this.fileName = newFileName;
	}
	public void setTreeKernelType(String kernelType){
		switch(kernelType){
			case "subTree":
				this.treeKernelType = kernelType;
				break;
			case "subsetTree":
				this.treeKernelType = kernelType;
				break;
			case "partialTree":
				this.treeKernelType = kernelType;
				break;
			case "smoothedPartialTree":
				this.treeKernelType = kernelType;
				break;
			default:
				this.treeKernelType = "subTree";
				System.err.println("defined kernelType \""+kernelType+"\" has not been found. Used default "+treeKernelType);
		}
		
	}


	public String getFileNameCentrality() {
		return this.fileNameCentrality;
	}
}
