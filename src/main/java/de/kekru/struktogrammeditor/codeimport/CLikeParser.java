package de.kekru.struktogrammeditor.codeimport;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.kekru.struktogrammeditor.control.GlobalSettings;
import de.kekru.struktogrammeditor.control.Struktogramm;

//Liest Quellcode mit C-ähnlicher Syntax (Java, C, C++, C#, JavaScript) und erzeugt daraus Struktogramme
public class CLikeParser {

	private final char[] s; //Quelltext, Kommentare sind durch Leerzeichen ersetzt
	private int pos;
	private final boolean semikolonBehalten;
	private final List<Diagramm> funktionen = new ArrayList<Diagramm>();
	private boolean letzterCaseMitPfeil;

	private static final Pattern jsPfeilFunktion = Pattern.compile("(?s)^(?:export\\s+)?(?:const|let|var)\\s+([\\w$]+)\\s*=\\s*(?:async\\s*)?(?:\\(([^()]*)\\)|([\\w$]+))\\s*=>$");
	private static final Pattern jsFunktionsAusdruck = Pattern.compile("(?s)^(?:export\\s+)?(?:const|let|var)\\s+([\\w$]+)\\s*=\\s*(?:async\\s+)?function\\s*\\*?\\s*[\\w$]*\\s*\\(([^()]*)\\)$");
	private static final Pattern typDeklaration = Pattern.compile("(?s)(^|.*[\\s>])(class|interface|enum|struct|namespace|record|union)(\\s.*|$)");
	private static final Pattern cPrototyp = Pattern.compile("(?s)^[A-Za-z_][\\w:<>,\\*&\\s\\[\\]]*[\\s\\*&]+[A-Za-z_]\\w*\\s*\\([^()]*\\)\\s*(const)?$");

	public CLikeParser(String code, boolean semikolonBehalten){
		this.s = kommentareEntfernen(code).toCharArray();
		this.semikolonBehalten = semikolonBehalten;
	}

	public List<Diagramm> parse() throws CodeParseException {
		pos = 0;
		List<CodeNode> haupt = new ArrayList<CodeNode>();
		bereichParsen(haupt, true);

		List<Diagramm> ergebnis = new ArrayList<Diagramm>();
		if (!haupt.isEmpty()){
			ergebnis.add(new Diagramm(funktionen.isEmpty() ? "" : "Hauptprogramm", haupt));
		}
		ergebnis.addAll(funktionen);
		return ergebnis;
	}


	//liest Anweisungen bis zur schließenden } (wird nicht verbraucht) oder bis zum Ende
	private void bereichParsen(List<CodeNode> ziel, boolean obersteEbene) throws CodeParseException {
		while (true){
			leerzeichenUeberspringen();
			if (ende()){
				return;
			}
			if (s[pos] == '}'){
				if (obersteEbene){
					throw fehler("Unerwartete '}'");
				}
				return;
			}
			anweisungParsen(ziel, obersteEbene);
		}
	}


