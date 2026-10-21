package de.kekru.struktogrammeditor.control;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.JOptionPane;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import javax.swing.plaf.nimbus.NimbusLookAndFeel;

import de.kekru.struktogrammeditor.codeimport.Diagramm;
import de.kekru.struktogrammeditor.export.VektorExport;
import de.kekru.struktogrammeditor.other.Helpers;
import de.kekru.struktogrammeditor.other.XActionCommands;
import de.kekru.struktogrammeditor.struktogrammelemente.StruktogrammElement;
import de.kekru.struktogrammeditor.view.CodeErzeuger;
import de.kekru.struktogrammeditor.view.CodeImportDialog;
import de.kekru.struktogrammeditor.view.Design;
import de.kekru.struktogrammeditor.view.EinstellungsDialog;
import de.kekru.struktogrammeditor.view.FontChooser;
import de.kekru.struktogrammeditor.view.GUI;
import de.kekru.struktogrammeditor.view.ZoomEinstellungen;

public class Controlling implements Konstanten, ActionListener, WindowListener, KeyListener {

	private GUI gui;
	private AutoSicherung autoSicherung;
	private enum Betriebssysteme {Windows, Mac, Linux};

	public Controlling(String[] params){
		handleOSSettingsAndLookAndFeel();

		gui = new GUI(this);
		neuesStruktogramm();		

		if(params != null){
			for(int i=0; i < params.length; i++){
				if(new File(params[i]).exists()){
					openStruktogramm(params[i]);
				}
			}
		}

		autoSicherung = new AutoSicherung(this);
		autoSicherung.starten();
	}



	public void handleOSSettingsAndLookAndFeel(){

		try{

			LookAndFeel lookAndFeel = null;

			switch (GlobalSettings.getLookAndFeelAktuell()) {
			case lookAndFeelOSStandard:
				try {
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				} catch (ClassNotFoundException e1) {
					e1.printStackTrace();
				} catch (InstantiationException e1) {
					e1.printStackTrace();
				} catch (IllegalAccessException e1) {
					e1.printStackTrace();
				} catch (UnsupportedLookAndFeelException e1) {
					e1.printStackTrace();
				}
				break;

			case lookAndFeelNimbus:
				lookAndFeel = new NimbusLookAndFeel();
				break;

			case lookAndFeelMotif: lookAndFeel = (LookAndFeel)Class.forName("com.sun.java.swing.plaf.motif.MotifLookAndFeel").newInstance(); //per Reflection, damit auch mit neueren JDKs kompilierbar
			break;

			case lookAndFeelModernHell:
			case lookAndFeelModernDunkel:
				Design.setzen(GlobalSettings.getLookAndFeelAktuell());
				break;
			}

			if(lookAndFeel != null){
				try {
					UIManager.setLookAndFeel(lookAndFeel);//Standard Swing Look And Feel
				} catch (UnsupportedLookAndFeelException e) {
					e.printStackTrace();
				}
			}


			if(getOS() == Betriebssysteme.Mac){
				new MacHandler(this);
			}

		}catch(Exception e){
			e.printStackTrace();
		}
	}



	private static Betriebssysteme getOS(){
		String s = System.getProperty("os.name").toLowerCase();

		if(s.startsWith("windows")){
			return Betriebssysteme.Windows;

		}else if(s.startsWith("mac")){
			return Betriebssysteme.Mac;

		}else if(s.startsWith("linux")){
			return Betriebssysteme.Linux;

		}else{
			return Betriebssysteme.Windows;
		}
	}


	public Struktogramm gibAktuellesStruktogramm(){
		return gui.gibTabbedpane().gibAktuellesStruktogramm();
	}



	public Struktogramm neuesStruktogramm(){
		Struktogramm str = gui.gibTabbedpane().struktogrammHinzufuegen();
		str.graphicsInitialisieren();//erst nach setVisible(true); sonst gibt es Probleme in Struktogramm.graphicsInitialisieren()
		str.zeichenbereichAktualisieren();
		str.zeichne();
		return str;
	}


