package net.tessel.laf;

import java.util.Locale;
import javax.swing.UIManager;

/**
 * The texts the Tessel controls show themselves ("File name:", "Minimize", ...), in English, German, French,
 * Spanish and Romanian. The language follows the default locale.
 *
 * <p>An application can replace any text, or add a language, through the {@code UIManager}:
 * {@code UIManager.put("Tessel.fileName", "Name of the file:")}.
 */
public final class TesselStrings {

    private static final String[] LANGUAGES = {"en", "de", "fr", "es", "ro"};

    /** {key, en, de, fr, es, ro} */
    private static final String[][] TABLE = {
            {"fileName", "File name:", "Dateiname:", "Nom du fichier :", "Nombre de archivo:", "Nume fișier:"},
            {"folderName", "Folder name:", "Ordnername:", "Nom du dossier :", "Nombre de carpeta:", "Nume folder:"},
            {"filesOfType", "Files of type:", "Dateityp:", "Type de fichier :", "Tipo de archivo:", "Tip fișier:"},
            {"back", "Back", "Zurück", "Précédent", "Atrás", "Înapoi"},
            {"forward", "Forward", "Vorwärts", "Suivant", "Adelante", "Înainte"},
            {"upOneLevel", "Up one level", "Eine Ebene nach oben", "Dossier parent", "Subir un nivel", "Un nivel mai sus"},
            {"newFolder", "New folder", "Neuer Ordner", "Nouveau dossier", "Nueva carpeta", "Folder nou"},
            {"viewList", "List", "Liste", "Liste", "Lista", "Listă"},
            {"viewDetails", "Details", "Details", "Détails", "Detalles", "Detalii"},
            {"viewIcons", "Icons", "Symbole", "Icônes", "Iconos", "Pictograme"},
            {"columnName", "Name", "Name", "Nom", "Nombre", "Nume"},
            {"columnDate", "Date modified", "Änderungsdatum", "Modifié le", "Fecha de modificación", "Data modificării"},
            {"columnType", "Type", "Typ", "Type", "Tipo", "Tip"},
            {"columnSize", "Size", "Größe", "Taille", "Tamaño", "Dimensiune"},
            {"typeFolder", "File folder", "Dateiordner", "Dossier de fichiers", "Carpeta de archivos", "Folder de fișiere"},
            {"typeFile", "File", "Datei", "Fichier", "Archivo", "Fișier"},
            {"typeFileOf", "{0} file", "{0}-Datei", "Fichier {0}", "Archivo {0}", "Fișier {0}"},
            {"placeHome", "Home", "Persönlicher Ordner", "Dossier personnel", "Carpeta personal", "Acasă"},
            {"placeDesktop", "Desktop", "Desktop", "Bureau", "Escritorio", "Desktop"},
            {"placeDocuments", "Documents", "Dokumente", "Documents", "Documentos", "Documente"},
            {"placeDownloads", "Downloads", "Downloads", "Téléchargements", "Descargas", "Descărcări"},
            {"placePictures", "Pictures", "Bilder", "Images", "Imágenes", "Imagini"},
            {"close", "Close", "Schließen", "Fermer", "Cerrar", "Închide"},
            {"more", "More", "Mehr", "Plus", "Más", "Mai multe"},
            {"minimize", "Minimize", "Minimieren", "Réduire", "Minimizar", "Minimizează"},
            {"maximize", "Maximize", "Maximieren", "Agrandir", "Maximizar", "Maximizează"},
            {"restore", "Restore", "Wiederherstellen", "Restaurer", "Restaurar", "Restaurează"},
            {"undo", "Undo", "Rückgängig", "Annuler", "Deshacer", "Anulează"},
            {"redo", "Redo", "Wiederholen", "Rétablir", "Rehacer", "Refă"},
            {"cut", "Cut", "Ausschneiden", "Couper", "Cortar", "Decupează"},
            {"copy", "Copy", "Kopieren", "Copier", "Copiar", "Copiază"},
            {"paste", "Paste", "Einfügen", "Coller", "Pegar", "Lipește"},
            {"delete", "Delete", "Löschen", "Supprimer", "Eliminar", "Șterge"},
            {"selectAll", "Select all", "Alles auswählen", "Tout sélectionner", "Seleccionar todo", "Selectează tot"},
            {"openLink", "Open link", "Link öffnen", "Ouvrir le lien", "Abrir vínculo", "Deschide linkul"},
            {"copyLink", "Copy link address", "Linkadresse kopieren", "Copier l'adresse du lien", "Copiar dirección del vínculo", "Copiază adresa linkului"},
            {"bold", "Bold", "Fett", "Gras", "Negrita", "Aldin"},
            {"italic", "Italic", "Kursiv", "Italique", "Cursiva", "Cursiv"},
            {"underline", "Underline", "Unterstrichen", "Souligné", "Subrayado", "Subliniat"},
            {"strikethrough", "Strikethrough", "Durchgestrichen", "Barré", "Tachado", "Tăiat"},
            {"fontFamily", "Font", "Schriftart", "Police", "Fuente", "Font"},
            {"fontSize", "Font size", "Schriftgrad", "Taille de police", "Tamaño de fuente", "Dimensiune font"},
            {"textColor", "Text color", "Textfarbe", "Couleur du texte", "Color del texto", "Culoare text"},
            {"highlightColor", "Highlight color", "Texthervorhebung", "Couleur de surlignage", "Color de resaltado", "Culoare evidențiere"},
            {"moreColors", "More colors...", "Weitere Farben...", "Plus de couleurs...", "Más colores...", "Mai multe culori..."},
            {"noColor", "No color", "Keine Farbe", "Aucune couleur", "Sin color", "Fără culoare"},
            {"alignLeft", "Align left", "Linksbündig", "Aligner à gauche", "Alinear a la izquierda", "Aliniază la stânga"},
            {"alignCenter", "Center", "Zentriert", "Centrer", "Centrar", "Centrează"},
            {"alignRight", "Align right", "Rechtsbündig", "Aligner à droite", "Alinear a la derecha", "Aliniază la dreapta"},
            {"alignJustify", "Justify", "Blocksatz", "Justifier", "Justificar", "Aliniază stânga-dreapta"},
            {"clearFormatting", "Clear formatting", "Formatierung löschen", "Effacer la mise en forme", "Borrar formato", "Șterge formatarea"},
    };

    private TesselStrings() {
    }

    /** The text for {@code key} in the default locale. */
    public static String get(String key) {
        return get(key, Locale.getDefault());
    }

    /** The text for {@code key}; the {@code UIManager} entry {@code "Tessel." + key} wins when present. */
    public static String get(String key, Locale locale) {
        Object custom = UIManager.get("Tessel." + key, locale);
        if (custom instanceof String) return (String) custom;
        int column = 1;
        String language = locale == null ? "en" : locale.getLanguage();
        for (int i = 0; i < LANGUAGES.length; i++) {
            if (LANGUAGES[i].equals(language)) column = i + 1;
        }
        for (String[] row : TABLE) {
            if (row[0].equals(key)) return row[column];
        }
        return key;
    }

    /** {@link #get(String)} with {@code {0}} replaced by {@code argument}. */
    public static String format(String key, String argument) {
        return get(key).replace("{0}", argument);
    }
}
