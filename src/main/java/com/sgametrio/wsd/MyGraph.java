package com.sgametrio.wsd;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import edu.mit.jwi.item.IWord;

public class MyGraph {
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
		gml += "graph [\n";
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
			System.out.println(e);
		}
		
		
	}

	public boolean containsWord(IWord word) {
		for (MyVertex v : vertexes) {
			if (v.getWord().equals(word)) {
				return true;
			}
		}
		return false;
	}
}
