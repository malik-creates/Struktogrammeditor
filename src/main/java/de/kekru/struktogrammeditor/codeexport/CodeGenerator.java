package de.kekru.struktogrammeditor.codeexport;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.kekru.struktogrammeditor.control.GlobalSettings;
import de.kekru.struktogrammeditor.control.Struktogramm;
import de.kekru.struktogrammeditor.struktogrammelemente.Fallauswahl;
import de.kekru.struktogrammeditor.struktogrammelemente.LeerElement;
import de.kekru.struktogrammeditor.struktogrammelemente.Schleife;
import de.kekru.struktogrammeditor.struktogrammelemente.StruktogrammElement;
import de.kekru.struktogrammeditor.struktogrammelemente.StruktogrammElementListe;
import de.kekru.struktogrammeditor.struktogrammelemente.Verzweigung;

//Erzeugt aus einem Struktogramm Quellcode in verschiedenen Sprachen.
//Die Struktur wird exakt übersetzt; typische Anweisungen (Ein-/Ausgabe, Zuweisungen, Deklarationen, Zählschleifen, Operatoren)
//werden zusätzlich an die Zielsprache angepasst. Beliebiger Code (z.B. Bibliotheksaufrufe) wird nicht übersetzt.
public class CodeGenerator {

	public enum Sprache {
		JAVA(0, "Java"), CSHARP(3, "C#"), JAVASCRIPT(4, "JavaScript"), C(5, "C / C++"), PYTHON(2, "Python"), PASCAL(1, "Pascal / Delphi");

		public final int nummer; //wird in den Einstellungen gespeichert (0 und 1 wie in älteren Versionen)
		private final String anzeige;

		Sprache(int nummer, String anzeige){
			this.nummer = nummer;
			this.anzeige = anzeige;
		}

		public static Sprache ausNummer(int nummer){
			for (Sprache s : values()){
				if (s.nummer == nummer){
					return s;
				}
			}
			return JAVA;
		}

		public boolean istCAehnlich(){
			return this != PYTHON && this != PASCAL;
		}

		@Override
		public String toString(){
			return anzeige;
		}
	}

	private final Sprache sprache;
	private final int einrueckungStart;
	private final int einrueckungProStufe;
	private final boolean alsKommentar;
	private final boolean semikolons;
	private final boolean rahmen;

	//Zustand während des Erzeugens
	private StringBuilder sb;
	private Set<String> deklariert;
	private Set<String> parameterNamen; //Parameter der Funktion, brauchen keine eigene Variable
	private Map<String, String> pascalVariablen; //Name -> Typ für den var-Abschnitt
	private String rueckgabeTyp; //geratener Typ aus "return wert"
	private boolean scannerBenoetigt, scannerVorhanden;

	public CodeGenerator(Sprache sprache, int einrueckungStart, int einrueckungProStufe, boolean alsKommentar, boolean semikolons, boolean rahmen){
		this.sprache = sprache;
		this.einrueckungStart = Math.max(0, einrueckungStart);
		this.einrueckungProStufe = Math.max(0, einrueckungProStufe);
		this.alsKommentar = alsKommentar;
		this.semikolons = semikolons;
		this.rahmen = rahmen;
	}


	public String erzeugen(StruktogrammElementListe liste, String name){
		deklariert = new HashSet<String>();
		parameterNamen = new HashSet<String>();
		pascalVariablen = new LinkedHashMap<String, String>();
		rueckgabeTyp = null;
		scannerBenoetigt = false;
		scannerVorhanden = false;

		name = name == null ? "" : name.trim();
		Matcher m = funktionsKopf.matcher(name);
		boolean istFunktion = m.matches();
		String angegebenerTyp = null;
		String fname = null;
		List<String[]> parameter = new ArrayList<String[]>();
		if (istFunktion){
			fname = m.group(2);
			angegebenerTyp = m.group(4) != null ? m.group(4) : m.group(1);
			if (angegebenerTyp != null && angegebenerTyp.replaceAll("\\b(public|private|protected|static|final|virtual|override|async)\\b", "").trim().isEmpty()){
				angegebenerTyp = null;
			}
			parameter = parameterZerlegen(m.group(3));
			for (String[] p : parameter){
				deklariert.add(p[0]);
				parameterNamen.add(p[0]);
			}
		}
		boolean mitRahmen = rahmen && (istFunktion || sprache == Sprache.JAVA || sprache == Sprache.CSHARP || sprache == Sprache.C || sprache == Sprache.PASCAL);

		//1. Rumpf erzeugen (dabei werden Variablen, Rückgabetyp und Eingaben gesammelt)
		sb = new StringBuilder();
		liste(liste, mitRahmen ? 1 : 0);
		String rumpf = sb.toString();
		if (sprache == Sprache.JAVA && scannerBenoetigt && !scannerVorhanden){
			sb = new StringBuilder();
			zeile(mitRahmen ? 1 : 0, "Scanner scanner = new Scanner(System.in);");
			rumpf = sb.toString() + rumpf;
		}

		//2. Rahmen davor und dahinter
		sb = new StringBuilder();
		String typ = angegebenerTyp != null ? typ(angegebenerTyp) : rueckgabeTyp != null ? typ(rueckgabeTyp) : null;
		boolean hatRueckgabe = typ != null && !typ.equalsIgnoreCase("void");

		if (!mitRahmen){
			if (!name.isEmpty()){
				zeile(0, kommentar(name));
			}
			if (sprache == Sprache.PYTHON && !istFunktion && rumpf.isEmpty()){
				zeile(0, "pass");
			}
			return sb.toString() + rumpf;
		}

		if (!istFunktion){ //Hauptprogramm
			switch (sprache){
			case JAVA: zeile(0, "public static void main(String[] args) {" + (name.isEmpty() ? "" : "  " + kommentar(name))); break;
			case CSHARP: zeile(0, "static void Main(string[] args) {" + (name.isEmpty() ? "" : "  " + kommentar(name))); break;
			case C: zeile(0, "int main(void) {" + (name.isEmpty() ? "" : "  " + kommentar(name))); break;
			default: zeile(0, "program " + (name.matches("[A-Za-z_]\\w*") ? name : "Hauptprogramm") + ";"); break;
			}
		}else{
			switch (sprache){
			case JAVA:
				zeile(0, "public static " + (hatRueckgabe ? typ : "void") + " " + fname + "(" + parameterMitTyp(parameter, "int") + ") {");
				break;
			case CSHARP:
				zeile(0, "static " + (hatRueckgabe ? typ : "void") + " " + fname + "(" + parameterMitTyp(parameter, "int") + ") {");
				break;
			case C:
				zeile(0, (hatRueckgabe ? typ : "void") + " " + fname + "(" + (parameter.isEmpty() ? "void" : parameterMitTyp(parameter, "int")) + ") {");
				break;
			case JAVASCRIPT:
				zeile(0, "function " + fname + "(" + parameterNamen(parameter) + ") {");
				break;
			case PYTHON:
				zeile(0, "def " + fname + "(" + parameterNamen(parameter) + "):");
				break;
			case PASCAL:
				String par = parameter.isEmpty() ? "" : "(" + parameterPascal(parameter) + ")";
				zeile(0, hatRueckgabe ? "function " + fname + par + ": " + typ + ";" : "procedure " + fname + par + ";");
				break;
			}
		}

		if (sprache == Sprache.PASCAL){
			if (!pascalVariablen.isEmpty()){
				zeile(0, "var");
				for (Map.Entry<String, String> v : pascalVariablen.entrySet()){
					zeile(1, v.getKey() + ": " + v.getValue() + ";");
				}
			}
			zeile(0, "begin");
		}

		String kopf = sb.toString();
		sb = new StringBuilder();
		if (sprache == Sprache.PASCAL){
			zeile(0, istFunktion ? "end;" : "end.");
		}else if (sprache == Sprache.PYTHON){
			if (rumpf.isEmpty()){
				zeile(1, "pass");
			}
		}else{
			if (sprache == Sprache.C && !istFunktion){
				zeile(1, "return 0;");
			}
			zeile(0, "}");
		}
		return kopf + rumpf + sb.toString();
	}


