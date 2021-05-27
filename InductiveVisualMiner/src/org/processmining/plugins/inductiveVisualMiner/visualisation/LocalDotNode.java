package org.processmining.plugins.inductiveVisualMiner.visualisation;

import org.processmining.plugins.graphviz.dot.Dot;
import org.processmining.plugins.graphviz.dot.DotNode;

import gnu.trove.map.hash.THashMap;

public class LocalDotNode extends DotNode {

	public class Appearance {
		public String stroke;
		public String strokeWidth;
		public String strokeDashArray;
	}

	public enum NodeType {
		source, sink, activity, xor, concurrentSplit, concurrentJoin, interleavedSplit, interleavedJoin, orSplit, orJoin, logMoveActivity
	}

	private NodeType type;
	private final int node;
	public final Appearance unselectedAppearance = new Appearance();

	public LocalDotNode(Dot dot, ProcessTreeVisualisationInfo info, NodeType type, String label, final int unode,
			final LocalDotNode correspondingSplit) {
		super(label, new THashMap<String, String>());

		this.node = unode;
		this.setType(type);

		switch (type) {
			case activity :
				setOption("shape", "box");
				setOption("style", "rounded,filled");
				setOption("fontsize", "30");
				break;
			case logMoveActivity :
				setOption("shape", "box");
				setOption("style", "rounded,filled");
				setOption("fontsize", "9");
				setOption("fillcolor", "red");
				break;
			case concurrentSplit :
				setOption("height", "0.3");
				setOption("width", "0.3");
				setOption("fontsize", "30");
				break;
			case concurrentJoin :
				setOption("height", "0.3");
				setOption("width", "0.3");
				setOption("fontsize", "30");
				break;
			case orSplit :
			case orJoin :
			case interleavedSplit :
			case interleavedJoin :
				setOption("shape", "diamond");
				setOption("fixedsize", "true");
				setOption("height", "0.25");
				setOption("width", "0.27");
				break;
			case sink :
				setOption("width", "0.2");
				setOption("shape", "circle");
				setOption("style", "filled");
				setOption("fillcolor", "#E40000");
				break;
			case source :
				setOption("width", "0.2");
				setOption("shape", "circle");
				setOption("style", "filled");
				setOption("fillcolor", "#80ff00");
				break;
			case xor :
				setOption("width", "0.05");
				setOption("shape", "circle");
				break;
		}

		dot.addNode(this);
		if (info != null) {
			info.addNode(unode, this, correspondingSplit);
		}
	}

	public int getUnode() {
		return node;
	}

	public NodeType getType() {
		return type;
	}

	public void setType(NodeType type) {
		this.type = type;
	}
}