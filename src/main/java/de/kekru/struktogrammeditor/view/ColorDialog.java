package de.kekru.struktogrammeditor.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class ColorDialog extends JDialog {

	private static final long serialVersionUID = 5666531548056491207L;
	private static final JColorChooser colorChooser = new JColorChooser(); //static und final -> nur einmal erzeugen, damit die Farben im Chooser bleiben
	private boolean okGeklickt = false;
	
	private ColorDialog(Dialog owner){

		super(owner, "Farbauswahl", true);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

		setLayout(new BorderLayout());
		add(colorChooser, BorderLayout.CENTER);

		JButton buttonOK = new JButton("OK");
		buttonOK.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				okButtonGeklickt();
			}
		});
		JButton buttonAbbrechen = new JButton("Abbrechen");
		buttonAbbrechen.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				abbrechenButtonGeklickt();
			}
		});

		JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
		panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 0, 0, 0));
		{
			JButton button = new JButton("Schwarz", new Symbole.FarbIcon(Color.black));
			button.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					colorChooser.setColor(Color.black);
				}
			});
			panel.add(button);
			
			button = new JButton("Weiß", new Symbole.FarbIcon(Color.white));
			button.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					colorChooser.setColor(Color.white);
				}
			});
			panel.add(button);
		}
		JPanel unten = new JPanel(new BorderLayout());
		unten.add(panel, BorderLayout.WEST);
		unten.add(Design.buttonLeiste(buttonOK, buttonAbbrechen), BorderLayout.EAST);
		add(unten, BorderLayout.SOUTH);

		Design.dialogEinrichten(this, buttonOK);
		pack();
		setLocationRelativeTo(owner);
		setVisible(true);


	}
	
	
	private void okButtonGeklickt(){
		okGeklickt = true;
		dispose();
	}
	
	
	private void abbrechenButtonGeklickt(){
		dispose();
	}
	
	public boolean isOkGeklickt() {
		return okGeklickt;
	}


	public static Color showColorChooser(Dialog owner, Color startFarbe){
		if(startFarbe != null){
			colorChooser.setColor(startFarbe);
		}
		
		ColorDialog colorDialog = new ColorDialog(owner);
		
		return colorDialog.isOkGeklickt() ? colorChooser.getColor() : startFarbe;		
	}	

}