	// ---------------- Struktur ----------------

	private void liste(StruktogrammElementListe liste, int tiefe){
		if (istLeer(liste)){
			if (sprache == Sprache.PYTHON){
				zeile(tiefe, "pass");
			}
			return;
		}
		for (StruktogrammElement e : liste){
			element(e, tiefe);
		}
	}

	private void element(StruktogrammElement e, int tiefe){
		switch (Struktogramm.strElementZuTypnummer(e)){
		case Struktogramm.typAnweisung:
		case Struktogramm.typAufruf:
			for (String z : zeilen(e)){
				if (alsKommentar){
					zeile(tiefe, kommentar(z));
				}else{
					for (String a : anweisung(z)){
						zeile(tiefe, a);
					}
				}
			}
			break;

		case Struktogramm.typAussprung:
			zeile(tiefe, aussprung(text(e)));
			break;

		case Struktogramm.typVerzweigung:
			verzweigung((Verzweigung)e, tiefe, false);
			break;

		case Struktogramm.typFallauswahl:
			fallauswahl((Fallauswahl)e, tiefe);
			break;

		case Struktogramm.typForSchleife:
			forSchleife((Schleife)e, tiefe);
			break;

		case Struktogramm.typWhileSchleife:
			String b = bedingung(ohnePraefix(text(e)));
			schleifeMitKopf(e, tiefe, sprache.istCAehnlich() ? "while (" + b + ") {" : sprache == Sprache.PYTHON ? "while " + b + ":" : "while " + b + " do");
			break;

		case Struktogramm.typEndlosschleife:
			String kopf = sprache == Sprache.C ? "while (1) {" : sprache.istCAehnlich() ? "while (true) {" : sprache == Sprache.PYTHON ? "while True:" : "while true do";
			String t = text(e);
			if (!t.isEmpty() && !t.equals(GlobalSettings.gibElementBeschriftung(Struktogramm.typEndlosschleife))){
				kopf += "  " + kommentar(t);
			}
			schleifeMitKopf(e, tiefe, kopf);
			break;

		case Struktogramm.typDoUntilSchleife:
			fussgesteuert((Schleife)e, tiefe);
			break;

		default: //LeerElement
			break;
		}
	}


	private void schleifeMitKopf(StruktogrammElement e, int tiefe, String kopf){
		schleifeMitKopf(((Schleife)e).gibListe(), tiefe, kopf, null);
	}

	private void schleifeMitKopf(StruktogrammElementListe inhalt, int tiefe, String kopf, String amEnde){
		zeile(tiefe, kopf);
		if (sprache == Sprache.PASCAL){
			zeile(tiefe, "begin");
		}
		liste(inhalt, tiefe + 1);
		if (amEnde != null){
			zeile(tiefe + 1, amEnde);
		}
		if (sprache == Sprache.PASCAL){
			zeile(tiefe, "end;");
		}else if (sprache != Sprache.PYTHON){
			zeile(tiefe, "}");
		}
	}


	//Text einer fußgesteuerten Schleife: "bis ..."/"until ..." = Abbruchbedingung, sonst (auch "solange ...") Wiederholungsbedingung wie bei do-while
	private void fussgesteuert(Schleife s, int tiefe){
		String t = text(s);
		boolean bis = t.matches("(?is)(bis|until)\\b.*");
		String b = bedingung(ohnePraefix(t));
		StruktogrammElementListe inhalt = s.gibListe();

		if (sprache.istCAehnlich()){
			zeile(tiefe, "do {");
			liste(inhalt, tiefe + 1);
			zeile(tiefe, "} while (" + (bis ? nicht(b) : b) + ");");
		}else if (sprache == Sprache.PYTHON){ //Python hat keine fußgesteuerte Schleife
			zeile(tiefe, "while True:");
			if (!istLeer(inhalt)){
				liste(inhalt, tiefe + 1);
			}
			zeile(tiefe + 1, "if " + (bis ? b : nicht(b)) + ":");
			zeile(tiefe + 2, "break");
		}else{
			zeile(tiefe, "repeat");
			liste(inhalt, tiefe + 1);
			zeile(tiefe, "until " + (bis ? b : nicht(b)) + ";");
		}
	}


	private void verzweigung(Verzweigung v, int tiefe, boolean alsElseIf){
		String b = bedingung(text(v).replaceAll("\\?\\s*$", ""));
		StruktogrammElementListe ja = v.gibJaSeite();
		StruktogrammElementListe nein = v.gibNeinSeite();
		StruktogrammElement einzigesNein = einzigesElement(nein);
		boolean elseIf = einzigesNein instanceof Verzweigung;

		if (sprache.istCAehnlich()){
			zeile(tiefe, (alsElseIf ? "} else if (" : "if (") + b + ") {");
			liste(ja, tiefe + 1);
			if (elseIf){
				verzweigung((Verzweigung)einzigesNein, tiefe, true);
				return;
			}
			if (!istLeer(nein)){
				zeile(tiefe, "} else {");
				liste(nein, tiefe + 1);
			}
			zeile(tiefe, "}");

		}else if (sprache == Sprache.PYTHON){
			zeile(tiefe, (alsElseIf ? "elif " : "if ") + b + ":");
			liste(ja, tiefe + 1);
			if (elseIf){
				verzweigung((Verzweigung)einzigesNein, tiefe, true);
			}else if (!istLeer(nein)){
				zeile(tiefe, "else:");
				liste(nein, tiefe + 1);
			}

		}else{
			zeile(tiefe, (alsElseIf ? "else if " : "if ") + b + " then");
			zeile(tiefe, "begin");
			liste(ja, tiefe + 1);
			if (elseIf){
				zeile(tiefe, "end");
				verzweigung((Verzweigung)einzigesNein, tiefe, true);
			}else if (!istLeer(nein)){
				zeile(tiefe, "end");
				zeile(tiefe, "else");
				zeile(tiefe, "begin");
				liste(nein, tiefe + 1);
				zeile(tiefe, "end;");
			}else{
				zeile(tiefe, "end;");
			}
		}
	}


	private void fallauswahl(Fallauswahl f, int tiefe){
		String ausdruck = bedingung(text(f).replaceAll("\\?\\s*$", ""));
		String[] namen = f.gibFaelle();
		int anzahl = f.gibAnzahlListen();
		StruktogrammElementListe sonst = f.gibListe(anzahl - 1);

		if (sprache.istCAehnlich()){
			zeile(tiefe, "switch (" + ausdruck + ") {");
			for (int i = 0; i < anzahl - 1; i++){
				for (String label : labelsTeilen(namen[i])){
					zeile(tiefe + 1, "case " + zeichenkette(label) + ":");
				}
				liste(f.gibListe(i), tiefe + 2);
				if (!endetMitSprung(f.gibListe(i))){
					zeile(tiefe + 2, "break;");
				}
			}
			if (!istLeer(sonst)){
				zeile(tiefe + 1, "default:");
				liste(sonst, tiefe + 2);
				if (!endetMitSprung(sonst)){
					zeile(tiefe + 2, "break;");
				}
			}
			zeile(tiefe, "}");

		}else if (sprache == Sprache.PYTHON){
			zeile(tiefe, "match " + ausdruck + ":");
			for (int i = 0; i < anzahl - 1; i++){
				zeile(tiefe + 1, "case " + verbinden(zeichenketten(labelsTeilen(namen[i])), " | ") + ":");
				liste(f.gibListe(i), tiefe + 2);
			}
			if (!istLeer(sonst)){
				zeile(tiefe + 1, "case _:");
				liste(sonst, tiefe + 2);
			}

		}else{
			zeile(tiefe, "case " + ausdruck + " of");
			for (int i = 0; i < anzahl - 1; i++){
				zeile(tiefe + 1, verbinden(zeichenketten(labelsTeilen(namen[i])), ", ") + ":");
				zeile(tiefe + 1, "begin");
				liste(f.gibListe(i), tiefe + 2);
				zeile(tiefe + 1, "end;");
			}
			if (!istLeer(sonst)){
				zeile(tiefe + 1, "else");
				liste(sonst, tiefe + 2);
			}
			zeile(tiefe, "end;");
		}
	}


