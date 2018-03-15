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
import evaluation.InputInstance;

import com.sgametrio.wsd.KelpAdapter;

public class MyExecutor {
	
	//used for lemmatization
	private StanfordUtilsAdapter stanfordAdapter = null;  
	private WordnetAdapter wordnet = null;
	private KelpAdapter kelp = null;
	
	//saving params
	private  int progrSaveName = 1;
	private String fileNameSentences = "sentences";
	private  int progrSaveNameCentrality = 1;
	
	//execution params
	private String treeKernelType = "subTree"; //subTree, subsetTree, partialTree, smoothedPartialTree
	private boolean saveExamples = false;
	private boolean saveGml = false;
	private boolean runSolver = false;
	private boolean evaluation = true;
	public boolean verbose = false;
	
	private int trial = 1;
	
	//CONSTRUCTOR
	public MyExecutor(){	
		this.stanfordAdapter = new StanfordUtilsAdapter();
		this.wordnet = new WordnetAdapter();
		this.kelp = new KelpAdapter();	
	}
	
	/**
	 * Calls all the functions needed to perform the disambiguation
	 * @param instances a map having word POS as keys and an array containing
	 * the lemma, the word as it was written in the sentence, the index of the word in the sentence and
	 * the params of the word given in the evaluation framework
	 */
	public void performDisambiguation(ArrayList<InputInstance> instances, boolean centrality){
		ArrayList<InputInstance> selectedInstances = this.mySelectPos(instances);

		//create the graph
		MyGraph graph = this.createDisambiguationGraph(selectedInstances, centrality);
		//save gml (optional)
		
		if(Globals.saveGml){
			graph.saveToGML(Globals.gmlPath + Globals.fileName+this.progrSaveName);
		}
		
		this.createDir(Globals.resultsPath);
		// Use centrality to disambiguate senses in a word
		if (centrality) {
			this.printCentralityDisambiguation(graph, false);
		}
		
		System.out.println(this.progrSaveName);
		//the following must be the last instruction of this method
		this.progrSaveName++;
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
		//System.out.print("Printing to file.. ");
		this.printMapToFile(disambiguationMap, Globals.fileNameCentrality, this.evaluation, sentences);
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
						keyFileWriter.write(v.getSentenceTermId()+" "+v.getGlossKey()+"\n");
					}
				} else { //sentence wsd output format
					keyFileWriter.write("Disambiguated \""+v.getWord()+"\" as \n\t\t ("+v.getGlossKey()+") \""+this.wordnet.getGloss(v.getWord().getID())+"\"\n");
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
					line = "echo \"PROBLEM_FILE = " + Globals.GTSPLIBDirectory + Globals.fileName+this.progrSaveName+".gtsp\" > $par";
				}
				if (line.contains("OUTPUT_TOUR_FILE")){
					line = "echo \"OUTPUT_TOUR_FILE = " + Globals.GTOURSDirectory + Globals.fileName+this.progrSaveName+".tour\" >> $par";
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
				// TODO: Dynamic check Operating System
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
	private MyGraph createDisambiguationGraph(ArrayList<InputInstance> instances, boolean centrality){
		
		MyGraph graph = new MyGraph();
		// Support graph on which we compute vertex centrality
		// Maybe we haven't to create a second graph
		MyGraph supportGraph = new MyGraph();
		
		// for every instance, I have to find all senses
		for (InputInstance instance : instances) {
			this.myCreateNodes(graph, instance);
			if (centrality)
				this.myCreateNodes(supportGraph, instance);
		}
		
		this.myCreateEdges(graph);
		if (centrality) {
			// check if we can compute distances on support nodes
			int depth = 1;
			this.myCreateEdges(supportGraph);
			this.addSupportNodes(supportGraph, depth);
			this.computeVertexCentrality(supportGraph);
			if (this.saveGml) 
				supportGraph.saveToGML(Globals.gmlPath + "supportGraph" + this.progrSaveName);
			this.copyCentrality(supportGraph, graph);
		}
		
		return graph;
	}

	/**
	 * Copy centrality from `from` to `to`. `to` has the same initial `from` vertexes. `from` can have more
	 * @param from
	 * @param to
	 */
	private void copyCentrality(MyGraph from, MyGraph to) {
		for (int i = 0; i < to.getNodes().size(); i++) {
			to.getNodes().get(i).setCentrality(from.getNodes().get(i).getCentrality());
		}
	}


	private void myCreateEdges(MyGraph graph) {
		//Edge creation
		ArrayList<MyVertex> vertices = graph.getNodes();
		int size = vertices.size();
		for(int i = 0; i < size; i++){
			// Start from i+1 cause it's undirected and it will create the other edges itself
			for(int j = i+1; j < size; j++){
				//doesn't create edges between vertexes representing the same word 
				if( vertices.get(i).getSentenceIndex() != vertices.get(j).getSentenceIndex()){
					// Se due sensi disambiguano due sentence index diversi e hanno la stessa gloss key allora ha senso computare
					if ( ! ( vertices.get(i).getGlossKey().equalsIgnoreCase(vertices.get(j).getGlossKey()))){

						// Compute similarity here
						double edgeWeight = this.kelp.computeTreeSimilarity(vertices.get(i).getTreeGlossRepr(),
								vertices.get(j).getTreeGlossRepr(), this.treeKernelType);
						
						graph.addEdge(vertices.get(i), vertices.get(j), edgeWeight);
					}
				}
			}
		}
		
	}


	private void myCreateNodes(MyGraph graph, InputInstance input) {
		//for all the WordNet glosses of that word and its lemma
		// TODO: Optimize access to DB
		for(IWord word : this.wordnet.getWordsList(input.lemma, input.pos)) {
			String[] glossAndSenseKey = this.wordnet.getGloss(word.getID());
			
			String[] glossExamples = glossAndSenseKey[0].split("\""); //separates glosses form examples
			//compute dependency trees for the gloss
			ArrayList<Tree> treeRepresentations = stanfordAdapter.computeDependencyTree(glossExamples[0]);
			if(treeRepresentations.size()>1){
				//if the gloss is composed by multiple sentences, there could be more dependency tree
				//in this case a message is given and only the first tree is considered
				System.out.println("More than one tree representation for a gloss of \""+input.lemma+"\" ("+glossExamples[0]+") computed. Took the first one.");
			}
			MyVertex v = new MyVertex(input, word, treeRepresentations.get(0).toString(), glossAndSenseKey[0]);
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
	 * @param supportGraph
	 * @param depth
	 */
	private void addSupportNodes(MyGraph supportGraph, int depth) {
		Map<IWord, ArrayList<MyVertex>> relatedWords = new HashMap<IWord, ArrayList<MyVertex>>();
		ArrayList<MyVertex> vertexes = supportGraph.getNodes();
		for (MyVertex v : vertexes) {
			// Prendo tutte le word dal synset, e dai synsets correlati
			ArrayList<IWord> vRelatedWords1 = this.wordnet.getSynsetWords(v.getWord().getID());
			ArrayList<IWord> vRelatedWords2 = this.wordnet.getRelatedSynsetWords(v.getWord().getID());
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
				if (supportGraph.containsWord(word))
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
			// TODO: modificare anche questo please
			String[] glossAndSenseKey = this.wordnet.getGloss(w.getID());
			
			String[] glossExamples = glossAndSenseKey[0].split("\""); //separates glosses form examples
			
			//compute dependency trees for the gloss
			ArrayList<Tree> treeRepresentations = stanfordAdapter.computeDependencyTree(glossExamples[0]);
			// Più di un vertice correlato, creo il nodo e gli collego i vertici
		
			MyVertex temp = new MyVertex(w, treeRepresentations.get(0).toString());
			supportGraph.addNode(temp);
			// Collego tutti i vertici a quel nodo
			for (MyVertex v : possibleVertexes) {
				double weight = this.kelp.computeTreeSimilarity(temp.getTreeGlossRepr(),
						v.getTreeGlossRepr(), this.treeKernelType);
				supportGraph.addEdge(v, temp, weight);
			}
		}
	}
	
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
					new FileReader(Globals.tspSolverPathToGTOURS+Globals.fileName+this.progrSaveName+".tour"));
			//open writer to write results
			PrintWriter keyFileWriter = new PrintWriter(
					new FileWriter(Globals.resultsPath + Globals.fileName+Globals.resultsFileName, true));
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
			log.write(Globals.fileName+this.progrSaveName+"\n");
			log.close();
			System.err.print(Thread.currentThread().getStackTrace()[1].getMethodName()+" threw: ");
			System.err.println(e);
		} catch (IOException e){
			System.err.print(Thread.currentThread().getStackTrace()[1].getMethodName()+" threw: ");
			System.err.println(e);
		}
	}
	
	public boolean getRunSolver(){
		return this.runSolver;
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
}
