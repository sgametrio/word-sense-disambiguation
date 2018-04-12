package com.sgametrio.wsd;

import evaluation.InputSentence;

public class SentenceRunner implements Runnable {
	private JExecutor jex;
	private InputSentence input;

	public SentenceRunner(JExecutor ex, InputSentence sentence) {
		this.jex = ex;
		this.input = sentence;
	}

	@Override
	public void run() {
		jex.performDisambiguation(input);
	}

}
