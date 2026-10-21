package de.kekru.struktogrammeditor.codeimport;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.kekru.struktogrammeditor.control.GlobalSettings;
import de.kekru.struktogrammeditor.control.Struktogramm;

//Liest Pascal-/Delphi-Quellcode und erzeugt daraus Struktogramme
public class PascalParser {

	private static class Token {
		final String text;
		final String klein;
		final int start;
		final int ende;
		final int zeile;

		Token(String text, int start, int ende, int zeile){
			this.text = text;
			this.klein = text.toLowerCase();
			this.start = start;
			this.ende = ende;
			this.zeile = zeile;
		}
	}

	private final String quelle; //Quelltext, Kommentare sind durch Leerzeichen ersetzt
	private final List<Token> tokens;
	private int i;
	private final boolean semikolonBehalten;
	private final List<Diagramm> funktionen = new ArrayList<Diagramm>();

	private static final Set<String> abschnitte = menge("var", "const", "type", "label", "resourcestring", "threadvar", "uses");
	private static final Set<String> routinen = menge("procedure", "function", "constructor", "destructor", "operator");
	private static final Set<String> direktiven = menge("forward", "overload", "override", "virtual", "abstract", "static", "inline",
			"cdecl", "stdcall", "register", "pascal", "safecall", "reintroduce", "external", "assembler", "export", "far", "near",
			"dynamic", "deprecated", "platform", "message", "final");
	private static final Set<String> anweisungsEnde = menge(";", "end", "else", "until", "except", "finally");

	public PascalParser(String code, boolean semikolonBehalten){
		this.quelle = kommentareEntfernen(code);
		this.tokens = zerlegen(quelle);
		this.semikolonBehalten = semikolonBehalten;
	}

	public List<Diagramm> parse() throws CodeParseException {
		i = 0;
		List<CodeNode> haupt = new ArrayList<CodeNode>();
		String programmName = "";

		Set<String> programmStart = new HashSet<String>(abschnitte);
		programmStart.addAll(routinen);
		programmStart.addAll(menge("program", "unit", "library", "begin", "class", "interface", "implementation"));

		if (ende() || !programmStart.contains(akt())){
			//nur einzelne Anweisungen ohne Programmrahmen
			haupt = anweisungsListe(menge());
			if (!ende()){
				throw fehler("Unerwartetes '" + tokens.get(i).text + "'");
			}
		}else{
			if (ist("program") || ist("library")){
				i++;
				programmName = aktText();
				bisSemikolonUeberspringen();
			}else if (ist("unit")){
				bisSemikolonUeberspringen();
			}

			while (!ende()){
				if (ist("interface")){ //Interface-Teil einer Unit enthält nur Deklarationen
					while (!ende() && !ist("implementation")){
						i++;
					}
				}else if (ist("implementation")){
					i++;
				}else if (deklarationParsen()){
					//bereits verarbeitet
				}else if (ist("begin")){
					i++;
					haupt.addAll(anweisungsListe(menge("end")));
					erwarte("end");
					if (ist(".")){
						i++;
					}
				}else if (ist("initialization") || ist("finalization")){
					i++;
					haupt.addAll(anweisungsListe(menge("finalization", "end")));
				}else if (ist("end")){ //Ende einer Unit
					i++;
					if (ist(".")){
						i++;
					}
				}else{
					throw fehler("Unerwartetes '" + tokens.get(i).text + "'");
				}
			}
		}

		List<Diagramm> ergebnis = new ArrayList<Diagramm>();
		if (!haupt.isEmpty()){
			ergebnis.add(new Diagramm(!programmName.isEmpty() ? programmName : funktionen.isEmpty() ? "" : "Hauptprogramm", haupt));
		}
		ergebnis.addAll(funktionen);
		return ergebnis;
	}


