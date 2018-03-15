package evaluation;

/**
 * Class that represents a single instance of a word in datasets
 * @author demetrio
 */
public class InputInstance {
	/**
	 * Input example:
	 * <instance id="d000.s000.t001" lemma="say" pos="VERB">said</instance>
	 * or:
	 * <wf lemma="have" pos="VERB">had</wf>
	 */
	public String id;
	public String lemma;
	public int index; // useful to keep track of word index inside its sentence
	public String pos;
	public String term; // real term read from file
	
	public String toString() {
		return "ID: " + id + "\n"
				+ "LEMMA: " + lemma + "\n"
				+ "INDEX: " + index + "\n"
				+ "POS: " + pos + "\n"
				+ "TERM: " + term + "\n";
	}
};