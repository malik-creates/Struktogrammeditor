package de.kekru.struktogrammeditor.codeimport;

import java.util.ArrayList;
import java.util.List;

import de.kekru.struktogrammeditor.control.GlobalSettings;
import de.kekru.struktogrammeditor.control.Struktogramm;

//Liest Python-Quellcode (Blöcke über Einrückung) und erzeugt daraus Struktogramme
public class PythonParser {

	//eine logische Zeile: über Klammern oder \ fortgesetzte Zeilen sind bereits zusammengefügt, Kommentare entfernt
	private static class Zeile {
		final int einrueckung;
		final String text;
		final int nummer;

		Zeile(int einrueckung, String text, int nummer){
			this.einrueckung = einrueckung;
			this.text = text;
			this.nummer = nummer;
		}
	}

	private final List<Zeile> zeilen;
	private int i;
	private final List<Diagramm> funktionen = new ArrayList<Diagramm>();

	public PythonParser(String code){
		zeilen = logischeZeilen(code);
	}

	public List<Diagramm> parse() throws CodeParseException {
		i = 0;
		List<CodeNode> haupt = new ArrayList<CodeNode>();
		if (!zeilen.isEmpty()){
			if (zeilen.get(0).einrueckung > 0){
				throw new CodeParseException("Die erste Zeile darf nicht eingerückt sein", zeilen.get(0).nummer);
			}
			blockParsen(haupt, 0, true);
		}

		List<Diagramm> ergebnis = new ArrayList<Diagramm>();
		if (!haupt.isEmpty()){
			ergebnis.add(new Diagramm(funktionen.isEmpty() ? "" : "Hauptprogramm", haupt));
		}
		ergebnis.addAll(funktionen);
		return ergebnis;
	}


	private void blockParsen(List<CodeNode> ziel, int einrueckung, boolean obersteEbene) throws CodeParseException {
		while (i < zeilen.size()){
			Zeile z = zeilen.get(i);
			if (z.einrueckung < einrueckung){
				return;
			}
			if (z.einrueckung > einrueckung){
				throw new CodeParseException("Unerwartete Einrückung", z.nummer);
			}
			anweisungParsen(ziel, obersteEbene);
		}
	}