	//verarbeitet var/const/type/uses-Abschnitte und Prozeduren/Funktionen, gibt false zurück, wenn keine Deklaration vorliegt
	private boolean deklarationParsen() throws CodeParseException {
		if (ist("class") && i + 1 < tokens.size() && routinen.contains(tokens.get(i + 1).klein)){
			i++; //class procedure / class function
		}

		if (routinen.contains(akt())){
			routineParsen();
			return true;
		}

		if (abschnitte.contains(akt())){
			boolean uses = ist("uses");
			i++;
			if (uses){
				bisSemikolonUeberspringen();
				return true;
			}
			//Deklarationen überspringen, dabei record/class/object ... end beachten
			int tiefe = 0;
			while (!ende()){
				String t = akt();
				String vorher = i > 0 ? tokens.get(i - 1).klein : "";
				if (tiefe == 0 && (abschnitte.contains(t) || t.equals("begin") || t.equals("implementation")
						|| t.equals("initialization") || t.equals("finalization")
						|| (routinen.contains(t) && !vorher.equals("=") && !vorher.equals(":") && !vorher.equals("of"))
						|| (t.equals("class") && i + 1 < tokens.size() && routinen.contains(tokens.get(i + 1).klein) && !vorher.equals("=")))){
					break;
				}
				if (t.equals("record") || (t.equals("object") && !vorher.equals("of")) || ((t.equals("class") || t.equals("interface")) && vorher.equals("=")
						&& i + 1 < tokens.size() && !tokens.get(i + 1).klein.equals(";") && !tokens.get(i + 1).klein.equals("of"))){
					tiefe++;
				}else if (t.equals("end")){
					if (tiefe == 0){
						break;
					}
					tiefe--;
				}
				i++;
			}
			return true;
		}

		return false;
	}


	private void routineParsen() throws CodeParseException {
		i++; //procedure, function, ...
		int nameStart = i;
		while (!ende() && !ist("(") && !ist(";") && !ist(":")){
			i++;
		}
		String name = ende() || nameStart == i ? "" : text(nameStart, i);
		String parameter = "";
		if (ist("(")){
			int auf = i;
			klammerUeberspringen();
			parameter = i - 1 > auf + 1 ? text(auf + 1, i - 1) : "";
		}
		String rueckgabe = "";
		if (ist(":")){ //Funktion mit Rückgabetyp
			int start = ++i;
			while (!ende() && !ist(";")){
				i++;
			}
			rueckgabe = i > start ? ": " + text(start, i) : "";
		}
		bisSemikolonUeberspringen();

		boolean ohneRumpf = false;
		while (!ende() && direktiven.contains(akt())){
			if (ist("forward") || ist("external")){
				ohneRumpf = true;
			}
			bisSemikolonUeberspringen();
		}
		if (ohneRumpf){
			return;
		}

		//lokale Deklarationen und verschachtelte Routinen
		while (!ende() && !ist("begin") && !ist("asm")){
			if (!deklarationParsen()){
				throw fehler("'begin' erwartet");
			}
		}
		if (ende()){
			throw fehler("'begin' erwartet");
		}

		List<CodeNode> inhalt;
		if (ist("asm")){
			blockBisEndUeberspringen();
			inhalt = new ArrayList<CodeNode>();
		}else{
			i++;
			inhalt = anweisungsListe(menge("end"));
			erwarte("end");
		}
		if (ist(";")){
			i++;
		}

		Diagramm.letztesReturnAlsAnweisung(inhalt);
		funktionen.add(new Diagramm(name + "(" + parameter + ")" + rueckgabe, inhalt));
	}


