package org.processmining.plugins.inductiveVisualMiner.visualisation;

import java.awt.Color;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.OptionalDouble;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.model.XEvent;
import org.processmining.plugins.InductiveMiner.MultiSet;
import org.processmining.plugins.InductiveMiner.Pair;
import org.processmining.plugins.InductiveMiner.Triple;
import org.processmining.plugins.InductiveMiner.efficienttree.EfficientTree;
import org.processmining.plugins.InductiveMiner.efficienttree.UnknownTreeNodeException;
import org.processmining.plugins.InductiveMiner.mining.logs.IMLog;
import org.processmining.plugins.InductiveMiner.mining.logs.IMTrace;
import org.processmining.plugins.graphviz.colourMaps.ColourMap;
import org.processmining.plugins.graphviz.colourMaps.ColourMaps;
import org.processmining.plugins.graphviz.dot.Dot;
import org.processmining.plugins.graphviz.dot.Dot.GraphDirection;
import org.processmining.plugins.inductiveVisualMiner.alignedLogVisualisation.data.AlignedLogVisualisationData;
import org.processmining.plugins.inductiveVisualMiner.alignment.LogMovePosition;
import org.processmining.plugins.inductiveVisualMiner.helperClasses.IvMEfficientTree;
import org.processmining.plugins.inductiveVisualMiner.helperClasses.IvMModel;
import org.processmining.plugins.inductiveVisualMiner.traceview.TraceViewEventColourMap;
import org.processmining.plugins.inductiveVisualMiner.visualisation.LocalDotEdge.EdgeType;
import org.processmining.plugins.inductiveVisualMiner.visualisation.LocalDotNode.NodeType;
import org.processmining.plugins.inductiveminer2.attributes.Attribute;
import org.processmining.plugins.inductiveminer2.attributes.AttributesInfo;


public class ProcessTreeVisualisation {

	private long maxCardinality;
	private long minCardinality;
	ProcessTreeVisualisationParameters parameters;

	private AlignedLogVisualisationData data;
	private AttributesInfo attributesInfo;
	private Dot dot;
	private ProcessTreeVisualisationInfo info;
	private TraceViewEventColourMap traceViewColourMap;
	private Boolean includeAttributes;
	private IMLog log;

	public Triple<Dot, ProcessTreeVisualisationInfo, TraceViewEventColourMap> fancy(IvMEfficientTree tree,
			AlignedLogVisualisationData data, ProcessTreeVisualisationParameters parameters, AttributesInfo attributesInfo, Boolean includeAttributes, IMLog log)
			throws UnknownTreeNodeException {
		return fancy(new IvMModel(tree), data, parameters, attributesInfo, includeAttributes, log);
	}

	public Triple<Dot, ProcessTreeVisualisationInfo, TraceViewEventColourMap> fancy(IvMModel model,
			AlignedLogVisualisationData data, ProcessTreeVisualisationParameters parameters, AttributesInfo attributesInfo, Boolean includeAttributes, IMLog log)
			throws UnknownTreeNodeException {
		this.parameters = parameters;
		this.data = data;
		this.attributesInfo = attributesInfo;
		this.includeAttributes = includeAttributes;
		this.log = log;

		IvMEfficientTree tree = model.getTree();

		//find maximum and minimum occurrences
		Pair<Long, Long> p = data.getExtremeCardinalities();
		minCardinality = p.getLeft();
		maxCardinality = p.getRight();

		dot = new Dot();
		dot.setDirection(GraphDirection.leftRight);
		int root = tree.getRoot();

		traceViewColourMap = new TraceViewEventColourMap(model);

		//source & sink
		info = new ProcessTreeVisualisationInfo();
		LocalDotNode source = new LocalDotNode(dot, info, NodeType.source, "", 0, null);
		LocalDotNode sink = new LocalDotNode(dot, info, NodeType.sink, "", 0, source);
		info.setRoot(source, sink);
		//convert root node
		convertNode(tree, root, source, sink, true);

		//add log-move-arcs to source and sink
		//a parallel root will project its own log moves 
		if (parameters.isShowLogMoves() && !(tree.isConcurrent(root) || tree.isOr(root) || tree.isInterleaved(root))) {
			visualiseLogMove(tree, source, source, root, LogMovePosition.atSource(root), true);
			visualiseLogMove(tree, sink, sink, root, LogMovePosition.atSink(root), false);
		}

		return Triple.of(dot, info, traceViewColourMap);
	}

