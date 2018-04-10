package com.sgametrio.wsd;

import java.util.ArrayList;

import edu.mit.jwi.item.IWord;
import evaluation.InputInstance;

public class MyVertex {
	private static int progressiveId = 0;
	private int id;
	private IWord word;
	private ArrayList<MyEdge> adjList;
	/** Sentence parameters	 */
	private String sentenceTermId;
	private int sentenceIndex;
	private String searchTerm;
	private String gloss;
	/** Tree repr */
	private String treeRep;
	
	private double centrality = 0;
	private ArrayList<IWord> relatedWords;
	
	/**
	 * Constructor used to create support nodes, I already have the IWord object
	 * @param word
	 */
	public MyVertex(IWord word) {
		this.id = progressiveId++;
		this.sentenceIndex = -1;
		this.word = word;
		this.gloss = "";
		this.searchTerm = "";
		this.treeRep = "";
		this.adjList = new ArrayList<MyEdge>();
	}
	
	public MyVertex(IWord word, String treeRep) {
		this.id = progressiveId++;
		this.sentenceIndex = -1;
		this.word = word;
		this.treeRep = treeRep;
		this.gloss = "";
		this.searchTerm = "";
		this.adjList = new ArrayList<MyEdge>();
	}
	
	/**
	 * Constructor used to create senses from terms in a sentence
	 * @param index
	 * @param searchTerm
	 * @param word
	 */
	public MyVertex(int index, String searchTerm, IWord word) {
		this.id = progressiveId++;
		this.sentenceIndex = index;
		this.searchTerm = searchTerm;
		this.word = word;
		this.gloss = "";
		this.treeRep = "";
		this.adjList = new ArrayList<MyEdge>();
	}
	
	/**
	 * Constructor used to create senses from terms in a sentence
	 * @param input
	 * @param word
	 */

	public MyVertex(InputInstance input, IWord word, String treeRep, String gloss) {
		this.id = progressiveId++;
		this.sentenceIndex = input.index;
		this.searchTerm = input.lemma;
		this.sentenceTermId = input.id;
		this.word = word;
		this.gloss = gloss;
		this.treeRep = treeRep;
		this.adjList = new ArrayList<MyEdge>();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((adjList == null) ? 0 : adjList.hashCode());
		long temp;
		temp = Double.doubleToLongBits(centrality);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + id;
		result = prime * result + ((searchTerm == null) ? 0 : searchTerm.hashCode());
		result = prime * result + sentenceIndex;
		result = prime * result + ((word == null) ? 0 : word.hashCode());
		return result;
	}

	public boolean equals(MyVertex v) {
		return this.id == v.getId();
	}

	public int getId() {
		return this.id;
	}

	public IWord getWord() {
		return word;
	}
	
	public String getGlossKey() {
		return word.getSenseKey().toString();
	}

	public void setWord(IWord word) {
		this.word = word;
	}

	public int getSentenceIndex() {
		return sentenceIndex;
	}

	public double getCentrality() {
		return centrality;
	}

	public void setCentrality(double centrality) {
		this.centrality = centrality;
	}

	public void addEdge(MyVertex target, double weight) {
		MyEdge edge = new MyEdge(target, weight);
		this.adjList.add(edge);
	}
	
	public ArrayList<MyEdge> getPositiveEdges() {
		ArrayList<MyEdge> edges = new ArrayList<MyEdge>();
		for (MyEdge e : this.adjList) {
			if (e.getWeight() != -1)
				edges.add(e);
		}
		return edges;
	}
	
	/**
	 * Pay attention, this returns edges with weight -1!
	 * @return
	 */
	public ArrayList<MyEdge> getEdges() {
		return this.adjList;
	}

	public String getTreeGlossRepr() {
		return this.treeRep;
	}
	
	public String getGloss() {
		return this.gloss;
	}

	public MyEdge getEdge(MyVertex target) {
		for (MyEdge e : this.adjList) {
			if (target.equals(e.getDest()) && e.getWeight() != -1)
				return e;
		}
		return null;
	}

	public String getSentenceTermId() {
		return this.sentenceTermId;
	}

	public String toGML() {
		String gml = "";
		gml += "\tnode [\n"
				+ "\t\tid " + this.getId() + "\n"
				+ "\t\tlabel \"" + this.getWord().getLemma() + "_" + this.getSentenceIndex() + "-" + this.getId() + "\"\n"
				+ "\t\tcentrality \"" + this.getCentrality() + "\"\n"
				+ "\t\tgloss_key \"" + this.getGlossKey() + "\"\n"
				+ "\t\tgloss \"" + this.getGloss().replaceAll("\"", "") + "\"\n"
				+ "\t]\n";
		return gml;
	}

	public void setRelatedWords(ArrayList<IWord> vRelatedWords) {
		this.relatedWords = vRelatedWords;
	}
	
	public ArrayList<IWord> getRelatedWords() {
		return this.relatedWords;
	}

	public int getOutDegree() {
		return this.adjList.size();
	}

	public void setTreeRep(String treeRep) {
		this.treeRep = treeRep;
	}

	public void removeEdge(MyEdge e) {
		e.setWeight(-1);
	}
}
