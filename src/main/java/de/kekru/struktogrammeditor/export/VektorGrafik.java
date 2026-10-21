package de.kekru.struktogrammeditor.export;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.text.AttributedCharacterIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

//Graphics2D, das nicht auf Pixel zeichnet, sondern Linien, Rechtecke und Texte mitschreibt (für den Export als SVG und PDF);
//es werden nur die Zeichenbefehle unterstützt, die die StruktogrammElemente verwenden
public class VektorGrafik extends Graphics2D {

	public enum Art { LINIE, RECHTECK, FLAECHE, TEXT }

	public static class Element {
		public final Art art;
		public final double x1, y1, x2, y2; //bei Rechteck/Fläche: x, y, Breite, Höhe
		public final Color farbe;
		public final Font font;
		public final String text;

		Element(Art art, double x1, double y1, double x2, double y2, Color farbe, Font font, String text){
			this.art = art;
			this.x1 = x1;
			this.y1 = y1;
			this.x2 = x2;
			this.y2 = y2;
			this.farbe = farbe;
			this.font = font;
			this.text = text;
		}
	}

	private final List<Element> elemente;
	private final Graphics2D messung; //liefert Schriftmaße wie beim Zeichnen auf dem Bildschirm
	private Color farbe = Color.black;
	private Color hintergrund = Color.white;
	private Font font;
	private Stroke stroke = new BasicStroke(1f);
	private double dx = 0, dy = 0;
	private Rectangle bereich = new Rectangle();

