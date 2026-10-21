package de.kekru.struktogrammeditor.view;

import java.awt.Color;
import java.awt.Font;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.UIManager;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;

import de.kekru.struktogrammeditor.control.Konstanten;

//modernes Erscheinungsbild (FlatLaf) und gemeinsame Hilfsmethoden für Dialoge
public class Design implements Konstanten {

	public static boolean istModern(int lookAndFeel){
		return lookAndFeel == lookAndFeelModernHell || lookAndFeel == lookAndFeelModernDunkel;
	}

	public static boolean istDunkel(){
		return UIManager.getLookAndFeel() instanceof FlatLaf && FlatLaf.isLafDark();
	}

	//setzt das moderne helle oder dunkle Design, beim Programmstart vor dem Erzeugen der GUI aufrufen
	public static void setzen(int lookAndFeel){
		Map<String, String> werte = new HashMap<String, String>();
		werte.put("@accentColor", "#2F6FED");
		werte.put("Component.arc", "8");
		werte.put("Button.arc", "8");
		werte.put("TextComponent.arc", "8");
		werte.put("CheckBox.arc", "4");
		werte.put("ScrollBar.thumbArc", "999");
		werte.put("ScrollBar.thumbInsets", "2,2,2,2");
		werte.put("ScrollBar.width", "12");
		werte.put("TabbedPane.tabHeight", "34");
		werte.put("TabbedPane.showTabSeparators", "false");
		werte.put("TitlePane.unifiedBackground", "true");
		werte.put("SplitPaneDivider.style", "plain");
		FlatLaf.setGlobalExtraDefaults(werte);

		if (lookAndFeel == lookAndFeelModernDunkel){
			FlatDarkLaf.setup();
		}else{
			FlatLightLaf.setup();
		}
	}

	//wechselt zur Laufzeit zwischen hellem und dunklem Design
	public static void umschalten(int lookAndFeel){
		setzen(lookAndFeel);
		FlatLaf.updateUI();
	}


	//Rand, Standard-Button (Enter) und Escape zum Schließen für einen Dialog einrichten
	public static void dialogEinrichten(final JDialog dialog, JButton standardButton){
		((JComponent)dialog.getContentPane()).setBorder(BorderFactory.createEmptyBorder(14, 14, 12, 14));
		if (standardButton != null){
			dialog.getRootPane().setDefaultButton(standardButton);
		}
		dialog.getRootPane().registerKeyboardAction(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dialog.dispatchEvent(new WindowEvent(dialog, WindowEvent.WINDOW_CLOSING));
			}
		}, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
	}

	//Buttons rechtsbündig in einer Zeile
	public static JPanel buttonLeiste(JButton... buttons){
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
		panel.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
		for (JButton b : buttons){
			panel.add(b);
		}
		return panel;
	}

	public static JLabel ueberschrift(String text){
		JLabel label = new JLabel(text);
		label.setFont(label.getFont().deriveFont(Font.BOLD));
		label.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
		return label;
	}

	//graue Hinweisschrift, Farbe wird beim Zeichnen ermittelt, damit sie auch nach einem Designwechsel passt
	public static JLabel hinweis(String text){
		return new JLabel(text) {
			private static final long serialVersionUID = 1L;

			@Override
			public Color getForeground(){
				Color c = UIManager.getColor("Label.disabledForeground");
				return c != null ? c : super.getForeground();
			}
		};
	}
}
