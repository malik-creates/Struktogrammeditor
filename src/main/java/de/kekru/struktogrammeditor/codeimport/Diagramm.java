package de.kekru.struktogrammeditor.codeimport;

import java.util.ArrayList;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;

import de.kekru.struktogrammeditor.control.Struktogramm;
import de.kekru.struktogrammeditor.control.XMLLeser;

//Ein aus Quellcode erzeugtes Struktogramm (z.B. eine Methode), wird später in einem eigenen Tab angezeigt
public class Diagramm {

	private final String name; //wird als Struktogrammname (Überschrift) angezeigt, "" für keinen Namen
	private final List<CodeNode> elemente;

	public Diagramm(String name, List<CodeNode> elemente){
		this.name = name == null ? "" : name;
		this.elemente = elemente;
	}

	public String getName(){
		return name;
	}

	public List<CodeNode> getElemente(){
		return elemente;
	}

	//erzeugt ein Document im gleichen Format wie beim Speichern einer .strk-Datei, damit es vom XMLLeser geladen werden kann
	public Document alsDocument(){
		Element wurzel = new Element("struktogramm");
		wurzel.setAttribute("caption", XMLLeser.encodeS(name));
		listeSchreiben(wurzel, elemente);
		return new Document(wurzel);
	}

	private static void listeSchreiben(Element parent, List<CodeNode> liste){
		for (CodeNode n : liste){
			parent.addContent(elementErstellen(n));
		}
	}

	private static Element elementErstellen(CodeNode n){
		Element e = new Element("strelem").setAttribute("typ", "" + n.getTyp());

		for (String zeile : textzeilen(n.getText())){
			e.addContent(new Element("text").addContent(XMLLeser.encodeS(zeile)));
		}

		if (n.istSchleife()){
			Element inhalt = new Element("schleifeninhalt");
			listeSchreiben(inhalt, n.getInhalt());
			e.addContent(inhalt);

		}else if (n.hatFaelle()){
			for (int i = 0; i < n.getFaelle().size(); i++){
				Element fall = new Element("fall").setAttribute("fallname", XMLLeser.encodeS(n.getFallNamen().get(i)));
				listeSchreiben(fall, n.getFaelle().get(i));
				e.addContent(fall);
			}
		}

		return e;
	}

	private static List<String> textzeilen(String text){
		List<String> zeilen = new ArrayList<String>();
		for (String s : text.split("\n")){
			zeilen.add(s);
		}
		if (zeilen.isEmpty()){
			zeilen.add("");
		}
		return zeilen;
	}

	//"return" als letzte Anweisung einer Funktion ist kein Aussprung, sondern eine normale Anweisung
	public static void letztesReturnAlsAnweisung(List<CodeNode> liste){
		if (!liste.isEmpty()){
			CodeNode letztes = liste.get(liste.size() - 1);
			if (letztes.getTyp() == Struktogramm.typAussprung && letztes.getText().matches("(?is)(return|exit|result\\s*:=)\\b.*")){
				letztes.setTyp(Struktogramm.typAnweisung);
			}
		}
	}
}
