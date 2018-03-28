package evaluation;

import java.util.ArrayList;
import java.util.HashMap;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class InputExtractor {
	/**
	 * Extract fields from the given xml sentence and output them in a format valid to be given to 
	 * performDisambiguation method
	 * @param xmlSentence
	 * @return a map having pos as key and an arrayList of lemma, word, wordIndex and params values for each
	 * word having that pos.
	 */
	public static HashMap<String, ArrayList<String[]>> extractInput(Node xmlSentence){
		HashMap<String, ArrayList<String[]>> pos_lemmaWordIndexParams = new HashMap<String, ArrayList<String[]>>();
		
		if (xmlSentence.getNodeType() == Node.ELEMENT_NODE) {
			int wordIndex = 1;	
			Element s = (Element) xmlSentence;
			NodeList children = s.getChildNodes(); 
			int numberOfInstances = 0;
			for(int i = 0; i<children.getLength(); i++){
				Node child = children.item(i);
				if(child.getNodeType()== Node.ELEMENT_NODE){
					Element elementChild = (Element) child;
					String lemma = elementChild.getAttribute("lemma");
					String pos = elementChild.getAttribute("pos");
					String params = "";
					//if the word has to be disambiguated and evaluated it will have params
					if(elementChild.getTagName().equalsIgnoreCase("instance")){
						params = elementChild.getAttribute("id");
						numberOfInstances++;
					//if the word only has to be disambiguated it won't have params
					}else if(elementChild.getTagName().equalsIgnoreCase("wf")){
						params = null;
					}
					ArrayList<String[]> tmp;
					if(pos_lemmaWordIndexParams.containsKey(pos)){
						tmp = pos_lemmaWordIndexParams.get(pos);
					}else{
						tmp = new ArrayList<String[]>();
					}
					String[] lemmaWordParams = {lemma, lemma, ""+wordIndex, params};
					tmp.add(lemmaWordParams);
					pos_lemmaWordIndexParams.put(pos, tmp);
					wordIndex++;
				}
			}
			if(numberOfInstances == 1)
				System.out.println("Only 1 instance to disambiguate");
		}
		return pos_lemmaWordIndexParams;
	}
	
	public static InputSentence myExtractInput(Node xmlSentence){
		InputSentence sentence = new InputSentence();
		if (xmlSentence.getNodeType() == Node.ELEMENT_NODE) {
			int wordIndex = 1;	
			Element s = (Element) xmlSentence;
			NodeList children = s.getChildNodes();
			sentence.instances = new ArrayList<InputInstance>();
			sentence.sentence = xmlSentence.getTextContent();
			int numberOfInstances = 0;
			for(int i = 0; i<children.getLength(); i++){
				Node child = children.item(i);
				if(child.getNodeType()== Node.ELEMENT_NODE){
					Element elementChild = (Element) child;
					String lemma = elementChild.getAttribute("lemma");
					String pos = elementChild.getAttribute("pos");
					String params = "";
					//if the word has to be disambiguated and evaluated it will have params
					if(elementChild.getTagName().equalsIgnoreCase("instance")){
						params = elementChild.getAttribute("id");
						numberOfInstances++;
					//if the word only has to be disambiguated it won't have params
					}else if(elementChild.getTagName().equalsIgnoreCase("wf")){
						params = null;
					}
					InputInstance instance = new InputInstance();
					instance.id = params;
					sentence.sentenceId = s.getAttribute("id");
					instance.lemma = lemma;
					instance.index = wordIndex;
					instance.pos = pos;
					instance.term = child.getTextContent();
					sentence.instances.add(instance);
					wordIndex++;
				}
			}
			/*if(numberOfInstances == 1)
				System.out.println("Only 1 instance to disambiguate");*/
		}
		return sentence;
	}
}