	private void convertNode(EfficientTree tree, int node, LocalDotNode source, LocalDotNode sink,
			boolean directionForward) throws UnknownTreeNodeException {
		if (tree.isSequence(node)) {
			convertSequence(tree, node, source, sink, directionForward);
		} else if (tree.isLoop(node)) {
			convertLoop(tree, node, source, sink, directionForward);
		} else if (tree.isInterleaved(node)) {
			convertInterleaved(tree, node, source, sink, directionForward);
		} else if (tree.isConcurrent(node)) {
			convertConcurrent(tree, node, source, sink, directionForward);
		} else if (tree.isOr(node)) {
			convertOr(tree, node, source, sink, directionForward);
		} else if (tree.isXor(node)) {
			convertXor(tree, node, source, sink, directionForward);
		} else if (tree.isActivity(node)) {
			convertActivity(tree, node, source, sink, directionForward);
		} else if (tree.isTau(node)) {
			convertTau(tree, node, source, sink, directionForward);
		} else {
			throw new UnknownTreeNodeException();
		}
	}

	private void convertActivity(EfficientTree tree, int unode, LocalDotNode source, LocalDotNode sink,
			boolean directionForward) throws UnknownTreeNodeException {
		Triple<String, Long, Long> cardinality = data.getNodeLabel(unode, false);
		LocalDotNode dotNode = convertActivity(tree, unode, cardinality);

		addArc(tree, source, dotNode, unode, directionForward, false);
		addArc(tree, dotNode, sink, unode, directionForward, false);

		//draw model moves
		if (parameters.isShowModelMoves()) {
			Pair<String, Long> modelMoves = data.getModelMoveEdgeLabel(unode);
			if (modelMoves.getB() != 0) {
				addMoveArc(tree, source, sink, unode, EdgeType.modelMove, -1, -1, modelMoves, directionForward);
			}
		}

		//draw log moves
		if (parameters.isShowLogMoves()) {
			visualiseLogMove(tree, dotNode, dotNode, unode, LogMovePosition.onLeaf(unode), directionForward);
		}
	}

