package org.processmining.plugins.inductiveVisualMiner;

import java.awt.Color;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.deckfour.xes.classification.XEventClass;
import org.processmining.plugins.InductiveMiner.MultiSet;
import org.processmining.plugins.InductiveMiner.Pair;
import org.processmining.plugins.InductiveMiner.Triple;
import org.processmining.plugins.InductiveMiner.efficienttree.UnknownTreeNodeException;
import org.processmining.plugins.graphviz.colourMaps.ColourMap;
import org.processmining.plugins.graphviz.colourMaps.ColourMaps;
import org.processmining.plugins.graphviz.dot.DotEdge;
import org.processmining.plugins.graphviz.visualisation.DotPanel;
import org.processmining.plugins.inductiveVisualMiner.alignedLogVisualisation.data.AlignedLogVisualisationData;
import org.processmining.plugins.inductiveVisualMiner.alignment.LogMovePosition;
import org.processmining.plugins.inductiveVisualMiner.animation.Animation;
import org.processmining.plugins.inductiveVisualMiner.helperClasses.IvMModel;
import org.processmining.plugins.inductiveVisualMiner.helperClasses.sizeMaps.SizeMap;
import org.processmining.plugins.inductiveVisualMiner.traceview.TraceViewEventColourMap;
import org.processmining.plugins.inductiveVisualMiner.visualisation.DFMEdgeType;
import org.processmining.plugins.inductiveVisualMiner.visualisation.LocalDotEdge;
import org.processmining.plugins.inductiveVisualMiner.visualisation.LocalDotEdge.EdgeType;
import org.processmining.plugins.inductiveVisualMiner.visualisation.LocalDotNode;
import org.processmining.plugins.inductiveVisualMiner.visualisation.ProcessTreeVisualisationInfo;
import org.processmining.plugins.inductiveVisualMiner.visualisation.ProcessTreeVisualisationParameters;

import com.kitfox.svg.Group;
import com.kitfox.svg.SVGDiagram;
import com.kitfox.svg.SVGElement;
import com.kitfox.svg.SVGException;
import com.kitfox.svg.Text;

import au.com.bytecode.opencsv.CSVReader;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.set.TIntSet;

public class InductiveVisualMinerSelectionColourer {
	
	
	