	private void anweisungParsen(List<CodeNode> ziel, boolean obersteEbene) throws CodeParseException {
		leerzeichenUeberspringen();
		if (ende()){
			return;
		}

		char c = s[pos];

		if (c == ';'){ //leere Anweisung
			pos++;
			return;
		}

		if (c == '{'){ //Block, Inhalt wird direkt übernommen
			int auf = pos;
			pos++;
			bereichParsen(ziel, false);
			schliessendeKlammer(auf);
			return;
		}

		if (c == '@' && pos + 1 < s.length && Character.isJavaIdentifierStart(s[pos + 1])){ //Annotation überspringen
			pos++;
			while (!ende() && (Character.isJavaIdentifierPart(s[pos]) || s[pos] == '.')){
				pos++;
			}
			leerzeichenUeberspringen();
			if (!ende() && s[pos] == '('){
				klammerInhalt();
			}
			return;
		}

		String wort = wortAnPos();

		if (wort.equals("if")){
			pos += 2;
			String bedingung = bedingungInKlammern();
			List<CodeNode> ja = new ArrayList<CodeNode>();
			anweisungParsen(ja, false);
			List<CodeNode> nein = new ArrayList<CodeNode>();
			leerzeichenUeberspringen();
			if (wortAnPos().equals("else")){
				pos += 4;
				anweisungParsen(nein, false);
			}
			ziel.add(CodeNode.verzweigung(norm(bedingung), ja, nein));
			return;
		}

		if (wort.equals("while")){
			pos += 5;
			String bedingung = norm(bedingungInKlammern());
			List<CodeNode> inhalt = new ArrayList<CodeNode>();
			leerzeichenUeberspringen();
			if (!ende() && s[pos] == ';'){
				pos++;
			}else{
				anweisungParsen(inhalt, false);
			}
			if (bedingung.matches("true|1|True")){
				ziel.add(CodeNode.schleife(Struktogramm.typEndlosschleife, GlobalSettings.gibElementBeschriftung(Struktogramm.typEndlosschleife), inhalt));
			}else{
				ziel.add(CodeNode.schleife(Struktogramm.typWhileSchleife, bedingung, inhalt));
			}
			return;
		}

		if (wort.equals("for") || wort.equals("foreach")){
			pos += wort.length();
			leerzeichenUeberspringen();
			if (wortAnPos().equals("await")){ //JavaScript: for await (...)
				pos += 5;
			}
			String kopf = norm(klammerInhalt());
			List<CodeNode> inhalt = new ArrayList<CodeNode>();
			anweisungParsen(inhalt, false);
			if (kopf.matches(";\\s*;")){
				ziel.add(CodeNode.schleife(Struktogramm.typEndlosschleife, GlobalSettings.gibElementBeschriftung(Struktogramm.typEndlosschleife), inhalt));
			}else{
				ziel.add(CodeNode.schleife(Struktogramm.typForSchleife, kopf, inhalt));
			}
			return;
		}

		if (wort.equals("do")){
			pos += 2;
			List<CodeNode> inhalt = new ArrayList<CodeNode>();
			anweisungParsen(inhalt, false);
			leerzeichenUeberspringen();
			if (!wortAnPos().equals("while")){
				throw fehler("'while' nach dem do-Block erwartet");
			}
			pos += 5;
			String bedingung = bedingungInKlammern();
			leerzeichenUeberspringen();
			if (!ende() && s[pos] == ';'){
				pos++;
			}
			ziel.add(CodeNode.schleife(Struktogramm.typDoUntilSchleife, norm(bedingung), inhalt));
			return;
		}

		if (wort.equals("switch")){
			pos += 6;
			ziel.add(switchParsen(norm(bedingungInKlammern())));
			return;
		}

		if (wort.equals("try")){
			pos += 3;
			leerzeichenUeberspringen();
			if (!ende() && s[pos] == '('){ //try-with-resources
				klammerInhalt();
			}
			anweisungParsen(ziel, false); //Inhalt des try-Blocks wird direkt übernommen
			while (true){
				leerzeichenUeberspringen();
				String w = wortAnPos();
				if (w.equals("catch")){ //catch-Blöcke werden nicht dargestellt
					pos += 5;
					leerzeichenUeberspringen();
					if (!ende() && s[pos] == '('){
						klammerInhalt();
					}
					leerzeichenUeberspringen();
					if (wortAnPos().equals("when")){
						pos += 4;
						klammerInhalt();
					}
					anweisungParsen(new ArrayList<CodeNode>(), false);
				}else if (w.equals("finally")){
					pos += 7;
					anweisungParsen(ziel, false);
				}else{
					break;
				}
			}
			return;
		}

		if (wort.equals("return") || wort.equals("break") || wort.equals("continue") || wort.equals("throw") || wort.equals("goto")){
			ziel.add(CodeNode.aussprung(norm(bisAnweisungsende())));
			return;
		}

		if (wort.equals("synchronized") || wort.equals("lock") || wort.equals("using") || wort.equals("fixed")
				|| wort.equals("checked") || wort.equals("unchecked") || wort.equals("unsafe")){
			int p = naechstesZeichenNach(pos + wort.length());
			if (p < s.length && (s[p] == '(' || s[p] == '{')){
				pos = p;
				if (s[pos] == '('){
					klammerInhalt();
				}
				anweisungParsen(ziel, false); //Inhalt wird direkt übernommen
				return;
			}
		}

		if (wort.equals("else")){
			throw fehler("'else' ohne passendes 'if'");
		}
		if (wort.equals("catch") || wort.equals("finally")){
			throw fehler("'" + wort + "' ohne passendes 'try'");
		}
		if (wort.equals("case") || wort.equals("default") && naechstesZeichenIst(pos + 7, ':')){
			throw fehler("'" + wort + "' außerhalb von switch");
		}

		//Sprungmarke (z.B. "aussen:" vor einer Schleife) überspringen
		if (!wort.isEmpty()){
			int p = naechstesZeichenNach(pos + wort.length());
			if (p < s.length && s[p] == ':' && (p + 1 >= s.length || s[p + 1] != ':')){
				pos = p + 1;
				return;
			}
		}

		allgemeineAnweisungParsen(ziel, obersteEbene);
	}