	// ---------------- Zählschleifen ----------------

	private static final Pattern forCStil = Pattern.compile("^\\s*(?:[A-Za-z_][\\w<>\\[\\]]*\\s+)?([A-Za-z_]\\w*)\\s*=\\s*([^;]+?)\\s*;\\s*\\1\\s*(<=|<|>=|>)\\s*([^;]+?)\\s*;\\s*"
			+ "(?:\\1\\s*(\\+\\+|--)|(\\+\\+|--)\\s*\\1|\\1\\s*(\\+=|-=)\\s*([^;]+?)|\\1\\s*=\\s*\\1\\s*([+-])\\s*([^;]+?))\\s*$");
	private static final Pattern forPascal = Pattern.compile("^\\s*([A-Za-z_]\\w*)\\s*:=\\s*(.+?)\\s+(to|downto)\\s+(.+?)\\s*$", Pattern.CASE_INSENSITIVE);
	private static final Pattern forRange = Pattern.compile("^\\s*([A-Za-z_]\\w*)\\s+in\\s+range\\s*\\((.*)\\)\\s*$");
	private static final Pattern forVon = Pattern.compile("^\\s*([A-Za-z_]\\w*)\\s+von\\s+(\\S+)\\s+bis\\s+(\\S+)(?:\\s*,?\\s*Schrittweite\\s+(\\S+))?\\s*$", Pattern.CASE_INSENSITIVE);
	private static final Pattern forJeder = Pattern.compile("^\\s*(?:(?:var|let|const|final)\\s+)?(?:([A-Za-z_][\\w<>\\[\\],]*)\\s+)?([A-Za-z_]\\w*)\\s*(?::|\\bin\\b|\\bof\\b)\\s*(.+?)\\s*$");

	//Zählschleife als einheitliche Beschreibung: Variable, Start, Ende (einschließlich), Schritt
	private static class Zaehlung {
		String variable, start, ende;
		long schritt;
	}

	private Zaehlung zaehlungErkennen(String kopf){
		Matcher m = forCStil.matcher(kopf);
		if (m.matches()){
			Zaehlung z = new Zaehlung();
			z.variable = m.group(1);
			z.start = m.group(2);
			String op = m.group(3);
			if (m.group(5) != null || m.group(6) != null){
				z.schritt = "++".equals(m.group(5)) || "++".equals(m.group(6)) ? 1 : -1;
			}else{
				String wert = m.group(8) != null ? m.group(8) : m.group(10);
				boolean minus = "-=".equals(m.group(7)) || "-".equals(m.group(9));
				try{
					z.schritt = Long.parseLong(wert.trim()) * (minus ? -1 : 1);
				}catch(NumberFormatException e){
					return null;
				}
			}
			if (op.equals("<")){
				z.ende = plus(m.group(4), -1);
			}else if (op.equals(">")){
				z.ende = plus(m.group(4), 1);
			}else{
				z.ende = m.group(4);
			}
			return z;
		}

		m = forPascal.matcher(kopf);
		if (m.matches()){
			Zaehlung z = new Zaehlung();
			z.variable = m.group(1);
			z.start = m.group(2);
			z.ende = m.group(4);
			z.schritt = m.group(3).equalsIgnoreCase("downto") ? -1 : 1;
			return z;
		}

		m = forVon.matcher(kopf);
		if (m.matches()){
			Zaehlung z = new Zaehlung();
			z.variable = m.group(1);
			z.start = m.group(2);
			z.ende = m.group(3);
			try{
				z.schritt = m.group(4) != null ? Long.parseLong(m.group(4)) : 1;
			}catch(NumberFormatException e){
				return null;
			}
			return z;
		}

		m = forRange.matcher(kopf);
		if (m.matches()){
			List<String> args = teilen(m.group(2), ',');
			Zaehlung z = new Zaehlung();
			z.variable = m.group(1);
			z.schritt = 1;
			if (args.size() == 3){
				try{
					z.schritt = Long.parseLong(args.get(2).trim());
				}catch(NumberFormatException e){
					return null;
				}
			}
			if (args.size() == 1){
				z.start = "0";
				z.ende = plus(args.get(0), -1);
			}else if (args.size() >= 2){
				z.start = args.get(0).trim();
				z.ende = plus(args.get(1), z.schritt > 0 ? -1 : 1);
			}else{
				return null;
			}
			return z;
		}
		return null;
	}


	private void forSchleife(Schleife s, int tiefe){
		String kopf = text(s);
		Zaehlung z = zaehlungErkennen(kopf);

		if (z != null && z.schritt != 0){
			z.start = ausdruck(z.start);
			z.ende = ausdruck(z.ende);
			switch (sprache){
			case PYTHON:
				String ende = plus(z.ende, z.schritt > 0 ? 1 : -1);
				schleifeMitKopf(s, tiefe, "for " + z.variable + " in range(" + (z.start.equals("0") && z.schritt == 1 ? "" : z.start + ", ") + ende + (z.schritt != 1 ? ", " + z.schritt : "") + "):");
				return;
			case PASCAL:
				pascalVariable(z.variable, "Integer");
				if (Math.abs(z.schritt) == 1){
					schleifeMitKopf(s, tiefe, "for " + z.variable + " := " + z.start + (z.schritt > 0 ? " to " : " downto ") + z.ende + " do");
				}else{ //Pascal kennt keine Schrittweite: als while-Schleife
					zeile(tiefe, z.variable + " := " + z.start + ";");
					schleifeMitKopf(s.gibListe(), tiefe, "while " + z.variable + (z.schritt > 0 ? " <= " : " >= ") + z.ende + " do",
							z.variable + " := " + z.variable + (z.schritt > 0 ? " + " + z.schritt : " - " + (-z.schritt)) + ";");
				}
				return;
			default:
				String typ = sprache == Sprache.JAVASCRIPT ? "let" : "int";
				String vergleich = z.schritt > 0 ? " <= " : " >= ";
				String schritt = z.schritt == 1 ? z.variable + "++" : z.schritt == -1 ? z.variable + "--" : z.variable + (z.schritt > 0 ? " += " + z.schritt : " -= " + (-z.schritt));
				schleifeMitKopf(s, tiefe, "for (" + typ + " " + z.variable + " = " + z.start + "; " + z.variable + vergleich + z.ende + "; " + schritt + ") {");
				return;
			}
		}

		Matcher m = forJeder.matcher(kopf);
		String zeile;
		if (m.matches() && !kopf.contains(";") && !kopf.contains(":=")){ //Schleife über alle Elemente einer Sammlung
			String typ = m.group(1);
			String var = m.group(2);
			String sammlung = ausdruck(m.group(3));
			boolean listenLiteral = sammlung.startsWith("[") && sammlung.endsWith("]");
			String inhalt = listenLiteral ? sammlung.substring(1, sammlung.length() - 1) : null;
			switch (sprache){
			case JAVA: zeile = "for (" + (typ != null ? typ(typ) : "var") + " " + var + " : " + (listenLiteral ? "List.of(" + inhalt + ")" : sammlung) + ") {"; break;
			case CSHARP: zeile = "foreach (" + (typ != null ? typ(typ) : "var") + " " + var + " in " + (listenLiteral ? "new[] { " + inhalt + " }" : sammlung) + ") {"; break;
			case JAVASCRIPT: zeile = "for (const " + var + " of " + sammlung + ") {"; break;
			case PYTHON: zeile = "for " + var + " in " + sammlung + ":"; break;
			case PASCAL:
				pascalVariable(var, typ != null ? typ(typ) : "Integer");
				zeile = "for " + var + " in " + sammlung + " do";
				break;
			default: zeile = "for (" + kopf + ") {"; break;
			}
		}else{
			zeile = sprache.istCAehnlich() ? "for (" + kopf + ") {" : sprache == Sprache.PYTHON ? "for " + kopf + ":" : "for " + kopf + " do";
		}
		schleifeMitKopf(s, tiefe, zeile);
	}


