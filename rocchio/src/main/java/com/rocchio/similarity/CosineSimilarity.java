package com.rocchio.similarity;

import org.apache.commons.math3.linear.RealVector;

public class CosineSimilarity {
   
    // Similaridade de Cosseno - Cosine Similarity
    // v1.dotProduct(v2) calcula o produto escalar - v1.dotProduct(v2) calculates the dot product
    // v1.getNorm() e v2.getNorm() calculam a norma - v1.getNorm() and v2.getNorm() calculate the norm
    public static double cosineSimilarity(RealVector v1, RealVector v2) {
        return v1.dotProduct(v2) / (v1.getNorm() * v2.getNorm());
    }
}
