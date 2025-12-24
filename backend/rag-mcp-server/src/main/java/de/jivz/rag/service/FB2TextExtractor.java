package de.jivz.rag.service;

import lombok.extern.slf4j.Slf4j;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

/**
 * SAX Handler für das Extrahieren von Text aus FB2 (FictionBook 2.0) Dateien.
 *
 * FB2 ist ein XML-basiertes Format für eBooks. Diese Klasse extrahiert
 * Text aus den wichtigsten Elementen und ignoriert Metadaten und Struktur-Tags.
 */
@Slf4j
public class FB2TextExtractor extends DefaultHandler {

    private final StringBuilder textBuilder = new StringBuilder();
    private final Stack<String> elementStack = new Stack<>();

    // Elemente, aus denen wir Text extrahieren möchten
    private static final Set<String> TEXT_ELEMENTS = new HashSet<>();

    // Elemente, die wir ignorieren möchten
    private static final Set<String> IGNORE_ELEMENTS = new HashSet<>();

    private boolean collectText = false;

    static {
        // Text-enthaltende Elemente in FB2
        TEXT_ELEMENTS.add("p");          // Paragraphen
        TEXT_ELEMENTS.add("text-author"); // Text-Autor
        TEXT_ELEMENTS.add("title");      // Titel
        TEXT_ELEMENTS.add("subtitle");   // Untertitel
        TEXT_ELEMENTS.add("annotation");  // Annotation
        TEXT_ELEMENTS.add("section");    // Abschnitte (zum Sammeln von Text)
        TEXT_ELEMENTS.add("body");       // Haupt-Body
        TEXT_ELEMENTS.add("stanza");     // Gedichtstrophe
        TEXT_ELEMENTS.add("v");          // Gedicht-Vers
        TEXT_ELEMENTS.add("citation");   // Zitate
        TEXT_ELEMENTS.add("td");         // Tabellenzellen
        TEXT_ELEMENTS.add("th");         // Tabellen-Header

        // Ignorierte Elemente
        IGNORE_ELEMENTS.add("description"); // Metadaten
        IGNORE_ELEMENTS.add("stylesheet");  // Stilinformationen
        IGNORE_ELEMENTS.add("binary");      // Binärdaten (Bilder etc.)
        IGNORE_ELEMENTS.add("empty-line");  // Leere Linien
        IGNORE_ELEMENTS.add("image");       // Bilder
        IGNORE_ELEMENTS.add("a");           // Links (nur als Struktur)
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        String elementName = localName.isEmpty() ? qName : localName;
        elementStack.push(elementName);

        // Prüfen, ob wir in einem ignorieren Element sind
        if (IGNORE_ELEMENTS.contains(elementName)) {
            collectText = false;
        }

        // Sammeln von Text aus bekannten Text-Elementen
        if (TEXT_ELEMENTS.contains(elementName)) {
            collectText = true;
        }

        // Für <a>-Elemente: Href hinzufügen, falls vorhanden
        if ("a".equals(elementName) && collectText) {
            String href = attributes.getValue("href");
            if (href != null && !href.isEmpty()) {
                textBuilder.append(" [").append(href).append("] ");
            }
        }

        // Für <br>-Elemente: Zeilenumbruch hinzufügen
        if ("br".equals(elementName) || "empty-line".equals(elementName)) {
            if (collectText && textBuilder.length() > 0 && !textBuilder.toString().endsWith("\n")) {
                textBuilder.append("\n");
            }
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (collectText && length > 0) {
            String text = new String(ch, start, length).trim();
            if (!text.isEmpty()) {
                if (textBuilder.length() > 0 && !textBuilder.toString().endsWith(" ")
                    && !textBuilder.toString().endsWith("\n")) {
                    textBuilder.append(" ");
                }
                textBuilder.append(text);
            }
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        String elementName = localName.isEmpty() ? qName : localName;

        if (!elementStack.isEmpty() && elementName.equals(elementStack.peek())) {
            elementStack.pop();
        }

        // Zeilenumbruch nach bestimmten Block-Elementen
        if (("p".equals(elementName) || "section".equals(elementName) ||
             "stanza".equals(elementName) || "title".equals(elementName) ||
             "subtitle".equals(elementName)) && collectText) {
            if (textBuilder.length() > 0 && !textBuilder.toString().endsWith("\n")) {
                textBuilder.append("\n");
            }
        }

        // Wenn der Stack leer ist, haben wir alle Elemente verarbeitet
        if (elementStack.isEmpty()) {
            collectText = false;
        }
    }

    /**
     * Gibt den extrahierten Text zurück.
     */
    public String getText() {
        String result = textBuilder.toString().trim();

        // Mehrfache Zeilenumbrüche vereinfachen
        result = result.replaceAll("\n\n+", "\n\n");

        log.debug("Extracted FB2 text: {} chars, {} lines",
                result.length(),
                result.split("\n").length);

        return result;
    }
}

