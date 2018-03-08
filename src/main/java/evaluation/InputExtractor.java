package evaluation;

import java.util.ArrayList;
import java.util.HashMap;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sgametrio.wsd.WsdLauncher;

public class InputExtractor {
	
	public static final String evaluationFrameworkPathToALLFile = WsdLauncher.frameworkFilePath+"ALL/ALL.data.xml";
	public static final String evaluationFrameworkPathToSemeval2007File = WsdLauncher.frameworkFilePath+"semeval2007/semeval2007.data.xml";
	
	public static final String evaluationFrameworkPathToSemeval20137File = WsdLauncher.frameworkFilePath+"semeval2013/semeval2013.data.xml";
	public static final String evaluationFrameworkPathToSemeval2015File = WsdLauncher.frameworkFilePath+"semeval2015/semeval2015.data.xml";
	public static final String evaluationFrameworkPathToSenseval2File = WsdLauncher.frameworkFilePath+"senseval2/senseval2.data.xml";
	public static final String evaluationFrameworkPathToSenseval3File = WsdLauncher.frameworkFilePath+"senseval3/senseval3.data.xml";
	
	public static String currentDataFile = evaluationFrameworkPathToSenseval3File;
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
}
