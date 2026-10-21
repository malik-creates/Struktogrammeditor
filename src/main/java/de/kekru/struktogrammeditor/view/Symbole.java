package de.kekru.struktogrammeditor.view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;

import javax.swing.Icon;
import javax.swing.UIManager;

import de.kekru.struktogrammeditor.control.Struktogramm;

//Vektor-Icons, die zur Laufzeit gezeichnet werden und sich so automatisch an helles und dunkles Design anpassen
public class Symbole {

	public static final Color akzent = new Color(0x2F6FED);

	public enum Art { NEU, OEFFNEN, SPEICHERN, RUECKGAENGIG, WIDERRUFEN, CODE_IMPORT, CODE_EXPORT, BILD, KOPIEREN, MOND, SONNE, PAPIERKORB, ABLAGE }

	private static Color vordergrund(Component c){
		Color col = UIManager.getColor("Label.foreground");
		if (c != null && !c.isEnabled()){
			col = UIManager.getColor("Label.disabledForeground");
		}
		return col != null ? col : Color.darkGray;
	}

	private static Graphics2D vorbereiten(Graphics g, int x, int y, float staerke){
		Graphics2D g2 = (Graphics2D)g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		g2.translate(x, y);
		g2.setStroke(new BasicStroke(staerke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		return g2;
	}


	//Symbole für Werkzeugleiste und Ablage, gezeichnet auf einem 16x16-Raster
	public static class SymbolIcon implements Icon {
		private final Art art;
		private final int groesse;

		public SymbolIcon(Art art, int groesse){
			this.art = art;
			this.groesse = groesse;
		}

		public int getIconWidth(){
			return groesse;
		}

		public int getIconHeight(){
			return groesse;
		}

		public void paintIcon(Component c, Graphics g, int x, int y){
			Graphics2D g2 = vorbereiten(g, x, y, 1.4f);
			double s = groesse / 16.0;
			g2.scale(s, s);
			g2.setColor(vordergrund(c));

			GeneralPath p = new GeneralPath();
			switch (art){
			case NEU:
				p.moveTo(3.5, 1.5); p.lineTo(9.5, 1.5); p.lineTo(12.5, 4.5); p.lineTo(12.5, 14.5); p.lineTo(3.5, 14.5); p.closePath();
				g2.draw(p);
				g2.draw(new Line2D.Double(9.5, 1.5, 9.5, 4.5));
				g2.draw(new Line2D.Double(9.5, 4.5, 12.5, 4.5));
				g2.setColor(akzent);
				g2.draw(new Line2D.Double(8, 8, 8, 12));
				g2.draw(new Line2D.Double(6, 10, 10, 10));
				break;
			case OEFFNEN:
				p.moveTo(1.5, 13.5); p.lineTo(1.5, 3); p.lineTo(6, 3); p.lineTo(7.5, 4.5); p.lineTo(13, 4.5); p.lineTo(13, 6.5);
				g2.draw(p);
				p = new GeneralPath();
				p.moveTo(1.5, 13.5); p.lineTo(4, 7); p.lineTo(15, 7); p.lineTo(12.5, 13.5); p.closePath();
				g2.setColor(new Color(akzent.getRed(), akzent.getGreen(), akzent.getBlue(), 60));
				g2.fill(p);
				g2.setColor(vordergrund(c));
				g2.draw(p);
				break;
			case SPEICHERN:
				g2.draw(new RoundRectangle2D.Double(2, 2, 12, 12, 3, 3));
				g2.draw(new Line2D.Double(5, 2, 5, 5.5));
				g2.draw(new Line2D.Double(5, 5.5, 10.5, 5.5));
				g2.draw(new Line2D.Double(10.5, 5.5, 10.5, 2));
				g2.setColor(akzent);
				g2.draw(new RoundRectangle2D.Double(5, 9, 6, 5, 1.5, 1.5));
				break;
			case RUECKGAENGIG:
			case WIDERRUFEN:
				if (art == Art.WIDERRUFEN){
					g2.translate(16, 0);
					g2.scale(-1, 1);
				}
				g2.draw(new Arc2D.Double(3, 4, 11, 9, 90, -200, Arc2D.OPEN));
				p.moveTo(6.5, 1.5); p.lineTo(3.5, 4.5); p.lineTo(6.5, 7.5);
				g2.draw(p);
				break;
			case CODE_IMPORT:
				p.moveTo(5, 4); p.lineTo(1.5, 8); p.lineTo(5, 12);
				p.moveTo(11, 4); p.lineTo(14.5, 8); p.lineTo(11, 12);
				g2.draw(p);
				g2.setColor(akzent);
				g2.draw(new Line2D.Double(9.2, 3, 6.8, 13));
				break;
			case CODE_EXPORT:
				g2.draw(new RoundRectangle2D.Double(1.5, 1.5, 13, 13, 2, 2));
				g2.draw(new Line2D.Double(1.5, 5.5, 14.5, 5.5));
				g2.draw(new Line2D.Double(1.5, 1.5, 8, 5.5));
				g2.draw(new Line2D.Double(14.5, 1.5, 8, 5.5));
				g2.setColor(akzent);
				g2.draw(new Line2D.Double(8, 5.5, 8, 14.5));
				g2.draw(new Line2D.Double(1.5, 10, 8, 10));
				break;
			case BILD:
				g2.draw(new RoundRectangle2D.Double(1.5, 2.5, 13, 11, 3, 3));
				p.moveTo(2, 12.5); p.lineTo(6, 8); p.lineTo(9, 11); p.lineTo(11, 9); p.lineTo(14, 12);
				g2.draw(p);
				g2.setColor(akzent);
				g2.fill(new java.awt.geom.Ellipse2D.Double(9.5, 4.5, 2.5, 2.5));
				break;
			case KOPIEREN:
				g2.draw(new RoundRectangle2D.Double(5.5, 5.5, 9, 9, 2.5, 2.5));
				p.moveTo(3, 10.5); p.lineTo(2.5, 10.5); p.quadTo(1.5, 10.5, 1.5, 9.5); p.lineTo(1.5, 2.5); p.quadTo(1.5, 1.5, 2.5, 1.5);
				p.lineTo(9.5, 1.5); p.quadTo(10.5, 1.5, 10.5, 2.5); p.lineTo(10.5, 3);
				g2.draw(p);
				break;
			case MOND:
				p.moveTo(10, 2); p.curveTo(4, 2, 1.5, 9, 6, 13); p.curveTo(9, 15.5, 13.5, 14, 14.5, 10.5);
				p.curveTo(10, 12, 6.5, 7, 10, 2); p.closePath();
				g2.draw(p);
				break;
			case SONNE:
				g2.draw(new java.awt.geom.Ellipse2D.Double(5, 5, 6, 6));
				for (int i = 0; i < 8; i++){
					double w = Math.PI / 4 * i;
					g2.draw(new Line2D.Double(8 + Math.cos(w) * 5.3, 8 + Math.sin(w) * 5.3, 8 + Math.cos(w) * 7, 8 + Math.sin(w) * 7));
				}
				break;
			case PAPIERKORB:
				g2.draw(new Line2D.Double(2, 4, 14, 4));
				p.moveTo(6, 4); p.lineTo(6, 2); p.lineTo(10, 2); p.lineTo(10, 4);
				p.moveTo(3.5, 4); p.lineTo(4.5, 14.5); p.lineTo(11.5, 14.5); p.lineTo(12.5, 4);
				p.moveTo(6.7, 7); p.lineTo(6.9, 12); p.moveTo(9.3, 7); p.lineTo(9.1, 12);
				g2.draw(p);
				break;
			case ABLAGE:
				g2.draw(new RoundRectangle2D.Double(2.5, 3, 11, 12, 2.5, 2.5));
				g2.setColor(akzent);
				g2.draw(new RoundRectangle2D.Double(5.5, 1.5, 5, 3, 1.5, 1.5));
				g2.setColor(vordergrund(c));
				g2.draw(new Line2D.Double(5.5, 8, 10.5, 8));
				g2.draw(new Line2D.Double(5.5, 11, 9, 11));
				break;
			}
			g2.dispose();
		}
	}


	//kleines Struktogramm-Bild für die Elementleiste
	public static class ElementIcon implements Icon {
		private static final int B = 38, H = 28;
		private final int typ;

		public ElementIcon(int typ){
			this.typ = typ;
		}

		public int getIconWidth(){
			return B;
		}

		public int getIconHeight(){
			return H;
		}

		private static Color fuellfarbe(int typ){
			switch (typ){
			case Struktogramm.typVerzweigung:
			case Struktogramm.typFallauswahl:
				return new Color(0xF59E0B);
			case Struktogramm.typForSchleife:
			case Struktogramm.typWhileSchleife:
			case Struktogramm.typDoUntilSchleife:
			case Struktogramm.typEndlosschleife:
				return new Color(0x10B981);
			case Struktogramm.typAussprung:
				return new Color(0xEF4444);
			case Struktogramm.typAufruf:
				return new Color(0x8B5CF6);
			default:
				return akzent;
			}
		}

		public void paintIcon(Component c, Graphics g, int x, int y){
			Graphics2D g2 = vorbereiten(g, x, y, 1.2f);
			Color farbe = fuellfarbe(typ);
			Color flaeche = new Color(farbe.getRed(), farbe.getGreen(), farbe.getBlue(), 45);
			Color linie = vordergrund(c);

			double l = 1.5, o = 2.5, r = B - 1.5, u = H - 2.5;

			//Fläche
			g2.setColor(flaeche);
			g2.fill(new RoundRectangle2D.Double(l, o, r - l, u - o, 3, 3));
			g2.setColor(new Color(farbe.getRed(), farbe.getGreen(), farbe.getBlue(), 90));

			switch (typ){
			case Struktogramm.typForSchleife:
			case Struktogramm.typWhileSchleife:
				g2.fill(new java.awt.geom.Rectangle2D.Double(l, o, r - l, 8));
				g2.fill(new java.awt.geom.Rectangle2D.Double(l, o, 7, u - o));
				break;
			case Struktogramm.typDoUntilSchleife:
				g2.fill(new java.awt.geom.Rectangle2D.Double(l, u - 8, r - l, 8));
				g2.fill(new java.awt.geom.Rectangle2D.Double(l, o, 7, u - o));
				break;
			case Struktogramm.typEndlosschleife:
				g2.fill(new java.awt.geom.Rectangle2D.Double(l, o, r - l, 6));
				g2.fill(new java.awt.geom.Rectangle2D.Double(l, u - 6, r - l, 6));
				g2.fill(new java.awt.geom.Rectangle2D.Double(l, o, 7, u - o));
				break;
			case Struktogramm.typVerzweigung:
			case Struktogramm.typFallauswahl:
				g2.fill(new java.awt.geom.Rectangle2D.Double(l, o, r - l, 11));
				break;
			}

			//Linien
			g2.setColor(linie);
			g2.draw(new RoundRectangle2D.Double(l, o, r - l, u - o, 3, 3));
			double m = (l + r) / 2;
			switch (typ){
			case Struktogramm.typVerzweigung:
				g2.draw(new Line2D.Double(l, o + 11, r, o + 11));
				g2.draw(new Line2D.Double(l, o, m, o + 11));
				g2.draw(new Line2D.Double(r, o, m, o + 11));
				g2.draw(new Line2D.Double(m, o + 11, m, u));
				break;
			case Struktogramm.typFallauswahl:
				double sp = l + (r - l) * 0.68;
				g2.draw(new Line2D.Double(l, o + 11, r, o + 11));
				g2.draw(new Line2D.Double(l, o, sp, o + 11));
				g2.draw(new Line2D.Double(r, o, sp, o + 11));
				g2.draw(new Line2D.Double(l + (sp - l) / 2, o + 5.5, l + (sp - l) / 2, u));
				g2.draw(new Line2D.Double(sp, o + 11, sp, u));
				break;
			case Struktogramm.typForSchleife:
			case Struktogramm.typWhileSchleife:
				g2.draw(new Line2D.Double(l + 7, o + 8, r, o + 8));
				g2.draw(new Line2D.Double(l + 7, o + 8, l + 7, u));
				break;
			case Struktogramm.typDoUntilSchleife:
				g2.draw(new Line2D.Double(l + 7, u - 8, r, u - 8));
				g2.draw(new Line2D.Double(l + 7, o, l + 7, u - 8));
				break;
			case Struktogramm.typEndlosschleife:
				g2.draw(new Line2D.Double(l + 7, o + 6, r, o + 6));
				g2.draw(new Line2D.Double(l + 7, u - 6, r, u - 6));
				g2.draw(new Line2D.Double(l + 7, o + 6, l + 7, u - 6));
				break;
			case Struktogramm.typAussprung:
				g2.draw(new Line2D.Double(l + 7, o, l, (o + u) / 2));
				g2.draw(new Line2D.Double(l + 7, u, l, (o + u) / 2));
				break;
			case Struktogramm.typAufruf:
				g2.draw(new Line2D.Double(l + 5, o, l + 5, u));
				g2.draw(new Line2D.Double(r - 5, o, r - 5, u));
				break;
			}
			g2.dispose();
		}
	}


	//Farbfeld für Farbauswahl-Buttons
	public static class FarbIcon implements Icon {
		private Color farbe;

		public FarbIcon(Color farbe){
			this.farbe = farbe;
		}

		public void setFarbe(Color farbe){
			this.farbe = farbe;
		}

		public int getIconWidth(){
			return 16;
		}

		public int getIconHeight(){
			return 16;
		}

		public void paintIcon(Component c, Graphics g, int x, int y){
			Graphics2D g2 = vorbereiten(g, x, y, 1f);
			g2.setColor(farbe != null ? farbe : Color.white);
			g2.fill(new RoundRectangle2D.Double(0.5, 0.5, 15, 15, 5, 5));
			g2.setColor(vordergrund(c));
			g2.draw(new RoundRectangle2D.Double(0.5, 0.5, 15, 15, 5, 5));
			g2.dispose();
		}
	}
}
