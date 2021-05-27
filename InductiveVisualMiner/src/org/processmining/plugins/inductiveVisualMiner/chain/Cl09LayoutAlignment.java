package org.processmining.plugins.inductiveVisualMiner.chain;

import org.processmining.plugins.InductiveMiner.Triple;
import org.processmining.plugins.InductiveMiner.mining.IMLogInfo;
import org.processmining.plugins.InductiveMiner.mining.logs.IMLog;
import org.processmining.plugins.graphviz.dot.Dot;
import org.processmining.plugins.graphviz.visualisation.DotPanel;
import org.processmining.plugins.graphviz.visualisation.DotPanelUserSettings;
import org.processmining.plugins.inductiveVisualMiner.alignedLogVisualisation.data.AlignedLogVisualisationData;
import org.processmining.plugins.inductiveVisualMiner.alignedLogVisualisation.data.AlignedLogVisualisationDataImplFrequencies;
import org.processmining.plugins.inductiveVisualMiner.configuration.InductiveVisualMinerConfiguration;
import org.processmining.plugins.inductiveVisualMiner.helperClasses.IvMModel;
import org.processmining.plugins.inductiveVisualMiner.ivmlog.IvMLogInfo;
import org.processmining.plugins.inductiveVisualMiner.mode.Mode;
import org.processmining.plugins.inductiveVisualMiner.traceview.TraceViewEventColourMap;
import org.processmining.plugins.inductiveVisualMiner.visualisation.DfmVisualisation;
import org.processmining.plugins.inductiveVisualMiner.visualisation.ProcessTreeVisualisation;
import org.processmining.plugins.inductiveVisualMiner.visualisation.ProcessTreeVisualisationInfo;
import org.processmining.plugins.inductiveVisualMiner.visualisation.ProcessTreeVisualisationParameters;
import org.processmining.plugins.inductiveminer2.attributes.AttributesInfo;

import com.kitfox.svg.SVGDiagram;

/**
 * This class is to be instantiated once for every mode.
 * 
 * @author sander
 *
 */
public class Cl09LayoutAlignment extends DataChainLinkComputationAbstract {

	private final InductiveVisualMinerConfiguration configuration;

	public Cl09LayoutAlignment(InductiveVisualMinerConfiguration configuration) {
		this.configuration = configuration;
	}

	@Override
	public String getName() {
		return "layout alignment";
	}

	@Override
	public String getStatusBusyMessage() {
		return "Layouting aligned model..";
	}

	@Override
	public IvMObject<?>[] createInputObjects() {
		return new IvMObject<?>[] { IvMObject.model, IvMObject.aligned_log_info, IvMObject.selected_visualisation_mode,
				IvMObject.selected_graph_user_settings, IvMObject.attributes_info, IvMObject.imlog_info, IvMObject.imlog_activity_filtered};
	}

	public IvMObject<?>[] createNonTriggerObjects() {
		return Mode.gatherInputsRequested(configuration);
	}

	@Override
	public IvMObject<?>[] createOutputObjects() {
		return new IvMObject<?>[] { IvMObject.graph_dot, IvMObject.graph_svg, IvMObject.graph_visualisation_info, IvMObject.boah};
	}

	@Override
	public IvMObjectValues execute(InductiveVisualMinerConfiguration configuration, IvMObjectValues inputs,
			IvMCanceller canceller) throws Exception {
		
		String boah = "hallo";
		IMLog log = inputs.get(IvMObject.imlog_activity_filtered);
		IMLogInfo imLogInfo = inputs.get(IvMObject.imlog_info);
		IvMModel model = inputs.get(IvMObject.model);
		IvMLogInfo logInfo = inputs.get(IvMObject.aligned_log_info);
		AttributesInfo attributesInfo = inputs.get(IvMObject.attributes_info);
		Mode mode = inputs.get(IvMObject.selected_visualisation_mode);
		DotPanelUserSettings settings = inputs.get(IvMObject.selected_graph_user_settings);

		IvMObjectValues modeInputs = inputs.getIfPresent(mode.getOptionalObjects());
		ProcessTreeVisualisationParameters visualisationParameters = mode
				.getVisualisationParametersWithAlignments(modeInputs);

		//compute dot
		AlignedLogVisualisationData data = new AlignedLogVisualisationDataImplFrequencies(model, logInfo);
		Triple<Dot, ProcessTreeVisualisationInfo, TraceViewEventColourMap> p;
		if (model.isTree()) {
			System.out.println("In CL9");
			ProcessTreeVisualisation visualiser = new ProcessTreeVisualisation();
			
			//activity_attributes als return wert von fancy!
			
			
			p = visualiser.fancy(model, data, visualisationParameters, attributesInfo, true, log);
		} else {
			DfmVisualisation visualiser = new DfmVisualisation();
			p = visualiser.fancy(model, data, visualisationParameters);
		}

		//keep the user settings of the dot panel
		settings.applyToDot(p.getA());
		//compute svg from dot
		SVGDiagram diagram = DotPanel.dot2svg(p.getA());

		//		//update the selection
		//		if (!mode.isShowDeviations()) {
		//			selection = new Selection(selection.getSelectedActivities(), new THashSet<LogMovePosition>(),
		//					new TIntHashSet(10, 0.5f, -1), selection.getSelectedTaus());
		//		}
		System.out.println("End of CL09");

		return new IvMObjectValues().//
				s(IvMObject.graph_dot, p.getA()).//
				s(IvMObject.graph_svg, diagram).//
				s(IvMObject.graph_visualisation_info, p.getB()).
				s(IvMObject.boah, boah);
	}
}