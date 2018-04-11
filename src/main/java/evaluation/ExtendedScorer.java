package evaluation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.sgametrio.wsd.Globals;
import com.sgametrio.wsd.WordnetAdapter;

import edu.mit.jwi.item.IWord;
import edu.mit.jwi.item.SenseKey;

public class ExtendedScorer extends Scorer {
	WordnetAdapter wordnet = null;
	
	public void doEvaluation(String goldFile, String evaluationFile) {
		// Read files and generate something cool
		File gold = new File(goldFile);
		if (!gold.exists()) 
			exit("Gold file not found at " + goldFile);
		File evaluation = new File(evaluationFile);
		if (!evaluation.exists()) 
			exit("Evaluation file not found at " + evaluationFile);
		String report = "";
		Map<String, Map<String, ArrayList<String>>> goldMap = readFileToMap(gold);
		Map<String, Map<String, ArrayList<String>>> evalMap = readFileToMap(evaluation);
		this.wordnet = new WordnetAdapter();
		// Global dataset statistics
		int goldTerms = 0;
		int goldMostCommonTerms = 0;
		int evalTerms = 0;
		int evalMostCommonTerms = 0;
		int correctEvalMostCommonTerms = 0;
		int correctDisambiguations = 0;
		int zeroCentralityTerms = 0;
		int zeroCentralityMostCommonTerms = 0;
		// 
		ArrayList<Duration> totalTimes = new ArrayList<Duration>();
		ArrayList<Duration> tspTimes = new ArrayList<Duration>();
		ArrayList<Duration> dfsTimes = new ArrayList<Duration>();
		ArrayList<Float> correctTermsPrecision = new ArrayList<Float>();
		ArrayList<Float> correctMostCommonPrecision = new ArrayList<Float>();
		ArrayList<Float> zeroCentralityPrecision = new ArrayList<Float>();
		ArrayList<Integer> mostCommons = new ArrayList<Integer>();
		// Read log file and extract statistics
		// Now sentence by sentence, evaluate 
		for (String sentence_id : evalMap.keySet()) {
			// If the fragment of text annotated by the system is not contained in the gold
			// standard then skip it.
			if (!goldMap.containsKey(sentence_id)) 
				continue;
			BufferedReader log;
			try {
				log = new BufferedReader(new FileReader(Globals.logsPath + sentence_id + ".log"));
				String line = "";
				while ((line = log.readLine()) != null) {
					if (line.contains("[SENTENCE TERMS]")) {
						// Read all disambiguations
						String term = "";
						// Sentence stats
						int sentenceTerms = 0;
						int sentenceMostCommon = 0;
						int sentenceGoldMostCommon = 0;
						int sentenceCorrectTerms = 0;
						int sentenceCorrectMostCommon = 0;
						int sentenceZeroCentrality = 0;
						while((term = log.readLine()).length() != 0) {
							// term_id sense_key centrality
							String[] info = term.split(" ");
							String term_id = info[0];
							int last_dot = term_id.lastIndexOf(".");
							String instance_id = term_id.substring(last_dot + 1);
							String eval_sense_key = info[1];
							float centrality = Float.parseFloat(info[2]);
							// for every sense_key find most common sense (the first retrieved by wordnet
							IWord mostCommon = this.wordnet.getMostCommonWord(eval_sense_key);
							String senseKeyMostCommon = SenseKey.toString(mostCommon.getSenseKey());
							String gold_sense_key = goldMap.get(sentence_id).get(instance_id).get(0);
							boolean most_common = false;
							if (eval_sense_key.equals(senseKeyMostCommon)) {
								most_common = true;
								sentenceMostCommon++;
							}
							if (gold_sense_key.equals(senseKeyMostCommon)) {
								sentenceGoldMostCommon++;
							}
							if (eval_sense_key.equals(gold_sense_key)) {
								sentenceCorrectTerms++;
								if (most_common) {
									sentenceCorrectMostCommon++;
								}
							}
							if (centrality == 0.0) {
								sentenceZeroCentrality++;
								if (most_common) {
									zeroCentralityMostCommonTerms++;
								}
							}
							sentenceTerms++;							
						}
						goldMostCommonTerms += sentenceGoldMostCommon;
						evalTerms += sentenceTerms;
						goldTerms += sentenceTerms;
						evalMostCommonTerms += sentenceMostCommon;
						correctDisambiguations += sentenceCorrectTerms;
						correctEvalMostCommonTerms += sentenceCorrectMostCommon;
						zeroCentralityTerms += sentenceZeroCentrality;
						// Now evaluate precisions for sentence
						correctTermsPrecision.add((float)sentenceCorrectTerms/sentenceTerms);
						correctMostCommonPrecision.add((float)sentenceCorrectMostCommon/sentenceGoldMostCommon);
						zeroCentralityPrecision.add((float)sentenceZeroCentrality/sentenceTerms);
						mostCommons.add(sentenceMostCommon);
					} else if (line.contains("[TIME]")) {
						String time = line.split(" ")[1];
						Duration d = Duration.parse(time);
						if (line.contains("[TSP]")) {
							tspTimes.add(d);
						} else if (line.contains("[DFS]")) {
							dfsTimes.add(d);
						}
					} else if (line.contains("[FINISHED]")) {
						String time = line.split(" ")[1];
						totalTimes.add(Duration.parse(time));
					}
				}
				
				// Report results by sentence
				for (int i = 0; i < totalTimes.size(); i++) {
					
				}
				// TODO: Generate report in a reusable format
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		// Report results by dataset
		report += "-- report on dataset " + Globals.currentDataset + " --\n"
				+ "centrality => " + Globals.computeCentrality + "\n"
				+ "auxiliary node depth => " + Globals.nodesDepth + "\n"
				+ "run solver => " + Globals.runSolver + "\n"
				+ "\n";
		report += "-- statistics  by dataset --\n"
				+ "total terms disambiguated => " + evalTerms + "\n"
				+ "gold terms => " + goldTerms + "\n"
				+ "correct disambiguations => " + correctDisambiguations + "\n"
				+ "most common terms disambiguated => " + evalMostCommonTerms + "\n"
				+ "gold most common terms => " + goldMostCommonTerms + "\n"
				+ "correct most common disambiguations => " + correctEvalMostCommonTerms + "\n"
				+ "correct most common terms precision => " + (float) correctEvalMostCommonTerms / goldMostCommonTerms + "\n"
				+ "terms disambiguated with zero centrality => " + zeroCentralityTerms + "\n"
				+ "\n";
		System.out.println(report);
		try {
			FileWriter reportFile = new FileWriter(Globals.logsPath + "report.txt");
			reportFile.write(report);
			reportFile.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	/**
	 * Helper that calls readFileExtended method to incapsulate exception handling and keep upper method cleaner
	 * @param evaluation
	 * @return map of given key file
	 */
	private Map<String, Map<String, ArrayList<String>>> readFileToMap(File key) {
		Map<String, Map<String, ArrayList<String>>> map = new HashMap<String, Map<String, ArrayList<String>>>();
		try {
			readFileExtended(key, map);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return map;
	}

	public static void readFileExtended(File file, Map<String, Map<String, ArrayList<String>>> map) throws IOException {
		BufferedReader in = new BufferedReader(new FileReader(file));
		String l;
		int cnt=0;
		while ((l = in.readLine()) != null) 
		{
			cnt++;
			String[] ll = l.split(" ");
			if (ll.length<2){
			   System.out.println("line number "+cnt+" not complete: "+l);
			   continue;
			}	
			
			// map is <sentence_id, instance_map> used to iterate over sentences
			// instance_map is <instance_id, set<glosses>> used to iterate over glosses
			// Update the map with a new set of answers
			int last_dot = ll[0].lastIndexOf(".");
			String instance_id = ll[0].substring(last_dot+1);
			String sentence_id = ll[0].substring(0, last_dot);
			//System.out.println(sentence_id + "  " + instance_id);
			// Create maps and hashset if not existing
			if (!map.containsKey(sentence_id)) 
				map.put(sentence_id, new HashMap<String, ArrayList<String>>()); 
			if (!map.get(sentence_id).containsKey(instance_id)) {
				map.get(sentence_id).put(instance_id, new ArrayList<String>());
			}
			// Add values
			for (int i = 1; i < ll.length; i++) {
				map.get(sentence_id).get(instance_id).add(ll[i]);
			}
		}
		in.close();
	}
	
	/**
	 * Exits scorer and prints message to stderr
	 * @param message
	 */
	public void exit(String message) {
		System.err.println(message);
		System.exit(0);
	}
}
