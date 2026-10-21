package de.kekru.struktogrammeditor.view;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

import de.kekru.struktogrammeditor.control.Controlling;
import de.kekru.struktogrammeditor.control.GlobalSettings;
import de.kekru.struktogrammeditor.control.Konstanten;
import de.kekru.struktogrammeditor.other.XActionCommands;
import de.kekru.struktogrammeditor.view.Symbole.Art;
import de.kekru.struktogrammeditor.view.Symbole.SymbolIcon;

/**
 *
 * Struktogrammeditor
 *
 * @version 1.0 vom 27.03.2011
 * @author Kevin Krummenauer
 */

public class GUI extends JFrame implements Konstanten{

	private static final long serialVersionUID = -3526840402506170333L;
	private AuswahlPanel auswahlPanel; //Panel an der linken Seite, wo die Labels zu finden sind, von denen man neue StruktogrammElemente in das Struktogramm ziehen kann
	private StrTabbedPane tabbedpane; //TabbedPane, in dem die Struktogramme sind
	private Controlling controlling;
	private final JMenuBar menubar;
	private final JRadioButtonMenuItem[] lookAndFeelItems = new JRadioButtonMenuItem[6];
	private JButton buttonDesign;
	private JButton labelZoom;

	public GUI(Controlling controlling) {

		super(GlobalSettings.guiTitel);
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
		setSize(Math.min(1280, d.width * 9 / 10), Math.min(820, d.height * 9 / 10));
		setMinimumSize(new Dimension(800, 500));
		setLocationRelativeTo(null);


		this.controlling = controlling;

		setLayout(new BorderLayout());

		setIconImage(new ImageIcon(getClass().getResource(GlobalSettings.logoName)).getImage());


		tabbedpane = new StrTabbedPane(controlling);

		auswahlPanel = new AuswahlPanel(controlling);
		JScrollPane auswahlScrollpane = new JScrollPane(auswahlPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		auswahlScrollpane.setBorder(BorderFactory.createEmptyBorder());
		auswahlScrollpane.getVerticalScrollBar().setUnitIncrement(16);

		JSplitPane splitpane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, auswahlScrollpane, tabbedpane);
		splitpane.setBorder(BorderFactory.createEmptyBorder());
		splitpane.setDividerLocation(auswahlPanel.getPreferredSize().width + 4);
		splitpane.setContinuousLayout(true);
		add(splitpane, BorderLayout.CENTER);

		add(werkzeugleisteErstellen(), BorderLayout.NORTH);

		JLabel statuszeile = Design.hinweis("Elemente aus der linken Leiste ins Struktogramm ziehen   ·   Klick: Text bearbeiten   ·   Rechtsklick: weitere Aktionen   ·   Strg+Mausrad: Zoom");
		statuszeile.setFont(statuszeile.getFont().deriveFont(statuszeile.getFont().getSize2D() - 1f));
		JPanel unten = new JPanel(new BorderLayout());
		unten.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 6));
		unten.add(statuszeile, BorderLayout.CENTER);
		unten.add(zoomLeisteErstellen(), BorderLayout.EAST);
		add(unten, BorderLayout.SOUTH);
		//Strg+Plus/Minus auch über den Ziffernblock und Strg+= (englische Tastatur)
		tastenkuerzel(KeyEvent.VK_ADD, XActionCommands.zoomGroesser);
		tastenkuerzel(KeyEvent.VK_EQUALS, XActionCommands.zoomGroesser);
		tastenkuerzel(KeyEvent.VK_SUBTRACT, XActionCommands.zoomKleiner);
		tastenkuerzel(KeyEvent.VK_NUMPAD0, XActionCommands.zoomOriginal);


		menubar = new JMenuBar();
		{
			JMenu menu = createMenu("Datei", KeyEvent.VK_D);
			{
				menu.add(createMenuItem("Neu", XActionCommands.neu, KeyEvent.VK_N, KeyEvent.VK_N));
				menu.add(createMenuItem("Öffnen...", XActionCommands.oeffnen, KeyEvent.VK_F, KeyEvent.VK_O));
				menu.add(new JSeparator());
				menu.add(createMenuItem("Speichern", XActionCommands.speichern, KeyEvent.VK_S, KeyEvent.VK_S));
				menu.add(createMenuItem("Speichern unter...", XActionCommands.speicherUnter, KeyEvent.VK_U));
				menu.add(new JSeparator());
				menu.add(createMenuItem("Als Bild speichern", XActionCommands.bildSpeichern, KeyEvent.VK_A));
				JMenu export = createMenu("Exportieren", KeyEvent.VK_X);
				{
					export.add(createMenuItem("PDF (Vektorgrafik, zum Drucken)...", XActionCommands.exportPdf, KeyEvent.VK_P));
					export.add(createMenuItem("SVG (Vektorgrafik, beliebig skalierbar)...", XActionCommands.exportSvg, KeyEvent.VK_S));
					export.add(createMenuItem("PNG in doppelter Auflösung (scharf in Word)...", XActionCommands.exportPng, KeyEvent.VK_N));
				}
				menu.add(export);
				menu.add(createMenuItem("Bild in Zwischenablage kopieren", XActionCommands.bildInZwischenAblage, KeyEvent.VK_Z, KeyEvent.VK_K));
				menu.add(new JSeparator());
				menu.add(createMenuItem("Quellcode erzeugen", XActionCommands.quellcodeErzeugen, KeyEvent.VK_Q));
				menu.add(createMenuItem("Struktogramm aus Code erzeugen...", XActionCommands.codeImportieren, KeyEvent.VK_C, KeyEvent.VK_I));
				menu.add(new JSeparator());
				menu.add(createMenuItem("Struktogramm Schließen", XActionCommands.struktogrammSchliessen, KeyEvent.VK_K));
				menu.add(new JSeparator());
				menu.add(createMenuItem("Programm beenden",	XActionCommands.programmBeenden, KeyEvent.VK_P));
			}
			menubar.add(menu);

			menu = createMenu("Bearbeiten", KeyEvent.VK_B);
			{
				menu.add(createMenuItem("Rückgängig", XActionCommands.rueckgaengig, KeyEvent.VK_R, KeyEvent.VK_Z));
				menu.add(createMenuItem("Widerrufen", XActionCommands.widerrufen, KeyEvent.VK_W, KeyEvent.VK_Y));
				menu.add(new JSeparator());
				menu.add(createMenuItem("Struktogrammname hinzufügen", XActionCommands.struktogrammbeschreibungHinzufuegen, KeyEvent.VK_S));
				menu.add(new JSeparator());
				menu.add(createMenuItem("Ganzes Struktogramm kopieren", XActionCommands.ganzesStruktogrammKopieren, KeyEvent.VK_G));
				menu.add(createMenuItem("Element unter der Maus kopieren", XActionCommands.elementUnterDerMausKopieren, KeyEvent.VK_K, KeyEvent.VK_C));
				menu.add(createMenuItem("Kopiertes Element an Mausposition einfügen", XActionCommands.elementEinfuegen, KeyEvent.VK_E, KeyEvent.VK_V));
			}
			menubar.add(menu);

			menu = createMenu("Ansicht", KeyEvent.VK_A);
			{
				menu.add(createMenuItem("Vergrößern", XActionCommands.zoomGroesser, KeyEvent.VK_V, KeyEvent.VK_PLUS));
				menu.add(createMenuItem("Verkleinern", XActionCommands.zoomKleiner, KeyEvent.VK_K, KeyEvent.VK_MINUS));
				menu.add(createMenuItem("Originalgröße (100 %)", XActionCommands.zoomOriginal, KeyEvent.VK_O, KeyEvent.VK_0));
				menu.add(createMenuItem("An Fenster anpassen", XActionCommands.zoomAnpassen, KeyEvent.VK_A));
				menu.add(new JSeparator());
				menu.add(createMenuItem("Helles / dunkles Design wechseln", XActionCommands.designUmschalten, KeyEvent.VK_D));
			}
			menubar.add(menu);

			menu = createMenu("Einstellungen", KeyEvent.VK_E);
			{
				menu.add(createMenuItem("Jeweils letztes Element bei Bedarf strecken", XActionCommands.letztesElementStrecken, KeyEvent.VK_J, GlobalSettings.gibLetzteElementeStrecken()));
				menu.add(new JSeparator());

				JMenu menu2 = createMenu("Beschriftungsstil", KeyEvent.VK_B);
				{
					ButtonGroup group = new ButtonGroup();

					JRadioButtonMenuItem radioMenuitem = new JRadioButtonMenuItem("Java-ähnlich");
					radioMenuitem.addActionListener(controlling);
					radioMenuitem.setActionCommand(XActionCommands.beschriftungsStilJava.toString());
					radioMenuitem.setSelected(GlobalSettings.getBeschriftungsStilAktuell() == beschriftungsStilJava);
					group.add(radioMenuitem);
					menu2.add(radioMenuitem);

					radioMenuitem = new JRadioButtonMenuItem("Allgemein");
					radioMenuitem.addActionListener(controlling);
					radioMenuitem.setActionCommand(XActionCommands.beschriftungsStilFormal.toString());
					radioMenuitem.setSelected(GlobalSettings.getBeschriftungsStilAktuell() == beschriftungsStilFormal);
					group.add(radioMenuitem);
					menu2.add(radioMenuitem);

					radioMenuitem = new JRadioButtonMenuItem("Element Shortcuts");
					radioMenuitem.addActionListener(controlling);
					radioMenuitem.setActionCommand(XActionCommands.beschriftungsStilKeineBeschriftungen.toString());
					radioMenuitem.setSelected(GlobalSettings.getBeschriftungsStilAktuell() == beschriftungsStilKeineBeschriftungen);
					group.add(radioMenuitem);
					menu2.add(radioMenuitem);
				}
				menu.add(menu2);

				menu2 = createMenu("Design", KeyEvent.VK_D);
				{
					ButtonGroup group = new ButtonGroup();
					lookAndFeelRadioItem(menu2, group, "Modern hell", XActionCommands.lookAndFeelModernHell, lookAndFeelModernHell);
					lookAndFeelRadioItem(menu2, group, "Modern dunkel", XActionCommands.lookAndFeelModernDunkel, lookAndFeelModernDunkel);
					menu2.add(new JSeparator());
					lookAndFeelRadioItem(menu2, group, "Klassisch: Betriebssystem Standard", XActionCommands.lookAndFeelOSStandard, lookAndFeelOSStandard);
					lookAndFeelRadioItem(menu2, group, "Klassisch: Java Swing Standard", XActionCommands.lookAndFeelSwingStandard, lookAndFeelSwingStandard);
					lookAndFeelRadioItem(menu2, group, "Klassisch: Nimbus", XActionCommands.lookAndFeelNimbus, lookAndFeelNimbus);
					lookAndFeelRadioItem(menu2, group, "Klassisch: Motif", XActionCommands.lookAndFeelMotif, lookAndFeelMotif);
				}
				menu.add(menu2);

				menu.add(createMenuItem("Startbeschriftungen ändern...", XActionCommands.startbeschriftungAendern, KeyEvent.VK_S));
				menu.add(createMenuItem("Schriftart ändern...", XActionCommands.schriftartAendern, KeyEvent.VK_F));
				menu.add(new JSeparator());
				menu.add(createMenuItem("Größe von Elementen mit Mausrad ändern", XActionCommands.groesseAendernMitMausrad, KeyEvent.VK_G, GlobalSettings.isBeiMausradGroesseAendern()));
				menu.add(createMenuItem("Zoomeinstellungen...", XActionCommands.zoomeinstellungen, KeyEvent.VK_Z));
				menu.add(createMenuItem("Alle Vergrößerungen rückgängig machen", XActionCommands.vergroesserungenRuckgaengigMachen, KeyEvent.VK_A));
				menu.add(new JSeparator());
				menu.add(createMenuItem("Shortcuts zum Elementeinfügen benutzen", XActionCommands.elementShortcutsVerwenden, KeyEvent.VK_C, GlobalSettings.isElementShortcutsVerwenden()));
				menu.add(new JSeparator());
				menu.add(createMenuItem("Kantenglättung verwenden", XActionCommands.kantenglaettungVerwenden, KeyEvent.VK_K, GlobalSettings.isKantenglaettungVerwenden()));
			}
			menubar.add(menu);

			menu = createMenu("Hilfe", KeyEvent.VK_H);
			{
				menu.add(createMenuItem("Info", XActionCommands.info, KeyEvent.VK_I));
				menu.add(createMenuItem("Quellcode dieser Version", XActionCommands.sourceCodeErweiterung, KeyEvent.VK_V));
				menu.add(createMenuItem("Quellcode des Originals", XActionCommands.sourceCode, KeyEvent.VK_Q));
			}
			menubar.add(menu);
		}
		setJMenuBar(menubar);

		designAktualisiert();

		addWindowListener(controlling);

		setResizable(true);
		setVisible(true);
	}


	private JToolBar werkzeugleisteErstellen(){
		JToolBar leiste = new JToolBar();
		leiste.setFloatable(false);
		leiste.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

		leiste.add(werkzeug(Art.NEU, null, "Neues Struktogramm (Strg+N)", XActionCommands.neu));
		leiste.add(werkzeug(Art.OEFFNEN, null, "Öffnen (Strg+O)", XActionCommands.oeffnen));
		leiste.add(werkzeug(Art.SPEICHERN, null, "Speichern (Strg+S)", XActionCommands.speichern));
		leiste.addSeparator();
		leiste.add(werkzeug(Art.RUECKGAENGIG, null, "Rückgängig (Strg+Z)", XActionCommands.rueckgaengig));
		leiste.add(werkzeug(Art.WIDERRUFEN, null, "Widerrufen (Strg+Y)", XActionCommands.widerrufen));
		leiste.addSeparator();
		leiste.add(werkzeug(Art.CODE_IMPORT, "Code → Struktogramm", "Struktogramm aus Quellcode erzeugen (Strg+I)", XActionCommands.codeImportieren));
		leiste.add(werkzeug(Art.CODE_EXPORT, "Struktogramm → Code", "Quellcode aus dem aktuellen Struktogramm erzeugen", XActionCommands.quellcodeErzeugen));
		leiste.addSeparator();
		leiste.add(werkzeug(Art.BILD, null, "Als Bild speichern", XActionCommands.bildSpeichern));
		leiste.add(werkzeug(Art.KOPIEREN, null, "Bild in die Zwischenablage kopieren (Strg+K)", XActionCommands.bildInZwischenAblage));
		leiste.add(Box.createHorizontalGlue());
		buttonDesign = werkzeug(Art.MOND, null, "Zwischen hellem und dunklem Design wechseln", XActionCommands.designUmschalten);
		leiste.add(buttonDesign);

		return leiste;
	}


	//Zoom-Regler unten rechts: [−] 100 % [+]
	private JPanel zoomLeisteErstellen(){
		JPanel leiste = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 2));
		leiste.add(kleinerButton("−", "Verkleinern (Strg+Minus)", XActionCommands.zoomKleiner));
		labelZoom = new JButton("100 %");
		labelZoom.setToolTipText("Auf 100 % zurücksetzen (Strg+0)");
		labelZoom.setActionCommand(XActionCommands.zoomOriginal.toString());
		labelZoom.addActionListener(controlling);
		labelZoom.setFocusable(false);
		labelZoom.putClientProperty("JButton.buttonType", "borderless");
		labelZoom.setPreferredSize(new Dimension(64, labelZoom.getPreferredSize().height));
		leiste.add(labelZoom);
		leiste.add(kleinerButton("+", "Vergrößern (Strg+Plus)", XActionCommands.zoomGroesser));
		return leiste;
	}

	private JButton kleinerButton(String text, String tooltip, XActionCommands actionCommand){
		JButton b = new JButton(text);
		b.setToolTipText(tooltip);
		b.setActionCommand(actionCommand.toString());
		b.addActionListener(controlling);
		b.setFocusable(false);
		b.putClientProperty("JButton.buttonType", "borderless");
		b.setFont(b.getFont().deriveFont(Font.BOLD));
		return b;
	}

	private void tastenkuerzel(int taste, final XActionCommands actionCommand){
		getRootPane().registerKeyboardAction(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				controlling.actionPerformed(new ActionEvent(GUI.this, ActionEvent.ACTION_PERFORMED, actionCommand.toString()));
			}
		}, KeyStroke.getKeyStroke(taste, GlobalSettings.strgOderApfelMask), JComponent.WHEN_IN_FOCUSED_WINDOW);
	}

	//Zoomanzeige an das aktuelle Struktogramm anpassen (nach Zoom oder Tabwechsel)
	public void zoomAnzeigeAktualisieren(){
		if (labelZoom != null && tabbedpane != null && tabbedpane.gibAktuellesStruktogramm() != null){
			labelZoom.setText(Math.round(tabbedpane.gibAktuellesStruktogramm().getZoom() * 100) + " %");
		}
	}


	private JButton werkzeug(Art art, String text, String tooltip, XActionCommands actionCommand){
		JButton button = new JButton(text, new SymbolIcon(art, 18));
		button.setToolTipText(tooltip);
		button.setActionCommand(actionCommand.toString());
		button.addActionListener(controlling);
		button.setFocusable(false); //sonst gehen die Tastenkürzel zum Einfügen von Elementen verloren
		button.setHorizontalTextPosition(SwingConstants.RIGHT);
		button.setIconTextGap(6);
		return button;
	}


	private void lookAndFeelRadioItem(JMenu menu, ButtonGroup group, String text, XActionCommands actionCommand, int lookAndFeel){
		JRadioButtonMenuItem item = new JRadioButtonMenuItem(text);
		item.addActionListener(controlling);
		item.setActionCommand(actionCommand.toString());
		group.add(item);
		menu.add(item);
		lookAndFeelItems[lookAndFeel] = item;
	}


	//nach einem Designwechsel Auswahl im Menü und Symbol des Design-Buttons anpassen
	public void designAktualisiert(){
		int aktuell = GlobalSettings.getLookAndFeelAktuell();
		if (aktuell >= 0 && aktuell < lookAndFeelItems.length && lookAndFeelItems[aktuell] != null){
			lookAndFeelItems[aktuell].setSelected(true);
		}
		buttonDesign.setIcon(new SymbolIcon(Design.istDunkel() ? Art.SONNE : Art.MOND, 18));
		repaint();
	}


	private JMenu createMenu(String name, int auswahlBuchstabe){
		JMenu neuMenu = new JMenu(name);
		neuMenu.setMnemonic(auswahlBuchstabe);
		return neuMenu;
	}

	private JMenuItem createMenuItem(String name, XActionCommands actionCommand, int auswahlBuchstabe){
		return createMenuItem(name, actionCommand, auswahlBuchstabe, -1, -1);
	}

	private JMenuItem createMenuItem(String name, XActionCommands actionCommand, int auswahlBuchstabe, int shortcutBuchstabe){
		return createMenuItem(name, actionCommand, auswahlBuchstabe, shortcutBuchstabe, GlobalSettings.strgOderApfelMask, false, false);
	}

	private JMenuItem createMenuItem(String name, XActionCommands actionCommand, int auswahlBuchstabe, int shortcutBuchstabe, int shortcutMask){
		return createMenuItem(name, actionCommand, auswahlBuchstabe, shortcutBuchstabe, shortcutMask, false, false);
	}

	private JMenuItem createMenuItem(String name, XActionCommands actionCommand, int auswahlBuchstabe, boolean isChecked){
		return createMenuItem(name, actionCommand, auswahlBuchstabe, -1, -1, true, isChecked);
	}

	private JMenuItem createMenuItem(String name, XActionCommands actionCommand, int auswahlBuchstabe, int shortcutBuchstabe, int shortcutMask, boolean isCheckBox, boolean isChecked){
		JMenuItem menuitem;

		if(isCheckBox){
			menuitem = new JCheckBoxMenuItem(name);
			((JCheckBoxMenuItem)menuitem).setSelected(isChecked);
		}else{
			menuitem = new JMenuItem(name);
		}

		menuitem.setActionCommand(actionCommand.toString());

		if(auswahlBuchstabe > -1){
			menuitem.setMnemonic(auswahlBuchstabe);
		}

		if(shortcutBuchstabe > -1){
			menuitem.setAccelerator(KeyStroke.getKeyStroke(shortcutBuchstabe, shortcutMask));
		}

		menuitem.addActionListener(controlling);

		return menuitem;
	}


	public StrTabbedPane gibTabbedpane(){
		return tabbedpane;
	}


	public AuswahlPanel gibAuswahlPanel(){
		return auswahlPanel;
	}


	public JMenuBar getMenubar() {
		return menubar;
	}

}