	//liest Anweisungen bis zu einem der Endwörter (wird nicht verbraucht); leere Menge = bis zum Ende des Codes
	private List<CodeNode> anweisungsListe(Set<String> endwoerter) throws CodeParseException {
		List<CodeNode> liste = new ArrayList<CodeNode>();
		while (true){
			if (ende()){
				if (endwoerter.isEmpty()){
					return liste;
				}
				throw fehler("'" + endwoerter.iterator().next() + "' fehlt");
			}
			if (endwoerter.contains(akt())){
				return liste;
			}
			if (ist(";")){
				i++;
				continue;
			}
			if (endwoerter.isEmpty() && ist("end") && i + 1 < tokens.size() && tokens.get(i + 1).klein.equals(".")){
				i += 2; //"end." am Ende einzelner Anweisungen
				continue;
			}

			anweisungParsen(liste);

			if (ende() || endwoerter.contains(akt()) || (endwoerter.isEmpty() && ist("end"))){
				continue;
			}
			if (ist(";")){
				i++;
			}else{
				throw fehler("';' erwartet vor '" + tokens.get(i).text + "'");
			}
		}
	}


	private void anweisungParsen(List<CodeNode> ziel) throws CodeParseException {
		String t = akt();

		if (t.equals("begin")){
			i++;
			ziel.addAll(anweisungsListe(menge("end")));
			erwarte("end");
			return;
		}

		if (t.equals("if")){
			i++;
			String bedingung = textBis(menge("then"));
			erwarte("then");
			List<CodeNode> ja = einzelneAnweisung();
			List<CodeNode> nein = new ArrayList<CodeNode>();
			if (ist("else")){
				i++;
				nein = einzelneAnweisung();
			}
			ziel.add(CodeNode.verzweigung(bedingung, ja, nein));
			return;
		}

		if (t.equals("while")){
			i++;
			String bedingung = textBis(menge("do"));
			erwarte("do");
			List<CodeNode> inhalt = einzelneAnweisung();
			if (bedingung.equalsIgnoreCase("true")){
				ziel.add(CodeNode.schleife(Struktogramm.typEndlosschleife, GlobalSettings.gibElementBeschriftung(Struktogramm.typEndlosschleife), inhalt));
			}else{
				ziel.add(CodeNode.schleife(Struktogramm.typWhileSchleife, bedingung, inhalt));
			}
			return;
		}

		if (t.equals("for")){
			i++;
			String kopf = textBis(menge("do"));
			erwarte("do");
			ziel.add(CodeNode.schleife(Struktogramm.typForSchleife, kopf, einzelneAnweisung()));
			return;
		}

		if (t.equals("repeat")){
			i++;
			List<CodeNode> inhalt = anweisungsListe(menge("until"));
			erwarte("until");
			String bedingung = textBis(anweisungsEnde);
			//"bis" kennzeichnet die Abbruchbedingung, ohne Präfix gilt die Bedingung wie bei do-while als Wiederholungsbedingung
			ziel.add(CodeNode.schleife(Struktogramm.typDoUntilSchleife, "bis " + bedingung, inhalt));
			return;
		}

		if (t.equals("case")){
			i++;
			ziel.add(caseParsen(textBis(menge("of"))));
			return;
		}

		if (t.equals("with")){ //Inhalt wird direkt übernommen
			i++;
			textBis(menge("do"));
			erwarte("do");
			ziel.addAll(einzelneAnweisung());
			return;
		}

		if (t.equals("try")){
			i++;
			ziel.addAll(anweisungsListe(menge("except", "finally", "end")));
			if (ist("except")){ //except-Teil wird nicht dargestellt
				i++;
				int tiefe = 0;
				while (!ende() && !(tiefe == 0 && ist("end"))){
					if (ist("begin") || ist("try") || ist("case") || ist("asm")){
						tiefe++;
					}else if (ist("end")){
						tiefe--;
					}
					i++;
				}
			}else if (ist("finally")){
				i++;
				ziel.addAll(anweisungsListe(menge("end")));
			}
			erwarte("end");
			return;
		}

		if (t.equals("asm")){
			blockBisEndUeberspringen();
			return;
		}

		if (t.equals("exit") || t.equals("break") || t.equals("continue") || t.equals("halt") || t.equals("raise") || t.equals("goto")){
			ziel.add(CodeNode.aussprung(textBis(anweisungsEnde)));
			return;
		}

		if (anweisungsEnde.contains(t) || t.equals("then") || t.equals("do") || t.equals("of")){
			throw fehler("Unerwartetes '" + tokens.get(i).text + "'");
		}

		//Sprungmarke "marke:" überspringen
		if (i + 1 < tokens.size() && tokens.get(i + 1).klein.equals(":") && !anweisungsEnde.contains(t)){
			i += 2;
			if (!ende() && !anweisungsEnde.contains(akt())){
				anweisungParsen(ziel);
			}
			return;
		}

		String text = textBis(anweisungsEnde);
		ziel.add(CodeNode.anweisung(semikolonBehalten ? text + ";" : text));
	}


