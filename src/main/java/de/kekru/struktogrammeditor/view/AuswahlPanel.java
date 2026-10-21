package de.kekru.struktogrammeditor.view;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

import org.jdom.Document;

import de.kekru.struktogrammeditor.control.Controlling;
import de.kekru.struktogrammeditor.control.GlobalSettings;
import de.kekru.struktogrammeditor.control.Struktogramm;
import de.kekru.struktogrammeditor.view.Symbole.Art;
import de.kekru.struktogrammeditor.view.Symbole.SymbolIcon;


public class AuswahlPanel extends JPanel implements DropTargetListener, DragGestureListener, DragSourceListener{

	private static final long serialVersionUID = 3619714917985247680L;
	private AuswahlPanelElement[] panelElemente = new AuswahlPanelElement[9]; //9 StruktogrammElemente stehen zur Auswahl
	private DragSource dragSource;
	//private DropTarget dropTarget;
	private AblageFeld muelleimer;
	private AblageFeld kopierFeld;
	private Controlling controlling;
	private Document kopiertesStrElement;

	public AuswahlPanel(Controlling controlling){

		this.controlling = controlling;

		setLayout(new GridBagLayout());
		setBorder(BorderFactory.createEmptyBorder(10, 8, 10, 8));

		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(1, 0, 1, 0);

		add(abschnittUeberschrift("ELEMENTE"), c);
		c.gridy++;

		for (int i = 0; i < panelElemente.length; i++){//Label für mit den einzelnen Elementen erzeugen
			panelElemente[i] = new AuswahlPanelElement(i);
			add(panelElemente[i], c);
			c.gridy++;
		}

		c.insets = new Insets(18, 0, 1, 0);
		add(abschnittUeberschrift("ABLAGE"), c);
		c.gridy++;
		c.insets = new Insets(3, 0, 3, 0);

		//Mülleimer erzeugen
		muelleimer = new AblageFeld(Art.PAPIERKORB, "Papierkorb");
		add(muelleimer, c);
		c.gridy++;

		//Kopier-Box erzeugen
		kopierFeld = new AblageFeld(Art.ABLAGE, "");
		setzeKopiertesStrElement(null);
		add(kopierFeld, c);
		c.gridy++;

		c.insets = new Insets(6, 2, 0, 2);
		c.weighty = 1;
		JLabel hinweis = Design.hinweis("<html><div style='width:170px'>Elemente aus dem Struktogramm hierher ziehen, um sie zu löschen oder für später abzulegen.</div></html>");
		hinweis.setFont(hinweis.getFont().deriveFont(hinweis.getFont().getSize2D() - 1f));
		add(hinweis, c);


		//Hervorheben des Elementes unter der Maus; die Elemente selbst dürfen keine Listener haben (siehe AuswahlPanelElement)
		MouseAdapter hover = new MouseAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				hervorheben(getComponentAt(e.getPoint()));
			}