	public VektorGrafik(){
		this(new ArrayList<Element>(), new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB).createGraphics());
		messung.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		messung.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		font = messung.getFont();
	}

	private VektorGrafik(List<Element> elemente, Graphics2D messung){
		this.elemente = elemente;
		this.messung = messung;
	}

	public List<Element> getElemente(){
		return elemente;
	}

	//auszugebender Bereich des Struktogramms
	public Rectangle getBereich(){
		return bereich;
	}

	public void setBereich(Rectangle bereich){
		this.bereich = bereich;
	}

	private void merken(Art art, double x1, double y1, double x2, double y2, String text){
		elemente.add(new Element(art, x1 + dx, y1 + dy, art == Art.LINIE ? x2 + dx : x2, art == Art.LINIE ? y2 + dy : y2, farbe, font, text));
	}


	//tatsächlich verwendete Zeichenbefehle

	@Override
	public void drawLine(int x1, int y1, int x2, int y2){
		merken(Art.LINIE, x1, y1, x2, y2, null);
	}

	@Override
	public void drawRect(int x, int y, int width, int height){
		if (width >= 0 && height >= 0){
			merken(Art.RECHTECK, x, y, width, height, null);
		}
	}

	@Override
	public void fillRect(int x, int y, int width, int height){
		if (width > 0 && height > 0){
			merken(Art.FLAECHE, x, y, width, height, null);
		}
	}

	@Override
	public void clearRect(int x, int y, int width, int height){
		Color c = farbe;
		farbe = hintergrund;
		fillRect(x, y, width, height);
		farbe = c;
	}

	@Override
	public void drawString(String str, int x, int y){
		drawString(str, (float)x, (float)y);
	}

	@Override
	public void drawString(String str, float x, float y){
		if (str != null && !str.isEmpty()){
			merken(Art.TEXT, x, y, 0, 0, str);
		}
	}

	@Override
	public void setColor(Color c){
		if (c != null){
			farbe = c;
		}
	}

	@Override
	public Color getColor(){
		return farbe;
	}

	@Override
	public void setFont(Font font){
		if (font != null){
			this.font = font;
			messung.setFont(font);
		}
	}

	@Override
	public Font getFont(){
		return font;
	}

	@Override
	public FontMetrics getFontMetrics(Font f){
		return messung.getFontMetrics(f);
	}

	@Override
	public FontRenderContext getFontRenderContext(){
		return messung.getFontRenderContext();
	}

	@Override
	public void setRenderingHint(RenderingHints.Key hintKey, Object hintValue){
		messung.setRenderingHint(hintKey, hintValue);
	}

	@Override
	public Object getRenderingHint(RenderingHints.Key hintKey){
		return messung.getRenderingHint(hintKey);
	}

	@Override
	public void setStroke(Stroke s){
		stroke = s;
	}

	@Override
	public Stroke getStroke(){
		return stroke;
	}

	@Override
	public void translate(int x, int y){
		dx += x;
		dy += y;
	}

	@Override
	public void translate(double tx, double ty){
		dx += tx;
		dy += ty;
	}

	@Override
	public Graphics create(){
		VektorGrafik kopie = new VektorGrafik(elemente, messung);
		kopie.farbe = farbe;
		kopie.hintergrund = hintergrund;
		kopie.font = font;
		kopie.stroke = stroke;
		kopie.dx = dx;
		kopie.dy = dy;
		kopie.bereich = bereich;
		return kopie;
	}


	//übrige Methoden werden von den StruktogrammElementen nicht benutzt

	@Override public void draw(Shape s) { }
	@Override public void fill(Shape s) { }
	@Override public boolean drawImage(Image img, AffineTransform xform, ImageObserver obs) { return true; }
	@Override public void drawImage(BufferedImage img, BufferedImageOp op, int x, int y) { }
	@Override public void drawRenderedImage(RenderedImage img, AffineTransform xform) { }
	@Override public void drawRenderableImage(RenderableImage img, AffineTransform xform) { }
	@Override public void drawString(AttributedCharacterIterator iterator, int x, int y) { }
	@Override public void drawString(AttributedCharacterIterator iterator, float x, float y) { }
	@Override public void drawGlyphVector(GlyphVector g, float x, float y) { }
	@Override public boolean hit(Rectangle rect, Shape s, boolean onStroke) { return false; }
	@Override public GraphicsConfiguration getDeviceConfiguration() { return messung.getDeviceConfiguration(); }
	@Override public void setComposite(Composite comp) { }
	@Override public void setPaint(Paint paint) { if (paint instanceof Color) { farbe = (Color)paint; } }
	@Override public void setRenderingHints(Map<?, ?> hints) { messung.setRenderingHints(hints); }
	@Override public void addRenderingHints(Map<?, ?> hints) { messung.addRenderingHints(hints); }
	@Override public RenderingHints getRenderingHints() { return messung.getRenderingHints(); }
	@Override public void rotate(double theta) { }
	@Override public void rotate(double theta, double x, double y) { }
	@Override public void scale(double sx, double sy) { }
	@Override public void shear(double shx, double shy) { }
	@Override public void transform(AffineTransform tx) { }
	@Override public void setTransform(AffineTransform tx) { }
	@Override public AffineTransform getTransform() { return AffineTransform.getTranslateInstance(dx, dy); }
	@Override public Paint getPaint() { return farbe; }
	@Override public Composite getComposite() { return messung.getComposite(); }
	@Override public void setBackground(Color color) { hintergrund = color; }
	@Override public Color getBackground() { return hintergrund; }
	@Override public void clip(Shape s) { }
	@Override public void setPaintMode() { }
	@Override public void setXORMode(Color c1) { }
	@Override public Rectangle getClipBounds() { return null; }
	@Override public void clipRect(int x, int y, int width, int height) { }
	@Override public void setClip(int x, int y, int width, int height) { }
	@Override public Shape getClip() { return null; }
	@Override public void setClip(Shape clip) { }
	@Override public void copyArea(int x, int y, int width, int height, int dx, int dy) { }
	@Override public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) { drawRect(x, y, width, height); }
	@Override public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) { fillRect(x, y, width, height); }
	@Override public void drawOval(int x, int y, int width, int height) { }
	@Override public void fillOval(int x, int y, int width, int height) { }
	@Override public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) { }
	@Override public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) { }
	@Override public void drawPolyline(int[] xPoints, int[] yPoints, int nPoints) { for (int i = 1; i < nPoints; i++) { drawLine(xPoints[i - 1], yPoints[i - 1], xPoints[i], yPoints[i]); } }
	@Override public void drawPolygon(int[] xPoints, int[] yPoints, int nPoints) { drawPolyline(xPoints, yPoints, nPoints); if (nPoints > 1) { drawLine(xPoints[nPoints - 1], yPoints[nPoints - 1], xPoints[0], yPoints[0]); } }
	@Override public void fillPolygon(int[] xPoints, int[] yPoints, int nPoints) { }
	@Override public boolean drawImage(Image img, int x, int y, ImageObserver observer) { return true; }
	@Override public boolean drawImage(Image img, int x, int y, int width, int height, ImageObserver observer) { return true; }
	@Override public boolean drawImage(Image img, int x, int y, Color bgcolor, ImageObserver observer) { return true; }
	@Override public boolean drawImage(Image img, int x, int y, int width, int height, Color bgcolor, ImageObserver observer) { return true; }
	@Override public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, ImageObserver observer) { return true; }
	@Override public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, Color bgcolor, ImageObserver observer) { return true; }
	@Override public void dispose() { }
}
