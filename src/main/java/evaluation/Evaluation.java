package evaluation;
import javax.xml.parsers.DocumentBuilder; 
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.PrintWriter;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException; 
import org.xml.sax.SAXParseException;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.*;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import java.util.*;

public class Evaluation {

	public static void main(String[] args) throws Exception {

		Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma, parse");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);	
		
	    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	    dbf.setNamespaceAware(true);
	    dbf.setValidating(false);
	    DocumentBuilder db = dbf.newDocumentBuilder();
	    Document doc = db.parse(new File("senseval3.data.xml"));	    
	    NodeList nodeListText = doc.getDocumentElement().getChildNodes();
	    for (int i = 0; i < nodeListText.getLength(); i++) {
	    NodeList nodeListSentence = nodeListText.item(i).getChildNodes();
	    	for (int l = 0; l < nodeListSentence.getLength(); l++) {
	    		if(nodeListSentence.item(l).hasAttributes()) {	    		
	    		    PrintWriter writer = new PrintWriter(nodeListSentence.item(l).getAttributes().getNamedItem("id").getNodeValue(), "UTF-8");
	    		    NodeList nodeListWord = nodeListSentence.item(l).getChildNodes();
	    		    StringBuffer text = new StringBuffer();
	    		    Hashtable lemmaId = new Hashtable();
	    		    for (int j = 0; j < nodeListWord.getLength(); j++) {
	    		    	if(!nodeListWord.item(j).getTextContent().trim().equals("")) {
	    		    		text.append(nodeListWord.item(j).getTextContent() + " ");	    		    		
	    		    	}
	    		    	if(nodeListWord.item(j).getNodeName() == "instance") {
	    		    		lemmaId.put(nodeListWord.item(j).getAttributes().getNamedItem("lemma").getNodeValue(), nodeListWord.item(j).getAttributes().getNamedItem("id").getNodeValue());
	    		    	}
	    		    }
		    
    		        Annotation document = new Annotation(text.toString());
    		        pipeline.annotate(document);
    		        List <CoreMap> sentences = document.get(SentencesAnnotation.class);
    		        for(CoreMap sentence: sentences) {
    		        	for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
    		            	String pos = token.get(PartOfSpeechAnnotation.class);
    		            	String lemma = token.get(LemmaAnnotation.class);
    		            	String lemmaid = "";
    		            	if (lemmaId.get(lemma) != null) lemmaid=lemmaId.get(lemma).toString();
    		            	if (pos.equals("NN") || pos.equals("NNP") || pos.equals("NNPS") || pos.equals("NNS")) {
    		            		writer.println(lemma+"#n "+ lemmaid);
    		            	}
    		            	else if (pos.equals("RB") || pos.equals("RBR") || pos.equals("RBS")) {
    		            		writer.println(lemma+"#r "+ lemmaid);
    		            	}        		
    		            	else if (pos.equals("VB") || pos.equals("VBD") || pos.equals("VBG") || pos.equals("VBN") || pos.equals("VBF") || pos.equals("VBZ") ) {
    		            		writer.println(lemma+"#v "+ lemmaid);
    		            	}
    		            	else if (pos.equals("JJ") || pos.equals("JJR") || pos.equals("JJS")) {
    		            		writer.println(lemma+"#a "+ lemmaid);
    		            	} else {
    		            		writer.println(lemma+"#");
    		            		}
    		            	}
    		            
    		        	SemanticGraph dependencies = sentence.get(CollapsedCCProcessedDependenciesAnnotation.class);
    		        	//System.out.println(dependencies.toDotFormat() + "\n");
    		        	for (SemanticGraphEdge e : dependencies.edgeIterable()) {
    		        		//if((e.getGovernor().get(PartOfSpeechAnnotation.class).equals("NN") ||
    		        			//	e.getGovernor().get(PartOfSpeechAnnotation.class).equals("NNP") ||
    		        			//	e.getGovernor().get(PartOfSpeechAnnotation.class).equals("NNPS") ||
    		        			//	e.getGovernor().get(PartOfSpeechAnnotation.class).equals("NNS")) && 
    		        			//	(e.getDependent().get(PartOfSpeechAnnotation.class).equals("NN") ||
    		        				//		e.getDependent().get(PartOfSpeechAnnotation.class).equals("NNP") ||
    		        				//		e.getDependent().get(PartOfSpeechAnnotation.class).equals("NNPS") ||
    		        				//
    		        		//e.getDependent().get(PartOfSpeechAnnotation.class).equals("NNS")))
    	                    writer.printf ("%s$%s%n",e.getGovernor().lemma(), e.getDependent().lemma());
    	                }
    		        }
    		        writer.close();
	    		}
	    	}
	    }
	}
	
	private static class MyErrorHandler implements ErrorHandler {
	     
	    private PrintWriter out;

	    MyErrorHandler(PrintWriter out) {
	        this.out = out;
	    }

	    private String getParseExceptionInfo(SAXParseException spe) {
	        String systemId = spe.getSystemId();
	        if (systemId == null) {
	            systemId = "null";
	        }

	        String info = "URI=" + systemId + " Line=" + spe.getLineNumber() +
	                      ": " + spe.getMessage();
	        return info;
	    }

	    public void warning(SAXParseException spe) throws SAXException {
	        out.println("Warning: " + getParseExceptionInfo(spe));
	    }
	        
	    public void error(SAXParseException spe) throws SAXException {
	        String message = "Error: " + getParseExceptionInfo(spe);
	        throw new SAXException(message);
	    }

	    public void fatalError(SAXParseException spe) throws SAXException {
	        String message = "Fatal Error: " + getParseExceptionInfo(spe);
	        throw new SAXException(message);
	    }
	}
}
