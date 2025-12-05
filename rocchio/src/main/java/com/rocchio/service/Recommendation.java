package com.rocchio.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.math3.linear.RealVector;
import org.springframework.stereotype.Service;

import com.rocchio.rocchio.RocchioAlgorithm;
import com.rocchio.similarity.CosineSimilarity;
import com.rocchio.tfidf.LatentSemanticAnalysis;
import com.rocchio.tfidf.TFIDF;

@Service
public class Recommendation {
    
    private List<String> books = new ArrayList<>();  // Títulos ou conteúdos dos livros
    private List<List<String>> tokenizedBooks = new ArrayList<>();  // Livros tokenizados (palavras)
    private List<RealVector> bookVectors = new ArrayList<>();  // Vetores TF-IDF dos livros
    private List<String> vocabulary = new ArrayList<>();  // Vocabulário único
    private LatentSemanticAnalysis lsa = new LatentSemanticAnalysis();
    private RealVector currentQueryVector;  // Query atual (para feedback iterativo)
    private boolean useLSA = false;  // Flag para usar LSA ou não
    private int lsaDimensions = 50;  // Dimensões para redução LSA
    
    /**
     * Inicializa o sistema com uma lista de livros
     */
    public void initialize(List<String> bookContents) {
        this.books = new ArrayList<>(bookContents);
        
        // Tokeniza os livros (divide em palavras)
        tokenizedBooks = books.stream()
            .map(content -> Arrays.asList(content.toLowerCase().split("\\s+")))
            .collect(Collectors.toList());
        
        // Cria vocabulário único
        vocabulary = extractVocabulary(tokenizedBooks);
        
        // Cria vetores TF-IDF para cada livro
        bookVectors = new ArrayList<>();
        for (List<String> book : tokenizedBooks) {
            RealVector vector = TFIDF.toTFIDFVector(book, tokenizedBooks, vocabulary);
            bookVectors.add(vector);
        }
        
        // Treina LSA se habilitado
        if (useLSA) {
            lsa.train(bookVectors, lsaDimensions);
            // Transforma todos os vetores para o espaço reduzido
            List<RealVector> reducedVectors = new ArrayList<>();
            for (RealVector vector : bookVectors) {
                reducedVectors.add(lsa.transform(vector));
            }
            bookVectors = reducedVectors;
        }
    }
    
    /**
     * Extrai vocabulário único de todos os livros
     */
    private List<String> extractVocabulary(List<List<String>> docs) {
        Set<String> vocabSet = new HashSet<>();
        for (List<String> doc : docs) {
            vocabSet.addAll(doc);
        }
        return new ArrayList<>(vocabSet);
    }
    
    /**
     * Recomenda livros baseado em uma query textual
     */
    public List<RecommendationResult> recommend(String queryText, int topN) {
        // Converte query para vetor
        List<String> queryTokens = Arrays.asList(queryText.toLowerCase().split("\\s+"));
        RealVector queryVector = TFIDF.toTFIDFVector(queryTokens, tokenizedBooks, vocabulary);
        
        // Aplica LSA se habilitado
        if (useLSA) {
            queryVector = lsa.transform(queryVector);
        }
        
        currentQueryVector = queryVector;
        
        // Calcula similaridade com todos os livros
        List<RecommendationResult> results = new ArrayList<>();
        for (int i = 0; i < books.size(); i++) {
            double similarity = CosineSimilarity.cosineSimilarity(queryVector, bookVectors.get(i));
            results.add(new RecommendationResult(books.get(i), similarity, i));
        }
        
        // Ordena por similaridade (maior primeiro)
        results.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        
        // Retorna top N
        return results.stream().limit(topN).collect(Collectors.toList());
    }
    
    /**
     * Refina recomendações com feedback (algoritmo de Rocchio)
     */
    public List<RecommendationResult> refineWithFeedback(
            List<Integer> relevantBookIndices, 
            List<Integer> nonRelevantBookIndices) {
        
        if (currentQueryVector == null) {
            throw new IllegalStateException("Execute uma busca primeiro antes de refinar");
        }
        
        // Converte índices para vetores
        List<RealVector> relevantDocs = relevantBookIndices.stream()
            .map(bookVectors::get)
            .collect(Collectors.toList());
        
        List<RealVector> nonRelevantDocs = nonRelevantBookIndices.stream()
            .map(bookVectors::get)
            .collect(Collectors.toList());
        
        // Aplica Rocchio para otimizar a query
        RealVector optimizedQuery = RocchioAlgorithm.optimizeQuery(
            currentQueryVector, relevantDocs, nonRelevantDocs);
        
        currentQueryVector = optimizedQuery;
        
        // Recalcula similaridades com a nova query
        List<RecommendationResult> results = new ArrayList<>();
        for (int i = 0; i < books.size(); i++) {
            double similarity = CosineSimilarity.cosineSimilarity(optimizedQuery, bookVectors.get(i));
            results.add(new RecommendationResult(books.get(i), similarity, i));
        }
        
        // Ordena por similaridade
        results.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        
        return results;
    }
    
    /**
     * Habilita/desabilita LSA
     */
    public void setUseLSA(boolean useLSA, int dimensions) {
        this.useLSA = useLSA;
        this.lsaDimensions = dimensions;
        
        // Re-inicializa se já tiver dados
        if (!books.isEmpty()) {
            initialize(books);
        }
    }
    
    /**
     * Classe para representar um resultado de recomendação
     */
    public static class RecommendationResult {
        private String bookTitle;
        private double score;
        private int index;
        
        public RecommendationResult(String bookTitle, double score, int index) {
            this.bookTitle = bookTitle;
            this.score = score;
            this.index = index;
        }
        
        // Getters e Setters
        public String getBookTitle() { return bookTitle; }
        public double getScore() { return score; }
        public int getIndex() { return index; }
        
        @Override
        public String toString() {
            return String.format("Livro: %s | Score: %.4f", bookTitle, score);
        }
    }
}