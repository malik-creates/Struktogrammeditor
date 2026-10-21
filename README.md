# Struktogrammeditor

Editor für Struktogramme (Nassi-Shneiderman-Diagramme): Struktogramme per Drag & Drop zeichnen, als Bild/PDF exportieren und in Quellcode umwandeln – und umgekehrt aus Quellcode ein Struktogramm erzeugen.

Erweiterte Fassung von [malik-creates](https://github.com/malik-creates), basierend auf dem [Struktogrammeditor von Kevin Krummenauer](https://github.com/kekru/struktogrammeditor).

## Funktionen

- **Struktogramm aus Code erzeugen** (Datei → Struktogramm aus Code erzeugen, Strg+I)
  - Java, C, C++, C#, JavaScript, Python und Pascal/Delphi, Sprache wird automatisch erkannt
  - jede Methode bzw. Funktion wird ein eigenes Struktogramm
  - Live-Vorschau beim Tippen, Fehler werden mit Zeilennummer angezeigt
- **Code aus dem Struktogramm erzeugen** in Java, C#, JavaScript, C/C++, Python und Pascal
  - typische Befehle (Ein-/Ausgabe, Zuweisungen, Zählschleifen, Operatoren) werden an die Sprache angepasst
- **Export** als PDF und SVG (Vektorgrafik) sowie als PNG in doppelter Auflösung
- **Zoom** der ganzen Ansicht (Strg+Mausrad, Strg+Plus/Minus)
- **Modernes Design** in hell und dunkel ([FlatLaf](https://www.formdev.com/flatlaf/)), scharfe Darstellung auch bei Windows-Skalierung
- **Automatische Sicherung**: nach einem Absturz werden ungespeicherte Struktogramme beim nächsten Start angeboten

Beispiele liegen im Ordner [`beispiele`](beispiele).

## Starten

Benötigt Java 8 oder neuer. Die fertige jar per Doppelklick starten oder:

```bash
java -jar struktogrammeditor-1.8-jar-with-dependencies.jar
```

## Selbst bauen

Windows (PowerShell):

```bash
.\mvnw.cmd clean package
```

Linux/macOS:

```bash
chmod +x mvnw
./mvnw clean package
```

Die ausführbare Datei liegt danach unter `target/struktogrammeditor-1.8-jar-with-dependencies.jar`.

## Lizenz

MIT-Lizenz, siehe [LICENSE](LICENSE). Enthält [JDOM](http://www.jdom.org/) und [FlatLaf](https://www.formdev.com/flatlaf/) (Apache License 2.0).
