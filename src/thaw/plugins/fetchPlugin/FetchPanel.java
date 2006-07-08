package thaw.plugins.fetchPlugin;

import java.awt.GridLayout;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.ButtonGroup;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import java.awt.Dimension;
import javax.swing.JFileChooser;

import java.io.File;
import java.util.Vector;
import java.util.Iterator;

import thaw.core.*;
import thaw.plugins.FetchPlugin;
import thaw.i18n.I18n;

public class FetchPanel implements java.awt.event.ActionListener {

	private JPanel mainPanel = null;
	private JPanel centeredPart = null; /* (below is the validation button) */
	private JButton validationButton = null;

	private JPanel filePanel = null;
	private JLabel fileLabel = null;
	private JTextArea fileList = null;
	private JButton loadListButton = null;
	

	private JPanel belowPanel = null; /* 1 x 2 */

	private JPanel priorityPanel = null; /* 2 x 1 */
	private JLabel priorityLabel = null;
	private String[] priorities = null;
	private JComboBox prioritySelecter = null;

	private JPanel persistencePanel = null;
	private JLabel persistenceLabel = null;
	private String[] persistences = null;
	private JComboBox persistenceSelecter = null;

	private JLabel destinationLabel = null;
	private JPanel dstChoosePanel = null; /* 3 x 1 */
	private JTextField destinationField = null;
	private JButton destinationButton = null;

	private JPanel queuePanel = null;
	private JLabel queueLabel = null;
	private String[] queues = null;
	private JComboBox queueSelecter = null;

	private Core core;
	private FetchPlugin fetchPlugin;

	public FetchPanel(Core core, FetchPlugin fetchPlugin) {
		this.core = core;
		this.fetchPlugin = fetchPlugin;

		mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout(20, 20));

		centeredPart = new JPanel();
		centeredPart.setLayout(new BorderLayout(10, 10));

		validationButton = new JButton(I18n.getMessage("thaw.common.fetch"));
		validationButton.setPreferredSize(new Dimension(300, 40));
		
		validationButton.addActionListener(this);

		filePanel = new JPanel();
		filePanel.setLayout(new BorderLayout());


		/* FILE LIST */

		fileList = new JTextArea();
		fileLabel = new JLabel(I18n.getMessage("thaw.plugin.fetch.keyList"));

		loadListButton = new JButton(I18n.getMessage("thaw.plugin.fetch.loadKeyListFromFile"));
		loadListButton.addActionListener(this);
		
		filePanel.add(fileLabel, BorderLayout.NORTH);
		filePanel.add(new JScrollPane(fileList), BorderLayout.CENTER);
		filePanel.add(loadListButton, BorderLayout.SOUTH);

		
		belowPanel = new JPanel();
		belowPanel.setLayout(new GridLayout(2, 2, 10, 10));


		/* PRIORITY */
		priorityPanel = new JPanel();
		priorityPanel.setLayout(new GridLayout(2, 1, 5, 5));

		priorityLabel = new JLabel(I18n.getMessage("thaw.common.priority"));
		priorities = new String[] {
			I18n.getMessage("thaw.plugin.priority.p0"),
			I18n.getMessage("thaw.plugin.priority.p1"),
			I18n.getMessage("thaw.plugin.priority.p2"),
			I18n.getMessage("thaw.plugin.priority.p3"),
			I18n.getMessage("thaw.plugin.priority.p4"),
			I18n.getMessage("thaw.plugin.priority.p5"),
			I18n.getMessage("thaw.plugin.priority.p6") 
			
		};
		prioritySelecter = new JComboBox(priorities);
		prioritySelecter.setSelectedItem(I18n.getMessage("thaw.plugin.priority.p3"));

		priorityPanel.add(priorityLabel);
		priorityPanel.add(prioritySelecter);
		
		/* PERSISTENCE */
		persistencePanel = new JPanel();
		persistencePanel.setLayout(new GridLayout(2, 1, 5, 5));

		persistenceLabel = new JLabel(I18n.getMessage("thaw.common.persistence"));
		persistences = new String[] {
			I18n.getMessage("thaw.common.persistenceReboot"),
			I18n.getMessage("thaw.common.persistenceForever"),
			I18n.getMessage("thaw.common.persistenceConnection")
		};
		persistenceSelecter = new JComboBox(persistences);

		persistencePanel.add(persistenceLabel);
		persistencePanel.add(persistenceSelecter);

