/**
 * Adapter class for the Stanford library utilities
 */
package com.sgametrio.wsd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.util.CoreMap;

public class StanfordUtilsAdapter {
	
	private Properties props;
	private StanfordCoreNLP pipeline; 
	
	/**CONSTRUCTORS
	 * 
	 */
	public StanfordUtilsAdapter(){
		
		 this.props = new Properties();
		 this.props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
		 this.pipeline = new StanfordCoreNLP(props);
	}
	
	/**
	 * Perform lemmatization and POS tagging over the given sentence
	 * @param sentence: the sentence on which lemmatization and POS taggin have to be performed
	 * @return: a map having POS as key and a list of lemmatized word having that POS as value
	 */
	public HashMap <String, ArrayList<String[]>>  calculateLemmasAndPOS(String sentence){
		HashMap <String, ArrayList<String[]>> pos_lemmaWordIndex = new HashMap<String, ArrayList<String[]>>();
		Annotation document = new Annotation(sentence);
		pipeline.annotate(document);
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		// for each sentence
		for (CoreMap sent : sentences) {
			// For each token
			for (CoreLabel token : sent.get(TokensAnnotation.class)) {
				ArrayList<String[]> temp;
				if(pos_lemmaWordIndex.containsKey(token.tag())){
					temp = pos_lemmaWordIndex.get(token.tag());
				}else{
					temp = new ArrayList<String[]>();
				}
				String[] lemmaWordIndexParams = new String[4];
				lemmaWordIndexParams[0] = token.lemma().toLowerCase();
				lemmaWordIndexParams[1] = token.word().toLowerCase();
				lemmaWordIndexParams[2] = ""+token.index();
				lemmaWordIndexParams[3] = null;
				temp.add(lemmaWordIndexParams);
				pos_lemmaWordIndex.put(token.tag(), temp);
			}
		}
		return pos_lemmaWordIndex;
	}

}
