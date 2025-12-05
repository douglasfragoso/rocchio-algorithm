package com.rocchio.tfidf;

import java.util.List;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

public class TFIDF {
    // Calcula o TF (Term Frequency) - Calcula a frequência do termo
    // TF(t;d) = freq(t) =(total de vezes que o termo aparece no documento)/(total de palavras que aparecem no documento) - TF(t;d) = freq(t) = (total times the term appears in the document) / (total words that appear in the document)
    public static double tf(List<String> doc, String term) {
        double count = 0;
        for (String word : doc) {
            if (term.equalsIgnoreCase(word)) {
                count++;
            }
        }
        return count / doc.size();
    }

    // Calcula o IDF (Inverse Document Frequency) - Calcula a frequência inversa do documento
    // IDF(t) = log(N/df(t))
    // N = total de documentos - N = total documents
    // df(t) = total de documentos que contêm o termo t - df(t) = total documents containing the term t
    public static double idf(List<List<String>> docs, String term) {
        double n = 0;
        for (List<String> doc : docs) {
            for (String word : doc) {
                if (term.equalsIgnoreCase(word)) {
                    n++;
                }
            }
        }
        return Math.log((docs.size() + 1) / (n + 1)); // Suavização de Laplace - Laplace smoothing
    }

    // Calcula o TF-IDF - Calculate the TF-IDF
    // TF-IDF(t,d) = TF(t,d)  x IDF(t)
    public static double tfIdf(List<String> doc, List<List<String>> docs, String term) {
        return tf(doc, term) * idf(docs, term);
    }

    // Converte um documento em um vetor TF-IDF - Converts a document to a TF-IDF vector
    public static RealVector toTFIDFVector(List<String> doc, List<List<String>> docs, List<String> terms) {
        RealVector vector = new ArrayRealVector(terms.size());
        for (int i = 0; i < terms.size(); i++) {
            vector.setEntry(i, tfIdf(doc, docs, terms.get(i)));
        }
        return vector;
    }
}
