package globals;

import java.io.File;

public class Globals {
	public static final String frameworkFilePath = "src/main/resources/evaluation-datasets/";
	
	public static final String wordnetHome = "/usr/local/WordNet-3.0";//path to WordNet home folder
	public static final String path = wordnetHome + File.separator + "dict";
	
	public static final String pathToAll = frameworkFilePath+"ALL/ALL";
	public static final String pathToSemeval2007 = frameworkFilePath+"semeval2007/semeval2007";
	public static final String pathToSemeval2013 = frameworkFilePath+"semeval2013/semeval2013";
	public static final String pathToSemeval2015 = frameworkFilePath+"semeval2015/semeval2015";
	public static final String pathToSenseval2 = frameworkFilePath+"senseval2/senseval2";
	public static final String pathToSenseval3 = frameworkFilePath+"senseval3/senseval3";

	public static final String dataFileSuffix = ".data.xml";
	public static final String goldFileSuffix = ".gold.key.txt";
	
	// Change this if you want to change evaluation dataset
	public static final String currentEvaluationPath = pathToAll;
	
	public static final String currentGoldFile = currentEvaluationPath + goldFileSuffix;
	public static final String currentDataFile = currentEvaluationPath + dataFileSuffix;
	
	public static final String tspSolverHomeDir = "src/main/resources/GLKH-1.0/";
	public static final String tspSolverPathToGTSPLIB = tspSolverHomeDir + "GTSPLIB/";
	public static final String tspSolverPathToGTOURS = tspSolverHomeDir + "G-TOURS/";
	public static final String tspSolverFileName = "runGLKH";
	public static final String tspSolverPathFileName = tspSolverHomeDir + tspSolverFileName;
	public static final String resultsPath = "RESULTS/";
	public static final String resultsFileName = "_wsdResults.KEY";
	public static final String gmlPath = "GML/";
	public static final String gtspPath = tspSolverPathToGTSPLIB;
	public static final String tourPath = tspSolverPathToGTOURS;
}
