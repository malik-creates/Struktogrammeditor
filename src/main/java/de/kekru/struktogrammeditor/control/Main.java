package de.kekru.struktogrammeditor.control;


public class Main {

	public static void main(String[] args) {

		System.setProperty("com.apple.mrj.application.apple.menu.about.name", GlobalSettings.guiTitel);
		//Standarddialoge (Ja/Nein, Dateiauswahl, Farbauswahl) auf Deutsch, passend zum Rest des Programms
		java.util.Locale.setDefault(java.util.Locale.GERMANY);
		javax.swing.JComponent.setDefaultLocale(java.util.Locale.GERMANY);
		GlobalSettings.init();
		new Controlling(args);
	}
}
