package de.kekru.struktogrammeditor.codeimport;

import java.util.ArrayList;
import java.util.List;

import de.kekru.struktogrammeditor.control.Struktogramm;

//Ein Knoten im Zwischenbaum, der beim Einlesen von Quellcode entsteht; typ entspricht den Typnummern in Struktogramm
public class CodeNode {

	private int typ;
	private String text;
	private List<CodeNode> inhalt = new ArrayList<CodeNode>(); //Unterelemente bei Schleifen
	private List<String> fallNamen = new ArrayList<String>(); //Spaltenüberschriften bei Verzweigung und Fallauswahl
	private List<List<CodeNode>> faelle = new ArrayList<List<CodeNode>>(); //Spalteninhalte bei Verzweigung und Fallauswahl

	public CodeNode(int typ, String text){
		this.typ = typ;
		this.text = text == null ? "" : text;
	}

	public static CodeNode anweisung(String text){
		return new CodeNode(Struktogramm.typAnweisung, text);
	}

	public static CodeNode aussprung(String text){
		return new CodeNode(Struktogramm.typAussprung, text);
	}

	public static CodeNode schleife(int typ, String text, List<CodeNode> inhalt){
		CodeNode n = new CodeNode(typ, text);
		n.inhalt = inhalt;
		return n;
	}

	public static CodeNode verzweigung(String bedingung, List<CodeNode> ja, List<CodeNode> nein){
		CodeNode n = new CodeNode(Struktogramm.typVerzweigung, bedingung);
		n.fallHinzufuegen("Ja", ja);
		n.fallHinzufuegen("Nein", nein);
		return n;
	}

	public void fallHinzufuegen(String name, List<CodeNode> inhalt){
		fallNamen.add(name);
		faelle.add(inhalt);
	}

	public int getTyp(){
		return typ;
	}

	public void setTyp(int typ){
		this.typ = typ;
	}

	public String getText(){
		return text;
	}

	public List<CodeNode> getInhalt(){
		return inhalt;
	}

	public List<String> getFallNamen(){
		return fallNamen;
	}

	public List<List<CodeNode>> getFaelle(){
		return faelle;
	}

	public boolean istSchleife(){
		return typ == Struktogramm.typForSchleife || typ == Struktogramm.typWhileSchleife
				|| typ == Struktogramm.typDoUntilSchleife || typ == Struktogramm.typEndlosschleife;
	}

	public boolean hatFaelle(){
		return typ == Struktogramm.typVerzweigung || typ == Struktogramm.typFallauswahl;
	}
}
