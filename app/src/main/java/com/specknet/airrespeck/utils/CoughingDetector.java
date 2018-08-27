package com.specknet.airrespeck.utils;

import android.util.Log;

import com.specknet.airrespeck.models.LogisticRegression;

import java.util.Arrays;
import java.util.LinkedList;

public class CoughingDetector {
    /*
    LinkedList<Complex> xs = new LinkedList<>();
    LinkedList<Complex> ys = new LinkedList<>();
    LinkedList<Complex> zs = new LinkedList<>();
    */
    LinkedList<Double> xs = new LinkedList<>();
    LinkedList<Double> ys = new LinkedList<>();
    LinkedList<Double> zs = new LinkedList<>();

    private static final int BUFFER_LENGTH = 25;

    public CoughingDetector() {
    }

    // Add new acceleration values to queue
    public void updateCoughing(float x, float y, float z) {
        /*xs.addLast(new Complex(x, 0));
        ys.addLast(new Complex(y, 0));
        zs.addLast(new Complex(z, 0));
        */
        xs.addLast((double) x);
        ys.addLast((double) y);
        zs.addLast((double) z);

        if (xs.size() > BUFFER_LENGTH) {
            xs.removeFirst();
            ys.removeFirst();
            zs.removeFirst();
        }
    }

    public double predictIsCouhging() {
        // If the buffer isn't full yet, return NaN
        if (xs.size() < BUFFER_LENGTH) {
            return 0.0;
        }

        // Calculate features
        double[] features = getFeatures();

        double[] coefficients = {2.14402996, 1.73683485};
        double intercept = -2.99343679;

        LogisticRegression reg = new LogisticRegression(coefficients, intercept);
        double estimation = reg.predictProba(features);

        Double[] zsList = zs.toArray(new Double[BUFFER_LENGTH]);
        ElementWithIndex maxDiffZ = maxAbsoluteDifference(zsList);

        Log.i("DF", "max diff z: " + maxDiffZ.getElement());

        return estimation;
    }

    /*
    private double[] getFeatures() {
        // If the buffer isn't full yet, return NaN
        if (xs.size() < BUFFER_LENGTH) {
            return new double[]{0, 0, 0, 0, 0, 0};
        }

        // Frequency values of values in spectrum. These were copied over from Python
        double[] frequencies = new double[]{0., 0.2016129, 0.40322581, 0.60483871, 0.80645161,
                1.00806452, 1.20967742, 1.41129032, 1.61290323, 1.81451613,
                2.01612903, 2.21774194, 2.41935484, 2.62096774, 2.82258065,
                3.02419355, 3.22580645, 3.42741935, 3.62903226, 3.83064516,
                4.03225806, 4.23387097, 4.43548387, 4.63709677, 4.83870968,
                5.04032258, 5.24193548, 5.44354839, 5.64516129, 5.84677419,
                6.0483871, 6.25};

        double[] means = new double[]{0.03874848, 2.83127713, 0.04635831, 14.60852182,
                0.03554058, 26.89250628};
        double[] stds = new double[]{0.03055302, 1.3347803, 0.08273226, 2.78647782, 0.05055122,
                1.50701587};

        Complex[] xsComplex = new Complex[64];
        Complex[] ysComplex = new Complex[64];
        Complex[] zsComplex = new Complex[64];

        // Get queues into array format
        xs.toArray(xsComplex);
        ys.toArray(ysComplex);
        zs.toArray(zsComplex);

        // Calculate the FFT spectra
        double[] fft_x = fftInRightFormat(xsComplex);
        double[] fft_y = fftInRightFormat(xsComplex);
        double[] fft_z = fftInRightFormat(xsComplex);

        ElementWithIndex elemIdxX, elemIdxY, elemIdxZ;
        double firstRangeEnegery, firstRangePosition, secondRangeEnegery, secondRangePosition,
                thirdRangeEnegery, thirdRangePosition;

        // Now that we have the spectra, extract the features
        elemIdxX = getMaxWithIndex(Arrays.copyOfRange(fft_x, 1, 8), 1);
        elemIdxY = getMaxWithIndex(Arrays.copyOfRange(fft_y, 1, 8), 1);
        elemIdxZ = getMaxWithIndex(Arrays.copyOfRange(fft_z, 1, 8), 1);

        firstRangeEnegery = (Math.sqrt(
                Math.pow(elemIdxX.element, 2) + Math.pow(elemIdxY.element, 2) + Math.pow(elemIdxZ.element,
                        2)) - means[0]) / stds[0];
        firstRangePosition = ((elemIdxX.index + elemIdxY.index + elemIdxZ.index) / 3. - means[1]) / stds[1];

        elemIdxX = getMaxWithIndex(Arrays.copyOfRange(fft_x, 8, 23), 8);
        elemIdxY = getMaxWithIndex(Arrays.copyOfRange(fft_y, 8, 23), 8);
        elemIdxZ = getMaxWithIndex(Arrays.copyOfRange(fft_z, 8, 23), 8);

        secondRangeEnegery = (Math.sqrt(
                Math.pow(elemIdxX.element, 2) + Math.pow(elemIdxY.element, 2) + Math.pow(elemIdxZ.element,
                        2)) - means[2]) / stds[2];
        secondRangePosition = ((elemIdxX.index + elemIdxY.index + elemIdxZ.index) / 3. - means[3]) / stds[3];

        elemIdxX = getMaxWithIndex(Arrays.copyOfRange(fft_x, 23, 64), 23);
        elemIdxY = getMaxWithIndex(Arrays.copyOfRange(fft_y, 23, 64), 23);
        elemIdxZ = getMaxWithIndex(Arrays.copyOfRange(fft_z, 23, 64), 23);

        thirdRangeEnegery = (Math.sqrt(
                Math.pow(elemIdxX.element, 2) + Math.pow(elemIdxY.element, 2) + Math.pow(elemIdxZ.element,
                        2)) - means[4]) / stds[4];
        thirdRangePosition = ((elemIdxX.index + elemIdxY.index + elemIdxZ.index) / 3. - means[5]) / stds[5];

        return new double[]{firstRangeEnegery, firstRangePosition, secondRangeEnegery, secondRangePosition, thirdRangeEnegery,
                thirdRangePosition};
    }
    */
    private double[] getFeatures() {
        // If the buffer isn't full yet, return NaN
        if (xs.size() < BUFFER_LENGTH) {
            return new double[]{0, 0, 0, 0, 0, 0};
        }

        // Model parameters
        double[] means = new double[]{0.13110132, 0.23785988};
        double[] stds = new double[]{0.14714726, 0.42879234};

        Double[] xsList = xs.toArray(new Double[BUFFER_LENGTH]);
        Double[] ysList = ys.toArray(new Double[BUFFER_LENGTH]);
        Double[] zsList = zs.toArray(new Double[BUFFER_LENGTH]);

        //return new double[]{(maxAbsoluteDifference(zsList) - means[0]) / stds[0]};
        //,
        //       (standardDeviation(xsList) - means[1]) / stds[1]};
        ElementWithIndex maxDiffZ = maxAbsoluteDifference(zsList);
        double diffX, diffY;
        if (maxDiffZ.getIndex() == 0) {
            diffX = 0;
            diffY = 0;
        } else {
            diffX = xsList[maxDiffZ.getIndex()] - xsList[maxDiffZ.getIndex() - 1];
            diffY = ysList[maxDiffZ.getIndex()] - ysList[maxDiffZ.getIndex() - 1];
        }
        return new double[]{(maxDiffZ.getElement() - means[0]) / stds[0],
                (correlation(ysList, zsList) - means[1]) / stds[1]};
    }