	public static HashMap<String, HashMap<String, String>> createColourArray()
	{
		HashMap <String, HashMap<String, String>> activity_colour = new HashMap<String, HashMap<String, String>>();
		List<List<String>> records = new ArrayList<List<String>>();
		try (CSVReader csvReader = new CSVReader(new FileReader("test.csv"));) {
		    String[] values = null;
		    while ((values = csvReader.readNext()) != null) {
		        records.add(Arrays.asList(values));
		    }
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        int lineNo = 0;
        for(List<String> line: records) {
            int columnNo = 0;
            String main_dep = records.get(0).get(lineNo);
          	 HashMap<String, String> act_similarities = new HashMap<String, String>();
            //System.out.println(main_dep);
            for (String value: line) {
            	if(!main_dep.equals("department") && columnNo != 0) {
            		String sec_dep = records.get(0).get(columnNo);
            		//System.out.println("Main " + main_dep + " Sec " + sec_dep + ": " + value);
            		act_similarities.put(sec_dep, value);
            	}
            	else {
            		//line contains only meta information and no values!
            	}
            	activity_colour.put(main_dep, act_similarities);
                //System.out.println("Line " + lineNo + " Column " + columnNo + ": " + value);
                columnNo++;
            }    
            lineNo++;
            
        }
        return activity_colour;
	}
	public static void colourSelection(SVGDiagram diagram, TIntSet selectedNodes, Set<LogMovePosition> selectedLogMoves,
			ProcessTreeVisualisationInfo visualisationInfo) {
		for (TIntIterator it = selectedNodes.iterator(); it.hasNext();) {
			int unode = it.next();
			LocalDotNode dotNode = Animation.getDotNodeFromActivity(unode, visualisationInfo);
			colourSelectedNode(diagram, dotNode, true, visualisationInfo);
		}
		//re-colour the selected log moves
		for (LogMovePosition logMove : selectedLogMoves) {
			LocalDotEdge dotEdge = Animation.getDotEdgeFromLogMove(logMove, visualisationInfo);
			colourSelectedEdge(diagram, dotEdge, true);
		}
	}

	public static void colourSelectedNode(SVGDiagram svg, LocalDotNode dotNode, boolean selected, ProcessTreeVisualisationInfo info) {
		//über controller makeNodeSelectable daten reinholen!
		Group svgGroup = DotPanel.getSVGElementOf(svg, dotNode);
		SVGElement shape = svgGroup.getChild(1);
		Text titleName =  (Text) svgGroup.getChild(3);
		List list_title = titleName.getContent();
		String str_title = String.join(",", list_title).replace(",", "");
		String main_title = str_title.split(":")[0];
		//future work
		if (selected & false) {
			HashMap <String, HashMap<String, String>> activity_colours = new HashMap<String, HashMap<String, String>>();
			activity_colours = createColourArray();
			HashMap<String, String> colours = activity_colours.get(main_title);
			Collection<LocalDotNode> activities = info.getAllActivityNodes();
			for (Iterator<LocalDotNode> iterator = activities.iterator(); iterator.hasNext();) {
		        Group sec_svgGroup = DotPanel.getSVGElementOf(svg,  iterator.next());
				SVGElement sec_shape = sec_svgGroup.getChild(1);
				Text sec_titleName =  (Text) sec_svgGroup.getChild(3);
				List sec_list_title = sec_titleName.getContent();
				String sec_str_title = String.join(",", sec_list_title).replace(",", "");
				String sec_title = sec_str_title.split(":")[0];
				DotPanel.setCSSAttributeOf(sec_shape, "stroke", colours.get(sec_title));
				
				
				if(main_title.equals(sec_title)) {
					DotPanel.setCSSAttributeOf(shape, "stroke-width", "20");
				}
				else {
					DotPanel.setCSSAttributeOf(sec_shape, "stroke-width", "15");
				}		
		        }

			
	        //Colour other activities!
			
			
			
	        		
			dotNode.unselectedAppearance.stroke = DotPanel.setCSSAttributeOf(shape, "stroke", colours.get(main_title));
			dotNode.unselectedAppearance.strokeWidth = DotPanel.setCSSAttributeOf(shape, "stroke-width", "20");
			//dotNode.unselectedAppearance.strokeDashArray = DotPanel.setCSSAttributeOf(shape, "stroke-dasharray", "5");
		} else {
			DotPanel.setCSSAttributeOf(shape, "stroke", dotNode.unselectedAppearance.stroke);
			DotPanel.setCSSAttributeOf(shape, "stroke-width", dotNode.unselectedAppearance.strokeWidth);
			//DotPanel.setCSSAttributeOf(shape, "stroke-dasharray", dotNode.unselectedAppearance.strokeDashArray);
		}
		
	}

	public static void colourSelectedEdge(SVGDiagram svg, LocalDotEdge dotEdge, boolean selected) {
		Group svgGroup = DotPanel.getSVGElementOf(svg, dotEdge);
		SVGElement line = svgGroup.getChild(1);
		SVGElement text = svgGroup.getChild(3);

		if (selected) {
			dotEdge.unselectedAppearance.textFill = DotPanel.setCSSAttributeOf(text, "fill", "none");
			if (dotEdge.getType() != EdgeType.model) {
				dotEdge.unselectedAppearance.textStroke = DotPanel.setCSSAttributeOf(text, "stroke", "red");
				dotEdge.unselectedAppearance.lineStrokeDashArray = DotPanel.setCSSAttributeOf(line, "stroke-dasharray",
						"2,5");
			} else {
				dotEdge.unselectedAppearance.textStroke = DotPanel.setCSSAttributeOf(text, "stroke", "black");
				dotEdge.unselectedAppearance.lineStrokeDashArray = DotPanel.setCSSAttributeOf(line, "stroke-dasharray",
						"2,2");
			}
			dotEdge.unselectedAppearance.textStrokeWidth = DotPanel.setCSSAttributeOf(text, "stroke-width", "0.55");

		} else {
			DotPanel.setCSSAttributeOf(text, "fill", dotEdge.unselectedAppearance.textFill);
			DotPanel.setCSSAttributeOf(text, "stroke", dotEdge.unselectedAppearance.textStroke);
			DotPanel.setCSSAttributeOf(text, "stroke-width", dotEdge.unselectedAppearance.textStrokeWidth);
			DotPanel.setCSSAttributeOf(line, "stroke-dasharray", dotEdge.unselectedAppearance.lineStrokeDashArray);
		}
	}

	public static TraceViewEventColourMap colourHighlighting(SVGDiagram svg, ProcessTreeVisualisationInfo info,
			IvMModel model, AlignedLogVisualisationData data,
			ProcessTreeVisualisationParameters visualisationParameters, TraceViewEventColourMap traceViewColourMap)
			throws UnknownTreeNodeException {

		//compute extreme cardinalities
		Pair<Long, Long> extremes = data.getExtremeCardinalities();
		long minCardinality = extremes.getLeft();
		long maxCardinality = extremes.getRight();

		try {
			//style nodes
			for (int unode : model.getAllNodes()) {
				Triple<String, Long, Long> cardinality = data.getNodeLabel(unode, false);
				Pair<Color, Color> colour = styleUnfoldedNode(model, unode, svg, info, cardinality, minCardinality,
						maxCardinality, visualisationParameters);

				if (model.isActivity(unode)) {
					traceViewColourMap.set(unode, colour.getA(), colour.getB());
				}
			}

			//style edges
			styleEdges(model, svg, info, data, visualisationParameters, traceViewColourMap, minCardinality,
					maxCardinality);

		} catch (SVGException e) {
			e.printStackTrace();
		}

		return traceViewColourMap;
	}

	public static Pair<Color, Color> styleUnfoldedNode(IvMModel model, int unode, SVGDiagram svg,
			ProcessTreeVisualisationInfo info, Triple<String, Long, Long> cardinality, long minCardinality,
			long maxCardinality, ProcessTreeVisualisationParameters visualisationParameters) throws SVGException {
		if (model.isActivity(unode)) {
			return styleManual(model, unode, svg, info, cardinality, minCardinality, maxCardinality,
					visualisationParameters);
		} else {
			styleNonManualNode(model, unode, svg, info, cardinality);
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	//before and on selection, iterates through each node and styles it --> can be used to access graphical elements (attributes), also iterates through it when filteronNodeSelection is called
	private static Pair<Color, Color> styleManual(IvMModel model, int unode, SVGDiagram svg,
			ProcessTreeVisualisationInfo info, Triple<String, Long, Long> cardinality, long minCardinality,
			long maxCardinality, ProcessTreeVisualisationParameters visualisationParameters) throws SVGException {
		LocalDotNode dotNode = info.getActivityDotNode(unode);

		Group group = DotPanel.getSVGElementOf(svg, dotNode);
		SVGElement polygon = group.getChild(2);
		Text titleName =  (Text) group.getChild(3);
		//Access whole shape


		//System.out.println(titleName.getContent());
		//Text titleCount = (Text) group.getChild(3);
		//System.out.println("TITLE COUNT" + titleCount.getContent());
		

		//recolour the polygon
		Color fillColour;
		Color fontColour = Color.black;
		if (cardinality.getB() > 0) {
			//fillColour = visualisationParameters.getColourNodes().colour(cardinality.getB(), minCardinality,
			//		maxCardinality);
			fillColour = Color.white;
			if (ColourMaps.getLuma(fillColour) < 128) {
				fontColour = Color.black;
			}
		} else {
			//fillColour = visualisationParameters.getColourNodes().colour(1, 0, 2);
			fillColour = Color.white;
		}
		DotPanel.setCSSAttributeOf(polygon, "fill", fillColour);

		//set label colour
		//DotPanel.setCSSAttributeOf(titleCount, "fill", fontColour);
		DotPanel.setCSSAttributeOf(titleName, "fill", fontColour);

		if (cardinality.getB() > 0) {
			DotPanel.setCSSAttributeOf(group, "opacity", "1.0");
		} else {
			DotPanel.setCSSAttributeOf(group, "opacity", "0.2");
		}
		
		
		//set title
		List list_title = titleName.getContent();
		String str_title = String.join(",", list_title);
		str_title = str_title.replace(",", "");
		String new_title = str_title.split(":")[0];
		titleName.getContent().clear();
		titleName.getContent().add(new_title + ": " + cardinality.getA());
		titleName.rebuild();
		fontColour = Color.black;
		fillColour = Color.red;
		return Pair.of(fillColour, fontColour);
	}

	private static void styleNonManualNode(IvMModel model, int node, SVGDiagram svg, ProcessTreeVisualisationInfo info,
			Triple<String, Long, Long> cardinality) {
		//colour non-activity nodes
		for (LocalDotNode dotNode : info.getNodes(node)) {
			if (cardinality.getB() > 0) {
				DotPanel.setCSSAttributeOf(svg, dotNode, "opacity", "1.0");
			} else {
				DotPanel.setCSSAttributeOf(svg, dotNode, "opacity", "0.2");
			}
		}
	}

	private static void styleEdges(IvMModel model, SVGDiagram svg, ProcessTreeVisualisationInfo info,
			AlignedLogVisualisationData data, ProcessTreeVisualisationParameters parameters,
			TraceViewEventColourMap traceViewColourMap, long minCardinality, long maxCardinality)
			throws SVGException, UnknownTreeNodeException {
		styleModelEdges(model, svg, info, data, parameters, traceViewColourMap, minCardinality, maxCardinality);
		styleMoveEdges(svg, info, data, parameters, minCardinality, maxCardinality);
	}

	private static void styleModelEdges(IvMModel model, SVGDiagram svg, ProcessTreeVisualisationInfo info,
			AlignedLogVisualisationData data, ProcessTreeVisualisationParameters parameters,
			TraceViewEventColourMap traceViewColourMap, long minCardinality, long maxCardinality)
			throws SVGException, UnknownTreeNodeException {
		for (LocalDotEdge dotEdge : info.getAllModelEdges()) {
			Pair<String, Long> cardinality;
			if (model.isTree()) {
				cardinality = data.getEdgeLabel(dotEdge.getUnode(), false);
			} else {
				//DFM model
				if (dotEdge.getDfmType() != DFMEdgeType.modelIntraActivity) {
					cardinality = data.getEdgeLabel(dotEdge.getLookupNode1(), dotEdge.getLookupNode2(),
							dotEdge.getDfmType().isFrequencyIncludesModelMoves());
				} else {
					cardinality = data.getEdgeLabel(dotEdge.getLookupNode1(),
							dotEdge.getDfmType().isFrequencyIncludesModelMoves());
				}
			}
			Color edgeColour = styleEdge(dotEdge, svg, cardinality, minCardinality, maxCardinality,
					parameters.getColourModelEdges(), parameters.isShowFrequenciesOnModelEdges(),
					parameters.getModelEdgesWidth());
			edgeColour = Color.black;
			if (model.isTau(dotEdge.getUnode())) {
				traceViewColourMap.set(dotEdge.getUnode(), edgeColour, edgeColour);
			}
		}
	}

	private static void styleMoveEdges(SVGDiagram svg, ProcessTreeVisualisationInfo info,
			AlignedLogVisualisationData data, ProcessTreeVisualisationParameters parameters, long minCardinality,
			long maxCardinality) throws SVGException {
		//style model move edges
		for (LocalDotEdge dotEdge : info.getAllModelMoveEdges()) {
			Pair<String, Long> cardinality = data.getModelMoveEdgeLabel(dotEdge.getUnode());
			styleEdge(dotEdge, svg, cardinality, minCardinality, maxCardinality, parameters.getColourMoves(),
					parameters.isShowFrequenciesOnMoveEdges(), parameters.getMoveEdgesWidth());
		}

		//style log moves
		for (LocalDotEdge dotEdge : info.getAllLogMoveEdges()) {
			LogMovePosition logMovePosition = LogMovePosition.of(dotEdge);
			Pair<String, MultiSet<XEventClass>> cardinality = data.getLogMoveEdgeLabel(logMovePosition);
			styleEdge(dotEdge, svg, Pair.of(cardinality.getA(), cardinality.getB().size()), minCardinality,
					maxCardinality, parameters.getColourMoves(), parameters.isShowFrequenciesOnMoveEdges(),
					parameters.getMoveEdgesWidth());
		}
	}

	@SuppressWarnings("unchecked")
	private static Color styleEdge(DotEdge edge, SVGDiagram svg, Pair<String, Long> cardinality, long minCardinality,
			long maxCardinality, ColourMap colourMap, boolean showFrequency, SizeMap widthMap) throws SVGException {

		//prepare parts of the rendered dot element
		Group group = DotPanel.getSVGElementOf(svg, edge);
		SVGElement line = group.getChild(1);
		SVGElement arrowHead = group.getChild(2);

		//stroke
		Color edgeColour = colourMap.colour(cardinality.getB(), minCardinality, maxCardinality);
		edgeColour = Color.black;
		double strokeWidth = widthMap.size(cardinality.getB(), minCardinality, maxCardinality);
		DotPanel.setCSSAttributeOf(line, "stroke", edgeColour);
		DotPanel.setCSSAttributeOf(arrowHead, "stroke", edgeColour);
		DotPanel.setCSSAttributeOf(arrowHead, "fill", edgeColour);
		DotPanel.setCSSAttributeOf(line, "stroke-width", strokeWidth + "");

		//transparency
		if (cardinality.getB() > 0) {
			DotPanel.setCSSAttributeOf(group, "opacity", "1.0");
		} else {
			DotPanel.setCSSAttributeOf(group, "opacity", "0.1");
		}

		//edge label
		if (showFrequency) {
			//deactivate content clearing and rebuilding of edges
			Text label = (Text) group.getChild(group.getChildren(null).size() - 1);
			label.getContent().clear();
			//label.getContent().add(cardinality.getA());
			label.rebuild();
		}

		return edgeColour;
	}
}
