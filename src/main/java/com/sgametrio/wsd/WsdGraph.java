/**
 * This class has the main purpose of implementing some unavailable or differently
 * developed functions in a different format in the JGraphT library
 */

package com.sgametrio.wsd;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.jgrapht.graph.SimpleWeightedGraph;

import edu.mit.jwi.item.IWord;
import edu.mit.jwi.item.IWordID;

public class WsdGraph extends SimpleWeightedGraph<WsdVertex, Integer> {
	
	private static final long serialVersionUID = 1L;
	
	private Map<Integer, WsdVertex> idToNode; 
	private int progressiveNodeId = 0;
	private int progressiveEdgeId = 0;
	
	/** CONSTRUCTORS
	 * 
	 */
	public WsdGraph(){
		
		super(Integer.class);
		idToNode = new HashMap<Integer, WsdVertex>();
	}
	
	/**
	 * Add an edge between nodes with the specified id assigning it the given weight
	 * @param id1: source vertex id
	 * @param id2: target vertex id
	 * @param weight: edge weight
	 */
	public void addEdge(int id1, int id2, double weight){
		
		if(this.idToNode.containsKey(id1) && this.idToNode.containsKey(id2)){
			
			this.addEdge(idToNode.get(id1), idToNode.get(id2), progressiveEdgeId);
			this.setEdgeWeight(progressiveEdgeId, weight);
			progressiveEdgeId++;	
			
		}else{
			
			System.out.println("Inexistent node with given ID. Check IDs.");
			
		}
	}
	
	/** FUNCIONS
	 * Add new vertex to the graph
	 * @param word: the word the vertex is representing
	 * @param pos: part of speech of the word
	 * @param gloss: word gloss
	 * @param examples: a list with the WordNet examples for the gloss
	 * @return true when the vertex is added
	 */
	public void addVertex(int sentenceIndex, String searchTerm, String originalWord, String pos, String gloss, String glossKey, String params, String lemmaWord[], ArrayList<String> examples, IWordID wordId){
		
		WsdVertex node = new WsdVertex(progressiveNodeId, sentenceIndex, searchTerm.trim(), originalWord.trim(), pos.trim(), gloss.trim(), glossKey, params, lemmaWord, examples, wordId);
		this.addVertex(node);
		idToNode.put(progressiveNodeId, node);
		progressiveNodeId++;
	}
	
	
	
	/**
	 * 
	 * 
	 */
	public double distance(int idA, int idB) {
		// TODO: Implement dijkstra here
		// Have to return a big number (or a negative one) if path doesn't exists or nodes doesn't exists
		WsdVertex A = this.idToNode.get(idA);
		WsdVertex B = this.idToNode.get(idB);
		return distance(A, B);
	}
	
	public double distance(WsdVertex A, WsdVertex B) {
		// TODO: Implement dijkstra here
		// Have to return a big number (or a negative one) if path doesn't exists or nodes doesn't exists
		if (A == null || B == null) {
			return -2;
		}
		// Compute path by # of edges or by weights?
		
		if (this.getEdge(A, B) != null) {
			return 1;
		}
		return -1;
	}
	
	/**
	 * Return an hashSet containing the sentence indices of the considered words
	 * @return an hashSet containing the sentence indices of the considered words
	 */
	public HashSet<Integer> getAllUniqueSentenceIndices(){
		HashSet<Integer> sentenceIndices = new HashSet<Integer>();
		for(WsdVertex v : this.getVerticesList()){
			sentenceIndices.add(v.getSentenceIndex());
		}
		return sentenceIndices;
	}
	
	/**
	 * Retrieve the edge with the specified id
	 * @param edgeId: id of the edge to be retrieved
	 * @return: a map with the parameters of the edge with the specified id
	 */
	public HashMap<String, String> getEdge(Integer edgeId){
		HashMap<String, String> edgesMap = new HashMap<String, String>();
		String source = "" + this.getEdgeSource(edgeId).getId();
		String target = "" + this.getEdgeTarget(edgeId).getId();
		String weight = "" + this.getEdgeWeight(edgeId);
		edgesMap.put("id", edgeId.toString());
		edgesMap.put("source", source);
		edgesMap.put("target", target);
		edgesMap.put("weight", weight);
		
		return edgesMap;
	}
	
	
	/**
	 * Returns a list of all graph edges in the format [sourceId,targetId,edgeWeight]
	 * @return the list of graph edges in the format [sourceId,targetId,edgeWeight]
	 */
	public ArrayList<HashMap<String,String>> getEdgesList(){
		ArrayList<HashMap<String,String>> edgeList = new ArrayList<HashMap<String,String>>();
		
		ArrayList<WsdVertex> verticesList = this.getVerticesList();
		
		int verticesNumber = this.getVerticesNumber();
		
		for(int i = 0; i < verticesNumber; i++){
			for(int j = i+1; j<verticesNumber; j++){
				
				for(Integer edgeId : this.getAllEdges(verticesList.get(i), verticesList.get(j))){
					HashMap<String,String> edge = this.getEdge(edgeId);
					edgeList.add(edge);
				}
				
			}
		}
		return edgeList;
	}
	