	//Anweisung ohne Schlüsselwort: einfache Anweisung, Klasse oder Funktion
	private void allgemeineAnweisungParsen(List<CodeNode> ziel, boolean obersteEbene) throws CodeParseException {
		int start = pos;
		int kopfEnde = kopfEndeFinden(start);

		if (kopfEnde < s.length && s[kopfEnde] == '{'){
			String kopf = new String(s, start, kopfEnde - start).trim();

			if (istTypDeklaration(kopf)){ //Klasse o.ä.: nur die Methoden darin werden zu Struktogrammen
				pos = kopfEnde + 1;
				bereichParsen(new ArrayList<CodeNode>(), false);
				schliessendeKlammer(kopfEnde);
				optionalesSemikolon();
				return;
			}

			String funktionsname = funktionsName(kopf);
			if (funktionsname != null){
				pos = kopfEnde + 1;
				List<CodeNode> inhalt = new ArrayList<CodeNode>();
				bereichParsen(inhalt, false);
				schliessendeKlammer(kopfEnde);
				optionalesSemikolon();
				Diagramm.letztesReturnAlsAnweisung(inhalt);
				funktionen.add(new Diagramm(funktionsname, inhalt));
				return;
			}

			int asiEnde = asiEndeFinden(start, kopfEnde);
			if (asiEnde < 0 && !istAusdruckMitBlock(kopf)){ //z.B. static { ... } oder extern "C" { ... }: Inhalt direkt übernehmen
				pos = kopfEnde + 1;
				bereichParsen(ziel, obersteEbene);
				schliessendeKlammer(kopfEnde);
				return;
			}
		}

		pos = start;
		String text = norm(bisAnweisungsende());
		if (text.isEmpty()){
			if (pos == start){ //kein Fortschritt, Zeichen überspringen damit keine Endlosschleife entsteht
				throw fehler("Unerwartetes Zeichen '" + s[pos] + "'");
			}
			return;
		}

		if (obersteEbene && istDeklarationOhneCode(text)){
			return;
		}

		ziel.add(CodeNode.anweisung(semikolonBehalten ? text + ";" : text));
	}


	private CodeNode switchParsen(String ausdruck) throws CodeParseException {
		erwarte('{');

		CodeNode knoten = new CodeNode(Struktogramm.typFallauswahl, ausdruck);
		List<String> offeneLabels = new ArrayList<String>(); //case-Labels ohne eigenen Inhalt (Durchfallen zum nächsten case)
		List<CodeNode> sonst = new ArrayList<CodeNode>();

		while (true){
			leerzeichenUeberspringen();
			if (ende()){
				throw fehler("'}' am Ende von switch fehlt");
			}
			if (s[pos] == '}'){
				pos++;
				break;
			}

			String wort = wortAnPos();
			boolean istDefault;
			String label = "";

			if (wort.equals("case")){
				pos += 4;
				label = caseLabelLesen();
				istDefault = false;
			}else if (wort.equals("default")){
				pos += 7;
				caseLabelLesen();
				istDefault = true;
			}else{
				throw fehler("'case' oder 'default' erwartet");
			}

			List<CodeNode> inhalt = new ArrayList<CodeNode>();
			if (letzterCaseMitPfeil){
				anweisungParsen(inhalt, false);
			}else{
				while (true){
					leerzeichenUeberspringen();
					String w = wortAnPos();
					if (ende() || s[pos] == '}' || w.equals("case") || w.equals("default")){
						break;
					}
					anweisungParsen(inhalt, false);
				}
			}

			entferneAbschliessendesBreak(inhalt);

			if (istDefault){
				sonst = inhalt;
				offeneLabels.clear();
			}else if (inhalt.isEmpty() && !letzterCaseMitPfeil){
				offeneLabels.add(label);
			}else{
				offeneLabels.add(label);
				knoten.fallHinzufuegen(verbinden(offeneLabels), inhalt);
				offeneLabels.clear();
			}
		}

		if (!offeneLabels.isEmpty()){
			knoten.fallHinzufuegen(verbinden(offeneLabels), new ArrayList<CodeNode>());
		}
		if (knoten.getFaelle().isEmpty()){
			knoten.fallHinzufuegen("", new ArrayList<CodeNode>());
		}
		knoten.fallHinzufuegen("Sonst", sonst);
		return knoten;
	}


