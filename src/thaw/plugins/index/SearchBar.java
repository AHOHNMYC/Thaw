package thaw.plugins.index;

import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JLabel;

import java.awt.BorderLayout;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import thaw.core.I18n;

import thaw.plugins.Hsqldb;

public class SearchBar implements ActionListener {
	private JPanel panel;

	private JTextField userText;
	private JButton validationButton;

	private Hsqldb db;
	private IndexTree tree;

	private Tables tables;

	public SearchBar(Hsqldb db, IndexTree indexTree, Tables tables) {
		this.db = db;
		this.tree = indexTree;
		this.tables = tables;

		panel = new JPanel();
		panel.setLayout(new BorderLayout(10, 10));

		userText = new JTextField("");
		validationButton = new JButton("  "+I18n.getMessage("thaw.common.search")+"  ");

		panel.add(new JLabel(I18n.getMessage("thaw.plugin.index.search.label")), BorderLayout.WEST);
		panel.add(userText, BorderLayout.CENTER);
		panel.add(validationButton, BorderLayout.EAST);

		userText.addActionListener(this);
		validationButton.addActionListener(this);
	}

	public JPanel getPanel() {
		return panel;
	}

	public void actionPerformed(ActionEvent e) {
		if (userText.getText() == null)
			return;

		if (tree.getSelectedNode() == null)
			return;

		SearchResult sr = new SearchResult(db, userText.getText(), tree.getSelectedNode());
		tables.setList(sr);
	}

}