	/**
	 * Return the matrix of the graph. Row and column indices represent the ids of the graph nodes
	 * and values reported are the weight of the edges between nodes having row/column indices as ids.
	 * The lowest integer value is used when there is no edge or between i-th and j-th node
	 * @return the graph matrix if it has vertices, null otherwise
	 */
	public int[][] getGraphMatrix(){
		int verticesNumber = this.getVerticesNumber();
		if(verticesNumber>0){
			int[][] weightMatrix = new int[verticesNumber][verticesNumber];
			//initialize the matrix with MIN values to discourage the usage of inexistent edges by tsp solver
			//this MIN values will be changed to MAX values when computing inverse matrix 
			for(int row = 0; row < verticesNumber; row++){
				for(int col = 0; col < verticesNumber; col++){
					weightMatrix[row][col] = Integer.MIN_VALUE;
				}
			}
			for(HashMap<String,String> edge: this.getEdgesList()){
				weightMatrix[Integer.parseInt(edge.get("source"))][Integer.parseInt(edge.get("target"))] = (int)Double.parseDouble(edge.get("weight"));
				weightMatrix[Integer.parseInt(edge.get("target"))][Integer.parseInt(edge.get("source"))] = (int)Double.parseDouble(edge.get("weight"));
			}
			return weightMatrix;
		}else{
			return null;
		}
	}
	
	
	/**
	 * Compute the inverse of the given similarity matrix. Lowest value is replaced with highest value.
	 * @param similarityMatrix: an NxN similarity matrix
	 * @return the inverse of the similarity matrix
	 */
	private int[][] getInvertedSimilarityMatrix(int[][] similarityMatrix){
		int dimension = similarityMatrix[0].length;
		int maxElement = this.getMaxMatrixElement(similarityMatrix);
		
		for(int row = 0; row < dimension; row++){
			for(int col = 0; col < dimension; col++){
				if(similarityMatrix[row][col] != Integer.MIN_VALUE){ //if the edge exist (even with 0 value)
					similarityMatrix[row][col] = maxElement-similarityMatrix[row][col]+1;	
				}else{
					similarityMatrix[row][col] = Integer.MAX_VALUE; //set to max_value to discourage the use of an inexistent edge	
				}
				
			}
		}
		return similarityMatrix;
	}
	
	
	/**
	 * Return the greatest value in the given NxN matrix
	 * @param an NxN matrix
	 * @return the greatest value in the given NxN matrix
	 */
	private int getMaxMatrixElement(int[][] matrix){
		
		int dimension = matrix[0].length;
		int maxElement = 0;
		
		for(int row = 0; row < dimension; row++){
			for(int col = 0; col < dimension; col++){
				if(matrix[row][col] > maxElement){
					maxElement = matrix[row][col];
				}
			}
		}
		return maxElement;
	}
	
	
	/**
	 * Returns a map which keys are the indices of the words in the sentence and values
	 * are lists of node ids having that sentence index
	 * @return
	 */
	public HashMap<Integer, ArrayList<Integer>> getSensesClusters(){
		
		HashMap<Integer, ArrayList<Integer>> clusters = new HashMap<Integer, ArrayList<Integer>>();
		
		for(WsdVertex v : this.getVerticesList()){
			ArrayList<Integer> tmp;
			if(clusters.containsKey(v.getSentenceIndex())){
				tmp = clusters.get(v.getSentenceIndex());
			}else{
				tmp = new ArrayList<Integer>();
			}
			tmp.add(v.getId());
			clusters.put(v.getSentenceIndex(), tmp);
		}
		return clusters;
	}

	
	/**
	 * Retrieve the vertex with the specified id
	 * @param nodeId: id of the vertex to be retrieved
	 * @return: the vertex with the specified id
	 */
	public WsdVertex getVertex(int nodeId){

		if(this.idToNode.containsKey(nodeId)){
		
			return idToNode.get(nodeId);
			
		}else{
			System.out.println("Inexistent node "+ nodeId);
			
			return null;
		}
	}
	
	
	/**
	 * Returns a list containing all the graph vertices
	 * @return a list containing all the graph vertices
	 */
	public ArrayList<WsdVertex> getVerticesList(){
		ArrayList<WsdVertex> verticesList = new ArrayList<WsdVertex>();
		Set<WsdVertex> vertex = this.vertexSet();
		Iterator<WsdVertex> it = vertex.iterator();
		
		while(it.hasNext()){
			WsdVertex currentVertex = it.next();
			verticesList.add(currentVertex);
		}
		
		return verticesList;
	}
	
	
	/**
	 * return the graph vertices number
	 * @return: vertices number
	 */
	public int getVerticesNumber(){
		return this.vertexSet().size();
	}
	
	
	/**
	 * Remove a the vertex with the specified vertex id updating the map of the correspondences
	 * between vertexId and PersonalVertex
	 * @param vertexId: the id of the vertex to remove
	 * @return true when the vertex is removed
	 */
	public boolean removeVertex(int vertexId){
		
		return this.removeVertex(this.idToNode.remove(vertexId));
	}
	
	
	/**
	 * Override of the toString() method. Returns all the nodes and the edges
	 * with their parameters
	 */
	@Override
	public String toString(){
		
		Set<WsdVertex> vertices = this.vertexSet();
		Iterator<WsdVertex> node = vertices.iterator();
		String output = "NODES:\n";
		
		while(node.hasNext()){
			WsdVertex currentNode = node.next();
			output += currentNode.toString();
		}
		
		Set<Integer> edges = this.edgeSet();
		Iterator<Integer> edge = edges.iterator();
		output = output + "EDGES:\n";
		
		while(edge.hasNext()){
			Integer currentEdge = edge.next();
			output += "EDGE ID: " + currentEdge + "\n";
			output += "EDGE SOURCE: " + this.getEdgeSource(currentEdge).getId() + "\n";
			output += "EDGE TARGET: " + this.getEdgeTarget(currentEdge).getId() + "\n";
			output += "EDGE WEIGHT: " + this.getEdgeWeight(currentEdge) + "\n\n";
		}
		
		return output;
	}
	
	
	/**
	 * Saves the given graph in GraphML format
	 * @param fileName: path and name of the file where the graph has to be saved
	 */
	public boolean saveToGML(String gmlPath, String fileName){
		if(this.getVerticesNumber()>0){
			// if the directory does not exist, create it
			File gmlDir = new File(gmlPath);
			if (!gmlDir.exists()) {
				try{
			    	gmlDir.mkdir();
			    } 
			    catch(SecurityException e){
			    	System.err.print(Thread.currentThread().getStackTrace()[1].getMethodName()+" threw: ");
					System.err.println(e);
			    }        
			}
			
			int glossNumber = -1; //used to differentiate word labels
			int sentenceIndex = -1;
			String currentWord = ""; //used to reset glossNumber when the word changes
			BufferedWriter writer = null;
			String gml = "";
			
			gml += "graph\n"
					+ "[\n\tcomment \"WSD graph\"\n"
					+ "\tlabel \"label\"\n";

			//saves vertices information
			for (WsdVertex v : this.getVerticesList()){
				gml += "\tnode \n"
						+ "\t[\n"
						+ "\t\tid "+ v.getId()+ "\n";
				if(!(sentenceIndex == v.getSentenceIndex() && 
						currentWord.equals(v.getWord()))){
					glossNumber = 1;
					currentWord = v.getWord();
					sentenceIndex = v.getSentenceIndex();
				}
				gml += "\t\tlabel \""+currentWord+"_"+sentenceIndex +"."+glossNumber+"\"\n";
				gml += "\t\tsentence_index \""+ sentenceIndex +"\"\n";
				gml += "\t\tused_term \""+v.getSearchTerm()+"\"\n";
		        gml += "\t\tpos \""+v.getPOS()+"\"\n";
		        gml += "\t\tgloss_key \""+v.getGlossKey()+"\"\n";
		        gml += "\t\tparams \""+v.getParams()+"\"\n";
		        gml += "\t\tcentrality \""+v.getCentrality()+"\"\n";
		        gml += "\t\tgloss \""+v.getGloss().replaceAll("\"", "")+"\"\n";
		        gml += "\t\tword_id \""+v.getWordId()+"\"\n";
		        gml += "\t\tlemma_word_pair \""+v.getLemmaWord()[0]+"-"+v.getLemmaWord()[1]+"\"\n";
		        ArrayList<String> examples = v.getExamples();
		        
		        for(int i = 0; i<examples.size(); i++){
		        	gml+= "\t\texample_"+(i+1)+" \""+ examples.get(i).replaceAll("\"", "")+"\"\n";
		        }
		        
		        gml += "\t]\n";
		        glossNumber++;
			}
			
			
			//saves edges information
			Set<Integer> edgeIds = this.edgeSet();
			Iterator<Integer> it = edgeIds.iterator();
			
			while(it.hasNext()){
				HashMap<String, String> edge = this.getEdge(it.next());
				gml += "\tedge [\n"
						+ "\t\tid "+edge.get("id")+"\n";
				gml += "\t\tsource "+edge.get("source")+"\n";
				gml += "\t\ttarget "+edge.get("target")+"\n";
				gml += "\t\tweight "+edge.get("weight")+"\n";
				gml += "\t]";
			}
			
			gml += "\n]";
			try {
				writer = new BufferedWriter(new FileWriter("GML/" + fileName + ".gml"));
				writer.write(gml);
				writer.close();
				System.out.println("Saved graph to GML/" + fileName + ".gml");
				return true;
			} catch (IOException e) {
				System.err.print(Thread.currentThread().getStackTrace()[1].getMethodName()+" threw: ");
				System.err.println(e);
			}
		}else{
			System.out.println(Thread.currentThread().getStackTrace()[1].getMethodName()+": The graph has no vertices.");
		}
		return false;
		
	}
	