	//eine einzelne Anweisung (z.B. nach then/else/do), kann auch leer sein
	private List<CodeNode> einzelneAnweisung() throws CodeParseException {
		List<CodeNode> liste = new ArrayList<CodeNode>();
		if (!ende() && !anweisungsEnde.contains(akt())){
			anweisungParsen(liste);
		}
		return liste;
	}


	private CodeNode caseParsen(String ausdruck) throws CodeParseException {
		erwarte("of");
		CodeNode knoten = new CodeNode(Struktogramm.typFallauswahl, ausdruck);
		List<CodeNode> sonst = new ArrayList<CodeNode>();

		while (true){
			if (ende()){
				throw fehler("'end' am Ende von case fehlt");
			}
			if (ist("end")){
				break;
			}
			if (ist(";")){
				i++;
				continue;
			}
			if (ist("else") || ist("otherwise")){
				i++;
				sonst = anweisungsListe(menge("end"));
				break;
			}
			String label = textBis(menge(":"));
			erwarte(":");
			knoten.fallHinzufuegen(label, einzelneAnweisung());
		}
		erwarte("end");

		if (knoten.getFaelle().isEmpty()){
			knoten.fallHinzufuegen("", new ArrayList<CodeNode>());
		}
		knoten.fallHinzufuegen("Sonst", sonst);
		return knoten;
	}


	//Text ab dem aktuellen Token bis vor eines der Stopp-Tokens (außerhalb von Klammern)
	private String textBis(Set<String> stopp) throws CodeParseException {
		int start = i;
		int tiefe = 0;
		while (!ende()){
			String t = akt();
			if (tiefe == 0 && stopp.contains(t)){
				break;
			}
			if (t.equals("(") || t.equals("[")){
				tiefe++;
			}else if ((t.equals(")") || t.equals("]")) && tiefe > 0){
				tiefe--;
			}
			i++;
		}
		if (ende() && !stopp.contains(";")){
			throw fehler("'" + stopp.iterator().next() + "' fehlt");
		}
		return start == i ? "" : text(start, i);
	}


	private void klammerUeberspringen() throws CodeParseException {
		int tiefe = 0;
		while (!ende()){
			if (ist("(")){
				tiefe++;
			}else if (ist(")")){
				tiefe--;
				if (tiefe == 0){
					i++;
					return;
				}
			}
			i++;
		}
		throw fehler("')' fehlt");
	}


	private void blockBisEndUeberspringen(){
		while (!ende() && !ist("end")){
			i++;
		}
		if (!ende()){
			i++;
		}
	}


	private void bisSemikolonUeberspringen(){
		int tiefe = 0;
		while (!ende()){
			if (ist("(")){
				tiefe++;
			}else if (ist(")")){
				tiefe--;
			}else if (tiefe <= 0 && ist(";")){
				i++;
				return;
			}
			i++;
		}
	}


	//Originaltext der Tokens von bis ausschließlich bis
	private String text(int von, int bis){
		return CLikeParser.norm(quelle.substring(tokens.get(von).start, tokens.get(bis - 1).ende));
	}

	private String akt(){
		return tokens.get(i).klein;
	}