	//liest das Label nach case bzw. default bis ':' oder '->'
	private String caseLabelLesen() throws CodeParseException {
		int start = pos;
		int tiefe = 0;
		while (!ende()){
			char c = s[pos];
			if (c == '"' || c == '\'' || c == '`'){
				pos = stringEnde(pos);
				continue;
			}
			if (c == '(' || c == '[' || c == '{'){
				tiefe++;
			}else if (c == ')' || c == ']' || c == '}'){
				tiefe--;
			}else if (tiefe == 0 && c == ':'){
				if (pos + 1 < s.length && s[pos + 1] == ':'){
					pos += 2;
					continue;
				}
				String label = new String(s, start, pos - start);
				pos++;
				letzterCaseMitPfeil = false;
				return norm(label);
			}else if (tiefe == 0 && c == '-' && pos + 1 < s.length && s[pos + 1] == '>'){
				String label = new String(s, start, pos - start);
				pos += 2;
				letzterCaseMitPfeil = true;
				return norm(label);
			}
			pos++;
		}
		throw fehler("':' nach case erwartet");
	}


	private static void entferneAbschliessendesBreak(List<CodeNode> liste){
		if (!liste.isEmpty()){
			CodeNode letztes = liste.get(liste.size() - 1);
			if (letztes.getTyp() == Struktogramm.typAussprung && letztes.getText().equals("break")){
				liste.remove(liste.size() - 1);
			}
		}
	}

	private static String verbinden(List<String> labels){
		StringBuilder sb = new StringBuilder();
		for (String l : labels){
			if (sb.length() > 0){
				sb.append(", ");
			}
			sb.append(l);
		}
		return sb.toString();
	}


	//sucht ab start das erste ';' oder '{' außerhalb von Klammern, bzw. eine schließende '}'
	private int kopfEndeFinden(int start){
		int tiefe = 0;
		int i = start;
		while (i < s.length){
			char c = s[i];
			if (c == '"' || c == '\'' || c == '`'){
				i = stringEnde(i);
				continue;
			}
			if (c == '(' || c == '['){
				tiefe++;
			}else if (c == ')' || c == ']'){
				if (tiefe > 0){
					tiefe--;
				}
			}else if (tiefe == 0 && (c == ';' || c == '{' || c == '}')){
				return i;
			}
			i++;
		}
		return s.length;
	}


	//liest eine Anweisung bis zum ';' (wird verbraucht) bzw. bis zur '}' des umgebenden Blocks;
	//fehlt das Semikolon (JavaScript), endet die Anweisung auch an einem passenden Zeilenumbruch
	private String bisAnweisungsende(){
		int start = pos;
		int tiefe = 0;
		while (!ende()){
			char c = s[pos];
			if (c == '"' || c == '\'' || c == '`'){
				pos = stringEnde(pos);
				continue;
			}
			if (c == '(' || c == '[' || c == '{'){
				tiefe++;
			}else if (c == ')' || c == ']' || c == '}'){
				if (tiefe == 0){
					return new String(s, start, pos - start); //'}' des umgebenden Blocks
				}
				tiefe--;
			}else if (tiefe == 0 && c == ';'){
				String text = new String(s, start, pos - start);
				pos++;
				return text;
			}else if (tiefe == 0 && c == '\n' && zeilenumbruchBeendetAnweisung(start, pos)){
				String text = new String(s, start, pos - start);
				pos++;
				return text;
			}
			pos++;
		}
		return new String(s, start, pos - start);
	}


