package st.kolkhor.projects.languageid.io;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Utility class for reading and writing documents in the format of the
 * 2014 shared task for Discriminating between Similar Languages (DSL).
 */
public class DslTaskIo {

    //utility class -> prevent initialization
    private DslTaskIo() {}

    /**
     * Reads a UTF-8 encoded file with one document per line.
     * If (tab-separated) class information is available, it is saved in the documents
     * @param filePath input file
     * @param keepRawData whether the uncleaned data of the documents should be kept
     * @return stream of parsed and cleaned documents
     */
    public static Stream<Document> readDocumentFile(Path filePath, boolean keepRawData) {

        try {
            Stream<Document> samples = Files.lines(filePath, StandardCharsets.UTF_8)
                    .map(s -> DslTaskIo.lineToDocument(s, keepRawData));
            return samples;
        } catch (IOException e) {
            e.printStackTrace();
            return Stream.empty();
        }
    }

    private static Document lineToDocument(String line, boolean keepRawData) {
        //expected format: sentence<tab>language group<tab>language
        //we ignore the language group
        String[] parts = line.split("\t");
        if (parts.length >= 3) {
            return new Document(parts[0], Optional.of(parts[2]), Optional.of(parts[1]), keepRawData);
        } else {
            return new Document(parts[0], Optional.empty(), Optional.empty(), keepRawData);
        }
    }

    /**
     * writes a sequence of labelled documents to a file
     * @param filePath output file
     * @param documents documents which are required to have language and language group labels
     */
    public static void writeDocumentFile(Path filePath, Stream<Document> documents) {
        Stream<String> lines = documents.map(doc -> doc.getRawText().get() + "\t" + doc.getLanguageGroup().get() + "\t" + doc.getLanguage().get());
        try {
            Files.createDirectories(filePath.getParent());
            Files.write(filePath, (Iterable<String>) lines::iterator,  StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