	// ---------------- Anweisungen ----------------

	private static final Pattern deklaration = Pattern.compile("^(?:final\\s+|const\\s+|static\\s+)*(int|long|short|byte|double|float|char|boolean|bool|String|string|var|let|const|auto|Integer|Double|Boolean)(\\[\\])?\\s+([A-Za-z_]\\w*)\\s*(?:=\\s*(.+))?$");
	private static final Pattern inkrement = Pattern.compile("^(?:([A-Za-z_][\\w.\\[\\]]*)\\s*(\\+\\+|--)|(\\+\\+|--)\\s*([A-Za-z_][\\w.\\[\\]]*))$");
	private static final Pattern zuweisung = Pattern.compile("^([A-Za-z_][\\w.]*(?:\\[[^\\]]*\\])?)\\s*(:=|[+\\-*/]?=(?!=))\\s*(.+)$");
	private static final Pattern ausgabeZeile = Pattern.compile("^(?:System\\.out\\.println|Console\\.WriteLine|console\\.log|print|writeln|WriteLn|Writeln|puts)\\s*\\((.*)\\)$");
	private static final Pattern ausgabe = Pattern.compile("^(?:System\\.out\\.print|Console\\.Write|write|Write|printf)\\s*\\((.*)\\)$");
	private static final Pattern eingabePascal = Pattern.compile("^(?:readln|read|ReadLn|Readln|Read)\\s*\\(\\s*([A-Za-z_][\\w.\\[\\]]*)\\s*\\)$");
	private static final Pattern scannerDeklaration = Pattern.compile("^(?:final\\s+)?Scanner\\s+\\w+\\s*=\\s*new\\s+Scanner\\s*\\(.*\\)$");

	//übersetzt eine Anweisung, kann mehrere Zeilen ergeben
	private List<String> anweisung(String a){
		List<String> ergebnis = new ArrayList<String>();
		a = a.trim();
		if (a.endsWith(";")){
			a = a.substring(0, a.length() - 1).trim();
		}
		if (a.isEmpty()){
			return ergebnis;
		}
		a = zeichenkette(a);

		if (scannerDeklaration.matcher(a).matches()){ //Java-Scanner wird nur in Java gebraucht
			if (sprache == Sprache.JAVA){
				scannerVorhanden = true;
				ergebnis.add(mitSemikolon(a));
			}
			return ergebnis;
		}

		if (a.matches("(?s)return\\b.*")){
			String wert = a.substring(6).trim();
			if (!wert.isEmpty() && rueckgabeTyp == null){
				rueckgabeTyp = typRaten(wert);
			}
			if (sprache == Sprache.PASCAL){
				ergebnis.add(wert.isEmpty() ? "exit;" : "Result := " + ausdruck(wert) + ";");
			}else{
				ergebnis.add(mitSemikolon(wert.isEmpty() ? "return" : "return " + ausdruck(wert)));
			}
			return ergebnis;
		}

		//Ausgabe
		Matcher m = ausgabeZeile.matcher(a);
		boolean neueZeile = m.matches();
		if (!neueZeile){
			m = ausgabe.matcher(a);
		}
		if (neueZeile || m.matches()){
			String args = m.group(1);
			if (args.matches("(?s).*,\\s*end\\s*=\\s*(\"\"|'')\\s*")){ //Python print(..., end="")
				args = args.replaceAll("(?s),\\s*end\\s*=\\s*(\"\"|'')\\s*$", "");
				neueZeile = false;
			}
			ergebnis.add(mitSemikolon(ausgabeErzeugen(args, neueZeile)));
			return ergebnis;
		}

		//Eingabe
		m = eingabePascal.matcher(a);
		if (m.matches()){
			eingabeErzeugen(m.group(1), "int", null, ergebnis);
			return ergebnis;
		}
		m = zuweisung.matcher(a.replaceFirst("^(?:final\\s+)?(?:int|long|double|float|String|string|var|let|const|auto|bool|boolean|char)\\s+(?=[A-Za-z_])", ""));
		if (m.matches() && (m.group(2).equals("=") || m.group(2).equals(":="))){
			String art = eingabeArt(m.group(3));
			if (art != null){
				eingabeErzeugen(m.group(1), art, eingabeText(m.group(3)), ergebnis);
				return ergebnis;
			}
		}

		//Deklaration mit Typ ("int x = 5")
		m = deklaration.matcher(a);
		if (m.matches()){
			String quelltyp = m.group(1).matches("let|const") ? "var" : m.group(1) + (m.group(2) != null ? "[]" : "");
			String name = m.group(3);
			String wert = m.group(4) != null ? ausdruck(m.group(4)) : null;
			deklariert.add(name);
			switch (sprache){
			case PYTHON:
				ergebnis.add(name + " = " + (wert != null ? wert : "None"));
				break;
			case PASCAL:
				pascalVariable(name, quelltyp.matches("var|let|auto") ? (wert != null ? typRaten(wert) : "Integer") : typ(quelltyp));
				if (wert != null){
					ergebnis.add(name + " := " + wert + ";");
				}
				break;
			case JAVASCRIPT:
				ergebnis.add(mitSemikolon("let " + name + (wert != null ? " = " + wert : "")));
				break;
			default:
				String t = quelltyp.matches("var|let|auto") ? (sprache == Sprache.C ? typ(wert != null ? typRaten(wert) : "int") : "var") : typ(quelltyp);
				ergebnis.add(mitSemikolon(t + " " + name + (wert != null ? " = " + wert : "")));
				break;
			}
			return ergebnis;
		}

		//i++ / i--
		m = inkrement.matcher(a);
		if (m.matches()){
			String var = m.group(1) != null ? m.group(1) : m.group(4);
			boolean plus = "++".equals(m.group(2)) || "++".equals(m.group(3));
			if (sprache == Sprache.PYTHON){
				ergebnis.add(var + (plus ? " += 1" : " -= 1"));
			}else if (sprache == Sprache.PASCAL){
				ergebnis.add((plus ? "Inc(" : "Dec(") + var + ");");
			}else{
				ergebnis.add(mitSemikolon(var + (plus ? "++" : "--")));
			}
			return ergebnis;
		}

		//Zuweisung
		m = zuweisung.matcher(a);
		if (m.matches()){
			String ziel = m.group(1);
			String op = m.group(2);
			String wert = ausdruck(m.group(3));
			boolean einfach = op.equals("=") || op.equals(":=");
			switch (sprache){
			case PASCAL:
				if (einfach){
					pascalVariable(ziel, typRaten(wert));
					ergebnis.add(ziel + " := " + wert + ";");
				}else{
					ergebnis.add(ziel + " := " + ziel + " " + pascalOperator(op.substring(0, 1)) + " " + wert + ";");
				}
				break;
			case PYTHON:
				ergebnis.add(ziel + " " + (einfach ? "=" : op) + " " + wert);
				break;
			default:
				String praefix = "";
				if (einfach && ziel.matches("[A-Za-z_]\\w*") && !deklariert.contains(ziel)){ //erste Zuweisung ohne Typ (z.B. aus Python): deklarieren
					deklariert.add(ziel);
					praefix = sprache == Sprache.JAVASCRIPT ? "let " : sprache == Sprache.C ? typ(typRaten(wert)) + " " : "var ";
				}
				ergebnis.add(mitSemikolon(praefix + ziel + " " + (einfach ? "=" : op) + " " + wert));
				break;
			}
			return ergebnis;
		}

		ergebnis.add(mitSemikolon(ausdruck(a)));
		return ergebnis;
	}

