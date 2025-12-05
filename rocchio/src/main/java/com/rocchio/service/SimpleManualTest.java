package com.rocchio.service;

import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class SimpleManualTest implements CommandLineRunner {
    
    @Override
    public void run(String... args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        Recommendation recommender = new Recommendation();
        
        List<String> books = Arrays.asList(
            "O Senhor dos Anéis - fantasia épica terra média",
            "Harry Potter - fantasia magia escola bruxos",
            "Duna - ficção científica deserto areia política",
            "1984 - distopia política vigilância governo",
            "Orgulho e Preconceito - romance clássico sociedade",
            "O Guia do Mochileiro - ficção científica humor espaço",
            "Cem Anos de Solidão - realismo mágico família amor",
            "A Revolução dos Bichos - alegoria política animais",
            "O Nome do Vento - fantasia música magia aventura",
            "Fundação - ficção científica império futuro psicohistória"
        );
        
        recommender.initialize(books);
        
        System.out.println("=== TESTE MANUAL DO SISTEMA DE RECOMENDAÇÃO ===\n");
        
        while (true) {
            System.out.println("\nDigite sua busca (ou 'sair' para encerrar): ");
            System.out.print("> ");
            String query = scanner.nextLine();
            
            if (query.equalsIgnoreCase("sair")) {
                break;
            }
            
            System.out.print("Quantos resultados? (padrão 5): ");
            String numStr = scanner.nextLine();
            int topN = numStr.isEmpty() ? 5 : Integer.parseInt(numStr);
            
            System.out.println("\nResultados para: \"" + query + "\"");
            System.out.println("--------------------------------------");
            
            List<Recommendation.RecommendationResult> results = 
                recommender.recommend(query, topN);
            
            for (int i = 0; i < results.size(); i++) {
                Recommendation.RecommendationResult result = results.get(i);
                System.out.printf("%d. [Score: %.4f] %s\n", 
                    i + 1, result.getScore(), result.getBookTitle());
            }
            
            // Opção para feedback rápido
            System.out.print("\nDeseja fornecer feedback? (s/n): ");
            String feedbackChoice = scanner.nextLine().toLowerCase();
            
            if (feedbackChoice.equals("s") || feedbackChoice.equals("sim")) {
                System.out.print("Índices dos relevantes (ex: 1,3): ");
                String relevantStr = scanner.nextLine();
                
                System.out.print("Índices dos não relevantes (ex: 2,4): ");
                String nonRelevantStr = scanner.nextLine();
                
                // Processar feedback
                List<Integer> relevant = parseSimpleInput(relevantStr, results);
                List<Integer> nonRelevant = parseSimpleInput(nonRelevantStr, results);
                
                if (!relevant.isEmpty() || !nonRelevant.isEmpty()) {
                    List<Recommendation.RecommendationResult> refined = 
                        recommender.refineWithFeedback(relevant, nonRelevant);
                    
                    System.out.println("\n=== RESULTADOS APÓS FEEDBACK ===");
                    for (int i = 0; i < Math.min(topN, refined.size()); i++) {
                        Recommendation.RecommendationResult result = refined.get(i);
                        System.out.printf("%d. [Score: %.4f] %s\n", 
                            i + 1, result.getScore(), result.getBookTitle());
                    }
                }
            }
        }
        
        scanner.close();
        System.out.println("\nTeste encerrado!");
    }
    
    private List<Integer> parseSimpleInput(String input, List<Recommendation.RecommendationResult> results) {
        List<Integer> indices = new java.util.ArrayList<>();
        if (input == null || input.trim().isEmpty()) {
            return indices;
        }
        
        for (String part : input.split(",")) {
            try {
                int num = Integer.parseInt(part.trim()) - 1;
                if (num >= 0 && num < results.size()) {
                    indices.add(results.get(num).getIndex());
                }
            } catch (NumberFormatException ignored) {
                // Ignorar entradas inválidas
            }
        }
        return indices;
    }
}