	private void anweisungParsen(List<CodeNode> ziel, boolean obersteEbene) throws CodeParseException {
		Zeile z = zeilen.get(i);
		String t = z.text;

		if (t.startsWith("@")){ //Dekorator
			i++;
			return;
		}

		String wort = erstesWort(t);
		if (wort.equals("async")){ //async def / async for / async with
			t = t.substring(5).trim();
			wort = erstesWort(t);
		}

		if (wort.equals("if")){
			String[] kopf = kopfLesen(t, 2, z);
			List<CodeNode> ja = koerperParsen(kopf[1], z);
			if (obersteEbene && kopf[0].matches("__name__\\s*==\\s*[\"']__main__[\"']")){ //Hauptprogramm-Block direkt übernehmen
				ziel.addAll(ja);
				return;
			}
			ziel.add(CodeNode.verzweigung(kopf[0], ja, elseKetteParsen(z.einrueckung)));
			return;
		}

		if (wort.equals("while")){
			String[] kopf = kopfLesen(t, 5, z);
			List<CodeNode> inhalt = koerperParsen(kopf[1], z);
			if (kopf[0].matches("True|1|\\(True\\)")){
				ziel.add(CodeNode.schleife(Struktogramm.typEndlosschleife, GlobalSettings.gibElementBeschriftung(Struktogramm.typEndlosschleife), inhalt));
			}else{
				ziel.add(CodeNode.schleife(Struktogramm.typWhileSchleife, kopf[0], inhalt));
			}
			schleifenElseParsen(ziel, z.einrueckung);
			return;
		}

		if (wort.equals("for")){
			String[] kopf = kopfLesen(t, 3, z);
			ziel.add(CodeNode.schleife(Struktogramm.typForSchleife, kopf[0], koerperParsen(kopf[1], z)));
			schleifenElseParsen(ziel, z.einrueckung);
			return;
		}

		if (wort.equals("def")){
			String[] kopf = kopfLesen(t, 3, z);
			String name = kopf[0].replaceAll("(?s)\\)\\s*->.*$", ")");
			List<CodeNode> inhalt = koerperParsen(kopf[1], z);
			Diagramm.letztesReturnAlsAnweisung(inhalt);
			funktionen.add(new Diagramm(name, inhalt));
			return;
		}

		if (wort.equals("class")){ //nur die Methoden der Klasse werden zu Struktogrammen
			String[] kopf = kopfLesen(t, 5, z);
			koerperParsen(kopf[1], z);
			return;
		}

		if (wort.equals("try")){
			String[] kopf = kopfLesen(t, 3, z);
			ziel.addAll(koerperParsen(kopf[1], z));
			while (i < zeilen.size() && zeilen.get(i).einrueckung == z.einrueckung){
				Zeile n = zeilen.get(i);
				String w = erstesWort(n.text);
				if (w.equals("except")){ //except-Blöcke werden nicht dargestellt
					koerperParsen(kopfLesen(n.text, 6, n)[1], n);
				}else if (w.equals("else") || w.equals("finally")){
					ziel.addAll(koerperParsen(kopfLesen(n.text, w.length(), n)[1], n));
				}else{
					break;
				}
			}
			return;
		}

		if (wort.equals("with")){
			String[] kopf = kopfLesen(t, 4, z);
			ziel.addAll(koerperParsen(kopf[1], z));
			return;
		}

		if (wort.equals("match") && t.endsWith(":") && !t.matches("match\\s*[=.(\\[].*")){
			String[] kopf = kopfLesen(t, 5, z);
			ziel.add(matchParsen(kopf[0], z));
			return;
		}

		if (wort.equals("elif") || wort.equals("else") || wort.equals("except") || wort.equals("finally") || wort.equals("case")){
			throw new CodeParseException("'" + wort + "' ohne passenden Anfang", z.nummer);
		}

		i++;
		einfacheAnweisungen(t, ziel);
	}


	private List<CodeNode> elseKetteParsen(int einrueckung) throws CodeParseException {
		List<CodeNode> nein = new ArrayList<CodeNode>();
		if (i < zeilen.size() && zeilen.get(i).einrueckung == einrueckung){
			Zeile n = zeilen.get(i);
			String w = erstesWort(n.text);
			if (w.equals("elif")){
				String[] kopf = kopfLesen(n.text, 4, n);
				List<CodeNode> ja = koerperParsen(kopf[1], n);
				nein.add(CodeNode.verzweigung(kopf[0], ja, elseKetteParsen(einrueckung)));
			}else if (w.equals("else")){
				nein = koerperParsen(kopfLesen(n.text, 4, n)[1], n);
			}
		}
		return nein;
	}


	//der else-Teil einer Schleife wird nach der Schleife angehängt
	private void schleifenElseParsen(List<CodeNode> ziel, int einrueckung) throws CodeParseException {
		if (i < zeilen.size() && zeilen.get(i).einrueckung == einrueckung && erstesWort(zeilen.get(i).text).equals("else")){
			Zeile n = zeilen.get(i);
			ziel.addAll(koerperParsen(kopfLesen(n.text, 4, n)[1], n));
		}
	}


	private CodeNode matchParsen(String ausdruck, Zeile kopfZeile) throws CodeParseException {
		i++;
		if (i >= zeilen.size() || zeilen.get(i).einrueckung <= kopfZeile.einrueckung){
			throw new CodeParseException("Eingerückter Block mit 'case' erwartet", kopfZeile.nummer);
		}
		int einrueckung = zeilen.get(i).einrueckung;

		CodeNode knoten = new CodeNode(Struktogramm.typFallauswahl, ausdruck);
		List<CodeNode> sonst = new ArrayList<CodeNode>();

		while (i < zeilen.size() && zeilen.get(i).einrueckung >= einrueckung){
			Zeile n = zeilen.get(i);
			if (n.einrueckung > einrueckung){
				throw new CodeParseException("Unerwartete Einrückung", n.nummer);
			}
			if (!erstesWort(n.text).equals("case")){
				throw new CodeParseException("'case' erwartet", n.nummer);
			}
			String[] kopf = kopfLesen(n.text, 4, n);
			List<CodeNode> inhalt = koerperParsen(kopf[1], n);
			if (kopf[0].equals("_")){
				sonst = inhalt;
			}else{
				knoten.fallHinzufuegen(kopf[0], inhalt);
			}
		}

		if (knoten.getFaelle().isEmpty()){
			knoten.fallHinzufuegen("", new ArrayList<CodeNode>());
		}
		knoten.fallHinzufuegen("Sonst", sonst);
		return knoten;
	}