	//prüft, ob zwischen start und kopfEnde ein Zeilenumbruch die Anweisung beendet (fehlendes Semikolon), gibt dessen Position zurück oder -1
	private int asiEndeFinden(int start, int kopfEnde){
		int tiefe = 0;
		int i = start;
		while (i < kopfEnde){
			char c = s[i];
			if (c == '"' || c == '\'' || c == '`'){
				i = stringEnde(i);
				continue;
			}
			if (c == '(' || c == '['){
				tiefe++;
			}else if (c == ')' || c == ']'){
				if (tiefe > 0){
					tiefe--;
				}
			}else if (tiefe == 0 && c == '\n' && zeilenumbruchBeendetAnweisung(start, i)){
				return i;
			}
			i++;
		}
		return -1;
	}


	private boolean zeilenumbruchBeendetAnweisung(int start, int umbruch){
		String bisher = new String(s, start, umbruch - start).trim();
		if (bisher.isEmpty()){
			return false;
		}
		char letztes = bisher.charAt(bisher.length() - 1);
		boolean postInkrement = bisher.endsWith("++") || bisher.endsWith("--");
		if (!postInkrement && "+-*/%=&|^!<>?:,.([{".indexOf(letztes) >= 0){
			return false;
		}
		int p = naechstesZeichenNach(umbruch + 1);
		if (p >= s.length){
			return true;
		}
		return ".+-*/%=&|^?:,)]}".indexOf(s[p]) < 0 && s[p] != '{';
	}


	private boolean istTypDeklaration(String kopf){
		Matcher m = typDeklaration.matcher(kopf);
		if (!m.matches() || hatZuweisungAufObersterEbene(kopf)){
			return false;
		}
		int klammer = kopf.indexOf('(');
		return klammer < 0 || m.group(2).equals("record");
	}


	//gibt "name(parameter)" zurück, wenn kopf der Kopf einer Funktions-/Methodendefinition ist, sonst null
	private String funktionsName(String kopf){
		Matcher m = jsPfeilFunktion.matcher(kopf);
		if (m.matches()){
			return m.group(1) + "(" + norm(m.group(2) != null ? m.group(2) : m.group(3)) + ")";
		}
		m = jsFunktionsAusdruck.matcher(kopf);
		if (m.matches()){
			return m.group(1) + "(" + norm(m.group(2)) + ")";
		}

		if (hatZuweisungAufObersterEbene(kopf) || kopf.matches("(?s)(new|return|else|throw|await|yield)\\b.*") || kopf.matches("(?s).*\\snew\\s.*")){
			return null;
		}

		int auf = -1;
		int tiefe = 0;
		for (int i = 0; i < kopf.length(); i++){
			char c = kopf.charAt(i);
			if (c == '"' || c == '\''){
				return null;
			}
			if (c == '(' && tiefe == 0){
				auf = i;
				break;
			}
			if (c == '<'){
				tiefe++;
			}else if (c == '>' && tiefe > 0){
				tiefe--;
			}
		}
		if (auf < 0){
			return null;
		}

		int zu = passendeKlammer(kopf, auf);
		if (zu < 0 || !kopf.substring(zu + 1).matches("(?s)[\\w\\s,.:<>\\[\\]]*")){
			return null;
		}

		int nameEnde = auf;
		while (nameEnde > 0 && Character.isWhitespace(kopf.charAt(nameEnde - 1))){
			nameEnde--;
		}
		int nameStart = nameEnde;
		while (nameStart > 0 && (Character.isJavaIdentifierPart(kopf.charAt(nameStart - 1)) || kopf.charAt(nameStart - 1) == '~')){
			nameStart--;
		}
		if (nameStart == nameEnde){
			return null;
		}

		//Rückgabetyp (ohne Modifizierer wie public/static) mit in den Namen übernehmen, z.B. "int fakultaet(int n)"
		String typ = kopf.substring(0, nameStart)
				.replaceAll("@\\w+(\\([^)]*\\))?", " ")
				.replaceAll("\\b(public|private|protected|internal|static|final|abstract|synchronized|native|strictfp|default|async|export|function|override|virtual|sealed|extern|inline|unsafe|new|readonly|get|set|constexpr|explicit|friend)\\b", " ")
				.replaceAll("^\\s*<[^>]*>", " ")
				.replaceAll("\\s+", " ").trim();
		if (!typ.matches("[\\w<>\\[\\],.:*& ]+")){
			typ = "";
		}
		return (typ.isEmpty() ? "" : typ + " ") + kopf.substring(nameStart, nameEnde) + "(" + norm(kopf.substring(auf + 1, zu)) + ")";
	}


