package com.specknet.airrespeck.utils;

public class CoughingNetwork {
    private enum Activation {IDENTITY, LOGISTIC, RELU, TANH, SOFTMAX}

    private Activation hidden;
    private Activation output;
    private double[][] network;
    private double[][][] weights;
    private double[][] bias;

    public CoughingNetwork(String hidden, String output, int[] layers, double[][][] weights, double[][] bias) {
        this.hidden = Activation.valueOf(hidden.toUpperCase());
        this.output = Activation.valueOf(output.toUpperCase());
        this.network = new double[layers.length + 1][];
        for (int i = 0, l = layers.length; i < l; i++) {
            this.network[i + 1] = new double[layers[i]];
        }
        this.weights = weights;
        this.bias = bias;
    }

    public CoughingNetwork(String hidden, String output, int neurons, double[][][] weights, double[][] bias) {
        this(hidden, output, new int[]{neurons}, weights, bias);
    }

    private double[] compute(Activation activation, double[] v) {
        switch (activation) {
            case LOGISTIC:
                for (int i = 0, l = v.length; i < l; i++) {
                    v[i] = 1. / (1. + Math.exp(-v[i]));
                }
                break;
            case RELU:
                for (int i = 0, l = v.length; i < l; i++) {
                    v[i] = Math.max(0, v[i]);
                }
                break;
            case TANH:
                for (int i = 0, l = v.length; i < l; i++) {
                    v[i] = Math.tanh(v[i]);
                }
                break;
            case SOFTMAX:
                double max = Double.NEGATIVE_INFINITY;
                for (double x : v) {
                    if (x > max) {
                        max = x;
                    }
                }
                for (int i = 0, l = v.length; i < l; i++) {
                    v[i] = Math.exp(v[i] - max);
                }
                double sum = 0.;
                for (double x : v) {
                    sum += x;
                }
                for (int i = 0, l = v.length; i < l; i++) {
                    v[i] /= sum;
                }
                break;
        }
        return v;
    }

    public int predict(double[] neurons) {
        this.network[0] = neurons;

        for (int i = 0; i < this.network.length - 1; i++) {
            for (int j = 0; j < this.network[i + 1].length; j++) {
                for (int l = 0; l < this.network[i].length; l++) {
                    this.network[i + 1][j] += this.network[i][l] * this.weights[i][l][j];
                }
                this.network[i + 1][j] += this.bias[i][j];
            }
            if ((i + 1) < (this.network.length - 1)) {
                this.network[i + 1] = this.compute(this.hidden, this.network[i + 1]);
            }
        }
        this.network[this.network.length - 1] = this.compute(this.output, this.network[this.network.length - 1]);

        if (this.network[this.network.length - 1].length == 1) {
            if (this.network[this.network.length - 1][0] > .5) {
                return 1;
            }
            return 0;
        } else {
            int classIdx = 0;
            for (int i = 0; i < this.network[this.network.length - 1].length; i++) {
                classIdx = this.network[this.network.length - 1][i] > this.network[this.network.length - 1][classIdx] ? i : classIdx;
            }
            return classIdx;
        }

    }

    public static void main(String[] args) {
        if (args.length == 12) {

            // Features:
            double[] features = new double[args.length];
            for (int i = 0, l = args.length; i < l; i++) {
                features[i] = Double.parseDouble(args[i]);
            }

            // Parameters:
            int[] layers = {6, 3, 1};
            double[][][] weights = {{{0.525960285821, 0.0327849317139, -0.530614484647, 0.25021810969, -0.363957631234, -0.056873412803}, {-0.347560870503, 0.608261553013, -0.0212959592255, 0.250104276471, -0.238356138334, 0.250993263323}, {-0.920498002729, 1.27878063359, -0.719830263347, -0.285728010763, -0.375512254723, -0.81687957654}, {0.0589064177379, -0.377417198629, 0.326581418689, 0.42395894117, 0.136474314156, 1.01100018251}, {-0.150832811117, 1.08663114655, -0.677354253897, -0.983698740114, -0.700711377309, 0.266695550106}, {-0.182002376334, -0.0349401428987, 0.225007767866, -0.0488715529796, 0.0414772568792, -0.0660264100321}, {-0.0717165780259, 1.01565572975, -0.72653972221, 0.05242827906, 0.324317719718, 0.220936905612}, {-0.816747383741, 1.06131671755, -0.700574619944, -0.566941521265, 0.139358774757, -0.629534254053}, {-0.492985147609, 0.192939055153, -0.718936907607, 0.00531688245464, -0.603892737371, 0.0993903976348}, {-0.53338368767, 0.18712208843, -0.101542480927, -0.860468874601, -0.197262490849, 0.256954288844}, {-0.952052262558, 0.40069040659, -0.0133656134445, -0.50313539808, -0.785289640617, 0.186853373137}, {-0.452472720454, -0.19072825376, 0.239339764801, -0.353634422078, 0.0915385481717, 0.662961482866}}, {{-0.611285213659, 0.753845675313, 1.40136826308}, {-0.323548417846, 1.11770526055, -0.57479968792}, {-0.799476252346, 0.784557362312, 0.15554619228}, {-0.0867362792114, 0.68252966272, 0.942295443513}, {-0.424745980062, 0.749089132353, -0.0810121981309}, {1.13509315569, -0.656237103793, -0.224131178666}}, {{1.46478086218}, {-0.767379433506}, {-0.962860314518}}};
            double[][] bias = {{-0.653420605231, -0.401152128832, -0.507807945032, -0.38193517084, 0.0582428253611, 1.09932300028}, {1.89068749231, -0.309369231322, -0.952150824207}, {1.36661222491}};

            // Prediction:
            CoughingNetwork clf = new CoughingNetwork("relu", "logistic", layers, weights, bias);
            int estimation = clf.predict(features);
            System.out.println(estimation);

        }
    }
}