	private LocalDotNode convertActivity(EfficientTree tree, int unode, Triple<String, Long, Long> cardinality) {
		//style the activity by the occurrences of it
		Color fillColour = Color.white;
		Color gradientColour = null;
		//deactivate node colouring
		if (cardinality.getB() != 0 && parameters.getColourNodes() != null & false) {
			fillColour = parameters.getColourNodes().colour((long) (getOccurrenceFactor(cardinality.getB()) * 100), 0,
					100);

			if (cardinality.getC() != 0 && parameters.getColourNodesGradient() != null) {
				gradientColour = parameters.getColourNodesGradient()
						.colour((long) (getOccurrenceFactor(cardinality.getC()) * 100), 0, 100);
			}
		}


		//determine label colour
		Color fontColour = Color.black;
		if (ColourMaps.getLuma(fillColour) < 128) {
			fontColour = Color.black;
		}
		traceViewColourMap.set(unode, fillColour, fontColour);
		//Event Attribute Aggregation per Node
		
		Attribute event_activities = attributesInfo.getEventAttributeValues("concept:name");
		// returns attribute object containing values	
		// Hashmap (Dictionary) linking activity to attributes 

		HashMap <String, Object[][]> attributesToDisplay = new HashMap<String, Object[][]>();
		//HashMap <String, >         Glucose --> [valuenom [mean, max, min], abnormal [category (1)]]
		// The HashMap and the arrays are supposed to be set by GUI! That is not yet implemented
		
		
		Object[][] lab_config = new Object[3][4];
		lab_config[0][0] = "valuenum";
		lab_config[0][1] = "max";
		lab_config[1][0] = "abnormal_low";
		lab_config[1][1] = "abnormal"; // say which category
		lab_config[2][0] = "abnormal_high";
		lab_config[2][1] = "abnormal"; // say which category
		Object[][] lab_config_2 = new Object[3][4];
		lab_config_2[0][0] = "valuenum";
		lab_config_2[0][1] = "min";
		lab_config_2[1][0] = "abnormal_low";
		lab_config_2[1][1] = "abnormal"; // say which category
		lab_config_2[2][0] = "abnormal_high";
		lab_config_2[2][1] = "abnormal"; // say which category

		Object[][] xray_config = new Object[3][2];
		xray_config[0][0] = "Pleural Effusion";
		xray_config[0][1] = 1.0;
		xray_config[1][0] = "Cardiomegaly";
		xray_config[1][1] = 1.0;
		xray_config[2][0] = "Atelectasis";
		xray_config[2][1] = 1.0;

		
		attributesToDisplay.put("Analyse TSH Value", lab_config);
		attributesToDisplay.put("Analyse Troponin T Value", lab_config);	
		attributesToDisplay.put("Perform General X-Ray", xray_config);

		
		Object[] activitiesToShowAttributes = attributesToDisplay.keySet().toArray();

		
		String label_content = "";
		String label_start = "<<table border=\"0\" cellborder=\"1\" cellspacing=\"0\">";
		String activity_label = "<tr><td>" + tree.getActivityName(unode) + "</td></tr>";
		String label_end = "</table>>";
		String label = "";
		

		if (!cardinality.getA().isEmpty()) {
			activity_label = "<tr><td><b>" + tree.getActivityName(unode) + ": " + cardinality.getA() + "</b></td></tr>";
		}
		label_content += activity_label;
		

		if(includeAttributes == true && Arrays.asList(activitiesToShowAttributes).contains(tree.getActivityName(unode))) {
			DecimalFormat df = new DecimalFormat("#.##");
			// Attribute Glucose --> List of all attribute values (no computation yet(mean, max, min etc))!
			HashMap <Object, List> valuesForAttributes = new HashMap<Object, List>();
			Object[][] attributesOfActivity = attributesToDisplay.get(tree.getActivityName(unode));
			Object[] attributeLabelsOfActivity = new Object[attributesOfActivity.length];
			//Getlabels of attributes
			for(int i = 0; i < attributesOfActivity.length;i++) {
				attributeLabelsOfActivity[i] = attributesOfActivity[i][0];
				valuesForAttributes.put(attributeLabelsOfActivity[i], new ArrayList());
			}

			//Retrieve attribute values for each activity
			for (Iterator<IMTrace> it = log.iterator(); it.hasNext();) {
				IMTrace trace = it.next();
					for (Iterator<XEvent> it1 = trace.iterator(); it1.hasNext();) {
						XEvent event = it1.next();
						// check if event is related to activity
						String event_activity = event_activities.getLiteral(event);
						if(event_activity.equals(tree.getActivityName(unode))) {
							// retrieve correct attribute
							for(int i = 0; i < attributeLabelsOfActivity.length;i++) {
								Attribute currAttribute = attributesInfo.getEventAttributeValues((String)attributeLabelsOfActivity[i]);
								//check for type!! currently only numeric and literal
								if(currAttribute.isNumeric()) {
								double attributeValue = currAttribute.getNumeric(event);
								//double event_value = selectedAttribute.getNumeric(event);					
								if(attributeValue != Double.NaN && attributeValue < 10000 && attributeValue > -10000) {
									//System.out.println("Numeric");
									//System.out.println(attributeValue);
									valuesForAttributes.get(attributeLabelsOfActivity[i]).add(attributeValue);
							}
								}
								else if(currAttribute.isLiteral()) {
									//System.out.println("Literal");
									String attributeValue = currAttribute.getLiteral(event);
									//System.out.println(attributeValue);
									valuesForAttributes.get(attributeLabelsOfActivity[i]).add(attributeValue);
								}

							}
						}
						
					}
				}
			//iterate through each attribute and calculate respective statistic
			for(int i = 0; i < attributesOfActivity.length;i++) {

					String currAttribute = (String)attributesOfActivity[i][0];

					for(int j = 1; j < attributesOfActivity[i].length; j++) {
						
						String unit = "";
						
						if(tree.getActivityName(unode).contains("Potassium")) {
							unit = "mEq/L";
						}
						else if(tree.getActivityName(unode).contains("Sodium")) {
							unit = "mEq/L";
						}
						else if(tree.getActivityName(unode).contains("Creatinine")) {
							unit = "mg/dl";
						}
						else if(tree.getActivityName(unode).contains("Glucose")) {
							unit = "mg/dl";					
						}
						else if(tree.getActivityName(unode).contains("Platelet")) {
							unit = "K/uL";
						}
						else if(tree.getActivityName(unode).contains("Urea")) {
							unit = "mg/dL";
						}
						else if(tree.getActivityName(unode).contains("Albumin")) {
							unit = "g/dL";
						}
						else if(tree.getActivityName(unode).contains("Troponin")) {
							unit = "ng/mL";
						}
						else if(tree.getActivityName(unode).contains("TSH")) {
							unit = "uIu/ML";
						}
									
						
						if(attributesOfActivity[i][j] != null) {
							List<?> currAttributeValues = valuesForAttributes.get(currAttribute);
							if(attributesOfActivity[i][j].equals("mean")) {
								double mean = ((List<Double>) currAttributeValues).stream().mapToDouble(Double::doubleValue).sum() / currAttributeValues.size();
								label_content +=  "<tr><td>" + "Mean " + currAttribute + ": " +  df.format(mean) + " " + unit + "</td></tr>";
							}
							else if(attributesOfActivity[i][j].equals("min")) {
								OptionalDouble min = ((List<Double>) currAttributeValues).stream().mapToDouble(Double::doubleValue).min();
								label_content += "<tr><td>" + "Min " + currAttribute + ": " + df.format(min.getAsDouble()) + " " + unit + "</td></tr>";
						 	}
							else if (attributesOfActivity[i][j].equals("max")) {
								OptionalDouble max = ((List<Double>) currAttributeValues).stream().mapToDouble(Double::doubleValue).max();
								label_content += "<tr><td>" + "Max " + currAttribute + ": " + df.format(max.getAsDouble()) + " " + unit + "</td></tr>";
							}
							else {
								// cover categorical values --> string contains which class is of interest to show: 1 --> all values which are 1
								double countCategory = 0;
								for (Object e : currAttributeValues) {
									if(e != null) {
										if (e.equals(attributesOfActivity[i][j])) {
											countCategory++;
										}
									}
								}
								//System.out.println(countCategory);
									double ratioCategory = (countCategory / currAttributeValues.size()) * 100;
									label_content +=  "<tr><td>" +  "Category " + currAttribute + " " + ": "  + df.format(ratioCategory) + "%" + "</td></tr>";
								

								
								
							}
							
					}
					
				}
		
			}

		}
		label = label_start + label_content + label_end;

		final LocalDotNode dotNode = new LocalDotNode(dot, info, NodeType.activity, label, unode, null);
		if (gradientColour == null) {
			dotNode.setOption("fillcolor", ColourMap.toHexString(fillColour));
		} else {
			dotNode.setOption("fillcolor",
					ColourMap.toHexString(fillColour) + ":" + ColourMap.toHexString(gradientColour));
		}
		dotNode.setOption("fontcolor", ColourMap.toHexString(fontColour));

		info.addNode(unode, dotNode, null);
		return dotNode;
	}

