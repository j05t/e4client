package com.jstappdev.e4client.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class CSVFile {

    private ArrayList<Double> x = new ArrayList<>();
    private ArrayList<Float> y = new ArrayList<>();

    private double initialTime;
    private double samplingRate;

    // same file format for EDA, HR, BVP, TEMP

    public CSVFile(InputStream inputStream) {

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

            String line = reader.readLine();

            if (null == line) return;

            // initial time of the session expressed as unix timestamp in UTC
            // and sample rate expressed in Hz.
            initialTime = Double.parseDouble(line);
            samplingRate = 1d / Double.parseDouble(reader.readLine());

            String csvLine;
            int lineNumber = 0;
            while ((csvLine = reader.readLine()) != null) {
                x.add(initialTime + (samplingRate * lineNumber++));
                y.add(Float.parseFloat(csvLine));
            }
        } catch (IOException ex) {
            throw new RuntimeException("Error in reading CSV file: " + ex);
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                throw new RuntimeException("Error while closing input stream: " + e);
            }
        }
    }


    public ArrayList<Double> getX() {
        return this.x;
    }

    public ArrayList<Float> getY() {
        return this.y;
    }

    public double getInitialTime() {
        return initialTime;
    }

    public double getSamplingRate() {
        return samplingRate;
    }
}