package org.processmining.plugins.inductiveVisualMiner.animation;

import org.processmining.plugins.inductiveVisualMiner.visualisation.LocalDotEdge;
import org.processmining.plugins.inductiveVisualMiner.visualisation.LocalDotNode;

public class DotTokenStep {
	private Double arrivalTime; //arrival time at the end of this step, null if undefined
	private final double timeWeight; //in interpolation, this step should take this weight
	private final LocalDotEdge edge; //the edge this step passes over
	private final LocalDotNode node; //the node this step passes over

	private DotTokenStep(LocalDotEdge edge, LocalDotNode node, Double arrivalTime, double timeWeight) {
		this.edge = edge;
		this.node = node;
		this.arrivalTime = arrivalTime;
		this.timeWeight = timeWeight;
	}

	public static DotTokenStep edge(LocalDotEdge edge, Double arrivalTime) {
		return new DotTokenStep(edge, null, arrivalTime, 1);
	}

	public static DotTokenStep node(LocalDotNode node, Double arrivalTime) {
		return new DotTokenStep(null, node, arrivalTime, 0);
	}

	public LocalDotNode getDestinationNode() {
		if (node != null) {
			return node;
		}
		return edge.getTarget();
	}

	public boolean hasArrivalTime() {
		return arrivalTime != null;
	}

	public double getArrivalTime() {
		return arrivalTime;
	}

	public void setArrivalTime(double arrivalTime) {
		this.arrivalTime = arrivalTime;
	}

	public boolean isOverEdge() {
		return edge != null;
	}

	public LocalDotEdge getEdge() {
		return edge;
	}

	public LocalDotNode getNode() {
		return node;
	}

	@Override
	public String toString() {
		if (isOverEdge()) {
			return "step over edge from " + edge.getSource().getLabel().replace("\n", " ") + " ("
					+ edge.getSource().getType() + " " + edge.getSource().getId() + ") to "
					+ edge.getTarget().getLabel().replace("\n", " ") + " (" + edge.getTarget().getType() + " "
					+ edge.getTarget().getId() + "), arrive @" + String.format("%.6f", arrivalTime);
		} else {
			return "step over node " + node.getLabel().replace("\n", " ") + " (" + node.getType() + " " + node.getId()
					+ "), arrive @" + String.format("%.6f", arrivalTime);
		}
	}
}
