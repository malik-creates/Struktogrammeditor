package de.kekru.struktogrammeditor.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;

import de.kekru.struktogrammeditor.codeimport.CodeImporter;
import de.kekru.struktogrammeditor.codeimport.CodeImporter.Sprache;
import de.kekru.struktogrammeditor.codeimport.CodeParseException;
import de.kekru.struktogrammeditor.codeimport.Diagramm;
import de.kekru.struktogrammeditor.control.Controlling;
import de.kekru.struktogrammeditor.control.Struktogramm;
import de.kekru.struktogrammeditor.control.XMLLeser;

//JDialog, in den Quellcode eingefügt wird, aus dem dann Struktogramme erzeugt werden (Gegenstück zum CodeErzeuger);
//rechts wird schon beim Tippen eine Vorschau angezeigt
public class CodeImportDialog extends JDialog {

	private static final long serialVersionUID = 1L;

	//Eingaben bleiben erhalten, solange das Programm läuft
	private static String letzterCode = "";
	private static Sprache letzteSprache = Sprache.AUTOMATISCH;
	private static boolean letztesSemikolonBehalten = false;
	private static File letzterOrdner = null;

	private final Controlling controlling;
	private final JTextArea textarea = new JTextArea(letzterCode, 25, 60);
	private final JComboBox<Sprache> comboSprache = new JComboBox<Sprache>(Sprache.values());
	private final JCheckBox checkboxSemikolon = new JCheckBox("Semikolons behalten");
	private final JButton buttonErzeugen = new JButton("Struktogramm erzeugen", new Symbole.SymbolIcon(Symbole.Art.CODE_EXPORT, 16));

	//Vorschau
	private final JComboBox<String> comboDiagramm = new JComboBox<String>();
	private final VorschauBild vorschauBild = new VorschauBild();
	private final JLabel labelStatus = new JLabel(" ");
	private final Timer verzoegerung;
	private List<Diagramm> aktuelleDiagramme = new ArrayList<Diagramm>();
	private Struktogramm vorschauStruktogramm;
	private boolean comboWirdGefuellt = false;
	private Object fehlerMarkierung;

