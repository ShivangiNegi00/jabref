package org.jabref.logic.openoffice.style;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.layout.Layout;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.entry.types.UnknownEntryType;
import org.jabref.model.openoffice.ootext.OOText;
import org.jabref.model.openoffice.style.CitationMarkerEntry;
import org.jabref.model.openoffice.style.CitationMarkerNumericBibEntry;
import org.jabref.model.openoffice.style.CitationMarkerNumericEntry;
import org.jabref.model.openoffice.style.NonUniqueCitationMarker;

import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class JStyleTest {
    private final LayoutFormatterPreferences layoutFormatterPreferences = mock(LayoutFormatterPreferences.class, Answers.RETURNS_DEEP_STUBS);
    private final JournalAbbreviationRepository abbreviationRepository = mock(JournalAbbreviationRepository.class);

    @Test
    void authorYear() throws IOException {
        JStyle style = new JStyle(JStyleLoader.DEFAULT_AUTHORYEAR_STYLE_PATH, layoutFormatterPreferences, abbreviationRepository);
        assertTrue(style.isValid());
        assertTrue(style.isInternalStyle());
        assertFalse(style.isCitationKeyCiteMarkers());
        assertFalse(style.isBoldCitations());
        assertFalse(style.isFormatCitations());
        assertFalse(style.isItalicCitations());
        assertFalse(style.isNumberEntries());
        assertFalse(style.isSortByPosition());
    }

    @Test
    void numerical() throws IOException {
        JStyle style = new JStyle(
                JStyleLoader.DEFAULT_NUMERICAL_STYLE_PATH,
                layoutFormatterPreferences,
                abbreviationRepository);
        assertTrue(style.isValid());
        assertFalse(style.isCitationKeyCiteMarkers());
        assertFalse(style.isBoldCitations());
        assertFalse(style.isFormatCitations());
        assertFalse(style.isItalicCitations());
        assertTrue(style.isNumberEntries());
        assertTrue(style.isSortByPosition());
    }

    // region: helpers

    static String runGetNumCitationMarker2a(JStyle style,
                                            List<Integer> num, int minGroupingCount, boolean inList) {
        return OOBibStyleTestHelper.runGetNumCitationMarker2a(style, num, minGroupingCount, inList);
    }

    static CitationMarkerNumericEntry numEntry(String key, int num, String pageInfoOrNull) {
        return OOBibStyleTestHelper.numEntry(key, num, pageInfoOrNull);
    }

    static CitationMarkerNumericBibEntry numBibEntry(String key, Optional<Integer> num) {
        return OOBibStyleTestHelper.numBibEntry(key, num);
    }

    static String runGetNumCitationMarker2b(JStyle style,
                                            int minGroupingCount,
                                            CitationMarkerNumericEntry... s) {
        List<CitationMarkerNumericEntry> input = Stream.of(s).collect(Collectors.toList());
        OOText res = style.getNumCitationMarker2(input, minGroupingCount);
        return res.toString();
    }

    static CitationMarkerEntry makeCitationMarkerEntry(BibEntry entry,
                                                       BibDatabase database,
                                                       String uniqueLetterQ,
                                                       String pageInfoQ,
                                                       boolean isFirstAppearanceOfSource) {
        return OOBibStyleTestHelper.makeCitationMarkerEntry(entry,
                database,
                uniqueLetterQ,
                pageInfoQ,
                isFirstAppearanceOfSource);
    }

    /*
     * Similar to old API. pageInfo is new, and unlimAuthors is
     * replaced with isFirstAppearanceOfSource
     */
    static String getCitationMarker2(JStyle style,
                                     List<BibEntry> entries,
                                     Map<BibEntry, BibDatabase> entryDBMap,
                                     boolean inParenthesis,
                                     String[] uniquefiers,
                                     Boolean[] isFirstAppearanceOfSource,
                                     String[] pageInfo) {
        return OOBibStyleTestHelper.getCitationMarker2(style,
                entries,
                entryDBMap,
                inParenthesis,
                uniquefiers,
                isFirstAppearanceOfSource,
                pageInfo);
    }

    static String getCitationMarker2b(JStyle style,
                                      List<BibEntry> entries,
                                      Map<BibEntry, BibDatabase> entryDBMap,
                                      boolean inParenthesis,
                                      String[] uniquefiers,
                                      Boolean[] isFirstAppearanceOfSource,
                                      String[] pageInfo) {
        return OOBibStyleTestHelper.getCitationMarker2b(style,
                entries,
                entryDBMap,
                inParenthesis,
                uniquefiers,
                isFirstAppearanceOfSource,
                pageInfo);
    }

    // endregion

    @Test
    void getNumCitationMarker() throws IOException {
        JStyle style = new JStyle(
                JStyleLoader.DEFAULT_NUMERICAL_STYLE_PATH,
                layoutFormatterPreferences,
                abbreviationRepository);
        assertEquals("[1] ", runGetNumCitationMarker2a(style, List.of(1), -1, true));

        assertEquals("[1]", runGetNumCitationMarker2a(style, List.of(1), -1, false));
        assertEquals("[1]", runGetNumCitationMarker2b(style, -1, numEntry("key", 1, null)));

        assertEquals("[1] ", runGetNumCitationMarker2a(style, List.of(1), 0, true));

        CitationMarkerNumericEntry e2 = numEntry("key", 1, "pp. 55-56");
        assertTrue(e2.getPageInfo().isPresent());
        assertEquals("pp. 55-56", e2.getPageInfo().get().toString());

        OOBibStyleTestHelper.testGetNumCitationMarkerExtra(style);
    }

    @Test
    void getNumCitationMarkerUndefined() throws IOException {
        JStyle style = new JStyle(
                JStyleLoader.DEFAULT_NUMERICAL_STYLE_PATH,
                layoutFormatterPreferences,
                abbreviationRepository);

        // unresolved citations look like [??key]
        assertEquals("[" + JStyle.UNDEFINED_CITATION_MARKER + "key" + "]",
                runGetNumCitationMarker2b(style, 1,
                        numEntry("key", 0, null)));

        // pageInfo is shown for unresolved citations
        assertEquals("[" + JStyle.UNDEFINED_CITATION_MARKER + "key" + "; p1]",
                runGetNumCitationMarker2b(style, 1,
                        numEntry("key", 0, "p1")));

        // unresolved citations sorted to the front
        assertEquals("[" + JStyle.UNDEFINED_CITATION_MARKER + "key" + "; 2-4]",
                runGetNumCitationMarker2b(style, 1,
                        numEntry("x4", 4, ""),
                        numEntry("x2", 2, ""),
                        numEntry("x3", 3, ""),
                        numEntry("key", 0, "")));

        assertEquals("[" + JStyle.UNDEFINED_CITATION_MARKER + "key" + "; 1-3]",
                runGetNumCitationMarker2b(style, 1,
                        numEntry("x1", 1, ""),
                        numEntry("x2", 2, ""),
                        numEntry("y3", 3, ""),
                        numEntry("key", 0, "")));

        // multiple unresolved citations are not collapsed
        assertEquals("["
                        + JStyle.UNDEFINED_CITATION_MARKER + "x1" + "; "
                        + JStyle.UNDEFINED_CITATION_MARKER + "x2" + "; "
                        + JStyle.UNDEFINED_CITATION_MARKER + "x3" + "]",
                runGetNumCitationMarker2b(style, 1,
                        numEntry("x1", 0, ""),
                        numEntry("x2", 0, ""),
                        numEntry("x3", 0, "")));

        /*
         * BIBLIOGRAPHY
         */
        CitationMarkerNumericBibEntry x = numBibEntry("key", Optional.empty());
        assertEquals("[" + JStyle.UNDEFINED_CITATION_MARKER + "key" + "] ",
                style.getNumCitationMarkerForBibliography(x).toString());
    }

    @Test
    void getCitProperty() throws IOException {
        JStyle style = new JStyle(
                JStyleLoader.DEFAULT_NUMERICAL_STYLE_PATH,
                layoutFormatterPreferences,
                abbreviationRepository);
        assertEquals(", ", style.getStringCitProperty("AuthorSeparator"));

        // old
        assertEquals(3, style.getIntCitProperty("MaxAuthors"));
        assertTrue(style.getBooleanCitProperty(JStyle.MULTI_CITE_CHRONOLOGICAL));
        // new
        assertEquals(3, style.getMaxAuthors());
        assertTrue(style.getMultiCiteChronological());

        assertEquals("Default", style.getCitationCharacterFormat());
        assertEquals("Default [number] style file.", style.getName());
        Set<String> journals = style.getJournals();
        assertTrue(journals.contains("Journal name 1"));
    }

    @Test
    void getCitationMarker() throws IOException {
        JStyle style = new JStyle(
                JStyleLoader.DEFAULT_NUMERICAL_STYLE_PATH,
                layoutFormatterPreferences,
                abbreviationRepository);
        BibEntry entry = new BibEntry()
                .withField(StandardField.AUTHOR, "Gustav Bostr\\\"{o}m and Jaana W\\\"{a}yrynen and Marine Bod\\'{e}n and Konstantin Beznosov and Philippe Kruchten")
                .withField(StandardField.YEAR, "2006")
                .withField(StandardField.BOOKTITLE, "SESS '06: Proceedings of the 2006 international workshop on Software engineering for secure systems")
                .withField(StandardField.PUBLISHER, "ACM")
                .withField(StandardField.TITLE, "Extending XP practices to support security requirements engineering")
                .withField(StandardField.PAGES, "11--18");
        entry.setCitationKey("Bostrom2006"); // citation key is not optional now
        BibDatabase database = new BibDatabase();
        database.insertEntry(entry);
        Map<BibEntry, BibDatabase> entryDBMap = new HashMap<>();
        entryDBMap.put(entry, database);

        // Check what unlimAuthors values correspond to isFirstAppearanceOfSource false/true
        assertEquals(3, style.getMaxAuthors());
        assertEquals(-1, style.getMaxAuthorsFirst());

        assertEquals("[Boström et al., 2006]",
                getCitationMarker2(style,
                        List.of(entry), entryDBMap,
                        true, null, null, null));

        assertEquals("Boström et al. [2006]",
                getCitationMarker2(style,
                        List.of(entry), entryDBMap,
                        false, null, new Boolean[]{false}, null));

        assertEquals("[Boström, Wäyrynen, Bodén, Beznosov & Kruchten, 2006]",
                getCitationMarker2(style,
                        List.of(entry), entryDBMap,
                        true,
                        null,
                        new Boolean[]{true},
                        null));
    }

    @Test
    void layout() throws IOException {
        JStyle style = new JStyle(
                JStyleLoader.DEFAULT_NUMERICAL_STYLE_PATH,
                layoutFormatterPreferences,
                abbreviationRepository);

        BibEntry entry = new BibEntry()
                .withField(StandardField.AUTHOR, "Gustav Bostr\\\"{o}m and Jaana W\\\"{a}yrynen and Marine Bod\\'{e}n and Konstantin Beznosov and Philippe Kruchten")
                .withField(StandardField.YEAR, "2006")
                .withField(StandardField.BOOKTITLE, "SESS '06: Proceedings of the 2006 international workshop on Software engineering for secure systems")
                .withField(StandardField.PUBLISHER, "ACM")
                .withField(StandardField.TITLE, "Extending XP practices to support security requirements engineering")
                .withField(StandardField.PAGES, "11--18");
        BibDatabase database = new BibDatabase();
        database.insertEntry(entry);

        Layout l = style.getReferenceFormat(new UnknownEntryType("default"));
        l.setPostFormatter(new OOPreFormatter());
        assertEquals(
                "Boström, G.; Wäyrynen, J.; Bodén, M.; Beznosov, K. and Kruchten, P. (<b>2006</b>). <i>Extending XP practices to support security requirements engineering</i>,   : 11-18.",
                l.doLayout(entry, database));

        l = style.getReferenceFormat(StandardEntryType.InCollection);
        l.setPostFormatter(new OOPreFormatter());
        assertEquals(
                "Boström, G.; Wäyrynen, J.; Bodén, M.; Beznosov, K. and Kruchten, P. (<b>2006</b>). <i>Extending XP practices to support security requirements engineering</i>. In:  (Ed.), <i>SESS '06: Proceedings of the 2006 international workshop on Software engineering for secure systems</i>, ACM.",
                l.doLayout(entry, database));
    }

    @Test
    void institutionAuthor() throws IOException {
        JStyle style = new JStyle(
                JStyleLoader.DEFAULT_NUMERICAL_STYLE_PATH,
                layoutFormatterPreferences,
                abbreviationRepository);
        BibDatabase database = new BibDatabase();

        Layout l = style.getReferenceFormat(StandardEntryType.Article);
        l.setPostFormatter(new OOPreFormatter());

        BibEntry entry = new BibEntry();
        entry.setType(StandardEntryType.Article);
        entry.setField(StandardField.AUTHOR, "{JabRef Development Team}");
        entry.setField(StandardField.TITLE, "JabRef Manual");
        entry.setField(StandardField.YEAR, "2016");
        database.insertEntry(entry);
        assertEquals("<b>JabRef Development Team</b> (<b>2016</b>). <i>JabRef Manual</i>,  .",
                l.doLayout(entry, database));
    }

    @Test
    void vonAuthor() throws IOException {
        JStyle style = new JStyle(
                JStyleLoader.DEFAULT_NUMERICAL_STYLE_PATH,
                layoutFormatterPreferences,
                abbreviationRepository);
        BibDatabase database = new BibDatabase();

        Layout l = style.getReferenceFormat(StandardEntryType.Article);
        l.setPostFormatter(new OOPreFormatter());

        BibEntry entry = new BibEntry();
        entry.setType(StandardEntryType.Article);
        entry.setField(StandardField.AUTHOR, "Alpha von Beta");
        entry.setField(StandardField.TITLE, "JabRef Manual");
        entry.setField(StandardField.YEAR, "2016");
        database.insertEntry(entry);
        assertEquals("<b>von Beta, A.</b> (<b>2016</b>). <i>JabRef Manual</i>,  .",
                l.doLayout(entry, database));
    }

    @Test
    void institutionAuthorMarker() throws IOException {
        JStyle style = new JStyle(
                JStyleLoader.DEFAULT_NUMERICAL_STYLE_PATH,
                layoutFormatterPreferences,
                abbreviationRepository);

        Map<BibEntry, BibDatabase> entryDBMap = new HashMap<>();

        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("JabRef2016")
                .withField(StandardField.AUTHOR, "{JabRef Development Team}")
                .withField(StandardField.TITLE, "JabRef Manual")
                .withField(StandardField.YEAR, "2016");
        List<BibEntry> entries = List.of(entry);
        BibDatabase database = new BibDatabase(entries);
        entryDBMap.put(entry, database);
        assertEquals("[JabRef Development Team, 2016]",
                getCitationMarker2(style,
                        entries, entryDBMap, true, null, null, null));
    }

    @Test
    void vonAuthorMarker() throws IOException {
        JStyle style = new JStyle(
                JStyleLoader.DEFAULT_NUMERICAL_STYLE_PATH,
                layoutFormatterPreferences,
                abbreviationRepository);

        Map<BibEntry, BibDatabase> entryDBMap = new HashMap<>();
        List<BibEntry> entries = new ArrayList<>();
        BibDatabase database = new BibDatabase();

        BibEntry entry = new BibEntry();
        entry.setType(StandardEntryType.Article);
        entry.setField(StandardField.AUTHOR, "Alpha von Beta");
        entry.setField(StandardField.TITLE, "JabRef Manual");
        entry.setField(StandardField.YEAR, "2016");
        entry.setCitationKey("a1");
        database.insertEntry(entry);
        entries.add(entry);
        entryDBMap.put(entry, database);
        assertEquals("[von Beta, 2016]", getCitationMarker2(style, entries, entryDBMap, true, null, null, null));
    }

    @Test
    void nullAuthorMarker() throws IOException {
        JStyle style = new JStyle(
                JStyleLoader.DEFAULT_NUMERICAL_STYLE_PATH,
                layoutFormatterPreferences,
                abbreviationRepository);

        Map<BibEntry, BibDatabase> entryDBMap = new HashMap<>();
        List<BibEntry> entries = new ArrayList<>();
        BibDatabase database = new BibDatabase();

        BibEntry entry = new BibEntry();
        entry.setType(StandardEntryType.Article);
        entry.setField(StandardField.YEAR, "2016");
        entry.setCitationKey("a1");
        database.insertEntry(entry);
        entries.add(entry);
        entryDBMap.put(entry, database);
        assertEquals("[, 2016]", getCitationMarker2(style, entries, entryDBMap, true, null, null, null));
    }

    @Test
    void nullYearMarker() throws IOException {
        JStyle style = new JStyle(
                JStyleLoader.DEFAULT_NUMERICAL_STYLE_PATH,
                layoutFormatterPreferences,
                abbreviationRepository);

        Map<BibEntry, BibDatabase> entryDBMap = new HashMap<>();
        List<BibEntry> entries = new ArrayList<>();
        BibDatabase database = new BibDatabase();

        BibEntry entry = new BibEntry();
        entry.setType(StandardEntryType.Article);
        entry.setField(StandardField.AUTHOR, "Alpha von Beta");
        entry.setCitationKey("a1");
        database.insertEntry(entry);
        entries.add(entry);
        entryDBMap.put(entry, database);
        assertEquals("[von Beta, ]", getCitationMarker2(style, entries, entryDBMap, true, null, null, null));
    }

    @Test
    void emptyEntryMarker() throws IOException {
        JStyle style = new JStyle(
                JStyleLoader.DEFAULT_NUMERICAL_STYLE_PATH,
                layoutFormatterPreferences,
                abbreviationRepository);

        Map<BibEntry, BibDatabase> entryDBMap = new HashMap<>();
        List<BibEntry> entries = new ArrayList<>();
        BibDatabase database = new BibDatabase();

        BibEntry entry = new BibEntry();
        entry.setType(StandardEntryType.Article);
        entry.setCitationKey("a1");
        database.insertEntry(entry);
        entries.add(entry);
        entryDBMap.put(entry, database);
        assertEquals("[, ]", getCitationMarker2(style, entries, entryDBMap, true, null, null, null));
    }

    @Test
    void getCitationMarkerInParenthesisUniquefiers() throws IOException {
        JStyle style = new JStyle(
                JStyleLoader.DEFAULT_NUMERICAL_STYLE_PATH,
                layoutFormatterPreferences,
                abbreviationRepository);

        Map<BibEntry, BibDatabase> entryDBMap = new HashMap<>();
        List<BibEntry> entries = new ArrayList<>();
        BibDatabase database = new BibDatabase();

        BibEntry entry1 = new BibEntry();
        entry1.setField(StandardField.AUTHOR, "Alpha Beta");
        entry1.setField(StandardField.TITLE, "Paper 1");
        entry1.setField(StandardField.YEAR, "2000");
        entry1.setCitationKey("a1");
        entries.add(entry1);
        database.insertEntry(entry1);
        BibEntry entry3 = new BibEntry();
        entry3.setField(StandardField.AUTHOR, "Alpha Beta");
        entry3.setField(StandardField.TITLE, "Paper 2");
        entry3.setField(StandardField.YEAR, "2000");
        entry3.setCitationKey("a3");
        entries.add(entry3);
        database.insertEntry(entry3);
        BibEntry entry2 = new BibEntry();
        entry2.setField(StandardField.AUTHOR, "Gamma Epsilon");
        entry2.setField(StandardField.YEAR, "2001");
        entry2.setCitationKey("a2");
        entries.add(entry2);
        database.insertEntry(entry2);
        for (BibEntry entry : database.getEntries()) {
            entryDBMap.put(entry, database);
        }

        assertEquals("[Beta, 2000; Beta, 2000; Epsilon, 2001]",
                getCitationMarker2b(style, entries, entryDBMap, true, null, null, null));
        assertEquals("[Beta, 2000a,b; Epsilon, 2001]",
                getCitationMarker2(style, entries, entryDBMap, true,
                        new String[]{"a", "b", ""},
                        new Boolean[]{false, false, false},
                        null));
    }

    @Test
    void getCitationMarkerInTextUniquefiers() throws IOException {
        JStyle style = new JStyle(
                JStyleLoader.DEFAULT_NUMERICAL_STYLE_PATH,
                layoutFormatterPreferences,
                abbreviationRepository);

        Map<BibEntry, BibDatabase> entryDBMap = new HashMap<>();
        List<BibEntry> entries = new ArrayList<>();
        BibDatabase database = new BibDatabase();

        BibEntry entry1 = new BibEntry();
        entry1.setField(StandardField.AUTHOR, "Alpha Beta");
        entry1.setField(StandardField.TITLE, "Paper 1");
        entry1.setField(StandardField.YEAR, "2000");
        entry1.setCitationKey("a1");
        entries.add(entry1);
        database.insertEntry(entry1);
        BibEntry entry3 = new BibEntry();
        entry3.setField(StandardField.AUTHOR, "Alpha Beta");
        entry3.setField(StandardField.TITLE, "Paper 2");
        entry3.setField(StandardField.YEAR, "2000");
        entry3.setCitationKey("a3");
        entries.add(entry3);
        database.insertEntry(entry3);
        BibEntry entry2 = new BibEntry();
        entry2.setField(StandardField.AUTHOR, "Gamma Epsilon");
        entry2.setField(StandardField.YEAR, "2001");
        entries.add(entry2);
        entry2.setCitationKey("a2");
        database.insertEntry(entry2);
        for (BibEntry entry : database.getEntries()) {
            entryDBMap.put(entry, database);
        }

        assertEquals("Beta [2000]; Beta [2000]; Epsilon [2001]",
                getCitationMarker2b(style, entries, entryDBMap, false, null, null, null));
        assertEquals("Beta [2000a,b]; Epsilon [2001]",
                getCitationMarker2(style, entries, entryDBMap, false,
                        new String[]{"a", "b", ""},
                        new Boolean[]{false, false, false},
                        null));
    }

    @Test
    void getCitationMarkerInParenthesisUniquefiersThreeSameAuthor() throws IOException {
        JStyle style = new JStyle(
                JStyleLoader.DEFAULT_NUMERICAL_STYLE_PATH,
                layoutFormatterPreferences,
                abbreviationRepository);

        Map<BibEntry, BibDatabase> entryDBMap = new HashMap<>();
        List<BibEntry> entries = new ArrayList<>();
        BibDatabase database = new BibDatabase();

        BibEntry entry1 = new BibEntry();
        entry1.setField(StandardField.AUTHOR, "Alpha Beta");
        entry1.setField(StandardField.TITLE, "Paper 1");
        entry1.setField(StandardField.YEAR, "2000");
        entry1.setCitationKey("a1");
        entries.add(entry1);
        database.insertEntry(entry1);
        BibEntry entry2 = new BibEntry();
        entry2.setField(StandardField.AUTHOR, "Alpha Beta");
        entry2.setField(StandardField.TITLE, "Paper 2");
        entry2.setField(StandardField.YEAR, "2000");
        entry2.setCitationKey("a2");
        entries.add(entry2);
        database.insertEntry(entry2);
        BibEntry entry3 = new BibEntry();
        entry3.setField(StandardField.AUTHOR, "Alpha Beta");
        entry3.setField(StandardField.TITLE, "Paper 3");
        entry3.setField(StandardField.YEAR, "2000");
        entry3.setCitationKey("a3");
        entries.add(entry3);
        database.insertEntry(entry3);
        for (BibEntry entry : database.getEntries()) {
            entryDBMap.put(entry, database);
        }

        assertEquals("[Beta, 2000a,b,c]",
                getCitationMarker2(style, entries, entryDBMap, true,
                        new String[]{"a", "b", "c"},
                        new Boolean[]{false, false, false},
                        null));
    }

    @Test
    void getCitationMarkerInTextUniquefiersThreeSameAuthor() throws IOException {
        JStyle style = new JStyle(
                JStyleLoader.DEFAULT_NUMERICAL_STYLE_PATH,
                layoutFormatterPreferences,
                abbreviationRepository);

        Map<BibEntry, BibDatabase> entryDBMap = new HashMap<>();
        List<BibEntry> entries = new ArrayList<>();
        BibDatabase database = new BibDatabase();

        BibEntry entry1 = new BibEntry();
        entry1.setField(StandardField.AUTHOR, "Alpha Beta");
        entry1.setField(StandardField.TITLE, "Paper 1");
        entry1.setField(StandardField.YEAR, "2000");
        entry1.setCitationKey("a1");
        entries.add(entry1);
        database.insertEntry(entry1);
        BibEntry entry2 = new BibEntry();
        entry2.setField(StandardField.AUTHOR, "Alpha Beta");
        entry2.setField(StandardField.TITLE, "Paper 2");
        entry2.setField(StandardField.YEAR, "2000");
        entry2.setCitationKey("a2");
        entries.add(entry2);
        database.insertEntry(entry2);
        BibEntry entry3 = new BibEntry();
        entry3.setField(StandardField.AUTHOR, "Alpha Beta");
        entry3.setField(StandardField.TITLE, "Paper 3");
        entry3.setField(StandardField.YEAR, "2000");
        entry3.setCitationKey("a3");
        entries.add(entry3);
        database.insertEntry(entry3);
        for (BibEntry entry : database.getEntries()) {
            entryDBMap.put(entry, database);
        }

        assertEquals("Beta [2000a,b,c]",
                getCitationMarker2(style, entries, entryDBMap, false,
                        new String[]{"a", "b", "c"},
                        new Boolean[]{false, false, false},
                        null));
    }

    @Test
        // TODO: equals only work when initialized from file, not from reader
    void equals() throws IOException {
        JStyle style1 = new JStyle(
                JStyleLoader.DEFAULT_NUMERICAL_STYLE_PATH,
                layoutFormatterPreferences,
                abbreviationRepository);
        JStyle style2 = new JStyle(
                JStyleLoader.DEFAULT_NUMERICAL_STYLE_PATH,
                layoutFormatterPreferences,
                abbreviationRepository);
        assertEquals(style1, style2);
    }

    @Test
        // TODO: equals only work when initialized from file, not from reader
    void notEquals() throws IOException {
        JStyle style1 = new JStyle(
                JStyleLoader.DEFAULT_NUMERICAL_STYLE_PATH,
                layoutFormatterPreferences,
                abbreviationRepository);
        JStyle style2 = new JStyle(
                JStyleLoader.DEFAULT_AUTHORYEAR_STYLE_PATH,
                layoutFormatterPreferences,
                abbreviationRepository);
        assertNotEquals(style1, style2);
    }

    @Test
    void compareToEqual() throws IOException {
        JStyle style1 = new JStyle(
                JStyleLoader.DEFAULT_NUMERICAL_STYLE_PATH,
                layoutFormatterPreferences,
                abbreviationRepository);
        JStyle style2 = new JStyle(
                JStyleLoader.DEFAULT_NUMERICAL_STYLE_PATH,
                layoutFormatterPreferences,
                abbreviationRepository);
        assertEquals(0, style1.compareTo(style2));
    }

    @Test
    void compareToNotEqual() throws IOException {
        JStyle style1 = new JStyle(
                JStyleLoader.DEFAULT_NUMERICAL_STYLE_PATH,
                layoutFormatterPreferences,
                abbreviationRepository);
        JStyle style2 = new JStyle(
                JStyleLoader.DEFAULT_AUTHORYEAR_STYLE_PATH,
                layoutFormatterPreferences,
                abbreviationRepository);
        assertTrue(style1.compareTo(style2) > 0);
        assertFalse(style2.compareTo(style1) > 0);
    }

    @Test
    void emptyStringPropertyAndOxfordComma() throws IOException {
        JStyle style = new JStyle("test.jstyle", layoutFormatterPreferences, abbreviationRepository);
        Map<BibEntry, BibDatabase> entryDBMap = new HashMap<>();
        List<BibEntry> entries = new ArrayList<>();
        BibDatabase database = new BibDatabase();

        BibEntry entry = new BibEntry();
        entry.setType(StandardEntryType.Article);
        entry.setField(StandardField.AUTHOR, "Alpha von Beta and Gamma Epsilon and Ypsilon Tau");
        entry.setField(StandardField.TITLE, "JabRef Manual");
        entry.setField(StandardField.YEAR, "2016");
        entry.setCitationKey("a1");
        database.insertEntry(entry);
        entries.add(entry);
        entryDBMap.put(entry, database);
        assertEquals("von Beta, Epsilon, & Tau, 2016",
                getCitationMarker2(style, entries, entryDBMap, true, null, null, null));
    }

    @Test
    void isValidWithDefaultSectionAtTheStart() throws IOException {
        JStyle style = new JStyle("testWithDefaultAtFirstLIne.jstyle", layoutFormatterPreferences, abbreviationRepository);
        assertTrue(style.isValid());
    }

    @Test
    void getCitationMarkerJoinFirst() throws IOException {
        JStyle style = new JStyle(
                JStyleLoader.DEFAULT_NUMERICAL_STYLE_PATH,
                layoutFormatterPreferences,
                abbreviationRepository);

        // Question: What should happen if some sources are
        // marked as isFirstAppearanceOfSource?
        // This test documents what is happening now.

        // Two entries with identical normalizedMarkers and many authors.
        BibEntry entry1 = new BibEntry()
                .withField(StandardField.AUTHOR,
                        "Gustav Bostr\\\"{o}m"
                                + " and Jaana W\\\"{a}yrynen"
                                + " and Marine Bod\\'{e}n"
                                + " and Konstantin Beznosov"
                                + " and Philippe Kruchten")
                .withField(StandardField.YEAR, "2006")
                .withField(StandardField.BOOKTITLE, "A book 1")
                .withField(StandardField.PUBLISHER, "ACM")
                .withField(StandardField.TITLE, "Title 1")
                .withField(StandardField.PAGES, "11--18");
        entry1.setCitationKey("b1");

        BibEntry entry2 = new BibEntry()
                .withField(StandardField.AUTHOR,
                        "Gustav Bostr\\\"{o}m"
                                + " and Jaana W\\\"{a}yrynen"
                                + " and Marine Bod\\'{e}n"
                                + " and Konstantin Beznosov"
                                + " and Philippe Kruchten")
                .withField(StandardField.YEAR, "2006")
                .withField(StandardField.BOOKTITLE, "A book 2")
                .withField(StandardField.PUBLISHER, "ACM")
                .withField(StandardField.TITLE, "title2")
                .withField(StandardField.PAGES, "11--18");
        entry2.setCitationKey("b2");

        // Last Author differs.
        BibEntry entry3 = new BibEntry()
                .withField(StandardField.AUTHOR,
                        "Gustav Bostr\\\"{o}m"
                                + " and Jaana W\\\"{a}yrynen"
                                + " and Marine Bod\\'{e}n"
                                + " and Konstantin Beznosov"
                                + " and Philippe NotKruchten")
                .withField(StandardField.YEAR, "2006")
                .withField(StandardField.BOOKTITLE, "A book 3")
                .withField(StandardField.PUBLISHER, "ACM")
                .withField(StandardField.TITLE, "title3")
                .withField(StandardField.PAGES, "11--18");
        entry3.setCitationKey("b3");

        BibDatabase database = new BibDatabase();
        database.insertEntry(entry1);
        database.insertEntry(entry2);
        database.insertEntry(entry3);

        // Without pageInfo, two isFirstAppearanceOfSource may be joined.
        // The third is NotKruchten, should not be joined.
        if (true) {
            List<CitationMarkerEntry> citationMarkerEntries = new ArrayList<>();
            CitationMarkerEntry cm1 =
                    makeCitationMarkerEntry(entry1, database, "a", null, true);
            citationMarkerEntries.add(cm1);
            CitationMarkerEntry cm2 =
                    makeCitationMarkerEntry(entry2, database, "b", null, true);
            citationMarkerEntries.add(cm2);
            CitationMarkerEntry cm3 =
                    makeCitationMarkerEntry(entry3, database, "c", null, true);
            citationMarkerEntries.add(cm3);

            assertEquals("[Boström, Wäyrynen, Bodén, Beznosov & Kruchten, 2006a,b"
                            + "; Boström, Wäyrynen, Bodén, Beznosov & NotKruchten, 2006c]",
                    style.createCitationMarker(citationMarkerEntries,
                            true,
                            NonUniqueCitationMarker.THROWS).toString());

            assertEquals("Boström, Wäyrynen, Bodén, Beznosov & Kruchten [2006a,b]"
                            + "; Boström, Wäyrynen, Bodén, Beznosov & NotKruchten [2006c]",
                    style.createCitationMarker(citationMarkerEntries,
                            false,
                            NonUniqueCitationMarker.THROWS).toString());
        }

        // Without pageInfo, only the first is isFirstAppearanceOfSource.
        // The second may be joined, based on expanded normalizedMarkers.
        // The third is NotKruchten, should not be joined.
        if (true) {
            List<CitationMarkerEntry> citationMarkerEntries = new ArrayList<>();
            CitationMarkerEntry cm1 =
                    makeCitationMarkerEntry(entry1, database, "a", null, true);
            citationMarkerEntries.add(cm1);
            CitationMarkerEntry cm2 =
                    makeCitationMarkerEntry(entry2, database, "b", null, false);
            citationMarkerEntries.add(cm2);
            CitationMarkerEntry cm3 =
                    makeCitationMarkerEntry(entry3, database, "c", null, false);
            citationMarkerEntries.add(cm3);

            assertEquals("[Boström, Wäyrynen, Bodén, Beznosov & Kruchten, 2006a,b"
                            + "; Boström et al., 2006c]",
                    style.createCitationMarker(citationMarkerEntries,
                            true,
                            NonUniqueCitationMarker.THROWS).toString());
        }
        // Without pageInfo, only the second is isFirstAppearanceOfSource.
        // The second is not joined, because it is a first appearance, thus
        // requires more names to be shown.
        // The third is NotKruchten, should not be joined.
        if (true) {
            List<CitationMarkerEntry> citationMarkerEntries = new ArrayList<>();
            CitationMarkerEntry cm1 =
                    makeCitationMarkerEntry(entry1, database, "a", null, false);
            citationMarkerEntries.add(cm1);
            CitationMarkerEntry cm2 =
                    makeCitationMarkerEntry(entry2, database, "b", null, true);
            citationMarkerEntries.add(cm2);
            CitationMarkerEntry cm3 =
                    makeCitationMarkerEntry(entry3, database, "c", null, false);
            citationMarkerEntries.add(cm3);

            assertEquals("[Boström et al., 2006a"
                            + "; Boström, Wäyrynen, Bodén, Beznosov & Kruchten, 2006b"
                            + "; Boström et al., 2006c]",
                    style.createCitationMarker(citationMarkerEntries,
                            true,
                            NonUniqueCitationMarker.THROWS).toString());
        }

        // Without pageInfo, neither is isFirstAppearanceOfSource.
        // The second is joined.
        // The third is NotKruchten, but is joined because NotKruchten is not among the names shown.
        // Is this the correct behaviour?
        if (true) {
            List<CitationMarkerEntry> citationMarkerEntries = new ArrayList<>();
            CitationMarkerEntry cm1 =
                    makeCitationMarkerEntry(entry1, database, "a", null, false);
            citationMarkerEntries.add(cm1);
            CitationMarkerEntry cm2 =
                    makeCitationMarkerEntry(entry2, database, "b", null, false);
            citationMarkerEntries.add(cm2);
            CitationMarkerEntry cm3 =
                    makeCitationMarkerEntry(entry3, database, "c", null, false);
            citationMarkerEntries.add(cm3);

            assertEquals("[Boström et al., 2006a,b,c]",
                    style.createCitationMarker(citationMarkerEntries,
                            true,
                            NonUniqueCitationMarker.THROWS).toString());
        }

        // With pageInfo: different entries with identical non-null pageInfo: not joined.
        // XY [2000a,b,c; p1] whould be confusing.
        if (true) {
            List<CitationMarkerEntry> citationMarkerEntries = new ArrayList<>();
            CitationMarkerEntry cm1 =
                    makeCitationMarkerEntry(entry1, database, "a", "p1", false);
            citationMarkerEntries.add(cm1);
            CitationMarkerEntry cm2 =
                    makeCitationMarkerEntry(entry2, database, "b", "p1", false);
            citationMarkerEntries.add(cm2);
            CitationMarkerEntry cm3 =
                    makeCitationMarkerEntry(entry3, database, "c", "p1", false);
            citationMarkerEntries.add(cm3);

            assertEquals("[Boström et al., 2006a; p1"
                            + "; Boström et al., 2006b; p1"
                            + "; Boström et al., 2006c; p1]",
                    style.createCitationMarker(citationMarkerEntries,
                            true,
                            NonUniqueCitationMarker.THROWS).toString());
        }

        // With pageInfo: same entries with identical non-null pageInfo: collapsed.
        // Note: "same" here looks at the visible parts and citation key only,
        //       but ignores the rest. Normally the citation key should distinguish.
        if (true) {
            List<CitationMarkerEntry> citationMarkerEntries = new ArrayList<>();
            CitationMarkerEntry cm1 =
                    makeCitationMarkerEntry(entry1, database, "a", "p1", false);
            citationMarkerEntries.add(cm1);
            CitationMarkerEntry cm2 =
                    makeCitationMarkerEntry(entry1, database, "a", "p1", false);
            citationMarkerEntries.add(cm2);
            CitationMarkerEntry cm3 =
                    makeCitationMarkerEntry(entry1, database, "a", "p1", false);
            citationMarkerEntries.add(cm3);

            assertEquals("[Boström et al., 2006a; p1]",
                    style.createCitationMarker(citationMarkerEntries,
                            true,
                            NonUniqueCitationMarker.THROWS).toString());
        }
        // With pageInfo: same entries with different pageInfo: kept separate.
        // Empty ("") and missing pageInfos considered equal, thus collapsed.
        if (true) {
            List<CitationMarkerEntry> citationMarkerEntries = new ArrayList<>();
            CitationMarkerEntry cm1 =
                    makeCitationMarkerEntry(entry1, database, "a", "p1", false);
            citationMarkerEntries.add(cm1);
            CitationMarkerEntry cm2 =
                    makeCitationMarkerEntry(entry1, database, "a", "p2", false);
            citationMarkerEntries.add(cm2);
            CitationMarkerEntry cm3 =
                    makeCitationMarkerEntry(entry1, database, "a", "", false);
            citationMarkerEntries.add(cm3);
            CitationMarkerEntry cm4 =
                    makeCitationMarkerEntry(entry1, database, "a", null, false);
            citationMarkerEntries.add(cm4);

            assertEquals("[Boström et al., 2006a; p1"
                            + "; Boström et al., 2006a; p2"
                            + "; Boström et al., 2006a]",
                    style.createCitationMarker(citationMarkerEntries,
                            true,
                            NonUniqueCitationMarker.THROWS).toString());
        }
    }
}