	private void convertTau(EfficientTree tree, int unode, LocalDotNode source, LocalDotNode sink,
			boolean directionForward) throws UnknownTreeNodeException {
		addArc(tree, source, sink, unode, directionForward, false);
	}

	private void convertSequence(EfficientTree tree, int node, LocalDotNode source, LocalDotNode sink,
			boolean directionForward) throws UnknownTreeNodeException {
		LocalDotNode split;
		LocalDotNode join = source;

		Iterator<Integer> it = tree.getChildren(node).iterator();
		while (it.hasNext()) {
			int child = it.next();

			split = join;
			if (it.hasNext()) {
				join = new LocalDotNode(dot, info, NodeType.xor, "", node, null);
			} else {
				join = sink;
			}

			convertNode(tree, child, split, join, directionForward);

			//draw log-move-arc if necessary
			if (parameters.isShowLogMoves()) {
				visualiseLogMove(tree, split, split, node, LogMovePosition.beforeChild(node, child), directionForward);
			}
		}
	}

	private void convertLoop(EfficientTree tree, int node, LocalDotNode source, LocalDotNode sink,
			boolean directionForward) throws UnknownTreeNodeException {

		//operator split
		LocalDotNode split = new LocalDotNode(dot, info, NodeType.xor, "", node, null);
		addArc(tree, source, split, node, directionForward, true);

		//operator join
		LocalDotNode join = new LocalDotNode(dot, info, NodeType.xor, "", node, null);

		int bodyChild = tree.getChild(node, 0);
		convertNode(tree, bodyChild, split, join, directionForward);

		int redoChild = tree.getChild(node, 1);
		convertNode(tree, redoChild, join, split, !directionForward);

		int exitChild = tree.getChild(node, 2);
		convertNode(tree, exitChild, join, sink, directionForward);

		//put log-moves on children
		if (parameters.isShowLogMoves()) {
			visualiseLogMove(tree, split, split, node, LogMovePosition.beforeChild(node, bodyChild), directionForward);
			visualiseLogMove(tree, join, join, node, LogMovePosition.beforeChild(node, redoChild), directionForward);
			/*
			 * In principle, there can be log moves before the exit node.
			 * However, we assume them to be mapped before the redo child, as
			 * that is the same position in the model. It's up to the log move
			 * mapping to assure this.
			 */
			assert (data.getLogMoveEdgeLabel(LogMovePosition.beforeChild(node, exitChild)).getB().size() == 0);

		}
	}

