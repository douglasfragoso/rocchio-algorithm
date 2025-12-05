package com.rocchio.rocchio;

import java.util.List;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

public class RocchioAlgorithm {

    private static final double ALPHA = 1.0;
    private static final double BETA = 0.75;
    private static final double GAMMA = 0.15;

    /**
     * Otimiza o vetor de query com base no feedback de relevância.
     *
     * originalQuery - O vetor da query inicial.
     * relevantDocs -  Lista de vetores dos documentos marcados como relevantes.
     * nonRelevantDocs - Lista de vetores dos documentos marcados como não relevantes.
     * return - O novo vetor da query otimizado.
     */
    public static RealVector optimizeQuery(RealVector originalQuery, 
                                           List<RealVector> relevantDocs, 
                                           List<RealVector> nonRelevantDocs) {
        
        // 1. Parte da Query Original (alpha * q0)
        RealVector newQuery = originalQuery.mapMultiply(ALPHA);

        // 2. Parte dos Relevantes (beta * média(Dr))
        if (relevantDocs != null && !relevantDocs.isEmpty()) {
            RealVector sumRelevant = new ArrayRealVector(originalQuery.getDimension());
            for (RealVector doc : relevantDocs) {
                sumRelevant = sumRelevant.add(doc);
            }
            // Média: (1 / |Dr|) * Soma
            RealVector meanRelevant = sumRelevant.mapMultiply(1.0 / relevantDocs.size());
            // Adiciona ao vetor principal ponderado por Beta
            newQuery = newQuery.add(meanRelevant.mapMultiply(BETA));
        }

        // 3. Parte dos Não Relevantes (gamma * média(Dnr))
        if (nonRelevantDocs != null && !nonRelevantDocs.isEmpty()) {
            RealVector sumNonRelevant = new ArrayRealVector(originalQuery.getDimension());
            for (RealVector doc : nonRelevantDocs) {
                sumNonRelevant = sumNonRelevant.add(doc);
            }
            // Média: (1 / |Dnr|) * Soma
            RealVector meanNonRelevant = sumNonRelevant.mapMultiply(1.0 / nonRelevantDocs.size());
            // Subtrai do vetor principal ponderado por Gamma
            newQuery = newQuery.subtract(meanNonRelevant.mapMultiply(GAMMA));
        }

        // Garante que não tenhamos valores negativos no vetor (comum em Rocchio)
        return ensureNonNegative(newQuery);
    }

    // Helper para zerar valores negativos (opcional, mas recomendado para TF-IDF)
    private static RealVector ensureNonNegative(RealVector vector) {
        RealVector result = vector.copy();
        for (int i = 0; i < result.getDimension(); i++) {
            if (result.getEntry(i) < 0) {
                result.setEntry(i, 0);
            }
        }
        return result;
    }
}