	private String mitSemikolon(String a){
		if (sprache == Sprache.PYTHON || !semikolons || a.isEmpty() || a.matches("(?s).*[;{}:]\\s*$")){
			return a;
		}
		return a + ";";
	}

	private static String pascalOperator(String op){
		return op.equals("/") ? "div" : op;
	}

	private void pascalVariable(String name, String typ){
		if (sprache == Sprache.PASCAL && name.matches("[A-Za-z_]\\w*") && !parameterNamen.contains(name) && !pascalVariablen.containsKey(name)){
			pascalVariablen.put(name, typ(typ));
		}
	}


	// ---------------- Ein- und Ausgabe ----------------

	//Teile der Ausgabe: Zeichenketten und Ausdrücke, aus "a" + x oder a, x
	private String ausgabeErzeugen(String args, boolean neueZeile){
		List<String> teile = new ArrayList<String>();
		List<String> kommas = teilen(args, ',');
		if (kommas.size() > 1){
			teile.addAll(kommas);
		}else{
			List<String> plusTeile = teilen(args, '+');
			boolean mitText = false;
			for (String p : plusTeile){
				if (istZeichenkette(p.trim())){
					mitText = true;
				}
			}
			if (mitText && plusTeile.size() > 1){
				teile.addAll(plusTeile);
			}else{
				teile.add(args);
			}
		}
		for (int i = 0; i < teile.size(); i++){
			String t = teile.get(i).trim();
			teile.set(i, t.isEmpty() ? t : istZeichenkette(t) ? t : ausdruck(t));
		}
		if (teile.size() == 1 && teile.get(0).isEmpty()){
			teile.clear();
		}

		switch (sprache){
		case JAVA: return (neueZeile ? "System.out.println(" : "System.out.print(") + plusVerbinden(teile) + ")";
		case CSHARP: return (neueZeile ? "Console.WriteLine(" : "Console.Write(") + plusVerbinden(teile) + ")";
		case JAVASCRIPT: return "console.log(" + plusVerbinden(teile) + ")";
		case PASCAL: return (neueZeile ? "writeln(" : "write(") + verbinden(teile, ", ") + ")";
		case PYTHON:
			String inhalt;
			if (teile.size() > 1){ //f-String: f"Text {wert} Text"
				StringBuilder f = new StringBuilder("f\"");
				for (String t : teile){
					f.append(istZeichenkette(t) ? textInhalt(t).replace("{", "{{").replace("}", "}}").replace("\"", "\\\"") : "{" + t + "}");
				}
				inhalt = f.append('"').toString();
			}else{
				inhalt = teile.isEmpty() ? "" : teile.get(0);
			}
			return "print(" + inhalt + (neueZeile ? "" : (inhalt.isEmpty() ? "" : ", ") + "end=\"\"") + ")";
		default: //C: printf mit Formatangaben
			StringBuilder format = new StringBuilder();
			List<String> werte = new ArrayList<String>();
			for (String t : teile){
				if (istZeichenkette(t)){
					format.append(textInhalt(t).replace("%", "%%"));
				}else{
					format.append(t.matches("-?\\d+\\.\\d+") ? "%f" : "%d");
					werte.add(t);
				}
			}
			return "printf(\"" + format + (neueZeile ? "\\n" : "") + "\"" + (werte.isEmpty() ? "" : ", " + verbinden(werte, ", ")) + ")";
		}
	}

	private String plusVerbinden(List<String> teile){
		if (teile.size() > 1 && !istZeichenkette(teile.get(0)) && !istZeichenkette(teile.get(1))){
			teile.add(0, "\"\""); //damit z.B. 1 + 2 nicht addiert, sondern hintereinander ausgegeben wird
		}
		return verbinden(teile, " + ");
	}

	private static String eingabeArt(String rechts){
		String r = rechts.trim();
		if (r.matches("\\w+\\.nextInt\\(\\)|\\w+\\.nextLong\\(\\)|int\\s*\\(\\s*input\\s*\\(.*\\)\\s*\\)|int\\.Parse\\s*\\(\\s*Console\\.ReadLine\\(\\)\\s*\\)|Convert\\.ToInt32\\s*\\(\\s*Console\\.ReadLine\\(\\)\\s*\\)|(?:Number|parseInt)\\s*\\(\\s*prompt\\s*\\(.*\\)\\s*\\)")){
			return "int";
		}
		if (r.matches("\\w+\\.nextDouble\\(\\)|float\\s*\\(\\s*input\\s*\\(.*\\)\\s*\\)|double\\.Parse\\s*\\(\\s*Console\\.ReadLine\\(\\)\\s*\\)|parseFloat\\s*\\(\\s*prompt\\s*\\(.*\\)\\s*\\)")){
			return "double";
		}
		if (r.matches("\\w+\\.next(?:Line)?\\(\\)|input\\s*\\(.*\\)|Console\\.ReadLine\\(\\)|prompt\\s*\\(.*\\)")){
			return "string";
		}
		return null;
	}

	//Eingabeaufforderung aus input("...") bzw. prompt("...")
	private static String eingabeText(String rechts){
		Matcher m = Pattern.compile("(?:input|prompt)\\s*\\(\\s*((\"|')(?:[^\\\\]|\\\\.)*?\\2)\\s*\\)").matcher(rechts);
		return m.find() ? m.group(1) : null;
	}

	private void eingabeErzeugen(String ziel, String art, String aufforderung, List<String> ergebnis){
		boolean neu = ziel.matches("[A-Za-z_]\\w*") && !deklariert.contains(ziel);
		if (neu){
			deklariert.add(ziel);
		}
		if (aufforderung != null){
			aufforderung = zeichenkette(aufforderung);
		}
		switch (sprache){
		case JAVA:
			scannerBenoetigt = true;
			if (aufforderung != null){
				ergebnis.add("System.out.print(" + aufforderung + ");");
			}
			ergebnis.add((neu ? (art.equals("int") ? "int " : art.equals("double") ? "double " : "String ") : "") + ziel + " = scanner." + (art.equals("int") ? "nextInt()" : art.equals("double") ? "nextDouble()" : "nextLine()") + ";");
			break;
		case CSHARP:
			if (aufforderung != null){
				ergebnis.add("Console.Write(" + aufforderung + ");");
			}
			ergebnis.add((neu ? (art.equals("int") ? "int " : art.equals("double") ? "double " : "string ") : "") + ziel + " = "
					+ (art.equals("int") ? "int.Parse(Console.ReadLine())" : art.equals("double") ? "double.Parse(Console.ReadLine())" : "Console.ReadLine()") + ";");
			break;
		case JAVASCRIPT:
			String p = "prompt(" + (aufforderung != null ? aufforderung : "") + ")";
			ergebnis.add((neu ? "let " : "") + ziel + " = " + (art.equals("string") ? p : "Number(" + p + ")") + ";");
			break;
		case PYTHON:
			String in = "input(" + (aufforderung != null ? aufforderung : "") + ")";
			ergebnis.add(ziel + " = " + (art.equals("int") ? "int(" + in + ")" : art.equals("double") ? "float(" + in + ")" : in));
			break;
		case PASCAL:
			pascalVariable(ziel, art.equals("int") ? "Integer" : art.equals("double") ? "Real" : "string");
			if (aufforderung != null){
				ergebnis.add("write(" + aufforderung + ");");
			}
			ergebnis.add("readln(" + ziel + ");");
			break;
		default:
			if (neu){
				ergebnis.add(art.equals("int") ? "int " + ziel + ";" : art.equals("double") ? "double " + ziel + ";" : "char " + ziel + "[100];");
			}
			if (aufforderung != null){
				ergebnis.add("printf(" + aufforderung + ");");
			}
			ergebnis.add("scanf(\"" + (art.equals("int") ? "%d" : art.equals("double") ? "%lf" : "%99s") + "\", " + (art.equals("string") ? "" : "&") + ziel + ");");
			break;
		}
	}


