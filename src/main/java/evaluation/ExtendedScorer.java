package evaluation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.sgametrio.wsd.Globals;
import com.sgametrio.wsd.WordnetAdapter;

import edu.mit.jwi.item.IWord;
import edu.mit.jwi.item.POS;
import edu.mit.jwi.item.SenseKey;

public class ExtendedScorer extends Scorer {
	WordnetAdapter wordnet = null;
	
	public void doEvaluation(String goldFile, String evaluationFile) {
		// Retrieve info by file name
		String run [] = evaluationFile.split("_");
		String dataset = run[0];
		String centralityMeasure = run[1];
		String maxDepth = run[2];
		String disambiguation = run[3];
		// Read files and generate something cool
		File gold = new File(goldFile);
		if (!gold.exists()) 
			exit("Gold file not found at " + goldFile);
		String correctEvaluationPath = Globals.resultsPath + evaluationFile + Globals.resultsExt;
		File evaluation = new File(correctEvaluationPath);
		if (!evaluation.exists()) 
			exit("Evaluation file not found at " + correctEvaluationPath);
		String report = "";
		Map<String, Map<String, ArrayList<String>>> goldMap = null;
		Map<String, Map<String, ArrayList<String>>> evalMap = null;
		goldMap = readFileToMap(gold);
		evalMap = readFileToMap(evaluation);
		this.wordnet = new WordnetAdapter();
		// Global dataset statistics
		int goldTerms = goldMap.size();
		int goldMostCommonTerms = 0;
		int evalTerms = 0;
		int evalMostCommonTerms = 0;
		int correctEvalMostCommonTerms = 0;
		int correctDisambiguations = 0;
		int zeroCentralityTerms = 0;
		int zeroCentralityMostCommonTerms = 0;
		int zeroCentralityMostCommonCorrect = 0;
		int zeroCentralityCorrect = 0;
		int sameCentralityMostCommon = 0;
		int sameCentralityDisambiguation = 0;
		int nouns = 0;
		int verbs = 0;
		int adj = 0;
		int adv = 0;
		int correctNouns = 0;
		int correctVerbs = 0;
		int correctAdj = 0;
		int correctAdv = 0;
		Map<POS, Integer> correctPOS = new HashMap<POS, Integer>();
		Map<POS, Integer> totalPOS = new HashMap<POS, Integer>();

		for (POS pos : POS.values()) {
			correctPOS.put(pos, 0);
			totalPOS.put(pos, 0);
		}
		// 
		ArrayList<Duration> totalTimes = new ArrayList<Duration>();
		ArrayList<Duration> tspTimes = new ArrayList<Duration>();
		ArrayList<Duration> dfsTimes = new ArrayList<Duration>();
		ArrayList<Float> correctTermsPrecision = new ArrayList<Float>();
		ArrayList<Float> correctMostCommonPrecision = new ArrayList<Float>();
		ArrayList<Float> zeroCentralityPrecision = new ArrayList<Float>();
		ArrayList<Integer> mostCommons = new ArrayList<Integer>();
		ArrayList<String> sameCentralityIds = new ArrayList<String>();
		// Read log file and extract statistics
		// Now sentence by sentence, evaluate 
		for (String sentence_id : evalMap.keySet()) {
			// If the fragment of text annotated by the system is not contained in the gold
			// standard then skip it.
			if (!goldMap.containsKey(sentence_id)) 
				continue;
			BufferedReader log;
			try {
				log = new BufferedReader(new FileReader(Globals.logsPath + evaluationFile + "_" + sentence_id + ".log"));
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
							float centrality = Float.parseFloat(info[info.length-1]);
							// for every sense_key find most common sense (the first retrieved by wordnet
							IWord mostCommon = this.wordnet.getMostCommonWord(eval_sense_key);
							String senseKeyMostCommon = SenseKey.toString(mostCommon.getSenseKey());
							
							String gold_sense_key = goldMap.get(sentence_id).get(instance_id).get(0);
							boolean most_common = false;
							boolean correct = false;
							if (eval_sense_key.equals(senseKeyMostCommon)) {
								most_common = true;
								sentenceMostCommon++;
								if (sameCentralityIds.contains(term_id)) {
									sameCentralityMostCommon++;
								}
							}
							if (gold_sense_key.equals(senseKeyMostCommon)) {
								sentenceGoldMostCommon++;
							}
							if (eval_sense_key.equals(gold_sense_key)) {
								correct = true;
								sentenceCorrectTerms++;
								if (most_common) {
									sentenceCorrectMostCommon++;
								}
							}
							if (centrality == 0.0) {
								sentenceZeroCentrality++;
								if (most_common) {
									zeroCentralityMostCommonTerms++;
									if (correct) {
										zeroCentralityMostCommonCorrect++;
									}
								}
								if (correct) {
									zeroCentralityCorrect++;
								}
							}
							// POS type
							POS pos = mostCommon.getPOS();
							totalPOS.replace(pos, totalPOS.get(pos) + 1);
							if (correct) {
								correctPOS.replace(pos, correctPOS.get(pos) + 1);
							}
							sentenceTerms++;							
						}
						goldMostCommonTerms += sentenceGoldMostCommon;
						evalTerms += sentenceTerms;
						evalMostCommonTerms += sentenceMostCommon;
						correctDisambiguations += sentenceCorrectTerms;
						correctEvalMostCommonTerms += sentenceCorrectMostCommon;
						zeroCentralityTerms += sentenceZeroCentrality;
						// Now evaluate precisions for sentence
						correctTermsPrecision.add((float)sentenceCorrectTerms/sentenceTerms);
						correctMostCommonPrecision.add((float)sentenceCorrectMostCommon/sentenceMostCommon);
						zeroCentralityPrecision.add((float)sentenceZeroCentrality/sentenceTerms);
						mostCommons.add(sentenceMostCommon);
					} else if (line.contains("[TIME]")) {
						String time = line.split(" ")[1];
						Duration d = Duration.parse(time);
						if (line.contains("[TSP]")) {
							tspTimes.add(d);
						} else if (line.contains("[DFS]")) {
							dfsTimes.add(d);
						} else if (line.contains("[TOTAL]")) {
							totalTimes.add(d);
						} 
					} else if (line.contains("[SAME CENTRALITIES]")) {
						String id = line.split(" ")[2];
						sameCentralityIds.add(id);
						sameCentralityDisambiguation++;
					}
				}				
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		// Report results by dataset
		report += evaluationFile + "";
		report += "-- statistics  by dataset --\n"
				+ "total terms disambiguated => " + evalTerms + "\n"
				+ "gold terms => " + goldTerms + "\n"
				+ "correct disambiguations => " + correctDisambiguations + "\n"
				+ "most common terms disambiguated => " + evalMostCommonTerms + "\n"
				+ "gold most common terms => " + goldMostCommonTerms + "\n"
				+ "correct most common disambiguations => " + correctEvalMostCommonTerms + "\n"
				+ "correct most common terms precision => " + (float) correctEvalMostCommonTerms / goldMostCommonTerms + "\n"
				+ "terms disambiguated with zero centrality => " + zeroCentralityTerms + "\n"
				+ "correct most common terms zero centrality => " + zeroCentralityMostCommonCorrect + "\n"
				+ "correct terms zero centrality => " + zeroCentralityCorrect + "\n"
				+ "zero centrality most common bias => " + (float) zeroCentralityMostCommonTerms / zeroCentralityTerms + "\n"
				+ "most common bias => " + (float) evalMostCommonTerms / evalTerms +  "\n"
				+ "\n";
		for (POS pos : POS.values()) {
			String log = pos + ": total => " + totalPOS.get(pos) + " correct => " + correctPOS.get(pos) + "\n";
			report += log;
		}
		String precision = "";
		for (POS pos : POS.values()) {
			double prec = (double) correctPOS.get(pos) / totalPOS.get(pos);
			precision += ";" + String.format("%.2f", prec*100);
		}
		// Computing time for knowing bottlenecks
		Instant now = Instant.now();
		Instant now2 = Instant.now();
		Duration zero = Duration.between(now, now2);
		Duration total = Duration.between(now, now2);
		Duration maxTSP = Duration.between(now, now2);
		Duration maxDFS = Duration.between(now, now2);

