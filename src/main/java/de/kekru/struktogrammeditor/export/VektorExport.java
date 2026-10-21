package de.kekru.struktogrammeditor.export;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import de.kekru.struktogrammeditor.export.VektorGrafik.Element;

//schreibt die mit VektorGrafik aufgezeichneten Elemente als SVG- oder PDF-Datei (ohne zusätzliche Bibliotheken)
public class VektorExport {

	private static final Charset UTF8 = Charset.forName("UTF-8");
	private static final Charset WINDOWS1252 = Charset.forName("windows-1252");
	private static final Charset LATIN1 = Charset.forName("ISO-8859-1");

	private static String zahl(double d){
		if (Math.abs(d - Math.rint(d)) < 0.0001){
			return Long.toString(Math.round(d));
		}
		return String.format(Locale.ROOT, "%.2f", d);
	}

	private static String farbeHex(Color c){
		return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
	}


	// ---------------- SVG ----------------

	public static void alsSvgSpeichern(VektorGrafik vg, File datei) throws IOException {
		Rectangle b = vg.getBereich();
		StringBuilder sb = new StringBuilder();
		sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		sb.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"").append(b.width).append("\" height=\"").append(b.height)
		.append("\" viewBox=\"").append(b.x).append(' ').append(b.y).append(' ').append(b.width).append(' ').append(b.height).append("\">\n");
		sb.append("<rect x=\"").append(b.x).append("\" y=\"").append(b.y).append("\" width=\"").append(b.width).append("\" height=\"").append(b.height).append("\" fill=\"#ffffff\"/>\n");

		for (Element e : vg.getElemente()){
			switch (e.art){
			case FLAECHE:
				sb.append("<rect x=\"").append(zahl(e.x1)).append("\" y=\"").append(zahl(e.y1)).append("\" width=\"").append(zahl(e.x2))
				.append("\" height=\"").append(zahl(e.y2)).append("\" fill=\"").append(farbeHex(e.farbe)).append("\"/>\n");
				break;
			case RECHTECK: //Linien um einen halben Pixel verschieben, damit sie scharf auf dem Pixelraster liegen
				sb.append("<rect x=\"").append(zahl(e.x1 + 0.5)).append("\" y=\"").append(zahl(e.y1 + 0.5)).append("\" width=\"").append(zahl(e.x2))
				.append("\" height=\"").append(zahl(e.y2)).append("\" fill=\"none\" stroke=\"").append(farbeHex(e.farbe)).append("\" stroke-width=\"1\"/>\n");
				break;
			case LINIE:
				sb.append("<line x1=\"").append(zahl(e.x1 + 0.5)).append("\" y1=\"").append(zahl(e.y1 + 0.5)).append("\" x2=\"").append(zahl(e.x2 + 0.5))
				.append("\" y2=\"").append(zahl(e.y2 + 0.5)).append("\" stroke=\"").append(farbeHex(e.farbe)).append("\" stroke-width=\"1\" stroke-linecap=\"square\"/>\n");
				break;
			case TEXT:
				Font f = e.font;
				sb.append("<text x=\"").append(zahl(e.x1)).append("\" y=\"").append(zahl(e.y1)).append("\" font-family=\"")
				.append(xml(f.getFamily())).append(", ").append(f.getFamily().toLowerCase().contains("serif") && !f.getFamily().toLowerCase().contains("sans") ? "serif" : "sans-serif")
				.append("\" font-size=\"").append(f.getSize()).append('"');
				if (f.isBold()){
					sb.append(" font-weight=\"bold\"");
				}
				if (f.isItalic()){
					sb.append(" font-style=\"italic\"");
				}
				sb.append(" fill=\"").append(farbeHex(e.farbe)).append("\" xml:space=\"preserve\">").append(xml(e.text)).append("</text>\n");
				break;
			}
		}
		sb.append("</svg>\n");

