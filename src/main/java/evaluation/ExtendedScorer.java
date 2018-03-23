package evaluation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.sgametrio.wsd.Globals;

public class ExtendedScorer extends Scorer {
	
	public void doEvaluation(String goldFile, String evaluationFile) {
		// Read files and generate something cool
		File gold = new File(goldFile);
		if (!gold.exists()) 
			exit("Gold file not found at " + goldFile);
		File evaluation = new File(evaluationFile);
		if (!evaluation.exists()) 
			exit("Evaluation file not found at " + evaluationFile);
		File statistics = new File(Globals.resultsPath + "statistics.json");
		
		Map<String, Map<String, Set<String>>> goldMap = readFileToMap(gold);
		Map<String, Map<String, Set<String>>> evalMap = readFileToMap(evaluation);
		// Now sentence by sentence, evaluate 
		for (String sentence_id : evalMap.keySet()) {
			// If the fragment of text annotated by the system is not contained in the gold
			// standard then skip it.
			if (!goldMap.containsKey(sentence_id)) 
				continue;
						
		}
	}
	
	/**
	 * Helper that calls readFileExtended method to incapsulate exception handling and keep upper method cleaner
	 * @param evaluation
	 * @return map of given key file
	 */
	private Map<String, Map<String, Set<String>>> readFileToMap(File key) {
		Map<String, Map<String, Set<String>>> map = new HashMap<String, Map<String, Set<String>>>();
		try {
			readFileExtended(key, map);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return map;
	}

	public static void readFileExtended(File file, Map<String, Map<String, Set<String>>> map) throws IOException {
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
			String instance_id = ll[0].substring(last_dot);
			String sentence_id = ll[0].substring(0, last_dot - 1);
			System.out.println(sentence_id + "  " + instance_id);
			// Create maps and hashset if not existing
			if (!map.containsKey(sentence_id)) 
				map.put(sentence_id, new HashMap<String, Set<String>>()); 
			if (!map.get(sentence_id).containsKey(instance_id)) {
				map.get(sentence_id).put(instance_id, new HashSet<String>());
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