	private String aussprung(String t){
		t = zeichenkette(t.trim()).replaceAll(";\\s*$", "");
		String wort = t.replaceAll("^\\s*([A-Za-z]+).*$", "$1").toLowerCase();
		if (wort.equals("return")){
			List<String> r = anweisung(t);
			if (sprache == Sprache.PASCAL){ //return mitten in einer Funktion: Ergebnis setzen und verlassen
				String wert = t.substring(6).trim();
				return wert.isEmpty() ? "exit;" : "exit(" + ausdruck(wert) + ");";
			}
			return r.isEmpty() ? "" : r.get(0);
		}
		switch (sprache){
		case PYTHON:
			if (wort.matches("break|continue|raise|pass")){
				return t;
			}
			if (wort.equals("throw")){
				return "raise " + t.replaceFirst("^\\s*throw\\s+(new\\s+)?", "");
			}
			if (wort.matches("exit|halt")){
				return "return";
			}
			return "break" + (t.isEmpty() ? "" : "  " + kommentar(t));
		case PASCAL:
			if (wort.matches("exit|break|continue|halt|raise|goto")){
				return mitSemikolon(t);
			}
			if (wort.equals("throw")){
				return "raise " + t.replaceFirst("^\\s*throw\\s+(new\\s+)?", "") + ";";
			}
			return "break;" + (t.isEmpty() ? "" : "  " + kommentar(t));
		default:
			if (wort.matches("break|continue|throw|goto|yield")){
				return mitSemikolon(t);
			}
			if (wort.matches("exit|halt")){
				return "return;";
			}
			if (wort.equals("raise")){
				return mitSemikolon("throw " + t.replaceFirst("^\\s*raise\\s+", ""));
			}
			return "break;" + (t.isEmpty() ? "" : "  " + kommentar(t));
		}
	}


	// ---------------- Ausdrücke und Bedingungen ----------------

	private static final String vergleich = "(<=|>=|==|!=|<>|<|>|(?<![:<>!=+\\-*/%&|^])=(?!=))";

	//Bedingung: Operatoren übersetzen und = als Vergleich behandeln
	private String bedingung(String b){
		return uebersetzen(zeichenkette(b.trim()), true);
	}

	//Ausdruck auf der rechten Seite einer Zuweisung o.ä.
	private String ausdruck(String a){
		return uebersetzen(zeichenkette(a.trim()), false);
	}

	private String nicht(String b){
		if (b.matches("[A-Za-z_][\\w.]*")){
			return sprache.istCAehnlich() ? "!" + b : "not " + b;
		}
		return sprache.istCAehnlich() ? "!(" + b + ")" : "not (" + b + ")";
	}

	private static String ohnePraefix(String t){
		return t.replaceFirst("(?is)^(bis|until|solange|while)\\s+", "").replaceAll("\\?\\s*$", "");
	}

	//übersetzt logische, Vergleichs- und Rechenoperatoren sowie Wahrheitswerte (nur außerhalb von Zeichenketten)
	private String uebersetzen(String s, boolean istBedingung){
		//1. einheitliche Wörter für and/or/not
		StringBuilder neutral = new StringBuilder();
		for (String[] teil : nachZeichenkettenTeilen(s)){
			if (teil[1] != null){
				neutral.append(platzhalter(teil[0]));
				continue;
			}
			String t = teil[0].replace("&&", " and ").replace("||", " or ").replaceAll("!(?!=)", " not ")
					.replaceAll("(?i)\\bAND\\b", "and").replaceAll("(?i)\\bOR\\b", "or").replaceAll("(?i)\\bNOT\\b", "not")
					.replaceAll("(?i)\\bdiv\\b", " div ").replaceAll("(?i)\\bmod\\b", " mod ");
			neutral.append(t);
		}
		String n = neutral.toString().replaceAll("\\s+", " ").trim();
		n = n.replaceAll("\\( ", "(").replaceAll(" \\)", ")");

		//2. "not x == 0" -> "not (x == 0)", damit die Bedeutung in jeder Sprache gleich bleibt
		Matcher m = Pattern.compile("\\bnot ((?:[^()]|\\([^()]*\\))+?)(?= and | or |\\)|$)").matcher(n);
		StringBuffer sb2 = new StringBuffer();
		while (m.find()){
			String x = m.group(1).trim();
			boolean klammern = Pattern.compile(vergleich).matcher(x).find() && !(x.startsWith("(") && x.endsWith(")"));
			m.appendReplacement(sb2, Matcher.quoteReplacement("not " + (klammern ? "(" + x + ")" : x)));
		}
		m.appendTail(sb2);
		n = sb2.toString();

		//3. Pascal: Vergleiche zwischen and/or einklammern (Pascal bindet and/or stärker als Vergleiche)
		if (sprache == Sprache.PASCAL && n.matches(".*\\b(and|or)\\b.*")){
			List<String> teile = new ArrayList<String>();
			for (String t : teilenAnWort(n)){
				String tt = t.trim();
				if (tt.equals("and") || tt.equals("or")){
					teile.add(tt);
				}else{
					boolean klammern = Pattern.compile(vergleich).matcher(tt).find() && !(tt.startsWith("(") && tt.endsWith(")")) && !tt.startsWith("not ");
					teile.add(klammern ? "(" + tt + ")" : tt);
				}
			}
			n = verbinden(teile, " ");
		}

		//4. in die Zielsprache
		switch (sprache){
		case PYTHON:
			n = n.replaceAll("\\btrue\\b", "True").replaceAll("\\bfalse\\b", "False").replaceAll("\\b(null|nil|NULL)\\b", "None")
			.replace("<>", "!=").replace(" div ", " // ").replace(" mod ", " % ");
			if (istBedingung){
				n = n.replaceAll("(?<![=!<>:+\\-*/%&|^])=(?!=)", "==");
			}
			break;
		case PASCAL:
			n = n.replace("!=", "<>").replace("==", "=").replaceAll("\\bTrue\\b", "true").replaceAll("\\bFalse\\b", "false")
			.replaceAll("\\b(null|None|NULL)\\b", "nil").replaceAll("(?<=\\S) ?% ?(?=\\S)", " mod ").replaceAll(" // ", " div ");
			break;
		default:
			n = n.replaceAll("\\band\\b", "&&").replaceAll("\\bor\\b", "||").replaceAll("\\bnot \\(", "!(").replaceAll("\\bnot ", "!")
			.replace("<>", "!=").replaceAll("\\bTrue\\b", "true").replaceAll("\\bFalse\\b", "false")
			.replaceAll("\\b(None|nil)\\b", sprache == Sprache.C ? "NULL" : "null")
			.replace(" div ", " / ").replace(" mod ", " % ").replace(" // ", " / ");
			if (istBedingung){
				n = n.replaceAll("(?<![=!<>:+\\-*/%&|^])=(?!=)", "==");
			}
			break;
		}
		return platzhalterZurueck(n.replaceAll("  +", " ").trim());
	}

