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
import java.nio.file.Path;
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
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import edu.mit.jwi.item.IWord;
import edu.mit.jwi.item.IWordID;

public class WsdExecutor {
	
	//used for lemmatization
	private StanfordUtilsAdapter stanfordAdapter = null;  
	private WordnetAdapter wordnet = null;
	
	//saving params
	private String fileName = "senseval3_subTrees";
	private  int progrSaveName = 1;

	//paths
	private final String tspSolverHomeDir = "src/main/resources/GLKH-1.0/";
	private final String tspSolverPathToGTSPLIB = "GTSPLIB/";
	private final String tspSolverPathToGTOURS = "G-TOURS/";
	private final String tspSolverFileName = "runGLKH";
	private final String resultsPath = "RESULTS/";
	private final String resultsFileName = "_wsdResults.KEY";
	private final String pathToGML = "GML/";
	
	//execution params
	private boolean saveExamples = false;
	private boolean saveGml = false;
	private boolean runSolver = false;
	private boolean evaluation = false;
	public boolean verbose = true;
	
	private int trial = 1;
	
	//CONSTRUCTOR
	public WsdExecutor(){
		
		this.stanfordAdapter = new StanfordUtilsAdapter();
		this.wordnet = new WordnetAdapter();
		
	}
	
	
	/**
	 * Calls all the functions needed to perform the disambiguation when param is a sentence
	 * @param sentence
	 */
	//@overload
	public void performDisambiguation(String sentence){
		
		//compute POS and Lemmas of the given sentence
		HashMap<String, ArrayList<String[]>> pos_lemmaWordIndexParams = 
				this.stanfordAdapter.calculateLemmasAndPOS(sentence);
		this.performDisambiguation(pos_lemmaWordIndexParams);
	}
	
	
	/**
	 * Calls all the functions needed to perform the disambiguation
	 * @param pos_lemmaWordIndexParamsMAP a map having word POS as keys and an array containing
	 * the lemma, the word as it was written in the sentence, the index of the word in the sentence and
	 * the params of the word given in the evaluation framework
	 */
	public void performDisambiguation(HashMap<String, ArrayList<String[]>> pos_lemmaWordIndexParamsMAP){
		HashMap<String, ArrayList<String[]>> selectedPos = this.selectPos(pos_lemmaWordIndexParamsMAP);
		//create the graph
		WsdGraph graph = this.createDisambiguationGraph(selectedPos);
		
		//save gml (optional)
		if(this.saveGml){
			graph.saveToGML(this.pathToGML, this.fileName+this.progrSaveName);
		}
		//save to gtsp (NEEDED). The solver and all dependent methods are invoked only if the graph is not empty
		if(graph.saveToGTSP(this.tspSolverHomeDir+this.tspSolverPathToGTSPLIB, this.fileName+this.progrSaveName)){
			this.setTSPSolver();
			if(this.getRunSolver()) {
				if(this.runSolver()){
					this.generateOutputFile(graph);
				}
			}
			
			
		}
		System.out.println(this.progrSaveName);
		//the following must be the last instruction of this method
		this.progrSaveName++;
		
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
	 * Change param of TSPSolver main file to set input and output files
	 */
	private void setTSPSolver(){
		
		BufferedReader oldFileReader = null;
		BufferedWriter newFileWriter = null;
		String tempFileName = "tmp_"+this.tspSolverFileName;
		
		try {
			
			oldFileReader = new BufferedReader(new FileReader(this.tspSolverHomeDir+this.tspSolverFileName));
			newFileWriter = new BufferedWriter(new FileWriter(this.tspSolverHomeDir+tempFileName));
			
			//reads from the old file and write on the new one
			String line;
			while ((line = oldFileReader.readLine()) != null) {
				if (line.contains("PROBLEM_FILE")){
					line = "echo \"PROBLEM_FILE = " + this.tspSolverPathToGTSPLIB + this.fileName+this.progrSaveName+".gtsp\" > $par";
				}
				if (line.contains("OUTPUT_TOUR_FILE")){
					line = "echo \"OUTPUT_TOUR_FILE = " + this.tspSolverPathToGTOURS + this.fileName+this.progrSaveName+".tour\" >> $par";
				}
				newFileWriter.write(line+"\n");
			}
			
			oldFileReader.close();
			newFileWriter.close();
			
			//delete old file
			File oldFile = new File(this.tspSolverHomeDir+this.tspSolverFileName);
			oldFile.delete();
			
			// And rename tmp file to old file name
			File newFile = new File(this.tspSolverHomeDir+tempFileName);
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
		
		File solver = new File(this.tspSolverHomeDir+this.tspSolverFileName);
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
				ProcessBuilder process = new ProcessBuilder("./"+this.tspSolverFileName);
				process.directory(new File(this.tspSolverHomeDir));

				Process p = process.start();
				//get the error stream
				/*InputStream is = p.getErrorStream();
				String errorStream = this.getProcessOutput(is);*/

				//if verbose mode is on, prints tsp solver output and errors
				if(this.verbose){
					System.out.println(this.getProcessOutput(p.getInputStream()));
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
				if (this.verbose)
					System.out.println(line);
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
	 * @param pos_lemmaWordIndexParams: a map having POS as key and a list of word (with their lemma
	 * index and evaluation params) having that POS as value
	 */
	private WsdGraph createDisambiguationGraph(Map<String, ArrayList<String[]>> pos_lemmaWordIndexParams){
		
		WsdGraph graph = new WsdGraph();
		// Support graph on which we compute vertex centrality
		//WsdGraph supportGraph = new WsdGraph();
		
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
		    	// Add nodes on support graph
		    	//this.createNodes(supportGraph, sentenceIndex, searchTerm, originalWord, posTag, lemmaWord, params);
		    	
		    	//if the lemma is different from the original word, search senses for both of them
		    	if(!searchTerm.equalsIgnoreCase(originalWord)){
		    		this.createNodes(graph, sentenceIndex, originalWord, originalWord, posTag, lemmaWord, params);
		    		// Add nodes on support graph
		    		//this.createNodes(supportGraph, sentenceIndex, originalWord, originalWord, posTag, lemmaWord, params);
		    	}
		    }
		    it_p_lwip.remove();
		    
		}
		
		//int depth = 1;
		//this.addSupportNodes(supportGraph, depth);
		//supportGraph.saveToGML("GML/", "supportGraph");
		this.createEdges(graph);
		//this.createEdges(supportGraph);
		//this.computeInDegVertexCentrality(graph);
		this.computeKppVertexCentrality(graph);
		//this.computeVertexCentrality(supportGraph);
		//this.copyCentrality(supportGraph.getVerticesList(), graph.getVerticesList());
		return graph;
		
	}
	
	
	private void copyCentrality(ArrayList<WsdVertex> from, ArrayList<WsdVertex> to) {
		// nodes present in `from` ArrayList are present in `to` too
		for (int i = 0; i < to.size(); i++) {
			to.get(i).setCentrality(from.get(i).getCentrality());			
		}
	}


	private void addSupportNodes(WsdGraph graph, int depth) {
		// For every node add support nodes if node connects each other and dist <= depth
		if (depth <= 0)
			return;
		WsdGraph subGraph = new WsdGraph();
		ArrayList<WsdVertex> vertexes = graph.getVerticesList();
		for (WsdVertex v : vertexes) {
			subGraph.addVertex(v);
			this.depthFirstSearch(graph, v, depth, subGraph);
			subGraph.removeVertex(v);
		}
	}

	private void depthFirstSearch(WsdGraph graph, WsdVertex start, int depth, WsdGraph tempGraph) {
		if (depth == 0) {
			return;
		}
		// Ricerca depth-first ad un massimo di profondità "depth" in wordnet
		for (IWord word : this.wordnet.getSynsetWords(start.getWordId())) {
			// Costruisco un vertice a partire da `word`
			
			// Se trovo una parola che ho già nel grafo metto l'arco

				// Qui dovrei copiare tutto il sottografo che ho creato nel mio grafo iniziale
				// Verificando ad esempio sentence index e word id
			
				// Altrimenti proseguo la ricerca
				// Crea il vertice di supporto corrispondente alla parola
				// System.out.println("DEPTH: "+depth+word.toString());
				
				//this.depthFirstSearch(graph, temp, depth-1, tempGraph);
			this.depthFirstSearch(graph, start, depth-1, tempGraph);
				
			
			/*tempGraph.removeEdge(start, temp);
			tempGraph.removeVertex(temp);*/
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
			graph.getVertex(vertices.get(i).getId()).setCentrality(indegreeCentrality);			
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
					if (distPath != -1)
						distance += 1 / distPath;
				}
			}
			if (nVertici > 1)
				kppCentrality = distance / (nVertici - 1);
			graph.getVertex(vertices.get(i).getId()).setCentrality(kppCentrality);			
		}
		
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

			ArrayList<String> examples = new ArrayList<String>();
			if(this.saveExamples){//store the examples
				
				for(int i = 1; i < glossExamples.length; i++){
					String example = glossExamples[i].trim();
					if(example.length()>1){//remove [;] generated by splitting glosses
						examples.add(example);
					}
				}
			}
			graph.addVertex(sentenceIndex, searchTerm, originalWord, posTag, glossExamples[0], senseKey, params, lemmaWord, examples, wordId);
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
						
					} else if ( ! ( vertices.get(i).getGlossKey().equalsIgnoreCase(vertices.get(j).getGlossKey()))){

						// Compute similarity here
						
						//Random r = new Random();
						//double edgeWeight = r.nextDouble();
						double edgeWeight = 1;
						
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
		
		PrintWriter log = null;
		try {
			//log used to write which sentences were composed only by one word and have not been disambiguated
			log = new PrintWriter(new FileWriter("log.txt", true));
			
			//open reader for the tspSolver output file
			BufferedReader tourFileReader = new BufferedReader(
					new FileReader(this.tspSolverHomeDir+this.tspSolverPathToGTOURS+this.fileName+this.progrSaveName+".tour"));
			//open writer to write results
			PrintWriter keyFileWriter = new PrintWriter(
					new FileWriter(this.resultsPath + this.fileName+this.resultsFileName, true));
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
	public String getGtspPath(){
		return this.tspSolverHomeDir+this.tspSolverPathToGTSPLIB;
	}
	public String getTourPath(){
		return this.tspSolverHomeDir+this.tspSolverPathToGTOURS;
	}
	public String getPathToGML(){
		return this.pathToGML;
	}
	public String getResultsPath(){
		return this.resultsPath;
	}
	public boolean getRunSolver(){
		return this.runSolver;
	}
	public String getResultsFileName(){
		return this.resultsFileName;
	}
	public void setFileName(String newFileName){
		this.fileName = newFileName;
	}
	/*public void setTreeKernelType(String kernelType){
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
		
	}*/
}