	//liest den Körper nach dem ':' einer Kopfzeile: entweder in derselben Zeile (rest) oder als eingerückter Block
	private List<CodeNode> koerperParsen(String rest, Zeile kopfZeile) throws CodeParseException {
		i++;
		List<CodeNode> inhalt = new ArrayList<CodeNode>();
		if (!rest.isEmpty()){
			einfacheAnweisungen(rest, inhalt);
			return inhalt;
		}
		if (i >= zeilen.size() || zeilen.get(i).einrueckung <= kopfZeile.einrueckung){
			throw new CodeParseException("Eingerückter Block erwartet", kopfZeile.nummer);
		}
		blockParsen(inhalt, zeilen.get(i).einrueckung, false);
		return inhalt;
	}


	//teilt die Kopfzeile am ':' auf, gibt {Kopf ohne Schlüsselwort, Rest nach ':'} zurück
	private String[] kopfLesen(String t, int schluesselwortLaenge, Zeile z) throws CodeParseException {
		int tiefe = 0;
		for (int k = schluesselwortLaenge; k < t.length(); k++){
			char c = t.charAt(k);
			if (c == '"' || c == '\''){
				k = stringEnde(t, k) - 1;
				continue;
			}
			if (c == '(' || c == '[' || c == '{'){
				tiefe++;
			}else if (c == ')' || c == ']' || c == '}'){
				tiefe--;
			}else if (c == ':' && tiefe == 0 && (k + 1 >= t.length() || t.charAt(k + 1) != '=')){
				return new String[] {t.substring(schluesselwortLaenge, k).trim(), t.substring(k + 1).trim()};
			}
		}
		throw new CodeParseException("':' am Ende der Zeile fehlt", z.nummer);
	}


	//einfache Anweisungen, ggf. mit ';' getrennt
	private void einfacheAnweisungen(String text, List<CodeNode> ziel){
		for (String teil : amSemikolonTeilen(text)){
			String a = teil.trim();
			String w = erstesWort(a);
			if (a.isEmpty() || w.equals("pass") || w.equals("import") || w.equals("from") || w.equals("global") || w.equals("nonlocal")){
				continue;
			}
			int q = 0;
			while (q < 2 && q < a.length() && "rRbBuUfF".indexOf(a.charAt(q)) >= 0){
				q++;
			}
			if (q < a.length() && (a.charAt(q) == '"' || a.charAt(q) == '\'') && stringEnde(a, q) >= a.length()){
				continue; //Docstring bzw. alleinstehender String
			}
			if (w.equals("return") || w.equals("break") || w.equals("continue") || w.equals("raise")){
				ziel.add(CodeNode.aussprung(CLikeParser.norm(a)));
			}else{
				ziel.add(CodeNode.anweisung(CLikeParser.norm(a)));
			}
		}
	}


	private static List<String> amSemikolonTeilen(String text){
		List<String> teile = new ArrayList<String>();
		int tiefe = 0;
		int start = 0;
		for (int k = 0; k < text.length(); k++){
			char c = text.charAt(k);
			if (c == '"' || c == '\''){
				k = stringEnde(text, k) - 1;
				continue;
			}
			if (c == '(' || c == '[' || c == '{'){
				tiefe++;
			}else if (c == ')' || c == ']' || c == '}'){
				tiefe--;
			}else if (c == ';' && tiefe == 0){
				teile.add(text.substring(start, k));
				start = k + 1;
			}
		}
		teile.add(text.substring(start));
		return teile;
	}