	//Zeichenketten werden während der Übersetzung durch Platzhalter ersetzt, damit ihr Inhalt unverändert bleibt
	private List<String> platzhalterWerte = new ArrayList<String>();
	private static final char platzhalterAnfang = (char)0xE000, platzhalterEnde = (char)0xE001; //privater Unicode-Bereich, wird von trim() nicht entfernt

	private String platzhalter(String zeichenkette){
		platzhalterWerte.add(zeichenkette);
		return platzhalterAnfang + String.valueOf(platzhalterWerte.size() - 1) + platzhalterEnde;
	}

	private String platzhalterZurueck(String s){
		Matcher m = Pattern.compile(platzhalterAnfang + "(\\d+)" + platzhalterEnde).matcher(s);
		StringBuffer ergebnis = new StringBuffer();
		while (m.find()){
			m.appendReplacement(ergebnis, Matcher.quoteReplacement(platzhalterWerte.get(Integer.parseInt(m.group(1)))));
		}
		m.appendTail(ergebnis);
		return ergebnis.toString();
	}

	//teilt an " and " / " or " außerhalb von Klammern
	private static List<String> teilenAnWort(String s){
		List<String> teile = new ArrayList<String>();
		int tiefe = 0;
		int start = 0;
		for (int i = 0; i < s.length(); i++){
			char c = s.charAt(i);
			if (c == '('){
				tiefe++;
			}else if (c == ')'){
				tiefe--;
			}else if (tiefe == 0 && c == ' '){
				for (String w : new String[] {" and ", " or "}){
					if (s.startsWith(w, i)){
						teile.add(s.substring(start, i));
						teile.add(w.trim());
						start = i + w.length();
						i = start - 1;
						break;
					}
				}
			}
		}
		teile.add(s.substring(start));
		return teile;
	}

	//Anführungszeichen von Zeichenketten anpassen: Pascal nutzt '...', Java/C#/C für Texte "..."
	private String zeichenkette(String s){
		StringBuilder ergebnis = new StringBuilder();
		for (String[] teil : nachZeichenkettenTeilen(s)){
			String t = teil[0];
			if (teil[1] != null && t.length() >= 2){
				String inhalt = t.substring(1, t.length() - 1);
				if (sprache == Sprache.PASCAL && teil[1].equals("\"")){
					t = "'" + inhalt.replace("\\\"", "\"").replace("'", "''") + "'";
				}else if (sprache != Sprache.PASCAL && teil[1].equals("'") && (sprache != Sprache.JAVA && sprache != Sprache.CSHARP && sprache != Sprache.C ? inhalt.contains("''") : inhalt.replace("''", "'").length() != 1)){
					t = "\"" + inhalt.replace("''", "'").replace("\"", "\\\"") + "\"";
				}
			}
			ergebnis.append(t);
		}
		return ergebnis.toString();
	}

	private List<String> zeichenketten(List<String> liste){
		List<String> ergebnis = new ArrayList<String>();
		for (String s : liste){
			ergebnis.add(zeichenkette(s));
		}
		return ergebnis;
	}

	private static boolean istZeichenkette(String t){
		t = t.trim();
		return t.length() >= 2 && (t.startsWith("\"") && t.endsWith("\"") || t.startsWith("'") && t.endsWith("'")) && nachZeichenkettenTeilen(t).size() == 1;
	}

	private static String textInhalt(String zeichenkette){
		String t = zeichenkette.trim();
		String inhalt = t.substring(1, t.length() - 1);
		return t.startsWith("'") ? inhalt.replace("''", "'") : inhalt.replace("\\\"", "\"");
	}

	//zerlegt s in Teile; Zeichenketten bekommen als zweiten Eintrag ihr Anführungszeichen, sonstiger Text null
	private static List<String[]> nachZeichenkettenTeilen(String s){
		List<String[]> teile = new ArrayList<String[]>();
		int start = 0;
		int i = 0;
		while (i < s.length()){
			char c = s.charAt(i);
			if (c == '"' || c == '\''){
				if (i > start){
					teile.add(new String[] {s.substring(start, i), null});
				}
				int j = i + 1;
				while (j < s.length()){
					if (s.charAt(j) == '\\' && c == '"'){
						j += 2;
						continue;
					}
					if (s.charAt(j) == c){
						if (c == '\'' && j + 1 < s.length() && s.charAt(j + 1) == '\''){ //Pascal ''
							j += 2;
							continue;
						}
						break;
					}
					j++;
				}
				j = Math.min(j + 1, s.length());
				teile.add(new String[] {s.substring(i, j), String.valueOf(c)});
				i = j;
				start = j;
				continue;
			}
			i++;
		}
		if (start < s.length()){
			teile.add(new String[] {s.substring(start), null});
		}
		return teile;
	}


	// ---------------- Funktionsrahmen und Typen ----------------

	//[Rückgabetyp] name(parameter) [: Rückgabetyp]
	private static final Pattern funktionsKopf = Pattern.compile("^(?:(.+?)\\s+)?([A-Za-z_]\\w*)\\s*\\((.*)\\)\\s*(?::\\s*([\\w\\[\\]]+))?$");

	//zerlegt eine Parameterliste in {Name, Typ oder null, Standardwert oder null}
	private static List<String[]> parameterZerlegen(String liste){
		List<String[]> ergebnis = new ArrayList<String[]>();
		if (liste == null || liste.trim().isEmpty() || liste.trim().equals("void")){
			return ergebnis;
		}
		String offenerTyp = null; //Pascal "a, b: Integer"
		List<String[]> ohneTyp = new ArrayList<String[]>();
		for (String p : teilen(liste.replace(';', ','), ',')){
			p = p.trim().replaceFirst("^(const|var|out|final|ref)\\s+", "");
			if (p.isEmpty()){
				continue;
			}
			String standard = null;
			int gleich = p.indexOf('=');
			if (gleich > 0){
				standard = p.substring(gleich + 1).trim();
				p = p.substring(0, gleich).trim();
			}
			int doppelpunkt = p.indexOf(':');
			if (doppelpunkt > 0){ //Pascal/Python: name: Typ
				offenerTyp = p.substring(doppelpunkt + 1).trim();
				for (String[] o : ohneTyp){ //vorherige Namen ohne Typ bekommen denselben Typ (Pascal)
					o[1] = offenerTyp;
				}
				ohneTyp.clear();
				ergebnis.add(new String[] {p.substring(0, doppelpunkt).trim(), offenerTyp, standard});
			}else{
				Matcher m = Pattern.compile("^(.*?)[\\s*&.]*([A-Za-z_]\\w*)$").matcher(p);
				if (m.matches() && !m.group(1).trim().isEmpty()){ //Java/C: Typ name
					ergebnis.add(new String[] {m.group(2), m.group(1).trim().replace("...", "[]"), standard});
				}else{
					String[] neu = new String[] {p, standard != null ? typRaten(standard) : null, standard};
					ergebnis.add(neu);
					if (standard == null){
						ohneTyp.add(neu);
					}
				}
			}
		}
		return ergebnis;
	}

