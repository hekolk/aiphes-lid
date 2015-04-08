package st.kolkhor.projects.languageid;

import st.kolkhor.projects.languageid.io.Document;
import st.kolkhor.projects.languageid.io.DslTaskIo;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Main class for running the following parts
 *  - reading training data from the 2014 shared task for Discriminating between Similar Languages (DSL)
 *  - training a Naive Bayes classifier based on (laplace-smoothed) words unigrams
 *  - evaluating the classifier on unseen test data
 *  - packaging the results for evaluating with the DSL python scoring script
 */
public class LanguageIdentifier {
    private static Logger log = Logger.getLogger(LanguageIdentifier.class.getName());

    private static Path trainFile = Paths.get("data", "DSLCC", "train.txt");

    private static Path testFile = Paths.get("data", "DSLCC-eval", "test.txt");

    private static Path evalDir = Paths.get("data", "eval");
    private static String evalScriptName = "dslevalscript.py";

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        Stream<Document> trainDocsStream = DslTaskIo.readDocumentFile(trainFile, false);

        //we want to use the original evaluation script of the DSL shared task, so we have some additional bookkeeping in this class
        //we build a map from languages to language group for evaluation, therefore, we parse the data twice and need to convert the stream
        List<Document> trainDocs = trainDocsStream.collect(Collectors.toList());
        Map<String, String> languageCodeToLanguageGroup = new ConcurrentHashMap<>();
        assert trainDocs.stream().allMatch(d -> d.getLanguage().isPresent() && d.getLanguageGroup().isPresent());
        //languages and groups must be available in the training data, so we use .get() directly
        trainDocs.parallelStream().forEach(d -> languageCodeToLanguageGroup.putIfAbsent(d.getLanguage().get(), d.getLanguageGroup().get()));

        //now, to the actual work
        NaiveBayesClassifier.NBModel nbModel = new NaiveBayesClassifier.NBModelBuilder().addDocuments(trainDocs.parallelStream()).buildModel();
        log.info("learned models for the following languages: " + nbModel.getAvailableLanguages().toString());


        //we keep the original data to be able to use the original evaluation script
        Stream<Document> testDocs = DslTaskIo.readDocumentFile(testFile, true);

        //classify each document separately
        Stream<Document> classifiedDocs = testDocs.parallel().map(doc -> {
            String estimatedLanguageCode = NaiveBayesClassifier.classify(doc, nbModel);
            Document classifiedDoc = new Document(
                    doc.getRawText().get(),
                    Optional.of(estimatedLanguageCode),
                    Optional.of(languageCodeToLanguageGroup.get(estimatedLanguageCode)),
                    true);
            return classifiedDoc;
        });

        //write results
        String hypoFileName = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm")) + "-aiphes-close-run1.txt";
        DslTaskIo.writeDocumentFile(evalDir.resolve(hypoFileName), classifiedDocs);
        log.info("learning and classification took " + ((1.0 * System.currentTimeMillis() - startTime) / 1000) + " seconds");
        log.info("the classified test data can be found in " + evalDir.resolve(hypoFileName).toAbsolutePath().toString());

        //now we package everything for the evaluation script and print the command line call
        try {
            String zipHypoFileName = hypoFileName + ".zip";
            new ProcessBuilder("zip", zipHypoFileName, hypoFileName).directory(evalDir.toFile()).start();
            new ProcessBuilder("python2", evalScriptName, zipHypoFileName)
                    .directory(evalDir.toFile())
                    .redirectError(evalDir.resolve(hypoFileName + ".eval.err").toFile())
                    .redirectOutput(evalDir.resolve(hypoFileName + ".eval.out").toFile())
                    .start();
            log.info("call 'cd " + evalDir.toString() + "; python2 dslevalscript.py " + zipHypoFileName + "' from the project dir for scoring results");
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}
