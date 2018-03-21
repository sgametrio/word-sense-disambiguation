/**
 * Adapter Class implementing some functionalities useful to perform WSD
 */

package com.sgametrio.wsd;

import java.time.Duration;
import java.time.Instant;

import it.uniroma2.sag.kelp.data.representation.structure.similarity.StructureElementSimilarityI;
import it.uniroma2.sag.kelp.data.representation.tree.TreeRepresentation;
import it.uniroma2.sag.kelp.input.parser.DependencyParser;
import it.uniroma2.sag.kelp.input.parser.impl.StanfordParserWrapper;
import it.uniroma2.sag.kelp.input.parser.model.DependencyGraph;
import it.uniroma2.sag.kelp.input.tree.TreeRepresentationGenerator;
import it.uniroma2.sag.kelp.input.tree.generators.LemmaCompactPOSLabelGeneratorLowerCase;
import it.uniroma2.sag.kelp.input.tree.generators.LexicalElementLabelGenerator;
import it.uniroma2.sag.kelp.input.tree.generators.OriginalPOSLabelGenerator;
import it.uniroma2.sag.kelp.input.tree.generators.PosElementLabelGenerator;
import it.uniroma2.sag.kelp.input.tree.generators.RelationNameLabelGenerator;
import it.uniroma2.sag.kelp.input.tree.generators.SyntElementLabelGenerator;
import it.uniroma2.sag.kelp.kernel.tree.PartialTreeKernel;
import it.uniroma2.sag.kelp.kernel.tree.SmoothedPartialTreeKernel;
import it.uniroma2.sag.kelp.kernel.tree.SubSetTreeKernel;
import it.uniroma2.sag.kelp.kernel.tree.SubTreeKernel;

public class KelpAdapter {
	private DependencyParser parser = null;
	private SyntElementLabelGenerator rg = null;
	private LexicalElementLabelGenerator ng = null;
	private PosElementLabelGenerator ig = null;
	
	//Kernel tree params
	private float lambda = 1f; //0.4f most used, used 1 to get int needed by tsp solver in subTrees subSetTrees e partialTrees
	private float mu = 1f; //0.4f most used, used 1 to get int needed by tsp solver in partialTrees
	private int terminalFactor = 1; //for partialTree/smoothedPT: substructure that contains a leaf are counted (terminalFactor X matching value). If it is set to 2 and lambda and mu are 1, they are coundetd double (2 X 1)
	private float threshold = 0.001f; //as suggested in KeLP official guede 
	private String representationIdentifier = "tree";
	private StructureElementSimilarityI similarityFunction = null;
	
	private boolean considerLeaves = true;
	
	public KelpAdapter(){
		this.parser = new StanfordParserWrapper();
		this.parser.initialize();
		//Initialize the label generators for: syntactic nodes, lexical nodes and part-of-speech nodes
		this.rg = new RelationNameLabelGenerator();
		this.ng = new LemmaCompactPOSLabelGeneratorLowerCase();
		this.ig = new OriginalPOSLabelGenerator();
		
	}
	public KelpAdapter(StructureElementSimilarityI similarityFunction){
		this();
		this.similarityFunction = similarityFunction;
	}
	
	
	/**
	 * compute the GRCT for the given sentence
	 * @param sentence: sentence of which GRCT is desired
	 * @return the GRCT for the given sentence with the specified bracket type
	 */
	public String generateGRCT(String sentence) {		
		// Parser a sentence
		DependencyGraph dependencyGraph = this.parser.parse(sentence);
		// Generate the KeLP TreeRepresentation
		TreeRepresentation grctGenerator = TreeRepresentationGenerator.grctGenerator(dependencyGraph, this.rg, this.ng, this.ig);
		String grct = grctGenerator.getTextFromData();

		return grct;
	}
	
	
	/**
	 * compute the compositional GRCT for the given sentence
	 * @param sentence: sentence of which compositional GRCT is desired
	 * @return the compositional GRCT for the given sentence
	 */
	public String generateCompositionalGRCT(String sentence){
		// Parser a sentence
		DependencyGraph dependencyGraph = this.parser.parse(sentence);
		// Generate the KeLP TreeRepresentation
		TreeRepresentation compositionalGrctGenerator = TreeRepresentationGenerator.cgrctGenerator(dependencyGraph, this.rg, this.ng, this.ig);
		String cgrct = compositionalGrctGenerator.getTextFromData();

		return cgrct;
	}
	
	/**
	 * compute the compositional LCT for the given sentence
	 * @param sentence: sentence of which compositional LCT is desired
	 * @return the compositional LCT for the given sentence
	 */
	public String generateLCT(String sentence){
		// Parser a sentence
		DependencyGraph dependencyGraph = this.parser.parse(sentence);
		// Generate the KeLP TreeRepresentation
		TreeRepresentation compositionalGrctGenerator = TreeRepresentationGenerator.lctGenerator(dependencyGraph, this.rg, this.ng, this.ig);
		String lct = compositionalGrctGenerator.getTextFromData();
		
		return lct;
	}
	

