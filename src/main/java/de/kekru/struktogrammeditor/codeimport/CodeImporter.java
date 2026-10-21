package de.kekru.struktogrammeditor.codeimport;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//Einstiegspunkt: wandelt Quellcode in eine Liste von Struktogrammen um
public class CodeImporter {

	public enum Sprache {
		AUTOMATISCH("Automatisch erkennen"),
		CLIKE("Java / C / C++ / C# / JavaScript"),
		PYTHON("Python"),
		PASCAL("Pascal / Delphi");

		private final String anzeige;

		Sprache(String anzeige){
			this.anzeige = anzeige;
		}

		@Override
		public String toString(){
			return anzeige;
		}
	}

	private static final Pattern pythonKopfzeile = Pattern.compile("(?m)^[ \\t]*(if|elif|else|while|for|def|class|try|except|with)\\b[^{;\\n]*:[ \\t]*(#.*)?$");

	public static List<Diagramm> importieren(String code, Sprache sprache, boolean semikolonBehalten) throws CodeParseException {
		if (sprache == Sprache.AUTOMATISCH){
			sprache = erkennen(code);
		}

		switch (sprache){
		case PYTHON:
			return new PythonParser(code).parse();
		case PASCAL:
			return new PascalParser(code, semikolonBehalten).parse();
		default:
			return new CLikeParser(code, semikolonBehalten).parse();
		}
	}


	public static Sprache erkennen(String code){
		String klein = code.toLowerCase();

		if (klein.matches("(?s).*\\bbegin\\b.*") && klein.matches("(?s).*\\bend\\b.*")
				&& (klein.contains(":=") || klein.matches("(?s).*\\b(writeln|readln|procedure|program)\\b.*"))){
			return Sprache.PASCAL;
		}

		int pythonZeilen = 0;
		Matcher m = pythonKopfzeile.matcher(code);
		while (m.find()){
			pythonZeilen++;
		}
		int geschweifteKlammern = code.length() - code.replace("{", "").length();
		if (pythonZeilen > 0 && pythonZeilen >= geschweifteKlammern){
			return Sprache.PYTHON;
		}

		if (code.matches("(?s)\\s*(print|input|import)\\b[^;{]*") ){ //kurzer Python-Code ohne Blöcke
			return Sprache.PYTHON;
		}

		return Sprache.CLIKE;
	}
}
