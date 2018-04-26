package com.sgametrio.wsd;

import edu.mit.jwi.item.IWord;
import evaluation.InputInstance;

public class JNode {
	private static int progressiveId = 0;
	private int id;
	private IWord word;
	private String termId = null;
	private int sentenceIndex;
	private double centrality = 0.0;

	public JNode() {
		this.id = progressiveId++;
		this.sentenceIndex = -1;
	}
	
	/**
	 * Constructor for auxiliary nodes (sentence index = -1)
	 * @param word
	 */
	public JNode(IWord word) {
		this();
		this.word = word;
	}
	
	/**
	 * Constructor for disambiguation nodes (sentence index != -1)
	 * @param word (sense to represent)
	 * @param instance (instance to disambiguate)
	 */
	public JNode(IWord word, InputInstance instance) {
		this(word);
		this.sentenceIndex = instance.index;
		this.termId = instance.id;
	}
	
	public static double mean(JNode n1, JNode n2) {
		double mean = (n1.centrality + n2.centrality) / 2;
		return mean;
	}
	
	public double getCentrality() {
		return centrality;
	}

	public void setCentrality(double centrality) {
		this.centrality = centrality;
	}
	
	public IWord getWord() {
		return word;
	}
	
	public void setWord(IWord word) {
		this.word = word;
	}

	public String getSenseKey() {
		return word.getSenseKey().toString();
	}
	
	public int getId() {
		return id;
	}
	
	public String getGloss() {
		return word.getSynset().getGloss();
	}
	public String getTermId() {
		return termId;
	}

	public void setTermId(String termId) {
		this.termId = termId;
	}

	public int getSentenceIndex() {
		return sentenceIndex;
	}

	public void setSentenceIndex(int sentenceIndex) {
		this.sentenceIndex = sentenceIndex;
	}

	public boolean equals(JNode node) {
		return this.id == node.id;
	}

	public String toGML() {
		String gml = "";
		gml += "\tnode [\n"
				+ "\t\tid " + this.getId() + "\n"
				+ "\t\tlabel \"" + this.getWord().getLemma() + "_" + this.getSentenceIndex() + "-" + this.getId() + "\"\n"
				+ "\t\tcentrality \"" + this.getCentrality() + "\"\n"
				+ "\t\tgloss_key \"" + this.getSenseKey() + "\"\n"
				+ "\t\tgloss \"" + this.getWord().getSynset().getGloss().replaceAll("\"", "") + "\"\n"
				+ "\t]\n";
		return gml;
	}
}
