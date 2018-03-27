package com.sgametrio.wsd;

import java.util.ArrayList;

import evaluation.InputInstance;

public class SentenceRunner implements Runnable {
	private MyExecutor ex;
	private ArrayList<InputInstance> input;
	
	public SentenceRunner (MyExecutor ex, ArrayList<InputInstance> instance) {
		this.ex = ex;
		this.input = instance;
	}

	@Override
	public void run() {
		ex.performDisambiguation(input);
	}

}
