package com.rocchio.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.InputMismatchException;
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
        
        // Dataset melhorado
        List<String> books = Arrays.asList(
            "O Senhor dos Anéis - Uma jornada épica pela Terra Média",
            "Harry Potter e a Pedra Filosofal - Um jovem bruxo descobre a magia",
            "Duna - Ficção científica em um planeta desértico",
            "1984 - Distopia sobre vigilância governamental",
            "Orgulho e Preconceito - Romance clássico sobre sociedade",
            "O Guia do Mochileiro das Galáxias - Comédia de ficção científica",
            "Cem Anos de Solidão - Realismo mágico da família Buendía",
            "A Revolução dos Bichos - Alegoria política com animais",
            "O Nome do Vento - Fantasia sobre música e magia",
            "Fundação - Ficção científica sobre psicohistória",
            "A Arte da Guerra - Estratégia militar e táticas",
            "Sherlock Holmes - Mistério e investigação policial",
            "O Pequeno Príncipe - Fábula sobre amizade e humanidade",
            "Dom Casmurro - Romance sobre ciúme e traição",
            "O Alquimista - Aventura espiritual em busca de destino"
        );
        
        System.out.println("=== SISTEMA DE RECOMENDAÇÃO INTERATIVO ===\n");
        recommender.initialize(books);
        
        boolean running = true;
        while (running) {
            System.out.println("\n=== MENU PRINCIPAL ===");
            System.out.println("1. Buscar livros");
            System.out.println("2. Refinar busca anterior");
            System.out.println("3. Ver todos os livros");
            System.out.println("4. Configurar LSA");
            System.out.println("5. Nova busca (limpar histórico)");
            System.out.println("6. Sair");
            System.out.print("Escolha uma opção: ");
            
            try {
                int choice = scanner.nextInt();
                scanner.nextLine(); // Limpar buffer
                
                switch (choice) {
                    case 1:
                        performSearch(scanner, recommender);
                        break;
                    case 2:
                        refineSearch(scanner, recommender);
                        break;
                    case 3:
                        listAllBooks(books);
                        break;
                    case 4:
                        configureLSA(scanner, recommender);
                        break;
                    case 5:
                        System.out.println("Histórico limpo. Pronto para nova busca.");
                        break;
                    case 6:
                        running = false;
                        System.out.println("Encerrando sistema...");
                        break;
                    default:
                        System.out.println("Opção inválida!");
                }
            } catch (InputMismatchException e) {
                System.out.println("Por favor, digite um número!");
                scanner.nextLine(); // Limpar entrada inválida
            } catch (Exception e) {
                System.out.println("Erro: " + e.getMessage());
            }
        }
        
        scanner.close();
    }
    
    private void performSearch(Scanner scanner, Recommendation recommender) {
        System.out.println("\n=== NOVA BUSCA ===");
        System.out.print("Digite os termos de busca: ");
        String query = scanner.nextLine();
        
        System.out.print("Número de resultados (1-15): ");
        int topN = scanner.nextInt();
        scanner.nextLine();
        
        if (topN < 1 || topN > 15) {
            topN = 5;
            System.out.println("Usando valor padrão: 5 resultados");
        }
        
        List<Recommendation.RecommendationResult> results = 
            recommender.recommend(query, topN);
        
        if (results.isEmpty()) {
            System.out.println("Nenhum resultado encontrado.");
        } else {
            System.out.println("\n=== RESULTADOS ===");
            for (int i = 0; i < results.size(); i++) {
                Recommendation.RecommendationResult result = results.get(i);
                System.out.printf("%2d. [%.4f] %s\n", 
                    i + 1, result.getScore(), result.getBookTitle());
            }
        }
    }
    
    private void refineSearch(Scanner scanner, Recommendation recommender) {
        System.out.println("\n=== REFINAR BUSCA ===");
        System.out.println("Digite os números dos livros RELEVANTES (ex: 1,3,5):");
        System.out.print("> ");
        String relevantInput = scanner.nextLine();
        
        System.out.println("Digite os números dos livros NÃO RELEVANTES (ex: 2,4,6):");
        System.out.print("> ");
        String nonRelevantInput = scanner.nextLine();
        
        List<Integer> relevant = parseIndices(relevantInput);
        List<Integer> nonRelevant = parseIndices(nonRelevantInput);
        
        try {
            List<Recommendation.RecommendationResult> refined = 
                recommender.refineWithFeedback(relevant, nonRelevant);
            
            System.out.println("\n=== RESULTADOS REFINADOS ===");
            for (int i = 0; i < Math.min(10, refined.size()); i++) {
                Recommendation.RecommendationResult result = refined.get(i);
                System.out.printf("%2d. [%.4f] %s\n", 
                    i + 1, result.getScore(), result.getBookTitle());
            }
        } catch (Exception e) {
            System.out.println("Erro ao refinar: " + e.getMessage());
            System.out.println("Execute uma busca primeiro (opção 1)");
        }
    }
    
    private List<Integer> parseIndices(String input) {
        List<Integer> indices = new ArrayList<>();
        if (input == null || input.trim().isEmpty()) {
            return indices;
        }
        
        String[] parts = input.split(",");
        for (String part : parts) {
            try {
                // Converter de 1-based para 0-based
                int index = Integer.parseInt(part.trim()) - 1;
                if (index >= 0) {
                    indices.add(index);
                }
            } catch (NumberFormatException e) {
                // Ignorar entradas inválidas
            }
        }
        return indices;
    }
    
    private void listAllBooks(List<String> books) {
        System.out.println("\n=== TODOS OS LIVROS ===");
        for (int i = 0; i < books.size(); i++) {
            System.out.printf("%2d. %s\n", i + 1, books.get(i));
        }
    }
    
    private void configureLSA(Scanner scanner, Recommendation recommender) {
        System.out.println("\n=== CONFIGURAR LSA ===");
        System.out.print("Ativar LSA? (s/n): ");
        String answer = scanner.nextLine().toLowerCase();
        
        if (answer.equals("s") || answer.equals("sim")) {
            System.out.print("Número de dimensões (10-100): ");
            try {
                int dimensions = scanner.nextInt();
                scanner.nextLine();
                
                if (dimensions < 10) dimensions = 10;
                if (dimensions > 100) dimensions = 100;
                
                recommender.setUseLSA(true, dimensions);
                System.out.println("LSA ativado com " + dimensions + " dimensões");
            } catch (InputMismatchException e) {
                System.out.println("Valor inválido. LSA desativado.");
                recommender.setUseLSA(false, 50);
                scanner.nextLine();
            }
        } else {
            recommender.setUseLSA(false, 50);
            System.out.println("LSA desativado");
        }
    }
}