package com.sgametrio.wsd;

import java.util.ArrayList;

import evaluation.InputInstance;

public class SentenceRunner implements Runnable {
	private MyExecutor ex;
	private ArrayList<InputInstance> input;
	private boolean centrality;
	
	public SentenceRunner (MyExecutor ex, ArrayList<InputInstance> instance, boolean centrality) {
		this.ex = ex;
		this.input = instance;
		this.centrality = centrality;
	}

	@Override
	public void run() {
		ex.performDisambiguation(input, centrality);
	}

}
