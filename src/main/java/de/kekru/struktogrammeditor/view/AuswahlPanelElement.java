package de.kekru.struktogrammeditor.view;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.UIManager;

import de.kekru.struktogrammeditor.control.GlobalSettings;
import de.kekru.struktogrammeditor.view.Symbole.ElementIcon;

//Eintrag in der linken Leiste, aus dem ein neues StruktogrammElement in das Struktogramm gezogen wird
public class AuswahlPanelElement extends JLabel{

   private static final long serialVersionUID = 5455270690460661892L;
   private int typ;
   private boolean hervorgehoben = false;


   public AuswahlPanelElement(int typ){
      super();

      this.typ = typ;
      setIcon(new ElementIcon(typ));
      setIconTextGap(10);
      setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
      setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      setText(GlobalSettings.getCurrentElementBeschriftungsstil()[typ]);
      //hier keine MouseListener/Tooltips registrieren, sonst erhält das AuswahlPanel die Maus-Events für Drag & Drop nicht mehr
   }

   public int gibTyp(){
      return typ;
   }

   public void setHervorgehoben(boolean hervorgehoben){
      if (this.hervorgehoben != hervorgehoben){
         this.hervorgehoben = hervorgehoben;
         repaint();
      }
   }

   @Override
   protected void paintComponent(Graphics g){
      if (hervorgehoben){
         Graphics2D g2 = (Graphics2D)g.create();
         g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
         Color c = UIManager.getColor("Button.hoverBackground");
         if (c == null){
            c = UIManager.getColor("List.selectionBackground");
         }
         g2.setColor(c != null ? c : new Color(0xE8EEF9));
         g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
         g2.dispose();
      }
      super.paintComponent(g);
   }
}