		/* QUEUE */
		queuePanel = new JPanel();
	        queuePanel.setLayout(new GridLayout(2, 1, 5, 5));
		
		queueLabel = new JLabel(I18n.getMessage("thaw.common.globalQueue"));
		queues = new String [] {
			I18n.getMessage("thaw.common.true"),
			I18n.getMessage("thaw.common.false"),
		};
		queueSelecter = new JComboBox(queues);

		queuePanel.add(queueLabel);
		queuePanel.add(queueSelecter);

		/* DESTINATION */
		destinationLabel = new JLabel(I18n.getMessage("thaw.plugin.fetch.destinationDirectory"));

		dstChoosePanel = new JPanel();
		dstChoosePanel.setLayout(new GridLayout(3,1, 5, 5));
		
		destinationField = new JTextField("");
		destinationField.setEditable(false);
		
		destinationButton = new JButton(I18n.getMessage("thaw.plugin.fetch.chooseDestination"));
		destinationButton.addActionListener(this);
		
		dstChoosePanel.add(destinationLabel);
		dstChoosePanel.add(destinationField);
		dstChoosePanel.add(destinationButton);

		belowPanel.add(priorityPanel);
		belowPanel.add(persistencePanel);
		belowPanel.add(queuePanel);
		belowPanel.add(dstChoosePanel);
		

		centeredPart.add(filePanel, BorderLayout.CENTER);
		centeredPart.add(belowPanel, BorderLayout.SOUTH);
		
		mainPanel.add(centeredPart, BorderLayout.CENTER);
		mainPanel.add(validationButton, BorderLayout.SOUTH);
	}


	public JPanel getPanel() {
		return mainPanel;
	}

	
	public void actionPerformed(java.awt.event.ActionEvent e) {
		if(e.getSource() == validationButton) {
			int priority = 6;
			int persistence = 0;
			boolean globalQueue = true;

			if(((String)persistenceSelecter.getSelectedItem()).equals(I18n.getMessage("thaw.common.persistenceForever")))
				persistence = 0;
			if(((String)persistenceSelecter.getSelectedItem()).equals(I18n.getMessage("thaw.common.persistenceReboot")))
				persistence = 1;
			if(((String)persistenceSelecter.getSelectedItem()).equals(I18n.getMessage("thaw.common.persistenceConnection")))
				persistence = 2;

			if(((String)queueSelecter.getSelectedItem()).equals(I18n.getMessage("thaw.common.false")))
				globalQueue = false;


			for(int i = 0; i < priorities.length ; i++) {
				if(((String)prioritySelecter.getSelectedItem()).equals(I18n.getMessage("thaw.plugin.priority.p"+i)))
					priority = i;
			}

			if(destinationField.getText() == null || destinationField.getText().equals("")) {
				new thaw.core.WarningWindow(core, "You must choose a destination");
				return;
			}

			fetchPlugin.fetchFiles(fileList.getText().split("\n"),
					       priority, persistence, globalQueue,
					       destinationField.getText());

			fileList.setText("");
		}


		if(e.getSource() == destinationButton) {
			FileChooser fileChooser = new FileChooser();
			File dir = null;

			fileChooser.setTitle(I18n.getMessage("thaw.plugin.fetch.destinationDirectory"));
			fileChooser.setDirectoryOnly(true);
			fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);

			dir = fileChooser.askOneFile();

			if(dir == null) {
				Logger.info(this, "Selection canceled");
				return;
			}

			destinationField.setText(dir.getPath());
		}

		if(e.getSource() == loadListButton) {
			FileChooser fileChooser = new FileChooser();
			File toParse = null;

			fileChooser.setTitle(I18n.getMessage("thaw.plugin.fetch.loadKeyListFromFile"));
			fileChooser.setDirectoryOnly(false);
			fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
			
			toParse = fileChooser.askOneFile();

			if(toParse == null) {
				Logger.info(this, "Nothing to parse");
				return;
			}
			
			Vector keys = KeyFileFilter.extractKeys(toParse);

			if(keys == null || keys.size() <= 0) {
				new WarningWindow(core, "No key found !");
				return;
			}


			String result = fileList.getText();

			for(Iterator i = keys.iterator(); i.hasNext() ;) {
				String key = (String)i.next();

				result = result + key + "\n";
			}
			
			fileList.setText(result);

		}
	}
}