		System.out.println("---- Total ----");

		for (Duration d : totalTimes) {
			System.out.println(d);
			total = total.plus(d);
		}

		System.out.println("---- TSP ----");
		for (Duration d : tspTimes) {
			System.out.println(d);
			if (d.compareTo(maxTSP) > 0) {
				maxTSP = d;
			}
		}
		System.out.println("---- DFS ----");

		for (Duration d : dfsTimes) {
			System.out.println(d);
			if (d.compareTo(maxDFS) > 0) {
				maxDFS = d;
			}
		}

		this.createCsvReportFile();
		String content = "";
		
		try {
			Double[] score = Scorer.score(gold, evaluation);
			content += dataset + ";" + maxDepth + ";" + centralityMeasure + ";" + disambiguation + ";" + String.format("%.2f", score[2]*100) + maxDFS + ";" + maxTSP + ";" + total + precision + "\n";
			
			FileWriter fileW = new FileWriter(Globals.csvReportFile, true);
			fileW.write(content);
			fileW.close();
			FileWriter reportFile = new FileWriter(Globals.logsPath + "report.txt", true);
			reportFile.write(report);
			reportFile.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	private void createCsvReportFile() {
		try {
			File file = new File(Globals.csvReportFile);
			if (!file.exists()) {
				file.createNewFile();
				FileWriter fileW = new FileWriter(Globals.csvReportFile, true);
				String headers = "Dataset;Max path length;Centrality;Disambiguation;F-Measure;max-dfs;max-tsp;total";
				String posHeaders = "";
				for (POS pos : POS.values()) {
					posHeaders +=  ";" + pos;
				}
				headers += posHeaders + "\n";
				fileW.write(headers);
				fileW.close();
			}
		} catch (IOException e) {
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
