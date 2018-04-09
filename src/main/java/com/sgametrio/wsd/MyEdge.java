package com.sgametrio.wsd;

public class MyEdge {
	private static int progressiveId = 0;
	private int id;
	private double weight;
	private MyVertex target;
	
	public MyEdge(MyVertex target, double weight) {
		this.id = progressiveId++;
		this.target = target;
		this.weight = weight;
	}
	
	public double getWeight() {
		return this.weight;
	}
	
	public MyVertex getDest() {
		return this.target;
	}
	
	public void setWeight(double weight) {
		this.weight = weight;
	}
	
	public int getId() {
		return this.id;
	}
}
