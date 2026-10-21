package de.kekru.struktogrammeditor.view;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import de.kekru.struktogrammeditor.codeexport.CodeGenerator;
import de.kekru.struktogrammeditor.codeexport.CodeGenerator.Sprache;
import de.kekru.struktogrammeditor.control.GlobalSettings;
import de.kekru.struktogrammeditor.control.Struktogramm;
import de.kekru.struktogrammeditor.other.JNumberField;
import de.kekru.struktogrammeditor.other.JTextAreaEasy;
import de.kekru.struktogrammeditor.view.Symbole.Art;
import de.kekru.struktogrammeditor.view.Symbole.SymbolIcon;


//JDialog, in dem aus dem Struktogramm Quellcode in verschiedenen Sprachen erzeugt wird; der Code wird bei jeder Änderung der Optionen sofort neu erzeugt
public class CodeErzeuger extends JDialog {

	private static final long serialVersionUID = 6073577055724789562L;
	private JComboBox<Sprache> comboSprache = new JComboBox<Sprache>(Sprache.values());
	private JTextAreaEasy textarea = new JTextAreaEasy();//Textarea zum Ausgeben des Quellcodes
	private JCheckBox checkboxKommentare = new JCheckBox("Anweisungen nur als Kommentare");
	private JCheckBox checkboxSemikolons = new JCheckBox("Fehlende Semikolons ergänzen");
	private JCheckBox checkboxRahmen = new JCheckBox("Methodenrahmen aus dem Struktogrammnamen");
	private JNumberField numberfieldEinrueckung = new JNumberField();//gibt an, wie weit die erste Zeile im generierten Quellcode eingerückt werden soll
	private JNumberField numberfieldZeichenzahl = new JNumberField();//gibt an, wie viele Leerzeichen pro Einrückung genutzt werden sollen
	private JButton buttonKopieren = new JButton("In Zwischenablage kopieren", new SymbolIcon(Art.KOPIEREN, 16));
	private JButton buttonSchliessen = new JButton("Schließen");
	private Struktogramm str;//das Struktogramm, zu dem Quellcode generiert werden soll

	//Konstanten, die definieren, in welcher Sprache der Quellcode generiert werden soll (werden von den StruktogrammElementen verwendet)
	public static final int typJava = 0;
	public static final int typDelphi = 1;



