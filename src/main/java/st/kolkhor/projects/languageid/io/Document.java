package st.kolkhor.projects.languageid.io;

import java.util.Arrays;
import java.util.Optional;

/**
 * POJO representing a document (sequence of words) in particular language
 * Language information is optional, so that the class can be used for unlabeled test data.
 */
public class Document {
    private final String[] words;
    private final Optional<String> rawText;
    private final Optional<String> language;
    //the language group is not necessary for any of the classification, but expected in the DSL task evaluation
    private final Optional<String> languageGroup;

    /**
     * Creates a new document and performs simple cleaning.
     * Some punctuation characters are stripped from the data
     * @param rawText raw text of the document
     * @param language optional language label for the document
     * @param languageGroup optional language class for the document
     * @param keepRawText whether the original uncleaned text should be kept in the class
     */
    public Document(String rawText, Optional<String> language, Optional<String> languageGroup, boolean keepRawText) {
        //very simple cleaning -> remove everything
        //better methods might include replacing numbers with a representative number tag etc
        String cleanedText = rawText.replaceAll("[\\./\\-0-9\\(\\),\"]", " ")
                .replaceAll("[ ]+", " ") //remove redundant spaces
                .trim();

        this.words = cleanedText.split(" ");
        assert Arrays.stream(words).allMatch(w -> w.length() > 0);
        this.rawText = keepRawText ? Optional.of(rawText) : Optional.<String>empty();
        this.language = language;
        this.languageGroup = languageGroup;
    }

    public String[] getWords() {
        return words;
    }

    public Optional<String> getLanguage() {
        return language;
    }

    @Override
    public String toString() {
        return "Document{" +
                "words='" + words + '\'' +
                ", language='" + language + '\'' +
                '}';
    }

    public Optional<String> getRawText() {
        return rawText;
    }

    public Optional<String> getLanguageGroup() {
        return languageGroup;
    }
}