			@Override
			public void mouseExited(MouseEvent e) {
				hervorheben(null);
			}
		};
		addMouseListener(hover);
		addMouseMotionListener(hover);


		//für Drag & Drop Aktionen vorbereiten
		dragSource = new DragSource();
		dragSource.createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_COPY_OR_MOVE, this);

		//dropTarget =
		new DropTarget(this, DnDConstants.ACTION_COPY_OR_MOVE,this, true, null);
	}


	private static JLabel abschnittUeberschrift(String text){
		JLabel label = Design.hinweis(text);
		label.setFont(label.getFont().deriveFont(Font.BOLD, label.getFont().getSize2D() - 1f));
		label.setBorder(BorderFactory.createEmptyBorder(0, 8, 4, 0));
		return label;
	}


	private void hervorheben(Component unterMaus){
		for (AuswahlPanelElement e : panelElemente){
			e.setHervorgehoben(e == unterMaus);
		}
	}


	public void aktualisiereBeschriftungen(){
		for(int i=0; i < panelElemente.length; i++){
			panelElemente[i].setText(GlobalSettings.getCurrentElementBeschriftungsstil()[i]);
		}
	}


	public void setzeKopiertesStrElement(Document doc){
		kopiertesStrElement = doc;
		kopierFeld.setText(doc == null ? "Kopie (leer)" : "Kopie (zum Einfügen ziehen)");
	}

	public Document gibKopiertesStrElement(){
		return kopiertesStrElement;
	}



	private void muelleimerAuf(boolean oeffnen){
		muelleimer.setAktiv(oeffnen);
	}


	private void kopierFeldAuf(boolean oeffnen){
		kopierFeld.setAktiv(oeffnen);
	}



	public void kopiereGanzesStruktogramm(){
		setzeKopiertesStrElement(controlling.gibAktuellesStruktogramm().xmlErstellen());
	}




	//Methoden Drag ausgelöst
	//http://www.java2s.com/Code/Java/Swing-JFC/MakingaComponentDraggable.htm
	public void dragGestureRecognized(DragGestureEvent evt){//User hat angefangen ein Objekt zu ziehen

		Point mausPos = bildschirmKoordZuLokalenKoord(evt.getDragOrigin());//Kordinaten der Maus auf dem AuswahlPanel

		Object element = getComponentAt(mausPos);//Komponente ermitteln, über der die Maus ist

		if (element instanceof AuswahlPanelElement){//ist die Komponente ein AuswahlPanelElement (der User will ein neues StruktogrammElement einfügen)...

			int typ = ((AuswahlPanelElement)element).gibTyp();//Typnummer für das entsprechende StruktogrammElement ermitteln


			Transferable t = new StringSelection("n"+typ);//Drag-Daten setzen, n steht für neues Element, und typ ist der Typ des neuen Elementes

			dragSource.startDrag(evt, DragSource.DefaultCopyDrop, t, this);//Drag wird ausgelöst

		}else if((element == kopierFeld) && (kopiertesStrElement != null)){//ist die Komponente das kopierFeld und ist ein Document in kopiertesStrElement abgelegt...

			Transferable t = new StringSelection("k");//Drag-Daten setzen, k für Element aus dem kopierFeld

			dragSource.startDrag(evt, DragSource.DefaultCopyDrop, t, this);//Drag wird ausgelöst
		}

	}

	public void dragEnter(DragSourceDragEvent evt){

	}

	public void dragOver(DragSourceDragEvent evt){

	}

	public void dragExit(DragSourceEvent evt){

	}

	public void dropActionChanged(DragSourceDragEvent evt){

	}

	public void dragDropEnd(DragSourceDropEvent evt){
		hervorheben(null);
	}



	//Drop empfangen
	//http://www.java2s.com/Code/Java/Swing-JFC/PanelDropTarget.htm
	public void drop(DropTargetDropEvent event){


		try{
			event.acceptDrop(event.getSourceActions());


			Transferable tr = event.getTransferable();
			String dragTyp = (String)tr.getTransferData(tr.getTransferDataFlavors()[0]);//Drag & Drop Transferdaten ermitteln (das ist in diesem Programm ein Buchstabe und eventuell eine Zahl dahinter (siehe: Struktogramm.drop(...))

			Component dropUeberComponent = getComponentAt(bildschirmKoordZuLokalenKoord(event.getLocation()));
			Struktogramm str = controlling.gibAktuellesStruktogramm();

			if (dragTyp.charAt(0) == 'z'){//z -> ein Drag wurde ausgelöst, indem ein StruktogrammElement aus dem Struktogramm gezogen wurde

				if(dropUeberComponent == muelleimer){
					//ein Element wurde aus dem aktuellen Struktogramm auf den Mülleimer gezogen -> aus dem Zwischenlager des Struktogramms entfernen

					str.elementAusZwischenlagerGanzEntfernen();
					str.zeichenbereichAktualisieren();
					str.zeichne();
					str.rueckgaengigPunktSetzen();

				}else if(dropUeberComponent == kopierFeld){

					//ein Element aus dem aktuellen Struktogramm wurde auf die Kopier-Box gezogen -> xml-Abbild generieren und behalten
					setzeKopiertesStrElement(str.xmlErstellen(str.gibZwischenlagerElement()));
				}
			}

			muelleimerAuf(false);
			kopierFeldAuf(false);
			event.dropComplete(true);
		}catch (Exception e){
			e.printStackTrace();
			event.rejectDrop();
		}
	}

	public void dragExit(DropTargetEvent evt){
		muelleimerAuf(false);
		kopierFeldAuf(false);
	}

	public void dropActionChanged(DropTargetDragEvent evt){

	}

	public void dragEnter(DropTargetDragEvent evt){

	}

	public void dragOver(DropTargetDragEvent evt){
		Component tmp = getComponentAt(bildschirmKoordZuLokalenKoord(evt.getLocation())); //Komponente ermitteln, die unter der Maus ist

		muelleimerAuf(tmp == muelleimer); //wenn die Komponente unter der Maus das Mülleimer-Label ist, dann geöffneten Mülleimer zeigen, sonst den Geschlossenen
		kopierFeldAuf(tmp == kopierFeld); //wie beim Mülleimer
	}



	//die Drag & Drop Methoden liefern bereits Koordinaten relativ zum AuswahlPanel
	//(früher wurde hier die Position des Panels abgezogen, was beim Scrollen der Leiste falsche Werte ergab)
	public Point bildschirmKoordZuLokalenKoord(Point bildschirmKoord){
		return new Point(bildschirmKoord);
	}



	//Ablagefläche (Papierkorb, Kopie) mit gestricheltem Rahmen, wird beim Darüberziehen farbig hervorgehoben
	private static class AblageFeld extends JLabel {
		private static final long serialVersionUID = 1L;
		private boolean aktiv = false;

		AblageFeld(Art art, String text){
			super(text, new SymbolIcon(art, 20), LEADING);
			setIconTextGap(10);
			setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
		}

		void setAktiv(boolean aktiv){
			if (this.aktiv != aktiv){
				this.aktiv = aktiv;
				repaint();
			}
		}

		@Override
		protected void paintComponent(Graphics g){
			Graphics2D g2 = (Graphics2D)g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			Color rand = UIManager.getColor("Component.borderColor");
			if (aktiv){
				g2.setColor(new Color(Symbole.akzent.getRed(), Symbole.akzent.getGreen(), Symbole.akzent.getBlue(), 40));
				g2.fillRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 12, 12);
				rand = Symbole.akzent;
			}
			g2.setColor(rand != null ? rand : Color.gray);
			g2.setStroke(new BasicStroke(1.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10f, new float[] {5f, 4f}, 0f));
			g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 12, 12);
			g2.dispose();
			super.paintComponent(g);
		}
	}

}