		OutputStream out = new FileOutputStream(datei);
		try{
			out.write(sb.toString().getBytes(UTF8));
		}finally{
			out.close();
		}
	}

	private static String xml(String s){
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}


	// ---------------- PDF ----------------

	//PDF-Standardschriften, die jeder PDF-Betrachter kennt
	private static final String[] pdfSchriften = {
		"Helvetica", "Helvetica-Bold", "Helvetica-Oblique", "Helvetica-BoldOblique",
		"Times-Roman", "Times-Bold", "Times-Italic", "Times-BoldItalic",
		"Courier", "Courier-Bold", "Courier-Oblique", "Courier-BoldOblique"};

	private static int schriftGruppe(Font f){
		String name = f.getFamily().toLowerCase();
		if (name.contains("mono") || name.contains("courier") || name.contains("consol")){
			return 2;
		}
		if ((name.contains("serif") && !name.contains("sans")) || name.contains("times") || name.contains("georgia") || name.contains("cambria") || name.contains("garamond")){
			return 1;
		}
		return 0;
	}

	private static int pdfSchriftIndex(Font f){
		int stil = (f.isBold() ? 1 : 0) + (f.isItalic() ? 2 : 0);
		return schriftGruppe(f) * 4 + stil;
	}


	//Schrift mit denselben Zeichenbreiten wie die PDF-Standardschrift (Arial = Helvetica, Times New Roman = Times, Courier New = Courier),
	//damit Texte im PDF genauso zentriert sind wie im Programm
	public static Font pdfKompatibleSchrift(Font f){
		String[][] kandidaten = {{"Arial", "Liberation Sans", "SansSerif"}, {"Times New Roman", "Liberation Serif", "Serif"}, {"Courier New", "Liberation Mono", "Monospaced"}};
		String[] vorhanden;
		try{
			vorhanden = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
		}catch(RuntimeException e){
			vorhanden = new String[0];
		}
		for (String k : kandidaten[schriftGruppe(f)]){
			for (String v : vorhanden){
				if (v.equalsIgnoreCase(k)){
					return new Font(v, f.getStyle(), f.getSize());
				}
			}
		}
		return new Font(kandidaten[schriftGruppe(f)][2], f.getStyle(), f.getSize());
	}


	public static void alsPdfSpeichern(VektorGrafik vg, File datei) throws IOException {
		Rectangle b = vg.getBereich();
		double massstab = 0.75; //1 Bildschirmpixel (96 dpi) = 0,75 pt
		double seitenBreite = b.width * massstab;
		double seitenHoehe = b.height * massstab;

		//Seiteninhalt: Koordinatensystem so drehen, dass y wie in Java nach unten zeigt
		StringBuilder inhalt = new StringBuilder();
		inhalt.append(zahl(massstab)).append(" 0 0 ").append(zahl(-massstab)).append(" 0 ").append(zahl(seitenHoehe)).append(" cm\n");
		inhalt.append("1 0 0 1 ").append(-b.x).append(' ').append(-b.y).append(" cm\n");
		inhalt.append("1 1 1 rg ").append(b.x).append(' ').append(b.y).append(' ').append(b.width).append(' ').append(b.height).append(" re f\n");
		inhalt.append("1 w 2 J\n");

		for (Element e : vg.getElemente()){
			String rgb = zahl(e.farbe.getRed() / 255.0) + " " + zahl(e.farbe.getGreen() / 255.0) + " " + zahl(e.farbe.getBlue() / 255.0);
			switch (e.art){
			case FLAECHE:
				inhalt.append(rgb).append(" rg ").append(zahl(e.x1)).append(' ').append(zahl(e.y1)).append(' ').append(zahl(e.x2)).append(' ').append(zahl(e.y2)).append(" re f\n");
				break;
			case RECHTECK:
				inhalt.append(rgb).append(" RG ").append(zahl(e.x1 + 0.5)).append(' ').append(zahl(e.y1 + 0.5)).append(' ').append(zahl(e.x2)).append(' ').append(zahl(e.y2)).append(" re S\n");
				break;
			case LINIE:
				inhalt.append(rgb).append(" RG ").append(zahl(e.x1 + 0.5)).append(' ').append(zahl(e.y1 + 0.5)).append(" m ")
				.append(zahl(e.x2 + 0.5)).append(' ').append(zahl(e.y2 + 0.5)).append(" l S\n");
				break;
			case TEXT:
				inhalt.append("BT ").append(rgb).append(" rg /F").append(pdfSchriftIndex(e.font) + 1).append(' ').append(e.font.getSize())
				.append(" Tf 1 0 0 -1 ").append(zahl(e.x1)).append(' ').append(zahl(e.y1)).append(" Tm (").append(pdfText(e.text)).append(") Tj ET\n");
				break;
			}
		}
		byte[] inhaltBytes = inhalt.toString().getBytes(LATIN1);

		//PDF-Objekte
		List<String> objekte = new ArrayList<String>();
		objekte.add("<< /Type /Catalog /Pages 2 0 R >>");
		objekte.add("<< /Type /Pages /Kids [3 0 R] /Count 1 >>");
		StringBuilder schriften = new StringBuilder();
		for (int i = 0; i < pdfSchriften.length; i++){
			schriften.append("/F").append(i + 1).append(' ').append(i + 5).append(" 0 R ");
		}
		objekte.add("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + zahl(seitenBreite) + " " + zahl(seitenHoehe) + "] /Contents 4 0 R /Resources << /Font << " + schriften + ">> >> >>");
		objekte.add(null); //Inhaltsstrom, wird unten gesondert geschrieben
		for (String s : pdfSchriften){
			objekte.add("<< /Type /Font /Subtype /Type1 /BaseFont /" + s + " /Encoding /WinAnsiEncoding >>");
		}

		ByteArrayOutputStream pdf = new ByteArrayOutputStream();
		schreiben(pdf, "%PDF-1.4\n%âãÏÓ\n");
		long[] positionen = new long[objekte.size()];
		for (int i = 0; i < objekte.size(); i++){
			positionen[i] = pdf.size();
			schreiben(pdf, (i + 1) + " 0 obj\n");
			if (objekte.get(i) == null){
				schreiben(pdf, "<< /Length " + inhaltBytes.length + " >>\nstream\n");
				pdf.write(inhaltBytes);
				schreiben(pdf, "\nendstream\n");
			}else{
				schreiben(pdf, objekte.get(i) + "\n");
			}
			schreiben(pdf, "endobj\n");
		}
		long xref = pdf.size();
		schreiben(pdf, "xref\n0 " + (objekte.size() + 1) + "\n0000000000 65535 f \n");
		for (long p : positionen){
			schreiben(pdf, String.format(Locale.ROOT, "%010d 00000 n \n", p));
		}
		schreiben(pdf, "trailer\n<< /Size " + (objekte.size() + 1) + " /Root 1 0 R >>\nstartxref\n" + xref + "\n%%EOF\n");

		OutputStream out = new FileOutputStream(datei);
		try{
			pdf.writeTo(out);
		}finally{
			out.close();
		}
	}

	private static void schreiben(ByteArrayOutputStream out, String s) throws IOException {
		out.write(s.getBytes(LATIN1));
	}

	//Text in Windows-1252 kodieren (wie WinAnsiEncoding) und Sonderzeichen für PDF-Strings maskieren
	private static String pdfText(String text){
		StringBuilder sb = new StringBuilder();
		for (char c : text.toCharArray()){
			byte[] bytes = String.valueOf(c).getBytes(WINDOWS1252);
			int b = bytes.length == 1 ? bytes[0] & 0xff : '?';
			if (b == '(' || b == ')' || b == '\\'){
				sb.append('\\').append((char)b);
			}else if (b < 32 || b > 126){
				sb.append('\\').append(String.format("%03o", b));
			}else{
				sb.append((char)b);
			}
		}
		return sb.toString();
	}
}
