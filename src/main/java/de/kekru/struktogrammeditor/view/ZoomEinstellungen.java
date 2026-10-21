package de.kekru.struktogrammeditor.view;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import de.kekru.struktogrammeditor.control.GlobalSettings;
import de.kekru.struktogrammeditor.other.JNumberField;

public class ZoomEinstellungen extends JDialog {
	private static final long serialVersionUID = -4780523744293396039L;
	private JNumberField numberfieldX, numberfieldY;

	public ZoomEinstellungen(GUI gui){
		super(gui, "Zoomeinstellungen", true);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		JPanel kopf = new JPanel(new BorderLayout());
		kopf.add(Design.ueberschrift("Vergrößern und Verkleinern von Elementen"), BorderLayout.NORTH);
		kopf.add(Design.hinweis("<html>Um wie viele Pixel ein Element pro Schritt (Mausrad, +/-, Rechtsklick → Zoom)<br>breiter bzw. höher wird.</html>"), BorderLayout.SOUTH);
		kopf.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 12, 0));
		add(kopf, BorderLayout.NORTH);

		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;
		c.insets = new Insets(3, 0, 3, 12);

		c.gridx = 0; c.gridy = 0;
		panel.add(new JLabel("Waagerecht (Pixel):"), c);
		c.gridx = 1;
		panel.add(numberfieldX = new JNumberField(), c);
		numberfieldX.setColumns(5);
		numberfieldX.setInt(GlobalSettings.getXZoomProSchritt());

		c.gridx = 0; c.gridy = 1;
		panel.add(new JLabel("Senkrecht (Pixel):"), c);
		c.gridx = 1;
		panel.add(numberfieldY = new JNumberField(), c);
		numberfieldY.setColumns(5);
		numberfieldY.setInt(GlobalSettings.getYZoomProSchritt());

		c.gridx = 2; c.weightx = 1;
		panel.add(new JLabel(), c);
		add(panel, BorderLayout.CENTER);

		JButton buttonOK = new JButton("OK");
		buttonOK.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				okGeklickt();
			}
		});
		JButton buttonAbbrechen = new JButton("Abbrechen");
		buttonAbbrechen.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				abbrechenGeklickt();
			}
		});
		add(Design.buttonLeiste(buttonOK, buttonAbbrechen), BorderLayout.SOUTH);

		Design.dialogEinrichten(this, buttonOK);
		pack();
		setLocationRelativeTo(gui);
		setVisible(true);
	}

	private void okGeklickt(){
		if(numberfieldX.isNumeric() && numberfieldY.isNumeric()){
			GlobalSettings.setXZoomProSchritt(numberfieldX.getInt());
			GlobalSettings.setYZoomProSchritt(numberfieldY.getInt());
			GlobalSettings.saveSettings();
			dispose();
		}
	}

	private void abbrechenGeklickt(){
		dispose();
	}
}
