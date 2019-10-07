package com.jstappdev.e4client.data;

import android.util.Log;

import com.jstappdev.e4client.MainActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;

public class CSVFile {

    private LinkedList<Double> x = new LinkedList<>();
    private LinkedList<Float> y = new LinkedList<>();

    private double initialTime;

    // same file format for EDA, HR, BVP, TEMP

    public CSVFile(InputStream inputStream) {

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

            // initial time of the session expressed as unix timestamp in UTC
            // and sample rate expressed in Hz.
            initialTime = Double.parseDouble(reader.readLine());
            final double samplingRate = Double.parseDouble(reader.readLine());

            String csvLine;
            int lineNumber = 1;
            while ((csvLine = reader.readLine()) != null) {
                x.add(initialTime + (60d / samplingRate * lineNumber++));
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


    public LinkedList<Double> getX() {
        return this.x;
    }

    public LinkedList<Float> getY() {
        return this.y;
    }

    public double getInitialTime() {
        return initialTime;
    }
}