	private String aktText(){
		return ende() ? "" : tokens.get(i).text;
	}

	private boolean ist(String klein){
		return !ende() && tokens.get(i).klein.equals(klein);
	}

	private boolean ende(){
		return i >= tokens.size();
	}

	private void erwarte(String klein) throws CodeParseException {
		if (!ist(klein)){
			throw fehler("'" + klein + "' erwartet" + (ende() ? "" : " statt '" + tokens.get(i).text + "'"));
		}
		i++;
	}

	private CodeParseException fehler(String meldung){
		int zeile = tokens.isEmpty() ? 0 : tokens.get(Math.min(i, tokens.size() - 1)).zeile;
		return new CodeParseException(meldung, zeile);
	}

	private static Set<String> menge(String... w){
		return new HashSet<String>(Arrays.asList(w));
	}


	private static String kommentareEntfernen(String code){
		StringBuilder sb = new StringBuilder(code.length());
		int k = 0;
		while (k < code.length()){
			char c = code.charAt(k);
			if (c == '\''){
				int j = k + 1;
				while (j < code.length() && code.charAt(j) != '\'' && code.charAt(j) != '\n'){
					j++;
				}
				j = Math.min(j + 1, code.length());
				sb.append(code, k, j);
				k = j;
				continue;
			}
			int ende = -1;
			if (c == '{'){
				ende = code.indexOf('}', k + 1);
				ende = ende < 0 ? code.length() : ende + 1;
			}else if (c == '(' && k + 1 < code.length() && code.charAt(k + 1) == '*'){
				ende = code.indexOf("*)", k + 2);
				ende = ende < 0 ? code.length() : ende + 2;
			}else if (c == '/' && k + 1 < code.length() && code.charAt(k + 1) == '/'){
				ende = code.indexOf('\n', k);
				ende = ende < 0 ? code.length() : ende;
			}
			if (ende >= 0){
				for (; k < ende; k++){
					sb.append(code.charAt(k) == '\n' ? '\n' : ' ');
				}
				continue;
			}
			sb.append(c);
			k++;
		}
		return sb.toString();
	}


	private static List<Token> zerlegen(String q){
		List<Token> liste = new ArrayList<Token>();
		int k = 0;
		int zeile = 1;
		while (k < q.length()){
			char c = q.charAt(k);
			if (c == '\n'){
				zeile++;
				k++;
				continue;
			}
			if (Character.isWhitespace(c)){
				k++;
				continue;
			}

			int start = k;
			if (Character.isLetter(c) || c == '_' || c == '&'){
				k++;
				while (k < q.length() && (Character.isLetterOrDigit(q.charAt(k)) || q.charAt(k) == '_')){
					k++;
				}
			}else if (Character.isDigit(c) || c == '$' || c == '#' || c == '%'){
				k++;
				while (k < q.length() && (Character.isLetterOrDigit(q.charAt(k))
						|| (q.charAt(k) == '.' && k + 1 < q.length() && Character.isDigit(q.charAt(k + 1))))){
					k++;
				}
			}else if (c == '\''){
				k++;
				while (k < q.length()){
					if (q.charAt(k) == '\''){
						if (k + 1 < q.length() && q.charAt(k + 1) == '\''){
							k += 2;
							continue;
						}
						k++;
						break;
					}
					if (q.charAt(k) == '\n'){
						break;
					}
					k++;
				}
			}else{
				String zwei = k + 1 < q.length() ? q.substring(k, k + 2) : "";
				if (zwei.equals(":=") || zwei.equals("<=") || zwei.equals(">=") || zwei.equals("<>") || zwei.equals("..")
						|| zwei.equals("+=") || zwei.equals("-=") || zwei.equals("*=") || zwei.equals("/=")){
					k += 2;
				}else{
					k++;
				}
			}
			liste.add(new Token(q.substring(start, k), start, k, zeile));
		}
		return liste;
	}
}