	/**
	 * compute the compositional LOCT for the given sentence
	 * @param sentence: sentence of which compositional LOCT is desired
	 * @return the compositional LOCT for the given sentence
	 */
	public String generateLOCT(String sentence){
		// Parser a sentence
		DependencyGraph dependencyGraph = this.parser.parse(sentence);
		// Generate the KeLP TreeRepresentation
		TreeRepresentation compositionalGrctGenerator = TreeRepresentationGenerator.loctGenerator(dependencyGraph, this.rg, this.ng, this.ig);
		String loct = compositionalGrctGenerator.getTextFromData();
		
		return loct;
	}
	
	/**
	 * Compute similarity between two given tree
	 * @param treeRepres1: string representation of the first tree
	 * @param treeRepres2: string representation of the second tree
	 * @param kernelType: type of structure to be used ("subTree","subsetTree" or "partialTree")
	 * @return: similarity score between the given tree considering the specified structure
	 */
	public double computeTreeSimilarity(String treeRepres1, String treeRepres2, String structureType){
		double similarity = -1000000;
		int terminalFactorOld = this.getTerminalFactor();
		
		//LexicalStructureElementSimilarity similarityFunction = new LexicalStructureElementSimilarity();
		//CompositionalNodeSimilarityProduct similarityFunction = new CompositionalNodeSimilarityProduct();
		//SameAdditionalInfoStructureElementSimilarity similarityFunction = new SameAdditionalInfoStructureElementSimilarity();
		
		TreeRepresentation t1 = new TreeRepresentation();
		TreeRepresentation t2 = new TreeRepresentation();
		try {
			t1.setDataFromText(treeRepres1);
			t2.setDataFromText(treeRepres2);
		} catch (Exception e) {
			System.err.print(Thread.currentThread().getStackTrace()[1].getMethodName()+" threw: ");
			System.err.println(e);
		}
		
		switch(structureType) {
			case "subTree":
//				System.out.println("Evaluating similarity using subTree \n");
				//using 1f returns the number of substructure that match
				SubTreeKernel subTreeKernel = new SubTreeKernel(this.lambda, this.representationIdentifier);
				if(!this.considerLeaves){
					subTreeKernel.setIncludeLeaves(false);
				}
				similarity = subTreeKernel.kernelComputation(t1, t2);
				break;
			case "subsetTree":
//				System.out.println("Evaluating similarity using subsetTree \n");
				//using 1f returns the number of substructure that match
				SubSetTreeKernel subSetTreeKernel = new SubSetTreeKernel(this.lambda, this.representationIdentifier);
				if(!this.considerLeaves){
					subSetTreeKernel.setIncludeLeaves(false);
				}
				similarity = subSetTreeKernel.kernelComputation(t1, t2);
				break;
			case "partialTree":
//				System.out.println("Evaluating similarity using partialTree \n");
				if(!this.considerLeaves){
					this.setTerminalFactor(0);
				}
				//using 1f returns the number of substructure that match
				PartialTreeKernel partialTreeKernel = new PartialTreeKernel(this.lambda, this.mu, this.terminalFactor, this.representationIdentifier);
				similarity = partialTreeKernel.kernelComputation(t1, t2);
				this.setTerminalFactor(terminalFactorOld);
				break;
			case "smoothedPartialTree":
//				System.out.println("Evaluating similarity using smoothedPartialTree \n");
				if(!this.considerLeaves){
					this.setTerminalFactor(0);
				}
				//float LAMBDA, float MU, float terminalFactor, float similarityThreshold, StructureElementSimilarityI nodeSimilarity, String representationIdentifier)
				
				SmoothedPartialTreeKernel sptk = new SmoothedPartialTreeKernel(this.lambda, this.mu, this.terminalFactor, this.threshold, this.similarityFunction, this.representationIdentifier);
				similarity = sptk.kernelComputation(t1, t2);
				this.setTerminalFactor(terminalFactorOld);
				break;
			default:
				System.err.println("Invalid stuctureType: "+ structureType);
				System.exit(1);
		}
		return similarity;
	}
	
	public void setConsiderLeaves(boolean considerLeaves){
		this.considerLeaves = considerLeaves;
	}
	public void setLambda(float lambda){
		this.lambda = lambda;
	}
	public void setMu(float mu){
		this.mu = mu;
	}
	public void setTerminalFactor(int terminalFactor){
		this.terminalFactor = terminalFactor;
	}
	public void setThreshold(float threshold){
		this.threshold = threshold;
	}
	public void setRepresentationIdentifier(String representationIdentifier){
		this.representationIdentifier = representationIdentifier;
	}
	public float getLambda(){
		return this.lambda;
	}
	public float getMu(){
		return this.mu;
	}
	public int getTerminalFactor(){
		return this.terminalFactor;
	}
	public float getThreshold(){
		return this.threshold;
	}
	public String getRepresentationIdentifier(){
		return this.representationIdentifier;
	}
	public boolean getConsiderLeaves(){
		return this.considerLeaves;
	}
}
