package com.sgametrio.wsd;

import java.util.concurrent.CountDownLatch;

import evaluation.InputSentence;

public class SentenceRunner implements Runnable {
	private JExecutor jex;
	private InputSentence input;
	private CountDownLatch cdl;

	public SentenceRunner(JExecutor ex, InputSentence sentence, CountDownLatch cdl) {
		this.jex = ex;
		this.input = sentence;
		this.cdl = cdl;
	}

	@Override
	public void run() {
		jex.performDisambiguation(input);
		cdl.countDown();
	}

}
