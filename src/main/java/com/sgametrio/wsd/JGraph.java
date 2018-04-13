package com.sgametrio.wsd;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.jgrapht.alg.scoring.ClosenessCentrality;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

public class JGraph extends SimpleWeightedGraph<JNode, DefaultWeightedEdge> {
	private static final long serialVersionUID = 1L;
	private static int progressiveId = 0;
	private int id;
	private String sentenceId = "";
	private String sentence = "";
	private String log = "";

	public JGraph() {
		super(DefaultWeightedEdge.class);
		this.id = progressiveId++;
	}
	
	public JGraph(String sentence, String sentenceId) {
		this();
		this.sentence = sentence;
		this.sentenceId = sentenceId;
	}
	
	/**
	 * Adds log to local variable and choose different actions based on severity
	 * @param severity
	 * @param log
	 */
	public void log(int severity, String log) {
		if (Globals.developmentLogs) {
			this.log += log + "\n";
			if (severity >= Globals.logWarning) {
				System.out.println("[GRAPH " + this.getSentenceId() + "]" + log);
			} 
		} else if (severity ==  Globals.logSevere) {
			this.log += log + "\n";
			System.out.println(log);
			this.logOnFile(this.getSentenceId());
			System.exit(1);
		} else if (severity == Globals.logStatistics) {
			this.log += log + "\n";
		}
	}

	private Map<JNode, Double> computeClosenessCentrality() {
		ClosenessCentrality<JNode, DefaultWeightedEdge> cc = new ClosenessCentrality<JNode, DefaultWeightedEdge>(this);
		Map<JNode, Double> scores = cc.getScores();
		return scores;
	}
	
	/**
	 * Logs log content to file
	 */
	public void logOnFile(String filename) {
		if (this.log.length() > 0) {
			try {
				FileWriter logFile = new FileWriter(Globals.logsPath + filename + ".log");
				logFile.write(this.log);
				logFile.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public String getSentenceId() {
		return sentenceId;
	}

	public void setSentenceId(String sentenceId) {
		this.sentenceId = sentenceId;
	}

	public String getSentence() {
		return sentence;
	}

	public void setSentence(String sentence) {
		this.sentence = sentence;
	}

	public void exportCustomGml(String filename) {
		if (!Globals.saveGml)
			return;
		// Assuming path to file exists
		String gml = "";
		gml += "graph [\n"
				+ "\tcomment \"" + getSentence().replaceAll("\"", "").replaceAll("\n", "") + "\"\n"
				+ "\tlabel \"" + getSentenceId() + "\"\n";
		// Add nodes, and then edges
		for (JNode v : vertexSet()) {
			gml += v.toGML();
		}
		for (DefaultWeightedEdge e : edgeSet()) {
			gml += "\tedge [\n"
					+ "\t\tsource " + this.getEdgeSource(e).getId() + "\n"
					+ "\t\ttarget " + this.getEdgeTarget(e).getId() + "\n"
					+ "\t\tweight " + this.getEdgeWeight(e) + "\n"
					+ "\t]\n";
		}
		gml += "]\n";
		try {
			BufferedWriter file = new BufferedWriter(new FileWriter(filename));
			file.write(gml);
			file.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * <sentence_index, senses> group senses by cluster
	 * @return
	 */
	public Map<Integer, Set<JNode>> getClusters() {
		Map<Integer, Set<JNode>> clusters = new HashMap<>();
		for (JNode n : this.vertexSet()) {
			int index = n.getSentenceIndex();
			if (!clusters.containsKey(index)) {
				clusters.put(index, new LinkedHashSet<JNode>());
			}
			clusters.get(index).add(n);
		}
		return clusters;
	}

	public JNode getFirstVertex() {
		return this.getVertexArray().get(0);
	}

	public boolean saveToGTSP(String path, String filename) {
		// Assuming path to file exists
		Set<JNode> nodes = this.vertexSet();
		Set<DefaultWeightedEdge> edges = this.edgeSet();
		Map<Integer, Set<JNode>> clusters = this.getClusters();
		int size = nodes.size();
		if (size == 0) {
			log(Globals.logWarning, "Graph has no vertexes, cannot save to GTSP format");
			return false;
		}
		if (clusters.size() == 1) {
			log(Globals.logWarning, "Graph has only 1 cluster, don't save to GTSP and don't run solver");
			return false;
		}
		int[][] matrix = this.getAdjacencyMatrix();
		String content = "";
		content += "NAME : " + filename + ".gtsp\n" ;
		content += "TYPE : GTSP\n";
		content += "COMMENT : " + getSentenceId() + " " + getSentence().replaceAll("\n", " ") + "\n";
		content += "DIMENSION : "+ size +"\n";
		content += "GTSP_SETS : "+ clusters.size() +"\n";
		content += "EDGE_WEIGHT_TYPE : EXPLICIT \n";
		content += "EDGE_WEIGHT_FORMAT : FULL_MATRIX \n";
		content += "EDGE_WEIGHT_SECTION : \n";
		// Prints edge weights
		for(int row = 0; row < size; row++){
			for(int col = 0; col < size; col++){
				if (matrix[row][col] <= 0) {
					log(Globals.logSevere, "[GTSP] Edge with weight < 0");
				}
				content += matrix[row][col]+" ";
			}
			content += "\n";
		}
		content += "GTSP_SET_SECTION : \n";
		int i = 0, j = 0;
		for (Set<JNode> clusterNodes : clusters.values()) {
			content += (i+1) + " ";
			for (JNode node : clusterNodes) {
				content += (j+1) + " "; 
				j++;
			}
			content += "-1\n";
			i++;
		}
		content += "EOF";
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(path + filename+".gtsp"));
			writer.write(content);
			writer.close();
			log(Globals.logInfo, "Saved graph to " + path + filename + ".gtsp");
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	private int[][] getAdjacencyMatrix() {
		Set<JNode> nodes = this.vertexSet();
		int size = nodes.size();
		int[][] matrix = new int [size][size];
		int i = 0;
		for (JNode n1 : nodes) {
			int j = 0;
			for (JNode n2 : nodes) {
				if (this.containsEdge(n1, n2)) {
					matrix[i][j] = (int)this.getEdgeWeight(this.getEdge(n1, n2));
				} else {
					matrix[i][j] = Globals.precision;
				}
				j++;
			}
			i++;
		}
		return matrix;
	}
	
	public ArrayList<JNode> getVertexArray() {
		Map<Integer, Set<JNode>> nodes = getClusters();
		ArrayList<JNode> array = new ArrayList<JNode>();
		for (Set<JNode> node : nodes.values()) {
			for (JNode n : node) {
				array.add(n);
			}
		}
		return array;
	}

	public String printCentrality() {
		String str = "";
		ArrayList<JNode> array = this.getVertexArray();
		for (JNode n : array) {
			str += n.getSenseKey() + " " + n.getSentenceIndex() + " " + n.getCentrality() + "\n";
		}
		str += "\n";
		return str;
	}

	public void resetLog() {
		this.log = "";
	}
}
