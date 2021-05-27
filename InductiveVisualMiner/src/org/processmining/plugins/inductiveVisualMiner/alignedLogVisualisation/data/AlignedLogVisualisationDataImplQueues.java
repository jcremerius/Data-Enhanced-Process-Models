package org.processmining.plugins.inductiveVisualMiner.alignedLogVisualisation.data;

import org.deckfour.xes.classification.XEventClass;
import org.processmining.plugins.InductiveMiner.MultiSet;
import org.processmining.plugins.InductiveMiner.Pair;
import org.processmining.plugins.InductiveMiner.Triple;
import org.processmining.plugins.InductiveMiner.efficienttree.UnknownTreeNodeException;
import org.processmining.plugins.inductiveVisualMiner.alignment.LogMovePosition;
import org.processmining.plugins.inductiveVisualMiner.helperClasses.IvMModel;
import org.processmining.plugins.inductiveVisualMiner.ivmlog.IvMLogInfo;
import org.processmining.plugins.inductiveVisualMiner.performance.PerformanceWrapper;

import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;

public class AlignedLogVisualisationDataImplQueues implements AlignedLogVisualisationData {

	private long minQueueLength;
	private long maxQueueLength;
	private TIntDoubleMap computedLengths;

	private IvMModel model;
	private PerformanceWrapper queueLengths;

	private AlignedLogVisualisationData dataForEdges;

	public AlignedLogVisualisationDataImplQueues(IvMModel model, PerformanceWrapper queueLengths, IvMLogInfo logInfo) {
		this.model = model;
		this.queueLengths = queueLengths;
		dataForEdges = new AlignedLogVisualisationDataImplFrequencies(model, logInfo);
		setTime(0);
	}

	public void setTime(long time) {
		//compute queue lengths
		computedLengths = new TIntDoubleHashMap(10, 0.5f, -1, 0);
		minQueueLength = Long.MAX_VALUE;
		maxQueueLength = Long.MIN_VALUE;
		{
			for (int node : model.getAllNodes()) {
				if (model.isActivity(node)) {
					double l = queueLengths.getQueueLength(node, time);
					computedLengths.put(node, l);
					if (l > maxQueueLength) {
						maxQueueLength = (long) (l + 0.5);
					}
					if (l < minQueueLength && l > -0.1) {
						minQueueLength = (long) (l + 0.5);
					}
				}
			}
		}
	}

	public Pair<Long, Long> getExtremeCardinalities() {
		return Pair.of(minQueueLength, maxQueueLength);
	}

	public Triple<String, Long, Long> getNodeLabel(int unode, boolean includeModelMoves) {
		long length = Math.round(computedLengths.get(unode));
		if (length >= 0) {
			return Triple.of(String.valueOf(length), length, 0l);
		} else {
			return Triple.of("-", -1l, -1l);
		}
	}

	public Pair<String, Long> getEdgeLabel(int unode, boolean includeModelMoves) throws UnknownTreeNodeException {
		return dataForEdges.getEdgeLabel(unode, includeModelMoves);
	}

	public Pair<String, Long> getEdgeLabel(int from, int to, boolean includeModelMoves)
			throws UnknownTreeNodeException {
		return dataForEdges.getEdgeLabel(from, to, includeModelMoves);
	}

	public Pair<String, Long> getModelMoveEdgeLabel(int unode) {
		return dataForEdges.getModelMoveEdgeLabel(unode);
	}

	public Pair<String, MultiSet<XEventClass>> getLogMoveEdgeLabel(LogMovePosition logMovePosition) {
		return dataForEdges.getLogMoveEdgeLabel(logMovePosition);
	}

	public AlignedLogVisualisationDataImplQueues clone() throws CloneNotSupportedException {
		AlignedLogVisualisationDataImplQueues c = (AlignedLogVisualisationDataImplQueues) super.clone();

		c.computedLengths = this.computedLengths;
		c.dataForEdges = this.dataForEdges;
		c.maxQueueLength = this.maxQueueLength;
		c.minQueueLength = this.minQueueLength;
		c.model = this.model;
		c.queueLengths = this.queueLengths;

		return c;
	}

}
