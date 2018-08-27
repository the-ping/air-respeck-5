package com.specknet.airrespeck.models;

// Logistic regression model which works like sklearn model.
// Get the parameters from python by calling reg.coef_ and reg.intercept_ on the regression model
public class LogisticRegression {

    private double[] coefficients;
    private double intercept;

    public LogisticRegression(double[] coefficients, double intercept) {
        this.coefficients = coefficients;
        this.intercept = intercept;
    }

    public int predict(double[] features) {
        return predictProba(features) >= 0.5 ? 1 : 0;
    }

    public double predictProba(double[] features) {
        return logisticFunction(dotProduct(features, coefficients) + intercept);
    }

    private double logisticFunction(double v) {
        return 1 / (1 + Math.exp(-v));
    }

    private double dotProduct(double[] v1, double[] v2) {
        if (v1.length != v2.length) {
            throw new IllegalArgumentException("Vectors for dot product have to be of same size!");
        }
        double result = 0;
        for (int i = 0; i < v1.length; i++) {
            result += v1[i] * v2[i];
        }
        return result;
    }
}
