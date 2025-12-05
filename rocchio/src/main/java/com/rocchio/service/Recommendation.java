package com.rocchio.service;

import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.math3.linear.RealVector;
import org.springframework.stereotype.Service;

import com.rocchio.rocchio.RocchioAlgorithm;
import com.rocchio.similarity.CosineSimilarity;
import com.rocchio.tfidf.LatentSemanticAnalysis;
import com.rocchio.tfidf.TFIDF;

@Service
public class Recommendation {
    
    private List<String> books = new ArrayList<>();
    private List<List<String>> tokenizedBooks = new ArrayList<>();
    private List<RealVector> bookVectors = new ArrayList<>();
    private List<String> vocabulary = new ArrayList<>();
    private LatentSemanticAnalysis lsa = new LatentSemanticAnalysis();
    private RealVector currentQueryVector;
    private boolean useLSA = false;
    private int lsaDimensions = 50;
    
    /**
     * Limpa e tokeniza texto
     */
    private List<String> cleanAndTokenize(String text) {
        // Remove pontuação e converte para minúsculas
        String cleaned = text.toLowerCase()
            .replaceAll("[^a-záàâãéèêíïóôõöúçñ\\s]", " ") // Mantém letras e espaços
            .replaceAll("\\s+", " ") // Remove múltiplos espaços
            .trim();
        
        return Arrays.asList(cleaned.split("\\s+"));
    }
    
    public void initialize(List<String> bookContents) {
        this.books = new ArrayList<>(bookContents);
        
        // Tokeniza os livros
        tokenizedBooks = books.stream()
            .map(this::cleanAndTokenize)
            .collect(Collectors.toList());
        
        // Cria vocabulário único
        vocabulary = extractVocabulary(tokenizedBooks);
        
        // Cria vetores TF-IDF
        bookVectors = new ArrayList<>();
        for (List<String> book : tokenizedBooks) {
            RealVector vector = TFIDF.toTFIDFVector(book, tokenizedBooks, vocabulary);
            bookVectors.add(vector);
        }
        
        // Treina LSA se habilitado
        if (useLSA) {
            lsa.train(bookVectors, lsaDimensions);
            List<RealVector> reducedVectors = new ArrayList<>();
            for (RealVector vector : bookVectors) {
                reducedVectors.add(lsa.transform(vector));
            }
            bookVectors = reducedVectors;
        }
    }
    
    private List<String> extractVocabulary(List<List<String>> docs) {
        Set<String> vocabSet = new HashSet<>();
        for (List<String> doc : docs) {
            vocabSet.addAll(doc);
        }
        return new ArrayList<>(vocabSet);
    }
    
    public List<RecommendationResult> recommend(String queryText, int topN) {
        // Limpa e tokeniza a query
        List<String> queryTokens = cleanAndTokenize(queryText);
        
        // Verifica se a query tem termos
        if (queryTokens.isEmpty() || queryTokens.get(0).isEmpty()) {
            return new ArrayList<>();
        }
        
        // Cria vetor TF-IDF para a query
        RealVector queryVector = TFIDF.toTFIDFVector(queryTokens, tokenizedBooks, vocabulary);
        
        // Aplica LSA se habilitado
        if (useLSA) {
            queryVector = lsa.transform(queryVector);
        }
        
        currentQueryVector = queryVector;
        
        // Calcula similaridade
        List<RecommendationResult> results = new ArrayList<>();
        for (int i = 0; i < books.size(); i++) {
            double similarity = CosineSimilarity.cosineSimilarity(queryVector, bookVectors.get(i));
            results.add(new RecommendationResult(books.get(i), similarity, i));
        }
        
        // Ordena por similaridade
        results.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        
        // Retorna top N
        return results.stream()
            .limit(Math.min(topN, results.size()))
            .collect(Collectors.toList());
    }
    
    public List<RecommendationResult> refineWithFeedback(
            List<Integer> relevantBookIndices, 
            List<Integer> nonRelevantBookIndices) {
        
        if (currentQueryVector == null) {
            throw new IllegalStateException("Execute uma busca primeiro antes de refinar");
        }
        
        // Filtra índices válidos
        List<Integer> validRelevant = relevantBookIndices.stream()
            .filter(idx -> idx >= 0 && idx < bookVectors.size())
            .collect(Collectors.toList());
        
        List<Integer> validNonRelevant = nonRelevantBookIndices.stream()
            .filter(idx -> idx >= 0 && idx < bookVectors.size())
            .collect(Collectors.toList());
        
        // Converte para vetores
        List<RealVector> relevantDocs = validRelevant.stream()
            .map(bookVectors::get)
            .collect(Collectors.toList());
        
        List<RealVector> nonRelevantDocs = validNonRelevant.stream()
            .map(bookVectors::get)
            .collect(Collectors.toList());
        
        // Aplica Rocchio
        RealVector optimizedQuery = RocchioAlgorithm.optimizeQuery(
            currentQueryVector, relevantDocs, nonRelevantDocs);
        
        currentQueryVector = optimizedQuery;
        
        // Recalcula similaridades
        List<RecommendationResult> results = new ArrayList<>();
        for (int i = 0; i < books.size(); i++) {
            double similarity = CosineSimilarity.cosineSimilarity(optimizedQuery, bookVectors.get(i));
            results.add(new RecommendationResult(books.get(i), similarity, i));
        }
        
        results.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        return results;
    }
    
    public void setUseLSA(boolean useLSA, int dimensions) {
        this.useLSA = useLSA;
        this.lsaDimensions = dimensions;
        
        if (!books.isEmpty()) {
            initialize(books);
        }
    }
    
    public static class RecommendationResult {
        private String bookTitle;
        private double score;
        private int index;
        
        public RecommendationResult(String bookTitle, double score, int index) {
            this.bookTitle = bookTitle;
            this.score = score;
            this.index = index;
        }
        
        public String getBookTitle() { return bookTitle; }
        public double getScore() { return score; }
        public int getIndex() { return index; }
        
        @Override
        public String toString() {
            return String.format("Livro: %s | Score: %.4f", bookTitle, score);
        }
    }
}