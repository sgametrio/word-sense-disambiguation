package com.sgametrio.wsd;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import edu.mit.jwi.item.IWord;

public class MyGraph {
	private String sentence = "";

	private ArrayList<MyVertex> vertexes;

	public MyGraph() {
		vertexes = new ArrayList<MyVertex>();
	}
	
	public ArrayList<MyVertex> getNodes() {
		return vertexes;
	}
	
	public MyVertex getNodeById(int id) {
		for (MyVertex v : vertexes) {
			if (v.getId() == id)
				return v;
		}
		return null;
	}
	
	public void addNode(MyVertex v) {
		vertexes.add(v);
	}
	
	public void addEdge(MyVertex source, MyVertex target, double weight) {
		int indexS = vertexes.indexOf(source);
		int indexT = vertexes.indexOf(target);
		if (indexS != -1 && indexT != -1 && weight >= 0.0) {
			source.addEdge(target, weight);
			// Undirected graph
			target.addEdge(source, weight);
		}
	}

	/**
	 * compute vertexes distance
	 * @param source
	 * @param target
	 * @return edge weight, -1 if it doesn't exist
	 */
	public double distance(MyVertex source, MyVertex target) {
		MyEdge edge = source.getEdge(target);
		if (edge != null)
			return edge.getWeight();
		return -1;
	}
	
	public void saveToGML(String path) {
		// Assuming path to file exists
		String gml = "";
		gml += "graph [\n"
				+ "\tcomment \"" + getSentence().replaceAll("\"", "") + "\"\n"
				+ "\tlabel \"" + getSentence().replaceAll("\"", "") + "\"\n";
		// Add nodes, and then edges
		for (MyVertex v : this.getNodes()) {
			gml += v.toGML();
		}
		for (MyVertex v : this.getNodes()) {
			for (MyEdge e : v.getEdges()) {
				gml += "\tedge [\n"
						+ "\t\tid " + e.getId() + "\n"
						+ "\t\tsource " + v.getId() + "\n"
						+ "\t\ttarget " + e.getDest().getId() + "\n"
						+ "\t\tweight " + e.getWeight() + "\n"
						+ "\t]\n";
			}
		}
		gml += "]\n";
		try {
			BufferedWriter file = new BufferedWriter(new FileWriter(path));
			file.write(gml);
			file.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Save to GTSP format file
	 * @param path
	 */
	public boolean saveToGTSP(String path) {
		// Assuming path to file exists
		int size = this.getNodes().size();
		if (size == 0) {
			System.err.println("Graph has no vertexes, cannot save to GTSP format");
			return false;
		}
		double[][] matrix = this.getGraphMatrix();
		String content = "";
		/**
		 * Save the graph in GTSP format that will be use to run TSP solver. To this purpose, the 
		 * similarity matrix of the graph is inverted to compute a "difference" matrix in which edges weights
		 * represent the differences between two vertices
		 * @param filePath
		 * @param fileName
		 * @return true if the graph has been correctly saved
		 */
		/*public boolean saveToGTSP(String filePath, String fileName){
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
					System.out.println("Saved graph to " + filePath + fileName+".gtsp");
					return true;
				} catch (IOException e) {
					System.err.print(Thread.currentThread().getStackTrace()[1].getMethodName()+" threw: ");
					System.err.println(e);
				}
			
			}else{
				System.out.println(Thread.currentThread().getStackTrace()[1].getMethodName()+": The graph has no vertices.");
			}
			return false;
			
		}*/
		try {
			BufferedWriter file = new BufferedWriter(new FileWriter(path));
			file.write(content);
			file.close();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	private double[][] getGraphMatrix() {
		int size = this.getNodes().size();
		if (size == 0)
			return null;
		double[][] matrix = new double[size][size];
		// Initialize matrix
		for (int i = 0; i < size; i++) 
			for (int j = 0; j < size; j++)
				matrix[i][j] = 0;
		for (MyVertex v : this.getNodes()) {
			for (MyEdge e : v.getEdges()) {
				matrix[v.getId()][e.getDest().getId()] = e.getWeight();
			}
		}
		return matrix;
	}

	public boolean containsWord(IWord word) {
		for (MyVertex v : vertexes) {
			if (v.getWord().equals(word)) {
				return true;
			}
		}
		return false;
	}
	
	public String getSentence() {
		return sentence;
	}

	public void setSentence(String sentence) {
		this.sentence = sentence;
	}

	/**
	 * 
	 * @return only disambiguation vertexes
	 */
	public ArrayList<MyVertex> getDisambiguationNodes() {
		ArrayList<MyVertex> disambiguationVertexes = new ArrayList<MyVertex>();
		for (MyVertex v : vertexes) {
			if (v.getSentenceIndex() != -1)
				disambiguationVertexes.add(v);
		}
		return disambiguationVertexes;
	}
	
	/**
	 * 
	 * @return only support nodes
	 */
	public ArrayList<MyVertex> getSupportNodes() {
		ArrayList<MyVertex> supportVertexes = new ArrayList<MyVertex>();
		for (MyVertex v : vertexes) {
			if (v.getSentenceIndex() == -1)
				supportVertexes.add(v);
		}
		return supportVertexes;
	}
}
