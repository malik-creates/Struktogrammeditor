package de.kekru.struktogrammeditor.view;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

import de.kekru.struktogrammeditor.control.Controlling;
import de.kekru.struktogrammeditor.control.GlobalSettings;
import de.kekru.struktogrammeditor.control.Struktogramm;


public class FontChooser extends JDialog {
	//siehe: http://www.java2s.com/Code/Java/Tiny-Application/FontChooser.htm

	private static final long serialVersionUID = -7360108534182191037L;
	private String[] schriftarten;
	private JComboBox<String> schriftartenChooser;
	private JComboBox<String> styleChooser;
	private JComboBox<String> sizeChooser;
	private JLabel vorschau = new JLabel("Anweisung   if (x > 0)   Ja / Nein", SwingConstants.CENTER);
	private Controlling controlling;


	// The names to appear in the "Style" menu
	static final String[] styleNames = new String[] { "Normal", "Kursiv",
		"Fett", "Fett und Kursiv" };

	// The style values that correspond to those names
	static final Integer[] styleValues = new Integer[] {
		Integer.valueOf(Font.PLAIN), Integer.valueOf(Font.ITALIC),
		Integer.valueOf(Font.BOLD), Integer.valueOf(Font.BOLD + Font.ITALIC) };

	// The size "names" to appear in the size menu
	static final String[] sizeNames = new String[] { "8", "10", "12", "13", "14", "15", "16",
		"18", "20", "24", "28", "32", "40", "48", "56", "64", "72" };

	public FontChooser(Controlling controlling, boolean modal) {

		super(controlling.getGUI(), "Schriftart ändern", modal);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		this.controlling = controlling;

		schriftarten = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();

		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(0, 0, 4, 8);
		c.anchor = GridBagConstraints.WEST;
		c.gridy = 0;
		c.gridx = 0;
		panel.add(new JLabel("Schriftart"), c);
		c.gridx = 1;
		panel.add(new JLabel("Stil"), c);
		c.gridx = 2;
		c.insets = new Insets(0, 0, 4, 0);
		panel.add(new JLabel("Größe"), c);

		c.gridy = 1;
		c.gridx = 0;
		c.insets = new Insets(0, 0, 0, 8);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		schriftartenChooser = new JComboBox<String>(schriftarten);
		schriftartenChooser.setMaximumRowCount(16);
		panel.add(schriftartenChooser, c);
		c.gridx = 1;
		c.weightx = 0;
		styleChooser = new JComboBox<String>(styleNames);
		panel.add(styleChooser, c);
		c.gridx = 2;
		c.insets = new Insets(0, 0, 0, 0);
		sizeChooser = new JComboBox<String>(sizeNames);
		panel.add(sizeChooser, c);

		//Vorschau auf weißem Grund wie im Struktogramm
		c.gridy = 2;
		c.gridx = 0;
		c.gridwidth = 3;
		c.insets = new Insets(14, 0, 0, 0);
		vorschau.setOpaque(true);
		vorschau.setBackground(Color.white);
		vorschau.setForeground(Color.black);
		vorschau.setBorder(BorderFactory.createLineBorder(Color.lightGray));
		vorschau.setPreferredSize(new Dimension(460, 80));
		panel.add(vorschau, c);
		add(panel, BorderLayout.CENTER);

		fontAufChooser(controlling.gibAktuellesStruktogramm().getFontStr());

		ActionListener vorschauAktualisieren = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				vorschau.setFont(gewaehlteSchrift());
			}
		};
		schriftartenChooser.addActionListener(vorschauAktualisieren);
		styleChooser.addActionListener(vorschauAktualisieren);
		sizeChooser.addActionListener(vorschauAktualisieren);
		vorschau.setFont(gewaehlteSchrift());


		JButton buttonZuruecksetzen = new JButton("Standard");
		buttonZuruecksetzen.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				buttonZuruecksetzen_ActionPerformed(evt);
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
		links.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
		links.add(buttonZuruecksetzen);
		unten.add(links, BorderLayout.WEST);
		unten.add(Design.buttonLeiste(buttonOK, buttonAbbrechen), BorderLayout.EAST);
		add(unten, BorderLayout.SOUTH);

		Design.dialogEinrichten(this, buttonOK);
		pack();
		setLocationRelativeTo(controlling.getGUI());
		setVisible(true); //alles nach setVisible(true) wird erst beim Schließen des Dialogs gemacht
	}


	private Font gewaehlteSchrift(){
		return new Font(schriftartenChooser.getSelectedItem().toString(), styleValues[styleChooser.getSelectedIndex()], Integer.parseInt(sizeNames[sizeChooser.getSelectedIndex()]));
	}


	private void fontAufChooser(Font font){

		int pos = 0;
		String schriftart = font.getFamily();
		for(int i=0; i < schriftarten.length; i++){
			if(schriftarten[i].toLowerCase().equals(schriftart.toLowerCase())){
				pos = i;
				break;
			}
		}

		schriftartenChooser.setSelectedIndex(pos);

		pos = 0;
		int a = font.getStyle();
		for(int i=0; i < styleValues.length; i++){
			if(styleValues[i] == a){
				pos = i;
				break;
			}
		}

		styleChooser.setSelectedIndex(pos);

		pos = 0;
		a = font.getSize();
		for(int i=0; i < sizeNames.length; i++){
			if(sizeNames[i].equals(""+a)){
				pos = i;
				break;
			}
		}

		sizeChooser.setSelectedIndex(pos);
	}


	public void buttonOK_ActionPerformed(ActionEvent evt) {
		Struktogramm str = controlling.gibAktuellesStruktogramm();
		str.setFontStr(gewaehlteSchrift());
		str.rueckgaengigPunktSetzen(true);
		str.graphicsInitialisieren();
		str.zeichenbereichAktualisieren();
		str.zeichne();
		dispose();
	}


	public void buttonAbbrechen_ActionPerformed(ActionEvent evt) {
		dispose();
	}


	public void buttonZuruecksetzen_ActionPerformed(ActionEvent evt) {
		fontAufChooser(GlobalSettings.fontStandard);
	}


}
