/**
 * Class that defines a customized vertex representation
 */

package com.sgametrio.wsd;

import java.util.ArrayList;

import edu.mit.jwi.item.IWord;
import edu.mit.jwi.item.IWordID;

public class WsdVertex {
    private int id;
    private int sentenceIndex;
    private String searchTerm;
	private String word; 
    private String pos;
    private String gloss;
    private String glossKey;
    private String params;
    private String[] lemmaWord;
    private IWordID uniqueWordNetID;
    private ArrayList<String> examples;
    private double weight;
	private double centrality;
	private boolean support = false;
    
    /**CONSTRUCTOS
     * 
     * @param id: vertexId
     * @param word: the word the vertex is representing
	 * @param pos: part of speech of the word
	 * @param gloss: word gloss
     * @param examples: a list with the WordNet examples for the gloss
     */
    public WsdVertex(int id, int sentenceIndex, String searchTerm, String word, String pos, String gloss, String glossKey, String params, String lemma_wordPair[], ArrayList<String> examples, IWordID wordId) {
        this.id = id;
        this.sentenceIndex = sentenceIndex;
        this.searchTerm = searchTerm;
    	this.word = word;
        this.pos = pos;
        this.gloss = gloss;
        this.glossKey = glossKey;
        this.params = params;
        this.lemmaWord = lemma_wordPair;
        this.examples = examples;
        this.weight = 0;
        this.uniqueWordNetID = wordId;
    }
    
    /**
     * @param id: vertexId
     * @param word: the word the vertex is representing
	 * @param pos: part of speech of the word
	 * @param gloss: word gloss
     */
    public WsdVertex(int id, int sentenceIndex, String usedTerm, String word, String pos, String gloss, String glossKey, String params, String lemma_wordPair[], IWordID wordId) {
        
    	this(id, sentenceIndex, usedTerm, word, pos, gloss, glossKey, params, lemma_wordPair, new ArrayList<String>(), wordId);
    	
    }
    
	public WsdVertex(int id, IWord word) {
		// Used to create support 
		this.id = id;
		this.sentenceIndex = -1;
		this.searchTerm = "";
		this.word = word.getLemma();
		this.pos = word.getPOS().toString();
		this.gloss = "";
		this.glossKey = word.getSenseKey().toString();
		this.params = "";
		String[] lemmaWord = {"",""};
		this.lemmaWord = lemmaWord;
		this.examples = new ArrayList<String>();
		this.uniqueWordNetID = word.getID();   
	}

/**
    * Customized method to check if two vertices are equals (have the same id)
    * @param node2: the vertex to be compared
    * @return: true if the ids matches, false otherwise
    */
    public boolean equals(WsdVertex node2){
    	return this.id == node2.id;
    }
    
    /** Customized method to check if two vertices are same concept (have the same id)
    * @param node2: the vertex to be compared
    * @return: true if the ids matches, false otherwise
    */
    public boolean equalsSupport(WsdVertex node2){
    	return this.uniqueWordNetID == node2.getWordId();
    }
    
    /**
     * override of the toString() function. Returns the vertex with all its parameters
     * @return output: a string containing all vertex parameters
     */
    @Override
	public String toString() { 
    	String output = 
    			"VERTEX ID: " + this.getId() +
    			"\nSENTENCE_INDEX: " + this.getSentenceIndex() +
    			"\nUSED_TERM: "+ this.getSearchTerm() + 
    			"\nWORD_ID: " + this.getWordId() + 
    			"\nWORD: "+ this.getWord() + 
    			"\nPOS: " + this.getPOS() + 
    			"\nGLOSS_KEY: " + this.getGlossKey() + 
    			"\nPARAMS: " + this.getParams() + 
    			"\nGLOSS: " + this.getGloss() + 
    			"\nLEMMA_WORD_PAIR: " + this.getLemmaWord()[0]+" "+this.getLemmaWord()[1];    			
		for(int i = 0; i < this.getExamples().size(); i++){
			output += "EXAMPLE_"+(i+1)+": "+ this.getExamples().get(i)+"\n";
		}
		
    	return output + "\n";
		
	} 
    
    //GETTER FUNCTIONS
    
	public int getId(){
		
		return this.id;
	}
	public IWordID getWordId(){
		
		return this.uniqueWordNetID;
	}
	public String getSearchTerm(){
		
		return this.searchTerm;
	}
	
	public String getWord(){
		
		return this.word;
	}
	
	public int getSentenceIndex(){
		return this.sentenceIndex;
	}
	
	public String getGlossKey(){
		
		return this.glossKey;
	}
	
	public String getGloss(){
		
		return this.gloss;
	}
	
	public String getPOS(){
		
		return this.pos;
	}
	
	public String[] getLemmaWord(){
		
		return this.lemmaWord;
	}
	
	public double getWeight() {
		return this.weight;
	}
	
	public void setWeight(double weight) {
		this.weight = weight;
	}
	
	public double getCentrality() {
		return this.centrality;
	}
	
	public void setCentrality(double centrality) {
		this.centrality = centrality;
	}
	
public String getParams(){
		
		return this.params;
	}
	
	public ArrayList<String> getExamples(){

		return this.examples;

	}

	public boolean getSupport() {
		return this.support;
	}
	
}