	public CodeImportDialog(Controlling controlling){
		super(controlling.getGUI(), "Struktogramm aus Code erzeugen", true);
		this.controlling = controlling;
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		//Optionen
		JPanel optionen = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		optionen.add(new JLabel("Sprache:  "));
		comboSprache.setSelectedItem(letzteSprache);
		optionen.add(comboSprache);
		optionen.add(javax.swing.Box.createHorizontalStrut(16));
		checkboxSemikolon.setSelected(letztesSemikolonBehalten);
		optionen.add(checkboxSemikolon);
		optionen.add(javax.swing.Box.createHorizontalStrut(16));
		JButton buttonDatei = new JButton("Datei laden...", new Symbole.SymbolIcon(Symbole.Art.OEFFNEN, 16));
		buttonDatei.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dateiLaden();
			}
		});
		optionen.add(buttonDatei);
		optionen.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
		add(optionen, BorderLayout.NORTH);

		//links: Code
		JPanel links = new JPanel(new BorderLayout(0, 6));
		links.add(Design.hinweis("<html>Code hier einfügen (Strg+V). Jede Methode/Funktion wird ein eigenes Struktogramm.</html>"), BorderLayout.NORTH);
		textarea.setFont(new Font("Monospaced", Font.PLAIN, 14));
		textarea.setTabSize(4);
		links.add(new JScrollPane(textarea), BorderLayout.CENTER);

		//rechts: Vorschau
		JPanel rechts = new JPanel(new BorderLayout(0, 6));
		JPanel kopf = new JPanel(new BorderLayout(8, 0));
		kopf.add(Design.ueberschrift("Vorschau"), BorderLayout.WEST);
		comboDiagramm.setVisible(false);
		kopf.add(comboDiagramm, BorderLayout.CENTER);
		rechts.add(kopf, BorderLayout.NORTH);
		vorschauBild.setBorder(BorderFactory.createLineBorder(new Color(0, 0, 0, 30)));
		rechts.add(vorschauBild, BorderLayout.CENTER);
		rechts.add(labelStatus, BorderLayout.SOUTH);
		rechts.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 0));

		JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, links, rechts);
		split.setResizeWeight(0.5);
		split.setBorder(BorderFactory.createEmptyBorder());
		split.setContinuousLayout(true);
		add(split, BorderLayout.CENTER);

		//Buttons
		buttonErzeugen.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				erzeugen();
			}
		});
		JButton buttonAbbrechen = new JButton("Abbrechen");
		buttonAbbrechen.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				eingabenMerken();
				dispose();
			}
		});
		add(Design.buttonLeiste(buttonErzeugen, buttonAbbrechen), BorderLayout.SOUTH);

		//Vorschau kurz nach dem letzten Tastendruck aktualisieren
		verzoegerung = new Timer(350, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				vorschauAktualisieren();
			}
		});
		verzoegerung.setRepeats(false);
		textarea.getDocument().addDocumentListener(new DocumentListener() {
			public void insertUpdate(DocumentEvent e) { verzoegerung.restart(); }
			public void removeUpdate(DocumentEvent e) { verzoegerung.restart(); }
			public void changedUpdate(DocumentEvent e) { verzoegerung.restart(); }
		});
		ActionListener sofort = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				vorschauAktualisieren();
			}
		};
		comboSprache.addActionListener(sofort);
		checkboxSemikolon.addActionListener(sofort);
		comboDiagramm.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!comboWirdGefuellt){
					diagrammAnzeigen();
				}
			}
		});

		Design.dialogEinrichten(this, null);
		setSize(1180, 720);
		setMinimumSize(new Dimension(760, 460));
		setLocationRelativeTo(controlling.getGUI());
		vorschauAktualisieren();
		setVisible(true);
	}


	@Override
	public void dispose(){
		verzoegerung.stop();
		super.dispose();
	}


	private void eingabenMerken(){
		letzterCode = textarea.getText();
		letzteSprache = (Sprache)comboSprache.getSelectedItem();
		letztesSemikolonBehalten = checkboxSemikolon.isSelected();
	}


	//Code einlesen und Vorschau, Statuszeile und Fehlermarkierung aktualisieren
	private void vorschauAktualisieren(){
		verzoegerung.stop();
		fehlerMarkierungEntfernen();
		String code = textarea.getText();

		if (code.trim().isEmpty()){
			aktuelleDiagramme = new ArrayList<Diagramm>();
			vorschauBild.setBild(null, "Hier erscheint das Struktogramm, sobald links Code steht.");
			status("", false);
			diagrammAuswahlFuellen();
			return;
		}

		try{
			Sprache sprache = (Sprache)comboSprache.getSelectedItem();
			aktuelleDiagramme = CodeImporter.importieren(code, sprache, checkboxSemikolon.isSelected());
			diagrammAuswahlFuellen();

			if (aktuelleDiagramme.isEmpty()){
				vorschauBild.setBild(null, "Im Code wurden keine Anweisungen gefunden.");
				status("", false);
			}else{
				diagrammAnzeigen();
				String erkannt = sprache == Sprache.AUTOMATISCH ? "Erkannt: " + CodeImporter.erkennen(code) + "   ·   " : "";
				status(erkannt + (aktuelleDiagramme.size() == 1 ? "1 Struktogramm" : aktuelleDiagramme.size() + " Struktogramme"), false);
			}

		}catch(CodeParseException e){
			status("Fehler: " + e.getMessage(), true);
			fehlerMarkieren(e.getZeile());
			if (vorschauBild.hatBild()){
				vorschauBild.setVeraltet(true); //letzte gültige Vorschau bleibt blass sichtbar
			}else{
				vorschauBild.setBild(null, "Der Code enthält einen Fehler (siehe unten).");
			}

		}catch(RuntimeException e){
			e.printStackTrace();
			status("Fehler: " + e, true);
		}

		buttonErzeugen.setText(aktuelleDiagramme.size() > 1 ? aktuelleDiagramme.size() + " Struktogramme erzeugen" : "Struktogramm erzeugen");
	}


	private void diagrammAuswahlFuellen(){
		comboWirdGefuellt = true;
		int vorher = comboDiagramm.getSelectedIndex();
		comboDiagramm.removeAllItems();
		for (int i = 0; i < aktuelleDiagramme.size(); i++){
			String name = aktuelleDiagramme.get(i).getName();
			comboDiagramm.addItem(name.isEmpty() ? "Struktogramm " + (i + 1) : name);
		}
		if (vorher >= 0 && vorher < aktuelleDiagramme.size()){
			comboDiagramm.setSelectedIndex(vorher);
		}
		comboDiagramm.setVisible(aktuelleDiagramme.size() > 1);
		comboWirdGefuellt = false;
	}


	//ausgewähltes Diagramm in ein unsichtbares Struktogramm laden und als Bild in Bildschirmauflösung erzeugen
	private void diagrammAnzeigen(){
		int index = Math.max(0, comboDiagramm.getSelectedIndex());
		if (index >= aktuelleDiagramme.size()){
			return;
		}
		if (vorschauStruktogramm == null){
			vorschauStruktogramm = new Struktogramm(null);
		}
		new XMLLeser().ladeXLM(aktuelleDiagramme.get(index).alsDocument(), vorschauStruktogramm);
		vorschauStruktogramm.zeichenbereichAktualisieren();
		double skalierung = getGraphicsConfiguration() != null ? getGraphicsConfiguration().getDefaultTransform().getScaleX() : 1.0;
		vorschauBild.setSkalierung(skalierung);
		vorschauBild.setBild(vorschauStruktogramm.generateImage(true, skalierung), null);
	}


	private void status(String text, boolean fehler){
		labelStatus.setText(text.isEmpty() ? " " : text);
		labelStatus.setForeground(fehler ? new Color(0xD93025) : UIManager.getColor("Label.disabledForeground"));
	}

	private void fehlerMarkieren(int zeile){
		if (zeile <= 0){
			return;
		}
		try{
			int z = Math.min(zeile, textarea.getLineCount()) - 1;
			fehlerMarkierung = textarea.getHighlighter().addHighlight(textarea.getLineStartOffset(z), textarea.getLineEndOffset(z),
					new DefaultHighlighter.DefaultHighlightPainter(new Color(255, 80, 80, 70)));		}catch(BadLocationException e){
			//keine Markierung
		}
	}

	private void fehlerMarkierungEntfernen(){
		if (fehlerMarkierung != null){
			textarea.getHighlighter().removeHighlight(fehlerMarkierung);
			fehlerMarkierung = null;
		}
	}


	private void erzeugen(){
		eingabenMerken();

		String code = textarea.getText();
		if (code.trim().isEmpty()){
			JOptionPane.showMessageDialog(this, "Bitte zuerst Code einfügen.", "Kein Code", JOptionPane.INFORMATION_MESSAGE);
			return;
		}

		try{
			List<Diagramm> diagramme = CodeImporter.importieren(code, letzteSprache, letztesSemikolonBehalten);

			if (diagramme.isEmpty()){
				JOptionPane.showMessageDialog(this, "Im Code wurden keine Anweisungen gefunden.", "Nichts gefunden", JOptionPane.INFORMATION_MESSAGE);
				return;
			}

			dispose();
			controlling.struktogrammeAusCodeErzeugen(diagramme);

		}catch(CodeParseException e){
			if (e.getZeile() > 0){
				try{
					textarea.setCaretPosition(textarea.getLineStartOffset(Math.min(e.getZeile(), textarea.getLineCount()) - 1));
				}catch(BadLocationException e1){
					//Cursor bleibt, wo er ist
				}
			}
			JOptionPane.showMessageDialog(this, "Der Code konnte nicht gelesen werden.\n" + e.getMessage(), "Fehler im Code", JOptionPane.ERROR_MESSAGE);
			textarea.requestFocusInWindow();

		}catch(RuntimeException e){
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, "Der Code konnte nicht gelesen werden.\n" + e, "Fehler", JOptionPane.ERROR_MESSAGE);
		}
	}


	private void dateiLaden(){
		JFileChooser chooser = new JFileChooser(letzterOrdner);
		chooser.setFileFilter(new FileNameExtensionFilter("Quellcode (java, c, cpp, cs, js, ts, py, pas, dpr, pp, txt)",
				"java", "c", "h", "cpp", "hpp", "cc", "cs", "js", "ts", "py", "pyw", "pas", "dpr", "pp", "lpr", "txt"));

		if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION){
			return;
		}

		File datei = chooser.getSelectedFile();
		letzterOrdner = datei.getParentFile();

		try{
			byte[] bytes = Files.readAllBytes(datei.toPath());
			String inhalt = new String(bytes, Charset.forName("UTF-8"));
			if (inhalt.indexOf('�') >= 0){ //kein gültiges UTF-8, dann Windows-Zeichensatz annehmen
				inhalt = new String(bytes, Charset.forName("windows-1252"));
			}
			if (inhalt.startsWith("﻿")){
				inhalt = inhalt.substring(1);
			}

			String name = datei.getName().toLowerCase();
			if (name.matches(".*\\.pyw?$")){
				comboSprache.setSelectedItem(Sprache.PYTHON);
			}else if (name.matches(".*\\.(pas|dpr|pp|lpr)$")){
				comboSprache.setSelectedItem(Sprache.PASCAL);
			}else if (name.matches(".*\\.(java|c|h|cpp|hpp|cc|cs|js|ts)$")){
				comboSprache.setSelectedItem(Sprache.CLIKE);
			}
			textarea.setText(inhalt);
			textarea.setCaretPosition(0);
			vorschauAktualisieren();
		}catch(IOException e){
			JOptionPane.showMessageDialog(this, "Die Datei konnte nicht gelesen werden:\n" + e.getMessage(), "Fehler", JOptionPane.ERROR_MESSAGE);
		}
	}



	//zeigt das Vorschaubild verkleinert auf die verfügbare Fläche (nie vergrößert), Bildpixel = Bildschirmpixel wenn es passt
	private static class VorschauBild extends JPanel {
		private static final long serialVersionUID = 1L;
		private BufferedImage bild;
		private String meldung;
		private double skalierung = 1.0;
		private boolean veraltet = false;

		void setBild(BufferedImage bild, String meldung){
			this.bild = bild;
			this.meldung = meldung;
			this.veraltet = false;
			repaint();
		}

		boolean hatBild(){
			return bild != null;
		}

		void setSkalierung(double skalierung){
			this.skalierung = skalierung > 0 ? skalierung : 1.0;
		}

		void setVeraltet(boolean veraltet){
			this.veraltet = veraltet;
			repaint();
		}

		@Override
		protected void paintComponent(Graphics g){
			Graphics2D g2 = (Graphics2D)g.create();
			g2.setColor(Design.istDunkel() ? new Color(0x1E1F22) : new Color(0xE9ECF1));
			g2.fillRect(0, 0, getWidth(), getHeight());

			if (bild != null){
				double breite = bild.getWidth() / skalierung;
				double hoehe = bild.getHeight() / skalierung;
				double faktor = Math.min(1.0, Math.min((getWidth() - 24) / breite, (getHeight() - 24) / hoehe));
				int b = (int)Math.round(breite * faktor);
				int h = (int)Math.round(hoehe * faktor);
				int x = (getWidth() - b) / 2;
				g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
				g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
				if (veraltet){
					g2.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 0.35f));
				}
				g2.drawImage(bild, x, 12, b, h, null);
			}else if (meldung != null){
				g2.setColor(UIManager.getColor("Label.disabledForeground"));
				g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
				int w = g2.getFontMetrics().stringWidth(meldung);
				g2.drawString(meldung, Math.max(8, (getWidth() - w) / 2), getHeight() / 2);
			}
			g2.dispose();
		}
	}
}