	public void speichern(boolean neuenSpeicherpfadAuswaehlenLassen){
		Struktogramm str = gibAktuellesStruktogramm();

		if(str != null){
			GlobalSettings.setzeSpeicherpfad(str.speichern(neuenSpeicherpfadAuswaehlenLassen,GlobalSettings.getZuletztGenutzterSpeicherpfad()));//Struktogramm wird gespeichert (zuletztGenutzterSpeicherpfad wird dabei übergeben, damit der JFileChooser, sofern er genutzt wird, dort startet) und der neue Speicherpfad wird gesichert
			GlobalSettings.saveSettings();
			titelleisteAktualisieren();
		}
	}

	public void laden(){

		String pfad = Struktogramm.oeffnenDialog(GlobalSettings.getZuletztGenutzterSpeicherpfad(), gui);//Parameter ist der Startordner für den OpenDialog

		if(!pfad.equals("")){
			openStruktogramm(pfad);
		}
	}



	private void openStruktogramm(String pfad) {
		Struktogramm str = gibAktuellesStruktogramm();
		if(str == null || !str.istLeerUndUnveraendert()){ //leeren, unveränderten Tab wiederverwenden
			str = neuesStruktogramm();
		}
		str.graphicsInitialisieren();
		str.laden(pfad);
		GlobalSettings.setzeSpeicherpfad(pfad);
		GlobalSettings.saveSettings();
		titelleisteAktualisieren();
	}

	//für jedes aus Quellcode erzeugte Diagramm wird ein Tab geöffnet; ein leerer, unveränderter Tab wird dabei wiederverwendet
	public void struktogrammeAusCodeErzeugen(List<Diagramm> diagramme){
		Struktogramm leeres = gibAktuellesStruktogramm();
		if (leeres != null && !leeres.istLeerUndUnveraendert()){
			leeres = null;
		}

		for (Diagramm d : diagramme){
			Struktogramm str = leeres != null ? leeres : neuesStruktogramm();
			leeres = null;

			if (!d.getName().isEmpty()){
				String tabTitel = d.getName();
				if (tabTitel.indexOf('(') > 0){ //nur der Funktionsname, ohne Rückgabetyp und Parameter
					tabTitel = tabTitel.substring(0, tabTitel.indexOf('(')).trim();
					tabTitel = tabTitel.substring(tabTitel.lastIndexOf(' ') + 1);
				}
				gui.gibTabbedpane().titelDerAktuellenSeiteSetzen(tabTitel);
			}

			str.ausDocumentErzeugen(d.alsDocument());
		}

		titelleisteAktualisieren();
	}

	public void bildSpeichern(){
		Struktogramm str = gibAktuellesStruktogramm();

		if(str != null){
			GlobalSettings.setzeBildSpeicherpfad(str.alsBilddateiSpeichern(GlobalSettings.getZuletztGenutzterPfadFuerBild()));
			GlobalSettings.saveSettings();
		}
	}

	public void titelleisteAktualisieren(){
		String titel = GlobalSettings.guiTitel;

		Struktogramm str = gibAktuellesStruktogramm();
		if(str != null){
			String pfad = str.gibAktuellenSpeicherpfad();
			if (!pfad.equals("")){
				titel = new File(pfad).getName() + " – " + titel + "   (" + new File(pfad).getParent() + ")"; //Dateiname zuerst, Ordner dahinter
			}
		}

		gui.setTitle(titel);
		gui.zoomAnzeigeAktualisieren();
	}