	private void convertConcurrent(EfficientTree tree, int node, LocalDotNode source, LocalDotNode sink,
			boolean directionForward) throws UnknownTreeNodeException {

		//operator split
		LocalDotNode split = new LocalDotNode(dot, info, NodeType.concurrentSplit, "+", node, null);
		addArc(tree, source, split, node, directionForward, true);

		//operator join
		LocalDotNode join = new LocalDotNode(dot, info, NodeType.concurrentJoin, "+", node, split);
		addArc(tree, join, sink, node, directionForward, true);

		for (int child : tree.getChildren(node)) {
			convertNode(tree, child, split, join, directionForward);
		}

		//put log-moves, if necessary
		if (parameters.isShowLogMoves()) {
			//on split
			visualiseLogMove(tree, split, split, node, LogMovePosition.atSource(node), directionForward);

			//on join
			visualiseLogMove(tree, join, join, node, LogMovePosition.atSink(node), directionForward);
		}
	}

	private void convertInterleaved(EfficientTree tree, int node, LocalDotNode source, LocalDotNode sink,
			boolean directionForward) throws UnknownTreeNodeException {

		//operator split
		LocalDotNode split = new LocalDotNode(dot, info, NodeType.interleavedSplit, "\u2194", node, null);
		addArc(tree, source, split, node, directionForward, true);

		//operator join
		LocalDotNode join = new LocalDotNode(dot, info, NodeType.interleavedJoin, "\u2194", node, split);
		addArc(tree, join, sink, node, directionForward, true);

		for (int child : tree.getChildren(node)) {
			convertNode(tree, child, split, join, directionForward);
		}

		//put log-moves, if necessary
		if (parameters.isShowLogMoves()) {
			//on split
			visualiseLogMove(tree, split, split, node, LogMovePosition.atSource(node), directionForward);

			//on join
			visualiseLogMove(tree, join, join, node, LogMovePosition.atSink(node), directionForward);
		}
	}

	private void convertOr(EfficientTree tree, int node, LocalDotNode source, LocalDotNode sink,
			boolean directionForward) throws UnknownTreeNodeException {

		//operator split
		LocalDotNode split = new LocalDotNode(dot, info, NodeType.orSplit, "o", node, null);
		addArc(tree, source, split, node, directionForward, true);

		//operator join
		LocalDotNode join = new LocalDotNode(dot, info, NodeType.orJoin, "o", node, split);
		addArc(tree, join, sink, node, directionForward, true);

		for (int child : tree.getChildren(node)) {
			convertNode(tree, child, split, join, directionForward);
		}

		//put log-moves, if necessary
		if (parameters.isShowLogMoves()) {
			//on split
			visualiseLogMove(tree, split, split, node, LogMovePosition.atSource(node), directionForward);

			//on join
			visualiseLogMove(tree, join, join, node, LogMovePosition.atSink(node), directionForward);
		}
	}

	private void convertXor(EfficientTree tree, int node, LocalDotNode source, LocalDotNode sink,
			boolean directionForward) throws UnknownTreeNodeException {

		//operator split
		LocalDotNode split = new LocalDotNode(dot, info, NodeType.xor, "", node, null);
		addArc(tree, source, split, node, directionForward, true);

		//operator join
		LocalDotNode join = new LocalDotNode(dot, info, NodeType.xor, "", node, split);
		addArc(tree, join, sink, node, directionForward, true);

		for (int child : tree.getChildren(node)) {
			convertNode(tree, child, split, join, directionForward);
		}

		//log-moves
		//are never put on xor
	}

