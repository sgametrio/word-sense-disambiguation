package com.sgametrio.wsd;

public class MyEdge {
	private static int progressiveId = 0;
	private int id;
	private float weight;
	private MyVertex target;
	
	public MyEdge(MyVertex target, float weight) {
		this.id = progressiveId++;
		this.target = target;
		this.weight = weight;
	}
	
	public float getWeight() {
		return this.weight;
	}
	
	public MyVertex getDest() {
		return this.target;
	}
	
	public void setWeight(float weight) {
		this.weight = weight;
	}
	
	public int getId() {
		return this.id;
	}
}
