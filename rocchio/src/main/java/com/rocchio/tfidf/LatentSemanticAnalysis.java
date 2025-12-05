package com.rocchio.tfidf;

import java.util.List;
import org.apache.commons.math3.linear.BlockRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.SingularValueDecomposition;

public class LatentSemanticAnalysis {

    private RealMatrix V_k; // Matriz de projeção reduzida

    /**
     * Treina o modelo LSA com a base de documentos atual.
     * documentVectors Lista de todos os vetores TF-IDF do corpus.
     * Número de dimensões latentes (tópicos) a manter.
     */
    public void train(List<RealVector> documentVectors, int k) {
        if (documentVectors.isEmpty()) return;

        // 1. Cria a Matriz Termo-Documento (ou Documento-Termo)
        // Aqui faremos Documento(linhas) x Termos(colunas) para facilitar o commons-math
        int numDocs = documentVectors.size();
        int numTerms = documentVectors.get(0).getDimension();

        double[][] matrixData = new double[numDocs][numTerms];
        for (int i = 0; i < numDocs; i++) {
            matrixData[i] = documentVectors.get(i).toArray();
        }

        RealMatrix A = new BlockRealMatrix(matrixData);

        // 2. Aplica SVD: A = U * S * V^T
        SingularValueDecomposition svd = new SingularValueDecomposition(A);

        // 3. Obtém a matriz V (que relaciona termos aos conceitos latentes)
        RealMatrix V = svd.getV();

        // 4. Trunca a matriz V para manter apenas as 'k' primeiras colunas (dimensões)
        // V tem dimensão [Termos x Termos]. Queremos [Termos x k]
        // Se k for maior que o número de colunas, ajustamos.
        int actualK = Math.min(k, V.getColumnDimension());
        
        this.V_k = V.getSubMatrix(0, V.getRowDimension() - 1, 0, actualK - 1);
    }

    /**
     * Projeta um vetor TF-IDF original no espaço semântico reduzido.
     * Fórmula: vec_reduzido = vec_original * V_k
     */
    public RealVector transform(RealVector tfidfVector) {
        if (this.V_k == null) {
            throw new IllegalStateException("O modelo LSA precisa ser treinado primeiro!");
        }
        
        // Multiplica o vetor (1 x Termos) pela matriz reduzida (Termos x k)
        // Resultado: (1 x k)
        // No commons-math, operate faz a multiplicação Matriz * Vetor ou Vetor * Matriz dependendo do contexto.
        // Aqui, preMultiply trata o vetor como linha: v * M
        return this.V_k.preMultiply(tfidfVector);
    }
}