	//aktuelles Struktogramm als PDF, SVG oder PNG in doppelter Auflösung speichern
	private void exportieren(String art){
		Struktogramm str = gibAktuellesStruktogramm();
		JFileChooser chooser = new JFileChooser();
		String beschreibung = art.equals("pdf") ? "PDF-Dokument (*.pdf)" : art.equals("svg") ? "SVG-Vektorgrafik (*.svg)" : "PNG-Bild (*.png)";
		chooser.setFileFilter(new FileNameExtensionFilter(beschreibung, art));
		chooser.setDialogTitle("Exportieren als " + art.toUpperCase());

		String startOrdner = GlobalSettings.getZuletztGenutzterPfadFuerBild();
		if(startOrdner.isEmpty()){
			startOrdner = str.gibAktuellenSpeicherpfad();
		}
		if(!startOrdner.isEmpty()){
			chooser.setCurrentDirectory(new File(startOrdner).isDirectory() ? new File(startOrdner) : new File(startOrdner).getParentFile());
		}
		String vorschlag = str.gibAktuellenSpeicherpfad().isEmpty() ? "Struktogramm" : new File(str.gibAktuellenSpeicherpfad()).getName().replaceAll("\\.[^.]*$", "");
		chooser.setSelectedFile(new File(vorschlag + "." + art));

		if(chooser.showSaveDialog(gui) != JFileChooser.APPROVE_OPTION){
			return;
		}
		File datei = chooser.getSelectedFile();
		if(!datei.getName().toLowerCase().endsWith("." + art)){
			datei = new File(datei.getPath() + "." + art);
		}
		if(datei.exists() && JOptionPane.showConfirmDialog(gui, "Die Datei " + datei.getName() + " existiert bereits.\nÜberschreiben?", "Datei existiert", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION){
			return;
		}

		try{
			if(art.equals("pdf")){
				VektorExport.alsPdfSpeichern(str.vektorAufzeichnen(true), datei);
			}else if(art.equals("svg")){
				VektorExport.alsSvgSpeichern(str.vektorAufzeichnen(false), datei);
			}else{
				ImageIO.write(str.generateImage(true, 2.0), "png", datei);
			}
			GlobalSettings.setzeBildSpeicherpfad(datei.getPath());
			GlobalSettings.saveSettings();
		}catch(IOException e){
			JOptionPane.showMessageDialog(gui, "Die Datei konnte nicht gespeichert werden:\n" + e.getMessage(), "Fehler beim Exportieren", JOptionPane.ERROR_MESSAGE);
		}
	}



	@Override
	public void actionPerformed(ActionEvent e) {

		switch(XActionCommands.valueOf(e.getActionCommand())){
		case neu:
			neuesStruktogramm();
			break;

		case oeffnen:
			laden();
			break;

		case speichern:
			speichern(false);
			break;

		case speicherUnter:
			speichern(true);
			break;

		case bildSpeichern:
			bildSpeichern();
			break;

		case bildInZwischenAblage:
			copyImagetoClipBoard(gibAktuellesStruktogramm().generateImage(false));
			break;

		case quellcodeErzeugen:
			new CodeErzeuger(gui, "Quellcode erzeugen", true, gibAktuellesStruktogramm());
			break;

		case codeImportieren:
			new CodeImportDialog(this);
			break;

		case struktogrammSchliessen:
			gui.gibTabbedpane().aktuellesStruktogrammschliessen();
			break;

		case programmBeenden:
			programmBeendenGeklickt();
			break;

		case rueckgaengig:
			gibAktuellesStruktogramm().schrittZurueck();
			break;

		case widerrufen:
			gibAktuellesStruktogramm().schrittNachVorne();
			break;

		case ganzesStruktogrammKopieren:
			gui.gibAuswahlPanel().kopiereGanzesStruktogramm();
			break;

		case elementUnterDerMausKopieren:
			kopiereElement();
			break;

		case elementEinfuegen:
			gibAktuellesStruktogramm().elementAusKopierFeldEinfuegenAnMausPos();
			break;

		case letztesElementStrecken:
			letzteElementeStreckenGeklickt(e.getSource());
			break;

		case startbeschriftungAendern:
			new EinstellungsDialog(gui,true);
			break;

		case schriftartAendern:
			new FontChooser(this,true);
			break;

		case groesseAendernMitMausrad:
			mitMausradElementeVergroessernGeklickt(e.getSource());
			break;

		case zoomeinstellungen:
			new ZoomEinstellungen(gui);
			break;

		case vergroesserungenRuckgaengigMachen:
			gibAktuellesStruktogramm().zoomsZuruecksetzen();
			break;

		case elementShortcutsVerwenden:
			elementEinfuegenShortcutsVerwendenGeklickt(e.getSource());
			break;

		case kantenglaettungVerwenden:
			kantenglaettungVerwendenGeklickt(e.getSource());
			break;

		case sourceCode:
			Helpers.openWebsite(GlobalSettings.quellcodeUrl);
			break;

		case sourceCodeErweiterung:
			Helpers.openWebsite(GlobalSettings.erweiterungUrl);
			break;

		case info:
			showInfo();
			break;

		case beschriftungsStilJava:
			changeBeschriftungsStil(beschriftungsStilJava);
			break;

		case beschriftungsStilFormal:
			changeBeschriftungsStil(beschriftungsStilFormal);
			break;

		case beschriftungsStilKeineBeschriftungen:
			changeBeschriftungsStil(beschriftungsStilKeineBeschriftungen);
			break;

		case lookAndFeelOSStandard:
			changeLookAndFeel(lookAndFeelOSStandard);
			break;

		case lookAndFeelSwingStandard:
			changeLookAndFeel(lookAndFeelSwingStandard);
			break;

		case lookAndFeelNimbus:
			changeLookAndFeel(lookAndFeelNimbus);
			break;

		case lookAndFeelMotif:
			changeLookAndFeel(lookAndFeelMotif);
			break;

		case lookAndFeelModernHell:
			changeLookAndFeel(lookAndFeelModernHell);
			break;

		case lookAndFeelModernDunkel:
			changeLookAndFeel(lookAndFeelModernDunkel);
			break;

		case exportPdf:
			exportieren("pdf");
			break;

		case exportSvg:
			exportieren("svg");
			break;

		case exportPng:
			exportieren("png");
			break;

		case zoomGroesser:
			gibAktuellesStruktogramm().zoomSchritt(true);
			break;

		case zoomKleiner:
			gibAktuellesStruktogramm().zoomSchritt(false);
			break;

		case zoomOriginal:
			gibAktuellesStruktogramm().setZoom(1.0);
			break;

		case zoomAnpassen:
			gibAktuellesStruktogramm().zoomAnFensterAnpassen();
			break;

		case designUmschalten:
			changeLookAndFeel(GlobalSettings.getLookAndFeelAktuell() == lookAndFeelModernDunkel ? lookAndFeelModernHell : lookAndFeelModernDunkel);
			break;
			
		case struktogrammbeschreibungHinzufuegen:
			addStruktogrammbeschriftung();
			break;

		}

	}


	private void addStruktogrammbeschriftung() {
		Struktogramm str = gibAktuellesStruktogramm();
		String s = JOptionPane.showInputDialog("Beschriftung", str.getStruktogrammBeschreibung());
		if(s == null){
			return;
		}
		str.setStruktogrammBeschreibung(s);
		str.rueckgaengigPunktSetzen();
		str.zeichenbereichAktualisieren();
		str.zeichne();		
	}



	private void kopiereElement(){
		StruktogrammElement element = gibAktuellesStruktogramm().getElementUnterMaus();

		if(element != null){
			getGUI().gibAuswahlPanel().setzeKopiertesStrElement(gibAktuellesStruktogramm().xmlErstellen(element));
		}
	}


	private void changeLookAndFeel(int lookAndFeelIndex){
		boolean warModern = Design.istModern(GlobalSettings.getLookAndFeelAktuell());
		GlobalSettings.setLookAndFeelAktuell(lookAndFeelIndex);
		GlobalSettings.saveSettings();

		if(warModern && Design.istModern(lookAndFeelIndex)){ //zwischen hell und dunkel kann sofort gewechselt werden
			Design.umschalten(lookAndFeelIndex);
		}else{
			JOptionPane.showMessageDialog(gui, "Die Änderungen werden beim Neustart des Programms wirksam.", "Design ändern", JOptionPane.INFORMATION_MESSAGE);
		}
		gui.designAktualisiert();
	}


	private void changeBeschriftungsStil(int beschriftungsStilIndex){
		GlobalSettings.setBeschriftungsStilAktuell(beschriftungsStilIndex);
		GlobalSettings.saveSettings();
		gui.gibAuswahlPanel().aktualisiereBeschriftungen();
	}


	public void showInfo(){
		final String separator = System.getProperty("line.separator");

		String datumsfolge = "";
		for(String s : GlobalSettings.updateDaten){
			datumsfolge += s + separator;
		}

		JOptionPane.showMessageDialog(gui,
				"Struktogrammeditor "+GlobalSettings.versionsString+separator+
				"Kevin Krummenauer - 2011-2012"+separator+
				"Informatik Projekt Stufe 13.2, AMG Beckum, Februar/März 2011"+separator+				
				separator+
				"Erweitert von " + GlobalSettings.erweiterungAutor + " - 2026"+separator+
				"Neu: Struktogramm aus Code (Java, C, C#, JavaScript, Python, Pascal),"+separator+
				"Code in 6 Sprachen, modernes Design, Zoom, PDF/SVG-Export, automatische Sicherung."+separator+
				separator+
				"Diese Version: " + GlobalSettings.erweiterungUrl +separator+
				"Original: " + GlobalSettings.quellcodeUrl +separator+
				"Git Hash: " + GlobalSettings.buildInfoGitHash +separator+
				"Build-Zeit: " + GlobalSettings.buildInfoBuildTime +separator+
				separator+
				"This product includes software developed by the JDOM Project (http://www.jdom.org/)."+separator+
				"Design: FlatLaf (https://www.formdev.com/flatlaf/), Apache License 2.0.",
				"Information - Struktogrammeditor "+GlobalSettings.versionsString, JOptionPane.INFORMATION_MESSAGE);
	}


	private void letzteElementeStreckenGeklickt(Object source){
		GlobalSettings.setzeLetzteElementeStrecken(((JCheckBoxMenuItem)source).isSelected());
		GlobalSettings.saveSettings();
		gibAktuellesStruktogramm().zeichenbereichAktualisieren();
		gibAktuellesStruktogramm().zeichne();
	}


	private void mitMausradElementeVergroessernGeklickt(Object source){
		boolean einOderAus = ((JCheckBoxMenuItem)source).isSelected();
		GlobalSettings.setBeiMausradGroesseAendern(einOderAus);
		GlobalSettings.saveSettings();
		gibAktuellesStruktogramm().mausradScrollEinOderAusschalten(einOderAus);
		gibAktuellesStruktogramm().zeichenbereichAktualisieren();
		gibAktuellesStruktogramm().zeichne();
	}


	private void elementEinfuegenShortcutsVerwendenGeklickt(Object source){
		boolean einOderAus = ((JCheckBoxMenuItem)source).isSelected();
		GlobalSettings.setElementShortcutsVerwenden(einOderAus);
		GlobalSettings.saveSettings();
	}


	private void kantenglaettungVerwendenGeklickt(Object source){
		GlobalSettings.setKantenglaettungVerwenden(((JCheckBoxMenuItem)source).isSelected());
		GlobalSettings.saveSettings();
		gibAktuellesStruktogramm().graphicsInitialisieren();
		gibAktuellesStruktogramm().zeichenbereichAktualisieren();
		gibAktuellesStruktogramm().zeichne();
	}



	//Sicherungen aufräumen und Programm beenden
	private void beendenOhneNachfrage(){
		if(autoSicherung != null){
			autoSicherung.beenden();
		}
		System.exit(0);
	}


	public GUI getGUI(){
		return gui;
	}






	public boolean programmBeendenGeklickt(){
		if(gui.gibTabbedpane().einOderMehrereStruktogrammeNichtGespeichert()){                  
			Object[] options = {"Ja", "Nein"}; //http://download.oracle.com/javase/1.4.2/docs/api/javax/swing/JOptionPane.html
			if (0 == JOptionPane.showOptionDialog(gui, "Ein oder mehrere Struktogramme wurden noch nicht gespeichert.\nWirklich Beenden ohne zu speichern?", "Noch nicht gespeichert", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[1])){

				beendenOhneNachfrage();
				return true;
			}

			return false;

		}else{
			beendenOhneNachfrage();
			return true;
		}
	}


	@Override
	public void windowOpened(WindowEvent e) {

	}


	@Override
	public void windowClosing(WindowEvent e) {
		programmBeendenGeklickt();
	}


	@Override
	public void windowClosed(WindowEvent e) {

	}


	@Override
	public void windowIconified(WindowEvent e) {

	}


	@Override
	public void windowDeiconified(WindowEvent e) {

	}


	@Override
	public void windowActivated(WindowEvent e) {

	}


	@Override
	public void windowDeactivated(WindowEvent e) {

	}


	@Override
	public void keyTyped(KeyEvent e) {
		if(e.getSource() == gui.gibTabbedpane() && e.getModifiers() != GlobalSettings.strgOderApfelMask){

			switch(e.getKeyChar()){
			case '+':
				gibAktuellesStruktogramm().zoomAktuellesElement(true);
				break;

			case '-':
				gibAktuellesStruktogramm().zoomAktuellesElement(false);
				break;
			}
		}
	}


	@Override
	public void keyPressed(KeyEvent e) {

	}


	@Override
	public void keyReleased(KeyEvent e) {
		if(e.getSource() == gui.gibTabbedpane() && e.getModifiers() != GlobalSettings.strgOderApfelMask){

			switch(e.getKeyCode()){
			case KeyEvent.VK_A:
				if(GlobalSettings.isElementShortcutsVerwenden()){
					gibAktuellesStruktogramm().neuesElementAnAktuellerStelleEinfuegen(Struktogramm.typAnweisung);
				}
				break;

			case KeyEvent.VK_I:
				if(GlobalSettings.isElementShortcutsVerwenden()){
					gibAktuellesStruktogramm().neuesElementAnAktuellerStelleEinfuegen(Struktogramm.typVerzweigung);
				}
				break;

			case KeyEvent.VK_S:
				if(GlobalSettings.isElementShortcutsVerwenden()){
					gibAktuellesStruktogramm().neuesElementAnAktuellerStelleEinfuegen(Struktogramm.typFallauswahl);
				}
				break;

			case KeyEvent.VK_F:
				if(GlobalSettings.isElementShortcutsVerwenden()){
					gibAktuellesStruktogramm().neuesElementAnAktuellerStelleEinfuegen(Struktogramm.typForSchleife);
				}
				break;

			case KeyEvent.VK_W:
				if(GlobalSettings.isElementShortcutsVerwenden()){
					gibAktuellesStruktogramm().neuesElementAnAktuellerStelleEinfuegen(Struktogramm.typWhileSchleife);
				}
				break;

			case KeyEvent.VK_D:
				if(GlobalSettings.isElementShortcutsVerwenden()){
					gibAktuellesStruktogramm().neuesElementAnAktuellerStelleEinfuegen(Struktogramm.typDoUntilSchleife);
				}
				break;

			case KeyEvent.VK_E:
				if(GlobalSettings.isElementShortcutsVerwenden()){
					gibAktuellesStruktogramm().neuesElementAnAktuellerStelleEinfuegen(Struktogramm.typEndlosschleife);
				}
				break;

			case KeyEvent.VK_B:
				if(GlobalSettings.isElementShortcutsVerwenden()){
					gibAktuellesStruktogramm().neuesElementAnAktuellerStelleEinfuegen(Struktogramm.typAussprung);
				}
				break;

			case KeyEvent.VK_M:
				if(GlobalSettings.isElementShortcutsVerwenden()){
					gibAktuellesStruktogramm().neuesElementAnAktuellerStelleEinfuegen(Struktogramm.typAufruf);
				}
				break;

			case KeyEvent.VK_DELETE:
				gibAktuellesStruktogramm().elementAnAktuellerStelleLoeschen();
				break;
			}
		}

	}



	//http://stackoverflow.com/questions/4552045/copy-bufferedimage-to-clipboard
	public static void copyImagetoClipBoard(final BufferedImage image) {

		Transferable transferable = new Transferable() {

			public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
				if(flavor.equals(DataFlavor.imageFlavor) && image != null) {
					return image;
				}else{
					throw new UnsupportedFlavorException(flavor);
				}
			}

			public DataFlavor[] getTransferDataFlavors() {
				return new DataFlavor[] {DataFlavor.imageFlavor};
			}

			public boolean isDataFlavorSupported(DataFlavor flavor) {
				DataFlavor[] flavors = getTransferDataFlavors();
				for(DataFlavor f : flavors){
					if(flavor.equals(f)) {
						return true;
					}
				}

				return false;
			}
		};

		ClipboardOwner clipboardOwner = new ClipboardOwner() {				
			@Override
			public void lostOwnership(Clipboard clipboard, Transferable contents) {

			}
		};

		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(transferable, clipboardOwner);

	}



}