	private static int passendeKlammer(String text, int auf){
		int tiefe = 0;
		for (int i = auf; i < text.length(); i++){
			char c = text.charAt(i);
			if (c == '('){
				tiefe++;
			}else if (c == ')'){
				tiefe--;
				if (tiefe == 0){
					return i;
				}
			}
		}
		return -1;
	}


	//true, wenn außerhalb von Klammern ein Zuweisungs-'=' steht (nicht ==, <=, >=, !=, =>)
	private static boolean hatZuweisungAufObersterEbene(String text){
		int tiefe = 0;
		for (int i = 0; i < text.length(); i++){
			char c = text.charAt(i);
			if (c == '(' || c == '['){
				tiefe++;
			}else if (c == ')' || c == ']'){
				tiefe--;
			}else if (c == '=' && tiefe == 0){
				char vorher = i > 0 ? text.charAt(i - 1) : ' ';
				char nachher = i + 1 < text.length() ? text.charAt(i + 1) : ' ';
				if (nachher != '=' && nachher != '>' && "=<>!".indexOf(vorher) < 0){
					return true;
				}
			}
		}
		return false;
	}


	private static boolean istAusdruckMitBlock(String kopf){
		return hatZuweisungAufObersterEbene(kopf) || kopf.matches("(?s).*(=>|->|[=,(\\]?:]|\\breturn)$") || kopf.matches("(?s)^(return|new|throw|await|yield)\\b.*");
	}


	//Importe, Präprozessor-Reste, Funktionsprototypen usw. auf oberster Ebene
	private static boolean istDeklarationOhneCode(String text){
		if (text.matches("(?s)^(import|package|typedef|#).*") || text.matches("(?s)^using\\s+[\\w.=\\s]+$")
				|| text.matches("^[\"']use strict[\"']$") || text.matches("(?s)^(struct|class|enum)\\s+\\w+$")){
			return true;
		}
		return cPrototyp.matcher(text).matches() && !text.matches("(?s)^(return|new|delete|throw|await|yield|typeof|else)\\b.*");
	}


	private String klammerInhalt() throws CodeParseException {
		return klammerInhalt(true);
	}

	//Inhalt einer Bedingung in Klammern (if, while, switch): ein Semikolon darin bedeutet, dass die ')' fehlt
	private String bedingungInKlammern() throws CodeParseException {
		return klammerInhalt(false);
	}

	private String klammerInhalt(boolean semikolonErlaubt) throws CodeParseException {
		leerzeichenUeberspringen();
		if (ende() || s[pos] != '('){
			throw fehler("'(' erwartet");
		}
		int start = pos + 1;
		int tiefe = 0;
		while (!ende()){
			char c = s[pos];
			if (c == '"' || c == '\'' || c == '`'){
				pos = stringEnde(pos);
				continue;
			}
			if (c == '(' || c == '[' || c == '{'){
				tiefe++;
			}else if (c == ')' || c == ']' || c == '}'){
				tiefe--;
				if (tiefe == 0){
					String inhalt = new String(s, start, pos - start);
					pos++;
					return inhalt;
				}
			}else if (c == ';' && tiefe == 1 && !semikolonErlaubt){
				break; //Semikolon in einer Bedingung: die schließende Klammer fehlt vermutlich hier
			}
			pos++;
		}
		pos = start - 1; //Fehler bei der öffnenden Klammer melden, nicht am Dateiende
		throw fehler("')' zu dieser Klammer fehlt");
	}


