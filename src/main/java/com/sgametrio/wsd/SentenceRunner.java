package com.sgametrio.wsd;

import evaluation.InputSentence;

public class SentenceRunner implements Runnable {
	private MyExecutor ex;
	private InputSentence input;
	private String id;
	
	public SentenceRunner (MyExecutor ex, InputSentence instance) {
		this.ex = ex;
		this.input = instance;
		this.id = instance.sentenceId;
	}

	@Override
	public void run() {
		ex.performDisambiguation(input);
	}

}
