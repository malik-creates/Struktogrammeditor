package de.kekru.struktogrammeditor.control;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.io.Writer;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.Timer;

import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import de.kekru.struktogrammeditor.view.StrTabbedPane;

//Sichert ungespeicherte Struktogramme regelmäßig in einen eigenen Ordner, damit sie nach einem Absturz wiederhergestellt werden können.
//Jede laufende Programminstanz nutzt einen eigenen Unterordner mit Sperrdatei; Ordner ohne Sperre stammen von abgestürzten Instanzen.
public class AutoSicherung {

	private static final int intervallMs = 60 * 1000;
	private static final String sperrName = ".sperre";

	private final Controlling controlling;
	private final File hauptordner = new File(System.getProperty("user.home"), ".struktogrammeditor" + File.separator + "wiederherstellung");
	private File sitzungsordner;
	private FileLock sperre;
	private RandomAccessFile sperrDatei;
	private Timer timer;

	public AutoSicherung(Controlling controlling){
		this.controlling = controlling;
	}


	//beim Programmstart: verwaiste Sicherungen anbieten, dann eigene Sicherung starten
	public void starten(){
		try{
			wiederherstellenAnbieten();
			sitzungsordner = new File(hauptordner, "sitzung-" + System.currentTimeMillis());
			if (!sitzungsordner.mkdirs()){
				return;
			}
			sperrDatei = new RandomAccessFile(new File(sitzungsordner, sperrName), "rw");
			sperre = sperrDatei.getChannel().tryLock();
		}catch(IOException e){
			e.printStackTrace();
			return;
		}catch(RuntimeException e){
			e.printStackTrace();
			return;
		}

		timer = new Timer(intervallMs, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				sichern();
			}
		});
		timer.start();
	}


	//alle ungespeicherten Struktogramme schreiben, Sicherungen von gespeicherten oder geschlossenen entfernen
	public void sichern(){
		if (sitzungsordner == null){
			return;
		}
		StrTabbedPane tabs = controlling.getGUI().gibTabbedpane();
		Set<String> aktuell = new HashSet<String>();

		for (int i = 0; i < tabs.getTabCount(); i++){
			Struktogramm str = (Struktogramm)((JScrollPane)tabs.getComponentAt(i)).getViewport().getView();
			if (!str.istUngespeichert()){
				continue;
			}
			String name = "struktogramm-" + System.identityHashCode(str) + ".strk";
			aktuell.add(name);

			Document doc = str.xmlErstellen();
			String titel = tabs.getTitleAt(i).replaceAll("\\*$", "");
			doc.getRootElement().setAttribute("sicherungtitel", XMLLeser.encodeS(titel));
			doc.getRootElement().setAttribute("sicherungpfad", XMLLeser.encodeS(str.gibAktuellenSpeicherpfad()));

			File ziel = new File(sitzungsordner, name);
			File temp = new File(sitzungsordner, name + ".tmp");
			try{
				Writer w = new OutputStreamWriter(new FileOutputStream(temp), "UTF-8");
				try{
					new XMLOutputter(Format.getCompactFormat()).output(doc, w);
				}finally{
					w.close();
				}
				if (ziel.exists() && !ziel.delete()){
					continue;
				}
				temp.renameTo(ziel);
			}catch(IOException e){
				e.printStackTrace();
			}
		}

		File[] dateien = sitzungsordner.listFiles();
		if (dateien != null){
			for (File f : dateien){
				if (f.getName().endsWith(".strk") && !aktuell.contains(f.getName())){
					f.delete();
				}
			}
		}
	}


	//beim normalen Beenden: eigene Sicherungen löschen
	public void beenden(){
		if (timer != null){
			timer.stop();
		}
		try{
			if (sperre != null){
				sperre.release();
			}
			if (sperrDatei != null){
				sperrDatei.close();
			}
		}catch(IOException e){
			//egal, Ordner wird trotzdem gelöscht
		}
		ordnerLoeschen(sitzungsordner);
	}


	private void wiederherstellenAnbieten(){
		File[] sitzungen = hauptordner.listFiles();
		if (sitzungen == null){
			return;
		}

		List<File> verwaisteOrdner = new ArrayList<File>();
		List<File> dateien = new ArrayList<File>();
		for (File ordner : sitzungen){
			if (!ordner.isDirectory() || wirdBenutzt(ordner)){
				continue; //gehört zu einem anderen, noch laufenden Struktogrammeditor
			}
			verwaisteOrdner.add(ordner);
			File[] inhalt = ordner.listFiles();
			if (inhalt != null){
				for (File f : inhalt){
					if (f.getName().endsWith(".strk")){
						dateien.add(f);
					}
				}
			}
		}

		if (!dateien.isEmpty()){
			int antwort = JOptionPane.showConfirmDialog(controlling.getGUI(),
					(dateien.size() == 1 ? "Ein Struktogramm wurde" : dateien.size() + " Struktogramme wurden") + " beim letzten Mal nicht gespeichert\n"
					+ "(z.B. weil das Programm abgestürzt ist).\n\nWiederherstellen?",
					"Struktogramme wiederherstellen", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

			if (antwort == JOptionPane.YES_OPTION){
				for (File f : dateien){
					wiederherstellen(f);
				}
			}else if (antwort != JOptionPane.NO_OPTION){
				return; //Dialog geschlossen: Sicherungen behalten und beim nächsten Start wieder fragen
			}
		}

		for (File ordner : verwaisteOrdner){
			ordnerLoeschen(ordner);
		}
	}


	private void wiederherstellen(File datei){
		try{
			Document doc = new SAXBuilder().build(datei);
			String titel = attribut(doc, "sicherungtitel");
			String pfad = attribut(doc, "sicherungpfad");

			Struktogramm str = controlling.gibAktuellesStruktogramm();
			if (str == null || !str.istLeerUndUnveraendert()){
				str = controlling.neuesStruktogramm();
			}
			if (pfad.isEmpty() && !titel.isEmpty()){
				controlling.getGUI().gibTabbedpane().titelDerAktuellenSeiteSetzen(titel);
			}
			str.wiederhergestelltLaden(doc, pfad);
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	private static String attribut(Document doc, String name){
		String wert = doc.getRootElement().getAttributeValue(name);
		if (wert == null || wert.isEmpty()){
			return "";
		}
		try{
			return XMLLeser.decodeS(wert);
		}catch(RuntimeException e){
			return "";
		}
	}


	//true, wenn eine andere laufende Instanz den Ordner gesperrt hat
	private static boolean wirdBenutzt(File ordner){
		File sperrDatei = new File(ordner, sperrName);
		if (!sperrDatei.exists()){
			return false;
		}
		RandomAccessFile raf = null;
		try{
			raf = new RandomAccessFile(sperrDatei, "rw");
			FileLock lock = raf.getChannel().tryLock();
			if (lock == null){
				return true;
			}
			lock.release();
			return false;
		}catch(Exception e){
			return true; //im Zweifel nicht anfassen
		}finally{
			if (raf != null){
				try{
					raf.close();
				}catch(IOException e){
					//ignorieren
				}
			}
		}
	}

	private static void ordnerLoeschen(File ordner){
		if (ordner == null){
			return;
		}
		File[] inhalt = ordner.listFiles();
		if (inhalt != null){
			for (File f : inhalt){
				f.delete();
			}
		}
		ordner.delete();
	}
}
