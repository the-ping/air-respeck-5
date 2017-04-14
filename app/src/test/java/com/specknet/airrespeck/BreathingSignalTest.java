package com.specknet.airrespeck;

import com.specknet.airrespeck.services.SpeckBluetoothService;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Locale;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

/**
 * Class for verifying that the breathing signal generated by the C code stays the same
 * Created by Darius
 */

public class BreathingSignalTest {

    private static final String directory = "C:/Users/Darius/Dropbox/Studium/ArbeitArvind/Projects/Western General" +
            "/RESpeck vs. Cannula/Comparison data breathing code/";
    private static final String rawModelFilePath = directory + "model_accel.csv";
    private static final String measuresModelFilePath = directory + "model_measures.csv";

    @Test
    public void allBreathingMeasuresSame() {
        ArrayList<Float[]> allMeasures = calculateMeasuresBasedOnCurrentLibrary(loadAccel(rawModelFilePath, 1, ","));
        ArrayList<Float[]> modelMeasures = loadModelMeasures(measuresModelFilePath);

        assertThat(allMeasures.size(), is(equalTo(modelMeasures.size())));

        for (int i = 0; i < modelMeasures.size(); i++) {
            for (int j = 0; j < modelMeasures.get(i).length; j++) {
                if (allMeasures.get(i)[j].equals(Float.NaN)) {
                    assertThat(modelMeasures.get(i)[j], is(Float.NaN));
                } else {
                    assertThat(String.format(Locale.UK, "in: %d, %d", i, j), allMeasures.get(i)[j],
                            is(equalTo(modelMeasures.get(i)[j])));
                }
            }
        }
    }

    @Test
    public void allBreathingMeasuresSimilar() {
        ArrayList<Float[]> allMeasures = calculateMeasuresBasedOnCurrentLibrary(loadAccel(rawModelFilePath, 1, ","));
        ArrayList<Float[]> modelMeasures = loadModelMeasures(measuresModelFilePath);

        assertThat(allMeasures.size(), is(equalTo(modelMeasures.size())));

        for (int i = 0; i < modelMeasures.size(); i++) {
            for (int j = 0; j < modelMeasures.get(i).length; j++) {
//                System.out.print(Double.toString(allMeasures.get(i)[j] - modelMeasures.get(i)[j]) + "\t");
                if (modelMeasures.get(i)[j].equals(Float.NaN)) {
                    assertThat(String.format(Locale.UK, "in: %d, %d", i, j), modelMeasures.get(i)[j], is(Float.NaN));
                } else {
                    assertThat(String.format(Locale.UK, "in: %d, %d", i, j), (double) allMeasures.get(i)[j],
                            is(closeTo(modelMeasures.get(i)[j], 1E-5f)));
                }
            }
//            System.out.println();
        }
    }

    @Test
    public void generateAndSaveMeasuresFromAccelFiles() {
        String pathAccelDir = "C:\\Users\\Darius\\Dropbox\\Studium\\ArbeitArvind\\Projects\\Western General" +
                "\\data\\respeck\\";
        String pathOutputFileMeasures = "C:\\Users\\Darius\\Dropbox\\Studium\\ArbeitArvind\\Projects\\Western General" +
                "\\data\\breathing measures\\";

        File[] listOfFiles = new File(pathAccelDir).listFiles();
        SpeckBluetoothService service = new SpeckBluetoothService();

        // Open each file, generate the breathing signal for it, and write the result into another file
        for (File file : listOfFiles) {
            service.initBreathing();
            ArrayList<Float[]> accelValues = loadAccel(file.getAbsolutePath(), 0, "\t");
            ArrayList<Float[]> allMeasures = new ArrayList<>();

            Float[] measures;
            for (Float[] accelVector : accelValues) {
                measures = new Float[5];
                service.updateBreathing(accelVector[0], accelVector[1], accelVector[2]);
                measures[0] = service.getBreathingSignal();
                measures[1] = service.getBreathingRate();
                measures[2] = service.getBreathingAngle();
                measures[3] = service.getLowerThreshold();
                measures[4] = service.getUpperThreshold();
                allMeasures.add(measures);
            }
            saveMeasures(allMeasures, pathOutputFileMeasures + file.getName());
        }
    }

    private static void saveOneMeasure(ArrayList<Float> breathingSignal, String filePath) {
        File file = new File(filePath);

        try {
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
            for (Float signalValue : breathingSignal) {
                out.write(Float.toString(signalValue) + "\n");
            }
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private ArrayList<Float[]> calculateMeasuresBasedOnCurrentLibrary(ArrayList<Float[]> accelValues) {
        SpeckBluetoothService service = new SpeckBluetoothService();
        service.initBreathing();

        ArrayList<Float[]> allMeasures = new ArrayList<>();

        for (Float[] accelVector : accelValues) {
            Float[] measures = new Float[7];
            service.updateBreathing(accelVector[0], accelVector[1], accelVector[2]);
            measures[0] = service.getBreathingSignal();
            measures[1] = service.getBreathingAngle();
            measures[2] = service.getBreathingRate();
            measures[3] = service.getActivityLevel();
            service.calculateAverageBreathing();
            measures[4] = service.getAverageBreathingRate();
            measures[5] = service.getStdDevBreathingRate();
            measures[6] = (float) service.getNumberOfBreaths();
            allMeasures.add(measures);
        }
        return allMeasures;
    }

    // This is used to store gold standard data which to compare to new data.
    // Should be called with with the .dll which definitely works
    @Test
    public void storeModelData() {
        ArrayList<Float[]> allMeasures = calculateMeasuresBasedOnCurrentLibrary(loadAccel(rawModelFilePath, 1, ","));
        saveMeasures(allMeasures, measuresModelFilePath);
    }

    // Load sample acceleration data
    private static ArrayList<Float[]> loadAccel(String filename, int accelStartIdx, String delimiter) {
        File accelFile = new File(filename);

        ArrayList<Float[]> accelValues = new ArrayList<>();
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(accelFile)));
            // Skip header
            in.readLine();

            String line;
            while ((line = in.readLine()) != null) {
                String[] accelVectorString = line.split(delimiter);
                if (accelVectorString.length >= 3) {
                    Float[] accelVector = new Float[]{Float.parseFloat(accelVectorString[accelStartIdx]),
                            Float.parseFloat(accelVectorString[accelStartIdx + 1]), Float.parseFloat(
                            accelVectorString[accelStartIdx + 2])};
                    accelValues.add(accelVector);
                }
            }
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return accelValues;
    }

    // Save all breathing measures in file
    private static void saveMeasures(ArrayList<Float[]> allMeasures, String filename) {
        File file = new File(filename);

        try {
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
            for (Float[] measures : allMeasures) {
                StringBuilder stringBuilder = new StringBuilder();
                for (int i = 0; i < measures.length - 1; i++) {
                    stringBuilder.append(measures[i]).append(",");
                }
                stringBuilder.append(measures[measures.length - 1]).append("\n");
                out.write(stringBuilder.toString());
            }
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Load all breathing measures from file
    private static ArrayList<Float[]> loadModelMeasures(String filename) {
        File accelFile = new File(filename);

        ArrayList<Float[]> allMeasures = new ArrayList<>();
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(accelFile)));

            String line;
            while ((line = in.readLine()) != null) {
                Float[] measures = new Float[7];
                String[] measuresString = line.split(",");
                for (int i = 0; i < measuresString.length; i++) {
                    measures[i] = Float.parseFloat(measuresString[i]);
                }
                allMeasures.add(measures);
            }
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return allMeasures;
    }

}


