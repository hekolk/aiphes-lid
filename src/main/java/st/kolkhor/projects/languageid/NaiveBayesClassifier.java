package st.kolkhor.projects.languageid;

import st.kolkhor.projects.languageid.io.Document;

import java.util.AbstractMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Simple Naive Bayes classifier for documents (use case language id).
 * The classifier is based on word unigram probabilities with laplacian smoothing.
 *
 * Implementation note:
 * This class makes heavy use of (regular) maps, which would be rather inefficient for large scale computation and has
 * higher memory requirements.
 * However, the code is more readable this way (as opposed to some index juggling on arrays etc) and it should suffice
 * for the use case.
 */
public class NaiveBayesClassifier {
    private static Logger log = Logger.getLogger(NaiveBayesClassifier.class.getName());

    //utility class, so prevent any instantiation
    private NaiveBayesClassifier() {
    }

    /**
     * Classify a document based on a previously learned model
     * @param doc the document to classify. language and languageGroup fields are ignored
     * @param model previously learned model
     * @return the label (language code) of the maximum a posteriori class
     */
    public static String classify(Document doc, NBModel model) {
        //we iterate over all known languages and keep track of the best one
        double maxScore = Double.NEGATIVE_INFINITY;
        String argMax = "NA";
        for (Map.Entry<String, Map<String, Double>> languageLogUnigramProbs : model.unigramLogProbsByLanguage.entrySet()) {
            String languageCode = languageLogUnigramProbs.getKey();
            //we only want the argmax, so just add the log probabilities
            Map<String, Double> logUnigramProbs = languageLogUnigramProbs.getValue();
            double curScore = model.languagePriors.get(languageCode);
            for (String word : doc.getWords()) {
                //if the words is completely new, we use the OOV's prob (laplace-smoothed, i.e. 1/|words(l)| )
                curScore += logUnigramProbs.computeIfAbsent(word, w -> logUnigramProbs.get(NBModel.OOV_MARKER));
            }
            if (curScore > maxScore) {
                maxScore = curScore;
                argMax = languageCode;
            }
        }
        return argMax;
    }

    /**
     * Class representing a learned Naive Bayes model
     */
    public static class NBModel {
        //we treat out-of-vocabulary words in evaluation the same as words unseen in a language -> laplace smoothing
        private final static String OOV_MARKER = "<OOV>";
        //maps language -> log(p(language))
        private final Map<String, Double> languagePriors;
        //maps languageCode -> word -> log(p(word | language))
        private final Map<String, Map<String, Double>> unigramLogProbsByLanguage;

        /**
         * Creates a new model instance
         * @param languagePriors prior log probability of document classes (languages)
         * @param unigramLogProbsByLanguage log probabilities of word unigrams for all languages
         */
        public NBModel(Map<String, Double> languagePriors, Map<String, Map<String, Double>> unigramLogProbsByLanguage) {
            this.languagePriors = languagePriors;
            this.unigramLogProbsByLanguage = unigramLogProbsByLanguage;
        }

        public Set<String> getAvailableLanguages() {
            return languagePriors.keySet();
        }
    }

    /**
     * Builder class for training a Naive Bayes classifier
     *
     * First, call addDocuments (multiple times, if necessary) for adding training data
     * Subsequently, class buildModel() to estimate the model parameters
     */
    public static class NBModelBuilder {
        //maps languageCode -> documentCount
        Map<String, LongAdder> documentCountsByLanguage = new ConcurrentHashMap<>();
        //maps languageCode -> total number of words in the language
        Map<String, LongAdder> wordCountsByLanguage = new ConcurrentHashMap<>();

        //maps languageCode -> word -> count
        Map<String, Map<String, LongAdder>> unigramCountsByLanguage = new ConcurrentHashMap<>();

        /**
         * Updates the uniqram counts based on the supplied documents
         * @param documents documents as training data
         * @return this instance
         */
        public NBModelBuilder addDocuments(Stream<Document> documents) {


            documents.parallel()
                    .filter(d -> d.getLanguage().isPresent()) //silently drop "unsupervised" data
                    .forEach(d -> {
                        //first, keep track of number of documents and total number of words
                        documentCountsByLanguage.computeIfAbsent(d.getLanguage().get(), k -> new LongAdder()).increment();
                        wordCountsByLanguage.computeIfAbsent(d.getLanguage().get(), k -> new LongAdder()).add(d.getWords().length);

                        //increment unigram counts with words from this document
                        Map<String, LongAdder> unigramCounts = unigramCountsByLanguage.computeIfAbsent(d.getLanguage().get(), language -> new ConcurrentHashMap<String, LongAdder>());
                        for (String word : d.getWords()) {
                            unigramCounts.computeIfAbsent(word, w -> new LongAdder()).increment();
                        }
                    });

            return this;
        }

        /**
         * build Naive Bayes models from observed unigram counts.
         * Laplace (add-one) smoothing is used
         * @return NB model for classification of new documents
         */
        public NBModel buildModel() {
            Set<String> vocabulary = new HashSet<>();
            vocabulary.add(NBModel.OOV_MARKER);
            for (Map<String, LongAdder> wordCounts : unigramCountsByLanguage.values()) {
                vocabulary.addAll(wordCounts.keySet());
            }
            log.info("building model for " + documentCountsByLanguage.keySet().size() + " languages and vocabulary of size " + vocabulary.size());
            log.info("using " + wordCountsByLanguage.values().stream().mapToLong(LongAdder::longValue).sum()
                    + " from " + documentCountsByLanguage.values().stream().mapToLong(LongAdder::longValue).sum() + " documents");

            //loop over languages
            ConcurrentMap<String, Map<String, Double>> unigramLogProbsByLanguage = unigramCountsByLanguage.entrySet().parallelStream()
                    .collect(Collectors.toConcurrentMap(Map.Entry::getKey, languageEntry -> {
                String languageCode = languageEntry.getKey();
                Map<String, LongAdder> wordCounts = languageEntry.getValue();
                //just sum over all words
                double logTotalWordCount = Math.log(wordCountsByLanguage.get(languageCode).longValue()
                        + vocabulary.size()); //add because of smoothing

                //now calculate (smoothed) probabilities for every word from the vocabulary
                ConcurrentMap<String, Double> unigramLogProbs = vocabulary.parallelStream().map(word -> {
                    long observedCount = wordCounts.computeIfAbsent(word, w -> new LongAdder()).longValue();

                    double logProb = Math.log1p(observedCount) - logTotalWordCount;
                    return new AbstractMap.SimpleImmutableEntry<String, Double>(word, logProb);
                }).collect(Collectors.toConcurrentMap(AbstractMap.SimpleImmutableEntry::getKey, AbstractMap.SimpleImmutableEntry::getValue));

                return unigramLogProbs;
            }));

            Map<String, Double> languagePriors = calculateLanguagePriors();

            return new NBModel(languagePriors, unigramLogProbsByLanguage);
        }

        //calculate the prior probabilities of the language as the fraction of observed documents in the corresponding language
        private Map<String, Double> calculateLanguagePriors() {
            double logTotalDocumentCount = Math.log(documentCountsByLanguage.values().stream().mapToLong(LongAdder::longValue).sum());

            return documentCountsByLanguage.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                    entry -> Math.log(entry.getValue().doubleValue()) - logTotalDocumentCount));
        }
    }




}
