package de.kekru.struktogrammeditor.control;

import java.awt.Image;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.swing.ImageIcon;

//Anbindung an macOS (Menüpunkt "Über", Dock-Symbol) über die Standard-API java.awt.Desktop/Taskbar (ab Java 9);
//per Reflection, damit das Programm weiterhin mit Java 8 kompiliert und läuft (dort ohne diese Anbindung)
public class MacHandler {

	public MacHandler(final Controlling controlling){

		System.getProperties().put("apple.laf.useScreenMenuBar", "true");
		System.setProperty("com.apple.mrj.application.apple.menu.about.name", GlobalSettings.guiTitel);

		try{
			Class<?> desktopKlasse = Class.forName("java.awt.Desktop");
			Object desktop = desktopKlasse.getMethod("getDesktop").invoke(null);
			Class<?> aboutHandler = Class.forName("java.awt.desktop.AboutHandler");
			Object handler = Proxy.newProxyInstance(MacHandler.class.getClassLoader(), new Class<?>[] {aboutHandler}, new InvocationHandler() {
				public Object invoke(Object proxy, Method method, Object[] args) {
					if (method.getName().equals("handleAbout")){
						controlling.showInfo();
					}else if (method.getName().equals("hashCode")){
						return System.identityHashCode(proxy);
					}else if (method.getName().equals("equals")){
						return proxy == args[0];
					}else if (method.getName().equals("toString")){
						return "MacHandler";
					}
					return null;
				}
			});
			desktopKlasse.getMethod("setAboutHandler", aboutHandler).invoke(desktop, handler);
		}catch(Exception e){
			//Java 8 oder nicht unterstützt: Standard-Menü von macOS bleibt
		}

		try{
			Image logo = new ImageIcon(getClass().getResource(GlobalSettings.logoName)).getImage();
			Class<?> taskbarKlasse = Class.forName("java.awt.Taskbar");
			Object taskbar = taskbarKlasse.getMethod("getTaskbar").invoke(null);
			taskbarKlasse.getMethod("setIconImage", Image.class).invoke(taskbar, logo);
		}catch(Exception e){
			//kein eigenes Dock-Symbol möglich
		}
	}
}