	public CodeErzeuger(JFrame owner, String title, boolean modal, Struktogramm str) {
		super(owner, title, modal);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		this.str = str;

		//Optionen, zeilenweise
		JPanel optionen = new JPanel(new GridLayout(3, 1, 0, 6));

		JPanel zeile = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		zeile.add(new JLabel("Sprache:  "));
		comboSprache.setMaximumRowCount(Sprache.values().length);
		zeile.add(comboSprache);
		zeile.add(Box.createHorizontalStrut(24));
		zeile.add(new JLabel("Einrückung erste Zeile:  "));
		numberfieldEinrueckung.setColumns(3);
		zeile.add(numberfieldEinrueckung);
		zeile.add(Box.createHorizontalStrut(16));
		zeile.add(new JLabel("Leerzeichen pro Stufe:  "));
		numberfieldZeichenzahl.setColumns(3);
		zeile.add(numberfieldZeichenzahl);
		optionen.add(zeile);

		zeile = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		zeile.add(checkboxRahmen);
		zeile.add(Box.createHorizontalStrut(16));
		zeile.add(checkboxSemikolons);
		zeile.add(Box.createHorizontalStrut(16));
		zeile.add(checkboxKommentare);
		optionen.add(zeile);

		optionen.add(Design.hinweis("Die Struktur wird genau übersetzt. Typische Befehle (Ausgabe, Zuweisungen, Zählschleifen, Bedingungen) werden an die Sprache angepasst."));

		optionen.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
		add(optionen, BorderLayout.NORTH);

		//Ausgabe
		textarea.setFont(new Font("Monospaced", Font.PLAIN, 14));
		textarea.setTabSize(4);
		textarea.setRows(22);
		textarea.setColumns(80);
		textarea.setEditable(false);
		add(new JScrollPane(textarea), BorderLayout.CENTER);

		//Buttons
		buttonKopieren.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(textarea.getText()), null);
				buttonKopieren.setText("Kopiert!");
			}
		});
		buttonSchliessen.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				buttonSchliessen_ActionPerformed(evt);
			}
		});
		add(Design.buttonLeiste(buttonKopieren, buttonSchliessen), BorderLayout.SOUTH);


		//gespeicherte Einstellungen übernehmen
		comboSprache.setSelectedItem(Sprache.ausNummer(GlobalSettings.getCodeErzeugerProgrammiersprache()));
		checkboxKommentare.setSelected(GlobalSettings.isCodeErzeugerAlsKommentar());
		checkboxSemikolons.setSelected(GlobalSettings.isCodeErzeugerSemikolons());
		checkboxRahmen.setSelected(GlobalSettings.isCodeErzeugerRahmen());
		numberfieldEinrueckung.setText(""+GlobalSettings.getCodeErzeugerEinrueckungGesamt());
		numberfieldZeichenzahl.setText(""+GlobalSettings.getCodeErzeugerEinrueckungProStufe());

		//bei jeder Änderung neu erzeugen
		ActionListener neuErzeugen = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				codeErzeugen();
			}
		};
		comboSprache.addActionListener(neuErzeugen);
		checkboxKommentare.addActionListener(neuErzeugen);
		checkboxSemikolons.addActionListener(neuErzeugen);
		checkboxRahmen.addActionListener(neuErzeugen);
		DocumentListener zahlGeaendert = new DocumentListener() {
			public void insertUpdate(DocumentEvent e) { codeErzeugen(); }
			public void removeUpdate(DocumentEvent e) { codeErzeugen(); }
			public void changedUpdate(DocumentEvent e) { codeErzeugen(); }
		};
		numberfieldEinrueckung.getDocument().addDocumentListener(zahlGeaendert);
		numberfieldZeichenzahl.getDocument().addDocumentListener(zahlGeaendert);

		codeErzeugen();

		Design.dialogEinrichten(this, buttonSchliessen);
		pack();
		setLocationRelativeTo(owner);
		setVisible(true);
	}



	//Quellcode neu erzeugen und Einstellungen merken; ungültige Zahlen werden ignoriert, bis sie korrigiert sind
	private void codeErzeugen(){
		if (!numberfieldEinrueckung.isNumeric() || !numberfieldZeichenzahl.isNumeric()){
			return;
		}

		Sprache sprache = (Sprache)comboSprache.getSelectedItem();
		final int einrueckung = numberfieldEinrueckung.getInt();
		final int einrueckungProStufe = numberfieldZeichenzahl.getInt();
		final boolean alsKommentar = checkboxKommentare.isSelected();
		checkboxSemikolons.setEnabled(sprache != Sprache.PYTHON);

		CodeGenerator generator = new CodeGenerator(sprache, einrueckung, einrueckungProStufe, alsKommentar, checkboxSemikolons.isSelected(), checkboxRahmen.isSelected());
		textarea.setText(generator.erzeugen(str.gibListe(), str.getStruktogrammBeschreibung()));
		textarea.setCaretPosition(0);
		buttonKopieren.setText("In Zwischenablage kopieren");

		GlobalSettings.setCodeErzeugerEinrueckungGesamt(einrueckung);
		GlobalSettings.setCodeErzeugerEinrueckungProStufe(einrueckungProStufe);
		GlobalSettings.setCodeErzeugerProgrammiersprache(sprache.nummer);
		GlobalSettings.setCodeErzeugerAlsKommentar(alsKommentar);
		GlobalSettings.setCodeErzeugerSemikolons(checkboxSemikolons.isSelected());
		GlobalSettings.setCodeErzeugerRahmen(checkboxRahmen.isSelected());
	}


	@Override
	public void dispose(){
		GlobalSettings.saveSettings(); //gewählte Optionen merken
		super.dispose();
	}


	//gibt an, welche Symbole in der jeweiligen Sprache für Kommentare genutzt werden
	public static String gibKommentarZeichen(boolean kommentarStart, int codeTyp){
		switch(codeTyp){
		case typJava:
			if (kommentarStart){
				return "/*";
			}else{
				return "*/";
			}


		case typDelphi:
			if (kommentarStart){
				return "{";
				}else{
					return "}";
				}
		}

		return "";
	}


	//gibt an, welche Zeichen in der jeweiligen Sprache für Befehlsblockanfang und -ende genutzt werden
	public static String gibBlockZeichen(boolean blockStart, int codeTyp){
		switch(codeTyp){
		case typJava:
			if (blockStart){
				return "{";
				}else{
					return "}";
				}


		case typDelphi:
			if (blockStart){
				return "begin";
			}else{
				return "end;";
			}
		}

		return "";
	}

	//bei Delphi setze ich das begin in die nächste Zeile, es muss daher eingerückt werden
	public static boolean mussBlockanfangEinruecken(int typ){
		return typ == typDelphi;
	}


	public void buttonSchliessen_ActionPerformed(ActionEvent evt) {
		dispose();
	}

}