    public static double correlation(Double[] xs, Double[] ys) {
        //TODO: check here that arrays are not null, of the same length etc

        double sx = 0.0;
        double sy = 0.0;
        double sxx = 0.0;
        double syy = 0.0;
        double sxy = 0.0;

        int n = xs.length;

        for (int i = 0; i < n; ++i) {
            double x = xs[i];
            double y = ys[i];

            sx += x;
            sy += y;
            sxx += x * x;
            syy += y * y;
            sxy += x * y;
        }

        // covariation
        double cov = sxy / n - sx * sy / n / n;
        // standard error of x
        double sigmax = Math.sqrt(sxx / n - sx * sx / n / n);
        // standard error of y
        double sigmay = Math.sqrt(syy / n - sy * sy / n / n);

        // correlation is just a normalized covariation
        return cov / sigmax / sigmay;
    }

    private double standardDeviation(Double[] array) {
        // Calculate the mean
        double sum = 0;
        for (double a : array) {
            sum += a;
        }
        double mean = sum / array.length;

        // Calculate the Std
        double sumOfSquares = 0;
        for (double a : array) {
            sumOfSquares += Math.pow(a - mean, 2);
        }
        return Math.sqrt(sumOfSquares / array.length);
    }

    private ElementWithIndex maxAbsoluteDifference(Double[] array) {
        double maxDiff = 0;
        int maxIndex = 0;
        for (int i = 1; i < array.length; i++) {
            double newDiff = array[i] - array[i - 1];
            if (newDiff > maxDiff) {
                maxDiff = newDiff;
                maxIndex = i;
            }
        }
        return new ElementWithIndex(maxDiff, maxIndex);
    }

    private ElementWithIndex cougetMaxWithIndex(double[] array, int indexOffset) {
        int maxIdx = 0;
        double maxElement = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] > maxElement) {
                maxIdx = i;
                maxElement = array[i];
            }
        }
        return new ElementWithIndex(maxElement, maxIdx + indexOffset);
    }

    private class ElementWithIndex {
        private double element;
        private int index;

        public ElementWithIndex(double element, int index) {
            this.element = element;
            this.index = index;
        }

        public double getElement() {
            return element;
        }

        public int getIndex() {
            return index;
        }
    }

    private double[] fftInRightFormat(Complex[] input) {
        Complex[] result = FFT.fft(input);
        double[] absolute = new double[32];
        for (int i = 0; i < 32; i++) {
            absolute[i] = result[i].abs() / 32.;
        }
        return absolute;
    }

    // Use for testing
    public static void main(String[] args) {
        double[] example = new double[]{0.00000000e+00, 8.41470985e-03, 9.09297427e-03,
                1.41120008e-03, -7.56802495e-03, -9.58924275e-03,
                -2.79415498e-03, 6.56986599e-03, 9.89358247e-03,
                4.12118485e-03, -5.44021111e-03, -9.99990207e-03,
                -5.36572918e-03, 4.20167037e-03, 9.90607356e-03,
                6.50287840e-03, -2.87903317e-03, -9.61397492e-03,
                -7.50987247e-03, 1.49877210e-03, 9.12945251e-03,
                8.36655639e-03, -8.85130929e-05, -8.46220404e-03,
                -9.05578362e-03};

        CoughingDetector cd = new CoughingDetector();
        for (int i = 0; i < 25; i++) {
            cd.updateCoughing((float) example[i], (float) example[i], (float) example[i]);
        }
        System.out.println(Arrays.toString(cd.getFeatures()));
        System.out.println(cd.predictIsCouhging());
    }
}
