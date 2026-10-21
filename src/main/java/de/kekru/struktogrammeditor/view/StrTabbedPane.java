package de.kekru.struktogrammeditor.view;
import java.awt.Color;
import java.util.function.BiConsumer;

import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JViewport;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.formdev.flatlaf.FlatClientProperties;

import de.kekru.struktogrammeditor.control.Controlling;
import de.kekru.struktogrammeditor.control.Struktogramm;

public class StrTabbedPane extends JTabbedPane implements ChangeListener{
   
   private static final long serialVersionUID = 1L;
   private Controlling controlling;
   //private boolean stateChangedFreigegeben;

   public StrTabbedPane(Controlling controlling){
      super();
      this.controlling = controlling;
      
      addChangeListener(this);
      addKeyListener(controlling);

      //Schließen-Kreuz an jedem Tab (nur beim modernen Design sichtbar)
      putClientProperty(FlatClientProperties.TABBED_PANE_TAB_CLOSABLE, true);
      putClientProperty(FlatClientProperties.TABBED_PANE_TAB_CLOSE_TOOLTIPTEXT, "Struktogramm schließen");
      putClientProperty(FlatClientProperties.TABBED_PANE_TAB_CLOSE_CALLBACK, new BiConsumer<JTabbedPane, Integer>() {
         public void accept(JTabbedPane tabbedPane, Integer index) {
            setSelectedIndex(index);
            aktuellesStruktogrammschliessen();
         }
      });
      putClientProperty(FlatClientProperties.TABBED_PANE_SCROLL_BUTTONS_PLACEMENT, FlatClientProperties.TABBED_PANE_PLACEMENT_BOTH);
      setTabLayoutPolicy(SCROLL_TAB_LAYOUT);
   }
   
   
   public GUI gibGUI(){
	   return controlling.getGUI();
   }
   
//   public void changeListenerAktivieren(){
//      addChangeListener(this);
//      stateChangedFreigegeben = true;
//   }
   
   
   public Struktogramm struktogrammHinzufuegen(){
      Struktogramm str = new Struktogramm(this);
      JScrollPane scrollpane = new JScrollPane();
      //graue Arbeitsfläche, auf der das weiße Struktogramm wie ein Blatt liegt; Farbe wird beim Zeichnen ermittelt, damit sie zum Design passt
      JViewport arbeitsflaeche = new JViewport() {
         private static final long serialVersionUID = 1L;

         @Override
         public Color getBackground(){
            return Design.istDunkel() ? new Color(0x1E1F22) : new Color(0xE9ECF1);
         }
      };
      arbeitsflaeche.setView(str);
      scrollpane.setViewport(arbeitsflaeche);
      scrollpane.setBorder(BorderFactory.createEmptyBorder());
      scrollpane.getVerticalScrollBar().setUnitIncrement(16);
      scrollpane.getHorizontalScrollBar().setUnitIncrement(16);
      add("Unbenannt", scrollpane);
      //stateChangedFreigegeben = false; //changeListener kurz deaktivieren...
      setSelectedIndex(getTabCount() -1); //...weil es sonst in graphicsInitialisieren Probleme gibt
      //stateChangedFreigegeben = true;
      return str;
   }
   
   
   public void titelDerAktuellenSeiteSetzen(String titel){
      setTitleAt(getSelectedIndex(),titel);
   }
   
   public void titelDerAktuellenSeiteAlsBearbeitetOderAlsGespespeichertMarkieren(boolean bearbeitet){
      if (getSelectedIndex() > -1){

         String titel = getTitleAt(getSelectedIndex());
      
         if (bearbeitet){//"*" anhängen
            if (titel.charAt(titel.length() -1) != '*'){
               titel += "*";
            }
         }else{//"*" entfernen
            if (titel.charAt(titel.length() -1) == '*'){
               titel = titel.substring(0,titel.length() -1);
            }
         }
      
         setTitleAt(getSelectedIndex(),titel);
      }
   }
   
   
   public void aktuellesStruktogrammschliessen(){
      if (getSelectedIndex() < 0){
         return;
      }

      String titel = getTitleAt(getSelectedIndex());
      if (!titel.endsWith("*")){ //nicht verändert, also ohne Nachfrage schließen
         remove(getSelectedIndex());
      }else{
         switch(JOptionPane.showConfirmDialog(controlling.getGUI(), "Vor dem Schließen \"" + titel.substring(0, titel.length() - 1) + "\" speichern?", "Vorher speichern?", JOptionPane.YES_NO_CANCEL_OPTION)){
            case JOptionPane.YES_OPTION:
               controlling.speichern(false);
               remove(getSelectedIndex());
               break;
            case JOptionPane.NO_OPTION:
               remove(getSelectedIndex());
               break;
         }
      }

      if (getTabCount() == 0){ //es soll immer ein Struktogramm offen sein
         controlling.neuesStruktogramm();
      }
   }
   
   
   public Struktogramm gibAktuellesStruktogramm(){
      if(getTabCount() > 0){
         //getSelectedComponent() liefert das JScrollPane, dessen Viewport das Struktogramm anzeigt
         return (Struktogramm)((JScrollPane)getSelectedComponent()).getViewport().getView();
      }else{
         return null;
      }
   }
   
   
   public void stateChanged(ChangeEvent e){
//      if (stateChangedFreigegeben && (getTabCount() > 1)){//wenn es das erste ist (nach Schließen), wird diese Methode aufgerufen und graphicsInitialisieren klappt noch nicht, darum nicht zulassen
//
//
//         Struktogramm str = gibAktuellesStruktogramm();
//         if (str != null){
//            str.graphicsInitialisieren();
//            str.zeichenbereichAktualisieren();
//            str.zeichne();
//            gui.titelleisteAktualisieren();
//         }
//      }
	   
	   controlling.titelleisteAktualisieren();
   }
   
   
   public boolean einOderMehrereStruktogrammeNichtGespeichert(){
      for(int i=0; i < getTabCount(); i++){
         if(getTitleAt(i).charAt(getTitleAt(i).length() -1) == '*'){
            return true;	 
         }
      }
      
      return false;
   }
}