	private String parameterMitTyp(List<String[]> parameter, String ersatzTyp){
		List<String> teile = new ArrayList<String>();
		for (String[] p : parameter){
			String t = p[1] != null ? typ(p[1]) : ersatzTyp;
			if (sprache == Sprache.C && t.endsWith("[]")){ //C: char* args[]
				teile.add(t.substring(0, t.length() - 2) + " " + p[0] + "[]");
			}else{
				teile.add(t + " " + p[0] + (p[2] != null && sprache == Sprache.CSHARP ? " = " + ausdruck(p[2]) : ""));
			}
		}
		return verbinden(teile, ", ");
	}

	private String parameterNamen(List<String[]> parameter){
		List<String> teile = new ArrayList<String>();
		for (String[] p : parameter){
			teile.add(p[0] + (p[2] != null ? "=" + ausdruck(p[2]) : ""));
		}
		return verbinden(teile, ", ");
	}

	private String parameterPascal(List<String[]> parameter){
		List<String> teile = new ArrayList<String>();
		for (String[] p : parameter){
			teile.add(p[0] + ": " + typ(p[1] != null ? p[1] : "Integer"));
		}
		return verbinden(teile, "; ");
	}

	//Typ eines Werts grob erraten (für Rückgabetypen und Pascal-Variablen)
	private static String typRaten(String wert){
		String w = wert == null ? "" : wert.trim();
		if (istZeichenkette(w) || w.matches("(?s).*\\b(input|ReadLine|nextLine|prompt)\\s*\\(.*")){
			return "string";
		}
		if (w.matches("(?i)true|false") || w.matches(".*(<=|>=|==|!=|<>|&&|\\|\\|).*") || w.matches(".*\\b(and|or|not)\\b.*")){
			return "boolean";
		}
		if (w.matches("-?\\d+\\.\\d+") || w.matches("(?s).*\\b(float|nextDouble|double\\.Parse)\\b.*")){
			return "double";
		}
		return "int";
	}

	//einfache Datentypen in die Zielsprache übertragen
	private String typ(String t){
		String k = t.trim().toLowerCase();
		boolean feld = k.endsWith("[]");
		if (feld){
			k = k.substring(0, k.length() - 2).trim();
		}
		if (k.startsWith("array of ")){
			feld = true;
			k = k.substring(9).trim();
		}
		String ergebnis;
		if (k.matches("int|integer|long|longint|short|byte|int64|cardinal|word")){
			ergebnis = sprache == Sprache.PASCAL ? "Integer" : "int";
		}else if (k.matches("double|float|real|single|extended")){
			ergebnis = sprache == Sprache.PASCAL ? "Real" : "double";
		}else if (k.matches("boolean|bool")){
			ergebnis = sprache == Sprache.PASCAL ? "Boolean" : sprache == Sprache.JAVA ? "boolean" : "bool";
		}else if (k.matches("string|str")){
			ergebnis = sprache == Sprache.PASCAL ? "string" : sprache == Sprache.JAVA ? "String" : sprache == Sprache.C ? "char*" : "string";
		}else if (k.matches("char")){
			ergebnis = sprache == Sprache.PASCAL ? "Char" : "char";
		}else if (k.equals("void")){
			ergebnis = "void";
		}else{
			return t.trim();
		}
		if (feld){
			return sprache == Sprache.PASCAL ? "array of " + ergebnis : ergebnis + "[]";
		}
		return ergebnis;
	}


	// ---------------- Hilfsmethoden ----------------

	private void zeile(int tiefe, String text){
		int anzahl = einrueckungStart + tiefe * einrueckungProStufe;
		for (int i = 0; i < anzahl; i++){
			sb.append(' ');
		}
		sb.append(text).append('\n');
	}

	private String kommentar(String t){
		switch (sprache){
		case PYTHON: return "# " + t;
		case PASCAL: return "{ " + t.replace("}", ")") + " }";
		default: return "// " + t;
		}
	}

	private static String text(StruktogrammElement e){
		return verbinden(zeilen(e), " ");
	}

	private static List<String> zeilen(StruktogrammElement e){
		List<String> ergebnis = new ArrayList<String>();
		for (String s : e.gibText()){
			if (s != null && !s.trim().isEmpty()){
				ergebnis.add(s.trim());
			}
		}
		return ergebnis;
	}

	private static boolean istLeer(StruktogrammElementListe liste){
		for (StruktogrammElement e : liste){
			if (!(e instanceof LeerElement)){
				return false;
			}
		}
		return true;
	}

	private static StruktogrammElement einzigesElement(StruktogrammElementListe liste){
		StruktogrammElement gefunden = null;
		for (StruktogrammElement e : liste){
			if (!(e instanceof LeerElement)){
				if (gefunden != null){
					return null;
				}
				gefunden = e;
			}
		}
		return gefunden;
	}

	//letztes Element ist ein Sprung (break/return/...), dann braucht ein case kein weiteres break
	private static boolean endetMitSprung(StruktogrammElementListe liste){
		StruktogrammElement letztes = null;
		for (StruktogrammElement e : liste){
			if (!(e instanceof LeerElement)){
				letztes = e;
			}
		}
		if (letztes == null){
			return false;
		}
		int typ = Struktogramm.strElementZuTypnummer(letztes);
		return (typ == Struktogramm.typAussprung || typ == Struktogramm.typAnweisung)
				&& text(letztes).matches("(?i)(break|continue|return|throw|exit|goto)\\b.*");
	}

	//"1, 2" bzw. "1 | 2" in einzelne Fälle zerlegen
	private static List<String> labelsTeilen(String label){
		List<String> ergebnis = new ArrayList<String>();
		for (String s : teilen(label.replace('|', ','), ',')){
			if (!s.trim().isEmpty()){
				ergebnis.add(s.trim());
			}
		}
		if (ergebnis.isEmpty()){
			ergebnis.add(label.trim());
		}
		return ergebnis;
	}

	//teilt an trenner, aber nicht innerhalb von Klammern oder Zeichenketten
	private static List<String> teilen(String s, char trenner){
		List<String> teile = new ArrayList<String>();
		int tiefe = 0;
		int start = 0;
		char inString = 0;
		for (int i = 0; i < s.length(); i++){
			char c = s.charAt(i);
			if (inString != 0){
				if (c == '\\' && inString == '"'){
					i++;
				}else if (c == inString){
					inString = 0;
				}
				continue;
			}
			if (c == '"' || c == '\''){
				inString = c;
			}else if (c == '(' || c == '[' || c == '{'){
				tiefe++;
			}else if (c == ')' || c == ']' || c == '}'){
				tiefe = Math.max(0, tiefe - 1);
			}else if (c == trenner && tiefe == 0){
				teile.add(s.substring(start, i));
				start = i + 1;
			}
		}
		teile.add(s.substring(start));
		return teile;
	}

	//Zahl addieren, bei Ausdrücken als Text ("n - 1")
	private static String plus(String ausdruck, long wert){
		String a = ausdruck.trim();
		try{
			return Long.toString(Long.parseLong(a) + wert);
		}catch(NumberFormatException e){
			Matcher m = Pattern.compile("^(.*?)\\s*([+-])\\s*(\\d+)$").matcher(a);
			if (m.matches() && !m.group(1).isEmpty()){ //z.B. "n - 1" + 1 = "n"
				long bisher = Long.parseLong(m.group(3)) * (m.group(2).equals("-") ? -1 : 1);
				long neu = bisher + wert;
				return neu == 0 ? m.group(1) : m.group(1) + (neu > 0 ? " + " : " - ") + Math.abs(neu);
			}
			return wert == 0 ? a : a + (wert > 0 ? " + " : " - ") + Math.abs(wert);
		}
	}

	private static String verbinden(List<String> teile, String trenner){
		StringBuilder sb = new StringBuilder();
		for (String s : teile){
			if (sb.length() > 0){
				sb.append(trenner);
			}
			sb.append(s);
		}
		return sb.toString();
	}
}