	private LocalDotEdge addArc(EfficientTree tree, final LocalDotNode from, final LocalDotNode to, final int node,
			boolean directionForward, boolean includeModelMoves) throws UnknownTreeNodeException {
		return addModelArc(tree, from, to, node, directionForward, data.getEdgeLabel(node, includeModelMoves));
	}

	private LocalDotEdge addModelArc(EfficientTree tree, final LocalDotNode from, final LocalDotNode to,
			final int unode, final boolean directionForward, final Pair<String, Long> cardinality) {

		final LocalDotEdge edge;
		if (directionForward) {
			edge = new LocalDotEdge(tree, dot, info, from, to, "", unode, EdgeType.model, null, -1, -1, directionForward);
		} else {
			edge = new LocalDotEdge(tree, dot, info, to, from, "", unode, EdgeType.model, null, -1, -1, directionForward);
			edge.setOption("dir", "back");
		}

		if (parameters.getColourModelEdges() != null) {
			String lineColour = parameters.getColourModelEdges().colourString(cardinality.getB(), minCardinality,
					maxCardinality);
			edge.setOption("color", lineColour);
		}

		edge.setOption("penwidth",
				"" + parameters.getModelEdgesWidth().size(cardinality.getB(), minCardinality, maxCardinality));

		if (parameters.isShowFrequenciesOnModelEdges() && !cardinality.getA().isEmpty()) {
			edge.setLabel(cardinality.getA());
			//DONT SHOW LABEL!!!!!!!!!
			//edge.setLabel(" ");
		} else {
			edge.setLabel(" ");
		}

		return edge;
	}

	private void visualiseLogMove(EfficientTree tree, LocalDotNode from, LocalDotNode to, int unode,
			LogMovePosition logMovePosition, boolean directionForward) {
		Pair<String, MultiSet<XEventClass>> logMoves = data.getLogMoveEdgeLabel(logMovePosition);
		Pair<String, Long> t = Pair.of(logMoves.getA(), logMoves.getB().size());
		if (logMoves.getB().size() > 0) {
			if (parameters.isRepairLogMoves()) {
				for (XEventClass e : logMoves.getB()) {
					LocalDotNode dotNode = new LocalDotNode(dot, info, NodeType.logMoveActivity, e.toString(), unode,
							null);
					addMoveArc(tree, from, dotNode, unode, EdgeType.logMove, logMovePosition.getOn(),
							logMovePosition.getBeforeChild(), t, directionForward);
					addMoveArc(tree, dotNode, to, unode, EdgeType.logMove, logMovePosition.getOn(),
							logMovePosition.getBeforeChild(), t, directionForward);
				}
			} else {
				addMoveArc(tree, from, to, unode, EdgeType.logMove, logMovePosition.getOn(),
						logMovePosition.getBeforeChild(), t, directionForward);
			}
		}
	}

	private LocalDotEdge addMoveArc(EfficientTree tree, LocalDotNode from, LocalDotNode to, int node, EdgeType type,
			int lookupNode1, int lookupNode2, Pair<String, Long> cardinality, boolean directionForward) {

		LocalDotEdge edge;
		if (directionForward) {
			edge = new LocalDotEdge(tree, dot, info, from, to, "", node, type, null, lookupNode1, lookupNode2,
					directionForward);
		} else {
			edge = new LocalDotEdge(tree, dot, info, to, from, "", node, type, null, lookupNode1, lookupNode2,
					directionForward);
			edge.setOption("dir", "back");
		}

		edge.setOption("style", "dashed");
		edge.setOption("arrowsize", ".5");

		if (parameters.getColourMoves() != null) {
			String lineColour = parameters.getColourMoves().colourString(cardinality.getB(), minCardinality,
					maxCardinality);
			edge.setOption("color", lineColour);
			edge.setOption("fontcolor", lineColour);
		}

		edge.setOption("penwidth",
				"" + parameters.getMoveEdgesWidth().size(cardinality.getB(), minCardinality, maxCardinality));

		if (parameters.isShowFrequenciesOnMoveEdges()) {
			//edge.setLabel(" ");
			edge.setLabel(cardinality.getA());
		} else {
			edge.setLabel(" ");
		}

		return edge;
	}

	private double getOccurrenceFactor(long cardinality) {
		assert (minCardinality <= cardinality);
		assert (cardinality <= maxCardinality);
		return ProcessTreeVisualisationHelper.getOccurrenceFactor(cardinality, minCardinality, maxCardinality);
	}
}
