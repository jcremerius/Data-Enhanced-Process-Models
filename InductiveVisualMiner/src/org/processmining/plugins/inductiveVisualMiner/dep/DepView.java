package org.processmining.plugins.inductiveVisualMiner.dep;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SpringLayout;

import org.processmining.plugins.InductiveMiner.Function;
import org.processmining.plugins.inductiveVisualMiner.InductiveVisualMinerPanel;
import org.processmining.plugins.inductiveVisualMiner.Selection;
import org.processmining.plugins.inductiveVisualMiner.dataanalysis.OnOffPanel;
import org.processmining.plugins.inductiveVisualMiner.helperClasses.IvMModel;
import org.processmining.plugins.inductiveVisualMiner.helperClasses.SideWindow;
import org.processmining.plugins.inductiveVisualMiner.helperClasses.decoration.IvMDecoratorI;
import org.processmining.plugins.inductiveVisualMiner.helperClasses.decoration.IvMPanel;
import org.processmining.plugins.inductiveVisualMiner.helperClasses.decoration.SwitchPanel;
import org.processmining.plugins.inductiveVisualMiner.tracecolouring.TraceColourMapSettings;
import org.processmining.plugins.inductiveminer2.attributes.Attribute;
import org.processmining.plugins.inductiveminer2.attributes.AttributesInfo;

import gnu.trove.iterator.TIntIterator;

public class DepView extends SideWindow {

	private static final long serialVersionUID = -4833037956665918455L;
	private final OnOffPanel<IvMPanel> onOffPanel;
	private final JComboBox<String> keySelector;
	private final DefaultComboBoxModel<String> keySelectorModel;
	private final JTextArea explanation;
	private final JLabel title;
	JPanel filterPanel;

	public static final int maxColours = 7;
	public static final String prefix = "       ";

	private Function<TraceColourMapSettings, Object> onUpdate;

	/**
	 * 
	 */
	private String selectedAttributeName;
	private AttributesInfo attributesInfo;
	private String selectedActivity;

	public DepView(IvMDecoratorI decorator, InductiveVisualMinerPanel parent) {
		super(parent, "Attribute Info " + InductiveVisualMinerPanel.title);
		setSize(300, 300);
		setMinimumSize(new Dimension(300, 300));
		IvMPanel content = new IvMPanel(decorator);

		onOffPanel = new OnOffPanel<>(decorator, content);
		onOffPanel.setOffMessage("Waiting for attributes..");
		add(onOffPanel);
		onOffPanel.on();

		parent.getSelectionLabel();
		
		BorderLayout layout = new BorderLayout();
		content.setLayout(layout);

		//explanation
		{
			explanation = new JTextArea(
					"Attribute info shows the attributes of the selected activity and helps to select attributes changing through the process for all activities sharing the same attribute");
			decorator.decorate(explanation);
			explanation.setWrapStyleWord(true);
			explanation.setLineWrap(true);
			explanation.setEnabled(false);
			explanation.setMargin(new Insets(5, 5, 5, 5));
			content.add(explanation, BorderLayout.PAGE_START);
		}


		//filter panel
		{
			SpringLayout filterPanelLayout = new SpringLayout();
			filterPanel = new SwitchPanel(decorator);
			filterPanel.setLayout(filterPanelLayout);
			filterPanel.setEnabled(false);
			content.add(filterPanel, BorderLayout.CENTER);

			//title
			{
				title = new JLabel("Selected Activity");
				decorator.decorate(title);

				filterPanel.add(title);
				filterPanelLayout.putConstraint(SpringLayout.NORTH, title, 10, SpringLayout.NORTH, filterPanel);
				filterPanelLayout.putConstraint(SpringLayout.WEST, title, 5, SpringLayout.WEST, filterPanel);
			}

			//key selector
			{
				keySelectorModel = new DefaultComboBoxModel<>();
				keySelector = new JComboBox<>();
				decorator.decorate(keySelector);
				keySelector.setModel(keySelectorModel);

				filterPanel.add(keySelector);
				filterPanelLayout.putConstraint(SpringLayout.VERTICAL_CENTER, keySelector, 0,
						SpringLayout.VERTICAL_CENTER, title);
				filterPanelLayout.putConstraint(SpringLayout.WEST, keySelector, 5, SpringLayout.EAST, title);

				filterPanelLayout.putConstraint(SpringLayout.EAST, keySelector, -5, SpringLayout.EAST, filterPanel);
			}




		}

		//set up the controller
		keySelector.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					if (onOffPanel.isOn()) {
						selectedAttributeName = (String) keySelector.getSelectedItem();
						update();
					}
				}
			}
		});


	}
	public void updateSelection(InductiveVisualMinerPanel panel, Selection selection, IvMModel model, String b) {
		if (selection.isAnActivitySelected()) {
			TIntIterator it = selection.getSelectedActivities().iterator();
			selectedActivity = model.getActivityName(it.next());
			keySelectorModel.removeAllElements();
			keySelectorModel.addElement(selectedActivity);
			System.out.println(b);
		}
		
	}

	public void setAttributes(AttributesInfo attributesInfo) {
		this.attributesInfo = attributesInfo;

		//populate the combobox with the trace attributes
		{
			keySelectorModel.removeAllElements();
			for (Attribute attribute : attributesInfo.getTraceAttributes()) {
				keySelectorModel.addElement(attribute.getName());
			}
		}

		//keep the selection
		{
			boolean found = false;
			if (selectedAttributeName != null
					&& attributesInfo.getTraceAttributeValues(selectedAttributeName) != null) {
				for (int i = 0; i < keySelector.getItemCount(); i++) {
					String key = keySelector.getItemAt(i);
					if (key.equals(selectedAttributeName)) {
						keySelector.setSelectedItem(selectedAttributeName);
						found = true;
					}
				}
			}
			if (!found) {
				keySelector.setSelectedIndex(0);
				selectedAttributeName = (String) keySelector.getSelectedItem();
			}
		}

		//enable the gui
		{
			onOffPanel.on();
		}

		update();
	}

	public void invalidateAttributes() {
		//disable the gui
		{
			onOffPanel.off();
		}
	}

	public void update() {
		//update values --> selectedAttributes
	}




	public Function<TraceColourMapSettings, Object> getOnUpdate() {
		return onUpdate;
	}

	public void setOnUpdate(Function<TraceColourMapSettings, Object> onUpdate) {
		this.onUpdate = onUpdate;
	}

	
}