	//gibt den Index nach dem String zurück, der bei i beginnt
	private int stringEnde(int i){
		char quote = s[i];
		if (quote == '"' && i + 2 < s.length && s[i + 1] == '"' && s[i + 2] == '"'){ //Java Textblock
			int j = i + 3;
			while (j + 2 < s.length && !(s[j] == '"' && s[j + 1] == '"' && s[j + 2] == '"')){
				j++;
			}
			return Math.min(j + 3, s.length);
		}
		int j = i + 1;
		while (j < s.length){
			if (s[j] == '\\'){
				j += 2;
				continue;
			}
			if (s[j] == quote){
				return j + 1;
			}
			if (s[j] == '\n' && quote != '`'){ //nicht geschlossener String endet am Zeilenende
				return j;
			}
			j++;
		}
		return s.length;
	}


	private static String kommentareEntfernen(String code){
		StringBuilder sb = new StringBuilder(code.length());
		int i = 0;
		boolean zeilenAnfang = true;
		while (i < code.length()){
			char c = code.charAt(i);

			if (c == '"' || c == '\'' || c == '`'){
				int j = i + 1;
				while (j < code.length() && code.charAt(j) != c && (code.charAt(j) != '\n' || c == '`')){
					if (code.charAt(j) == '\\'){
						j++;
					}
					j++;
				}
				j = Math.min(j + 1, code.length());
				sb.append(code, i, j);
				i = j;
				zeilenAnfang = false;
				continue;
			}

			if (c == '/' && i + 1 < code.length() && code.charAt(i + 1) == '/'){
				while (i < code.length() && code.charAt(i) != '\n'){
					sb.append(' ');
					i++;
				}
				continue;
			}

			if (c == '/' && i + 1 < code.length() && code.charAt(i + 1) == '*'){
				int ende = code.indexOf("*/", i + 2);
				ende = ende < 0 ? code.length() : ende + 2;
				for (; i < ende; i++){
					sb.append(code.charAt(i) == '\n' ? '\n' : ' ');
				}
				continue;
			}

			if (c == '#' && zeilenAnfang){ //Präprozessor-Zeile (C/C++) bzw. #region (C#)
				while (i < code.length() && code.charAt(i) != '\n'){
					sb.append(' ');
					i++;
				}
				continue;
			}

			if (c == '\n'){
				zeilenAnfang = true;
			}else if (!Character.isWhitespace(c)){
				zeilenAnfang = false;
			}
			sb.append(c);
			i++;
		}
		return sb.toString();
	}


	//Zeilenumbrüche samt Einrückung durch ein Leerzeichen ersetzen
	static String norm(String text){
		return text.replaceAll("[ \\t]*\\r?\\n\\s*", " ").trim();
	}

	private String wortAnPos(){
		if (ende() || !Character.isJavaIdentifierStart(s[pos])){
			return "";
		}
		int i = pos;
		while (i < s.length && Character.isJavaIdentifierPart(s[i])){
			i++;
		}
		return new String(s, pos, i - pos);
	}

	private int naechstesZeichenNach(int i){
		while (i < s.length && Character.isWhitespace(s[i])){
			i++;
		}
		return i;
	}

	private boolean naechstesZeichenIst(int i, char c){
		int p = naechstesZeichenNach(i);
		return p < s.length && s[p] == c;
	}

	private void leerzeichenUeberspringen(){
		while (!ende() && Character.isWhitespace(s[pos])){
			pos++;
		}
	}

	private void optionalesSemikolon(){
		int p = naechstesZeichenNach(pos);
		if (p < s.length && s[p] == ';'){
			pos = p + 1;
		}
	}

	//erwartet die '}' zu der '{' an Position auf; fehlt sie bis zum Ende, wird der Fehler bei der öffnenden Klammer gemeldet
	private void schliessendeKlammer(int auf) throws CodeParseException {
		leerzeichenUeberspringen();
		if (ende()){
			pos = auf;
			throw fehler("'}' zu dieser Klammer fehlt");
		}
		erwarte('}');
	}

	private void erwarte(char c) throws CodeParseException {
		leerzeichenUeberspringen();
		if (ende() || s[pos] != c){
			throw fehler("'" + c + "' erwartet");
		}
		pos++;
	}

	private boolean ende(){
		return pos >= s.length;
	}

	private CodeParseException fehler(String meldung){
		int zeile = 1;
		for (int i = 0; i < Math.min(pos, s.length); i++){
			if (s[i] == '\n'){
				zeile++;
			}
		}
		return new CodeParseException(meldung, zeile);
	}
}