	//Index nach dem String, der bei k beginnt (einfache und dreifache Anführungszeichen)
	private static int stringEnde(String t, int k){
		char q = t.charAt(k);
		boolean dreifach = k + 2 < t.length() && t.charAt(k + 1) == q && t.charAt(k + 2) == q;
		int j = k + (dreifach ? 3 : 1);
		while (j < t.length()){
			char c = t.charAt(j);
			if (c == '\\'){
				j += 2;
				continue;
			}
			if (c == q){
				if (!dreifach){
					return j + 1;
				}
				if (j + 2 < t.length() && t.charAt(j + 1) == q && t.charAt(j + 2) == q){
					return j + 3;
				}
			}
			j++;
		}
		return t.length();
	}


	private static String erstesWort(String t){
		int k = 0;
		while (k < t.length() && (Character.isLetterOrDigit(t.charAt(k)) || t.charAt(k) == '_')){
			k++;
		}
		return t.substring(0, k);
	}


	//zerlegt den Code in logische Zeilen mit Einrückung
	private static List<Zeile> logischeZeilen(String code){
		List<Zeile> ergebnis = new ArrayList<Zeile>();
		code = code.replace("\r\n", "\n").replace('\r', '\n');

		StringBuilder aktuell = new StringBuilder();
		int einrueckung = 0;
		int startZeile = 1;
		int zeilenNummer = 1;
		int tiefe = 0;
		boolean zeilenAnfang = true;
		int k = 0;

		while (k <= code.length()){
			char c = k < code.length() ? code.charAt(k) : '\n';

			if (zeilenAnfang){ //Einrückung zählen
				int spalte = 0;
				int j = k;
				while (j < code.length() && (code.charAt(j) == ' ' || code.charAt(j) == '\t')){
					spalte = code.charAt(j) == '\t' ? (spalte / 8 + 1) * 8 : spalte + 1;
					j++;
				}
				einrueckung = spalte;
				startZeile = zeilenNummer;
				zeilenAnfang = false;
				k = j;
				continue;
			}

			if (c == '"' || c == '\''){
				int ende = stringEnde(code, k);
				String str = code.substring(k, ende);
				for (int j = 0; j < str.length(); j++){
					if (str.charAt(j) == '\n'){
						zeilenNummer++;
					}
				}
				aktuell.append(str);
				k = ende;
				continue;
			}

			if (c == '#'){
				while (k < code.length() && code.charAt(k) != '\n'){
					k++;
				}
				continue;
			}

			if (c == '\\' && k + 1 < code.length() && code.charAt(k + 1) == '\n'){
				aktuell.append(' ');
				zeilenNummer++;
				k += 2;
				while (k < code.length() && (code.charAt(k) == ' ' || code.charAt(k) == '\t')){
					k++;
				}
				continue;
			}

			if (c == '(' || c == '[' || c == '{'){
				tiefe++;
			}else if ((c == ')' || c == ']' || c == '}') && tiefe > 0){
				tiefe--;
			}

			if (c == '\n'){
				zeilenNummer++;
				if (tiefe > 0 && k < code.length()){
					//Fortsetzungszeile: Zeilenumbruch und Einrückung durch ein Leerzeichen ersetzen
					while (aktuell.length() > 0 && aktuell.charAt(aktuell.length() - 1) == ' '){
						aktuell.setLength(aktuell.length() - 1);
					}
					aktuell.append(' ');
					while (k + 1 < code.length() && (code.charAt(k + 1) == ' ' || code.charAt(k + 1) == '\t')){
						k++;
					}
				}else{
					String text = aktuell.toString().trim();
					if (!text.isEmpty()){
						ergebnis.add(new Zeile(einrueckung, text.replaceAll("\\s+$", ""), startZeile));
					}
					aktuell.setLength(0);
					tiefe = 0;
					zeilenAnfang = true;
				}
				k++;
				continue;
			}

			aktuell.append(c);
			k++;
		}

		return ergebnis;
	}
}