	/**
	 * Save the graph in GTSP format that will be use to run TSP solver. To this purpose, the 
	 * similarity matrix of the graph is inverted to compute a "difference" matrix in which edges weights
	 * represent the differences between two vertices
	 * @param filePath
	 * @param fileName
	 * @return true if the graph has been correctly saved
	 */
	public boolean saveToGTSP(String filePath, String fileName){
		int verticesNumber = this.getVerticesNumber();
		if(verticesNumber>0){
			BufferedWriter writer = null;
			int[][] graphMatrix = this.getGraphMatrix();
			int[][] invertedSimilarityMatrix = this.getInvertedSimilarityMatrix(graphMatrix);	
			HashSet<Integer> sentenceIndices = this.getAllUniqueSentenceIndices();
			HashMap<Integer, ArrayList<Integer>> senseCluster = this.getSensesClusters();
			
			String matrix = "NAME : "+ fileName + ".gtsp\n";
			matrix += "TYPE : GTSP\n";
			matrix += "COMMENT : wsd graph with edges weighted\n";
			matrix += "DIMENSION : "+ verticesNumber +"\n";
			matrix += "GTSP_SETS : "+ this.getAllUniqueSentenceIndices().size() +"\n";
			matrix += "EDGE_WEIGHT_TYPE : EXPLICIT \n";
			matrix += "EDGE_WEIGHT_FORMAT : FULL_MATRIX \n";
			matrix += "EDGE_WEIGHT_SECTION\n";
			
			for(int row = 0; row < verticesNumber; row++){
				for(int col = 0; col < verticesNumber; col++){
					matrix += invertedSimilarityMatrix[row][col]+" ";
				}
				matrix += "\n";
			}
			
			matrix += "GTSP_SET_SECTION\n";
			
			Iterator<Integer> sentenceIndex = sentenceIndices.iterator();
			int clusterNumber = 1;
			while(sentenceIndex.hasNext()){
				int index = sentenceIndex.next();
				matrix += clusterNumber + " ";
				for(Integer vertexId : senseCluster.get(index)){
					matrix +=  (vertexId+1) + " "; //ids starts from 0, we want to start from 1
				}
				matrix += "-1\n";
				clusterNumber++;
			}
			matrix += "EOF";
			
			try {
				writer = new BufferedWriter(new FileWriter(filePath + fileName+".gtsp"));
				writer.write(matrix);
				writer.close();
				//System.out.println("Saved graph to " + filePath + fileName+".gtsp");
				return true;
			} catch (IOException e) {
				System.err.print(Thread.currentThread().getStackTrace()[1].getMethodName()+" threw: ");
				System.err.println(e);
			}
		
		}else{
			System.out.println(Thread.currentThread().getStackTrace()[1].getMethodName()+": The graph has no vertices.");
		}
		return false;
		
	}

	/**
	 * Returns siblings (same word_id but different vertex id)
	 * @param v
	 * @return ArrayList of siblings
	 */
	public ArrayList<WsdVertex> findSiblings(WsdVertex v) {
		ArrayList<WsdVertex> siblings = new ArrayList<WsdVertex>();
		for (WsdVertex v2 : this.getVerticesList()) {
			if (v2.getId() != v.getId() && v2.getWordId() == v.getWordId()) {
				siblings.add(v2);
			}
		}
		return siblings;
	}
	
	// Used for support nodes
	public WsdVertex addVertex(IWord word) {
		WsdVertex v = new WsdVertex(this.progressiveNodeId, word);
		this.addVertex(v);
		idToNode.put(progressiveNodeId, v);
		this.progressiveNodeId++;
		return v;
	}
}
