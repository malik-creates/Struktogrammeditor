package de.kekru.struktogrammeditor.view;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;

import de.kekru.struktogrammeditor.control.GlobalSettings;
import de.kekru.struktogrammeditor.other.JListEasy;
import de.kekru.struktogrammeditor.other.JTextAreaEasy;
import de.kekru.struktogrammeditor.struktogrammelemente.Fallauswahl;
import de.kekru.struktogrammeditor.struktogrammelemente.StruktogrammElement;
import de.kekru.struktogrammeditor.struktogrammelemente.Verzweigung;
import de.kekru.struktogrammeditor.view.Symbole.FarbIcon;


public class EingabeDialog extends JDialog {

	private static final long serialVersionUID = -7385908673937166978L;
	private JTextAreaEasy textarea;
	private JListEasy list;
	private String[] rueckgabeInhalt;
	private StruktogrammElement element;
	private boolean okWurdeGedrueckt = false;
	private Color schriftfarbeNeu, hintergrundfarbeNeu;
	private final FarbIcon iconSchriftfarbe, iconHintergrundfarbe;
	private final JButton buttonSchriftfarbe, buttonHintergrundfarbe;


	public EingabeDialog(JFrame owner, String title, boolean modal, StruktogrammElement element) {

		super(owner, title, modal);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		this.element = element;

		schriftfarbeNeu = element.getFarbeSchrift();
		hintergrundfarbeNeu = element.getFarbeHintergrund();

		JPanel inhalt = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(0, 0, 4, 0);

		inhalt.add(Design.ueberschrift("Text"), c);

		c.gridy++;
		c.weighty = 1;
		c.fill = GridBagConstraints.BOTH;
		c.insets = new Insets(0, 0, 4, 0);
		textarea = new JTextAreaEasy();
		textarea.setRows(5);
		inhalt.add(new JScrollPane(textarea), c);

		c.gridy++;
		c.weighty = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(0, 0, 12, 0);
		inhalt.add(Design.hinweis("Jede Zeile wird eine eigene Textzeile.  Strg+Enter = OK"), c);

		String[] vorherigerInhalt = element.gibText();

		for (int i=0; i < vorherigerInhalt.length; i++){
			textarea.hinzufuegen(vorherigerInhalt[i]);

			if (i != vorherigerInhalt.length -1){//nach der letzten Zeile soll kein Zeilenumbruch passieren, damit man direkt in der letzten Zeile weiterschreiben kann
				textarea.zeilenumbruch();
			}
		}

		rueckgabeInhalt = vorherigerInhalt;


		int anzahlListen = element.gibAnzahlListen();

		if (anzahlListen > 0){
			//Element ist Verzweigung oder Fallauswahl, anzahlListen ist für Schleifen 0, weil diese keine Überschrift brauchen
			//JListEasy erzeugen, die die Fallnamen, oder bei einer Verzweigung "Ja" und "Nein" enthält, um diese umbenennen zu können

			c.gridy++;
			c.insets = new Insets(0, 0, 4, 0);
			inhalt.add(Design.ueberschrift(element instanceof Verzweigung ? "Beschriftungen links und rechts" : "Beschriftungen der Fälle"), c);

			c.gridy++;
			c.weighty = 0.6;
			c.fill = GridBagConstraints.BOTH;
			list = new JListEasy();
			list.setVisibleRowCount(Math.min(anzahlListen, 5));
			inhalt.add(new JScrollPane(list), c);

			String[] inhaltVorher = element.gibFaelle();
			for(int i=0; i < anzahlListen; i++){
				list.fuegeHinzu(inhaltVorher[i]);
			}
			list.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					if (e.getClickCount() == 2){
						buttonFallname_ActionPerformed(null);
					}
				}
			});

			c.gridy++;
			c.weighty = 0;
			c.fill = GridBagConstraints.NONE;
			c.insets = new Insets(4, 0, 12, 0);
			JPanel zeile = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
			JButton button = new JButton("Umbenennen...");
			button.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					buttonFallname_ActionPerformed(e);
				}
			});
			zeile.add(button);
			JLabel tipp = Design.hinweis("   oder Doppelklick auf einen Eintrag");
			zeile.add(tipp);
			inhalt.add(zeile, c);
		}


		c.gridy++;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(0, 0, 4, 0);
		inhalt.add(Design.ueberschrift("Farben"), c);

		c.gridy++;
		c.insets = new Insets(0, 0, 0, 0);
		JPanel farben = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		buttonSchriftfarbe = new JButton("Schrift", iconSchriftfarbe = new FarbIcon(schriftfarbeNeu));
		buttonSchriftfarbe.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				buttonSchriftfarbeGeklickt();
			}
		});
		farben.add(buttonSchriftfarbe);
		farben.add(javax.swing.Box.createHorizontalStrut(8));
		buttonHintergrundfarbe = new JButton("Hintergrund", iconHintergrundfarbe = new FarbIcon(hintergrundfarbeNeu));
		buttonHintergrundfarbe.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				buttonHintergrundfarbeGeklickt();
			}
		});
		farben.add(buttonHintergrundfarbe);
		inhalt.add(farben, c);

		add(inhalt, BorderLayout.CENTER);


		JButton buttonOK = new JButton("OK");
		buttonOK.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				buttonOK_ActionPerformed(e);
			}
		});

		JButton buttonAbbrechen = new JButton("Abbrechen");
		buttonAbbrechen.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				buttonAbbrechen_ActionPerformed(e);
			}
		});
		add(Design.buttonLeiste(buttonOK, buttonAbbrechen), BorderLayout.SOUTH);

		//Strg+Enter bestätigt, normales Enter erzeugt in der Textarea eine neue Zeile
		textarea.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK), "bestaetigen");
		textarea.getActionMap().put("bestaetigen", new AbstractAction() {
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent e) {
				buttonOK_ActionPerformed(e);
			}
		});

		Design.dialogEinrichten(this, null);
		pack();
		setSize(Math.max(getWidth(), 460), Math.max(getHeight(), 300));
		setMinimumSize(new Dimension(360, 260));
		setLocationRelativeTo(owner);
		//noch unveränderte Startbeschriftung markieren, damit sie direkt überschrieben werden kann
		boolean istStartbeschriftung = false;
		for (int i = 0; i < EinstellungsDialog.anzahlStruktogrammElemente; i++){
			if (textarea.getText().equals(GlobalSettings.gibElementBeschriftung(i))){
				istStartbeschriftung = true;
			}
		}
		if (istStartbeschriftung){
			textarea.selectAll();
		}else{
			textarea.setCaretPosition(textarea.getDocument().getLength());
		}
		textarea.requestFocusInWindow();
		setVisible(true); //alles nach setVisible(true) wird erst beim Schließen des Dialogs gemacht
	}


	//OK-Button wurde gedrückt
	public void buttonOK_ActionPerformed(ActionEvent evt) {
		if (element instanceof Fallauswahl){//Element ist vom Typ Fallauswahl oder Verzweigung

			//alle Zeilen der JListEasy auslesen und in ein String-Array schreiben
			String[] fallBezeichnungen = new String[list.gibAnzahl()];
			for (int i=0; i < fallBezeichnungen.length; i++){
				fallBezeichnungen[i] = list.gibInhalt(i);
			}

			element.setzeFaelle(fallBezeichnungen);//Fallüberschriften setzen
		}


		element.setFarbeSchrift(schriftfarbeNeu);
		element.setFarbeHintergrund(hintergrundfarbeNeu);

		rueckgabeInhalt = textarea.gibTextzeilenArray();

		okWurdeGedrueckt = true;

		dispose();
	}


	private void buttonSchriftfarbeGeklickt(){
		schriftfarbeNeu = ColorDialog.showColorChooser(this, schriftfarbeNeu);
		aktualisiereButtonfarben();
	}

	private void buttonHintergrundfarbeGeklickt(){
		hintergrundfarbeNeu = ColorDialog.showColorChooser(this, hintergrundfarbeNeu);
		aktualisiereButtonfarben();
	}

	//Abbrechen-Button wurde gedrückt
	public void buttonAbbrechen_ActionPerformed(ActionEvent evt) {
		dispose();
	}


	//Button zum Ändern der Fallnamen wurde gedrückt
	public void buttonFallname_ActionPerformed(ActionEvent evt) {
		if(list.gibIndex() >= 0){

			String fallname = (String)JOptionPane.showInputDialog(this, "Neue Beschriftung:", "Umbenennen", JOptionPane.PLAIN_MESSAGE, null, null, list.gibMarkiertenInhalt());//User nach neuen Fallnamen fragen

			if (fallname != null){
				if (fallname.equals("")){
					fallname = " ";//wenn der Fallname ein leerer String ist, daraus einen String mit einem Leerzeichen machen, ansonsten ist die Zeile nur noch sehr klein in der Liste (von der Höhe her), wenn nichts drin steht
				}
				list.setzeText(fallname,list.gibIndex());//Fallname setzen
			}

		}else{//es ist noch kein Fall in der JListEasy ausgewählt
			JOptionPane.showMessageDialog(this, "Bitte zuerst eine Beschriftung in der Liste auswählen.", "Auswählen", JOptionPane.INFORMATION_MESSAGE);
		}
	}



	//wenn zuvor der OK-Button gedrückt wurde, wird der Inhalt der TextArea zurückgegeben, ansonsten null (dann soll nichts verändert werden, weil der User Abbrechen oder das Schließen-X angeklickt hat)
	public String[] gibTextArray(){
		return okWurdeGedrueckt ? rueckgabeInhalt : null;
	}

	public Color getSchriftfarbeNeu() {
		return schriftfarbeNeu;
	}

	public Color getHintergrundfarbeNeu() {
		return hintergrundfarbeNeu;
	}


	private void aktualisiereButtonfarben(){
		iconSchriftfarbe.setFarbe(schriftfarbeNeu);
		iconHintergrundfarbe.setFarbe(hintergrundfarbeNeu);
		buttonSchriftfarbe.repaint();
		buttonHintergrundfarbe.repaint();
	}

}
