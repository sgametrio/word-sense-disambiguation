package com.sgametrio.wsd;

import java.io.File;

public class Globals {
	public static final String frameworkFilePath = "src/main/resources/evaluation-datasets/";
	
	public static final String wordnetHome = "/usr/local/WordNet-3.0";//path to WordNet home folder
	public static final String path = wordnetHome + File.separator + "dict";
	
	public static final String resultsPath = "RESULTS/";
	public static final String resultsExt = "_eval.KEY";	
	
	public static final String All = "ALL";
	public static final String Semeval2007 = "semeval2007";
	public static final String Semeval2013 = "semeval2013";
	public static final String Semeval2015 = "semeval2015";
	public static final String Senseval2 = "senseval2";
	public static final String Senseval3 = "senseval3";
	public static final String MiniSenseval3 = "mini-senseval3";
	public static final String MediumSenseval3 = "medium-senseval3";
	// Available centrality measures
	public static final String degreeCentrality = "degree";
	public static final String pageRankCentrality = "page-rank";
	public static final String kppCentrality = "kpp";
	public static final String allCentrality = "all";
	public static final String kppBellmanFordCentrality = "bellman-ford";
	public static final String closenessCentrality = "closeness";
	public static final String eigenvectorCentrality = "eigenvector"; // not deterministic
	/**
	 *  Change this if you want to change current evaluation dataset
	 */
	public static final String currentDataset = All;
	public static int nodesDepth = 5;
	public static int minDepth = 2;
	public static int maxDepth = 4;
	// Current centrality method
	public static final String computeCentrality = closenessCentrality;
	public static final String[] centralities = { 
		closenessCentrality,
		eigenvectorCentrality,
		pageRankCentrality,
		degreeCentrality
	};
	                                           
	
	public static final String resultsFile = resultsPath + currentDataset + resultsExt;
	
	// Current tree
	public static final String treeKernelType = "subTree"; //subTree, subsetTree, partialTree, smoothedPartialTree
	public static final int precision = 10000;
	/*** Execution parameters ***/
	public static boolean saveGml = false;
	public static boolean solverVerbosity = false;
	// true if you want to use solver to disambiguate, false if you want to disambiguate by centrality
	public static boolean runSolver = true;
	public static boolean developmentLogs = true;
	public static boolean evaluation = true;
	// true if you want to compute node centrality and distribute it on edges
	public static boolean centrality = true;

	public static float dampingFactor = (float)1;

	public static boolean useAdditionalInstances = true;	
	
	public static final String pathToDataset = frameworkFilePath + currentDataset + "/" + currentDataset;

	public static final String dataFileSuffix = ".data.xml";
	public static final String goldFileSuffix = ".gold.key.txt";
	
	public static final String currentGoldFile = pathToDataset + goldFileSuffix;
	public static final String currentDataFile = pathToDataset + dataFileSuffix;
	public static final String myKeyFile = resultsPath + currentDataset + resultsExt;
	
	public static final String tspSolverHomeDir = "src/main/resources/GLKH-1.0/";
	public static final String GTSPLIBDirectory = "GTSPLIB/";
	public static final String GTOURSDirectory = "G-TOURS/";

	public static final String piFiles = "PI_FILES/";
	public static final String tspSolverPathToGTSPLIB = tspSolverHomeDir + GTSPLIBDirectory;
	public static final String tspSolverPathToGTOURS = tspSolverHomeDir + GTOURSDirectory;
	public static final String tspSolverFileName = "runGLKH";
	public static final String tspSolverPathFileName = tspSolverHomeDir + tspSolverFileName;
	
	public static final String gmlPath = "GML/";
	public static final String logsPath = "logs/" + new java.util.Date().toString() + "/";
	public static final String gtspPath = tspSolverPathToGTSPLIB;
	public static final String tourPath = tspSolverPathToGTOURS;
	public static final String piFilesPath = tspSolverHomeDir + piFiles;
	
	public static final int logInfo = -1;
	public static final int logStatistics = 0;
	public static final int logWarning = 1;
	public static final int logSevere = 2;
	
	public static final String fileName = currentDataset;
	
	public static final String csvReportFile = logsPath + "report.csv";

	/**
	 * Configurable solver runs
	 */
	public static final int runs = 1;
}
