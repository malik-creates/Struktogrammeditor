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
import javax.swing.JTextField;
import javax.swing.WindowConstants;

import de.kekru.struktogrammeditor.control.GlobalSettings;
import de.kekru.struktogrammeditor.view.Symbole.ElementIcon;


public class EinstellungsDialog extends JDialog{

	private static final long serialVersionUID = -6402017961524470279L;
	public static final int anzahlStruktogrammElemente = 10;
	private JTextField[] textfields = new JTextField[anzahlStruktogrammElemente];
	private final String[] beschreibungen = GlobalSettings.getCurrentElementBeschriftungsstil();
	public static final String[] standardWerte = {"Anweisung",
		"Verzweigung",
		"Fallauswahl",
		"0 < i < anzahl",
		"While Schleife",
		"Do-While Schleife",
		"Endlosschleife",
		"Aussprung",
		"Aufruf",
	"ø"};



	public EinstellungsDialog(GUI gui, boolean modal) {

		super(gui, "Startbeschriftungen", modal);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		JPanel kopf = new JPanel(new BorderLayout());
		kopf.add(Design.ueberschrift("Text von neu eingefügten Elementen"), BorderLayout.NORTH);
		kopf.add(Design.hinweis("Dieser Text steht in einem Element, sobald es ins Struktogramm gezogen wird."), BorderLayout.SOUTH);
		kopf.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 12, 0));
		add(kopf, BorderLayout.NORTH);

		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(3, 0, 3, 12);
		c.anchor = GridBagConstraints.WEST;

		for(int i=0; i < anzahlStruktogrammElemente; i++){
			c.gridy = i;
			c.gridx = 0;
			c.weightx = 0;
			c.fill = GridBagConstraints.NONE;
			JLabel label = new JLabel(beschreibungen[i]);
			if (i < 9){
				label.setIcon(new ElementIcon(i));
				label.setIconTextGap(10);
			}
			panel.add(label, c);

			c.gridx = 1;
			c.weightx = 1;
			c.fill = GridBagConstraints.HORIZONTAL;
			c.insets = new Insets(3, 0, 3, 0);
			textfields[i] = new JTextField(GlobalSettings.gibElementBeschriftung(i), 18);
			panel.add(textfields[i], c);
			c.insets = new Insets(3, 0, 3, 12);
		}
		add(panel, BorderLayout.CENTER);


		JButton buttonStandardWerte = new JButton("Standardwerte");
		buttonStandardWerte.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				buttonStandardWerte_ActionPerformed(evt);
			}
		});
		JButton buttonOK = new JButton("OK");
		buttonOK.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				buttonOK_ActionPerformed(evt);
			}
		});
		JButton buttonAbbrechen = new JButton("Abbrechen");
		buttonAbbrechen.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				buttonAbbrechen_ActionPerformed(evt);
			}
		});

		JPanel unten = new JPanel(new BorderLayout());
		JPanel links = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 0));
		links.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 0, 0, 0));
		links.add(buttonStandardWerte);
		unten.add(links, BorderLayout.WEST);
		unten.add(Design.buttonLeiste(buttonOK, buttonAbbrechen), BorderLayout.EAST);
		add(unten, BorderLayout.SOUTH);

		Design.dialogEinrichten(this, buttonOK);
		pack();
		setLocationRelativeTo(gui);
		setVisible(true); //alles nach setVisible(true) wird erst beim Schließen des Dialogs gemacht
	}



	public void buttonOK_ActionPerformed(ActionEvent evt) {
		String[] neueWerte = new String[anzahlStruktogrammElemente];

		for(int i=0; i < anzahlStruktogrammElemente; i++){
			neueWerte[i] = textfields[i].getText();
		}

		GlobalSettings.setzeElementBeschriftungen(neueWerte);
		GlobalSettings.saveSettings();
		dispose();
	}


	public void buttonAbbrechen_ActionPerformed(ActionEvent evt) {
		dispose();
	}

	public void buttonStandardWerte_ActionPerformed(ActionEvent evt) {
		for(int i=0; i < anzahlStruktogrammElemente; i++){
			textfields[i].setText(standardWerte[i]);
		}
	}

}
