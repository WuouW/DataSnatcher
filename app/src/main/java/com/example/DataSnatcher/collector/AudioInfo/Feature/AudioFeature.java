package com.example.DataSnatcher.collector.AudioInfo.Feature;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

public class AudioFeature {
    private final short[] timeSignal;
    private final double[] freqSignal;
    private final int[] frequencies; // freSignal和frequencies长度一致

    // cache
    private double RMS;
    private double[] w;
    private double miu;
    private double sigma;

    public AudioFeature(short[] timeSignal, double[] freqSignal, int[] frequencies){
        this.timeSignal = timeSignal;
        this.freqSignal = freqSignal;
        this.frequencies = frequencies;
    }

    public JSONObject calFeature(){
        JSONObject audioFeature = new JSONObject();
        try {
            audioFeature.put("RmsEnergy", getRmsEnergy());
            audioFeature.put("ZCR", getZCR());
            audioFeature.put("LowEnergyRate", getLowEnergyRate());
            audioFeature.put("SpectralCentroid", getSpectralCentroid());
            audioFeature.put("SpectralEntropy", getSpectralEntropy());
            audioFeature.put("SpectralIrregularity", getSpectralIrregularity());
            audioFeature.put("SpectralSpread", getSpectralSpread());
            audioFeature.put("SpectralSkewness", getSpectralSkewness());
            audioFeature.put("SpectralKurtosis", getSpectralKurtosis());
            audioFeature.put("SpectralRolloff", getSpectralRolloff());
            audioFeature.put("MFCC", Arrays.toString(getMFCC()));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return audioFeature;
    }

    private double getRmsEnergy(){
        double sumOfSquares = 0;
        for (int num : timeSignal) {
            sumOfSquares += num * num;
        }
        double meanSquare = sumOfSquares / timeSignal.length;
        RMS = Math.sqrt(meanSquare);
        return RMS;
    }

    private double getZCR(){
        int[] ZR = new int[timeSignal.length];
        double sumOfZR = 0;
        for(int i = 0; i < ZR.length; i++){
            ZR[i] = (timeSignal[i] > 0) ? 1 : 0;
            if(i != 0) {
                sumOfZR += Math.abs(ZR[i] - ZR[i-1]);
            }
        }
        return sumOfZR / ZR.length;
    }

    private double getLowEnergyRate(){
        double cnt = 0;
        double len = timeSignal.length;
        for(double signal : timeSignal){
            if(signal < RMS){
                cnt += 1;
            }
        }
        return cnt / len;
    }

    private double getSpectralCentroid(){
        double sumFreqAmplitude = 0;
        double sumAmplitude = 0;
        for(int i = 0; i < frequencies.length; i++){
            sumFreqAmplitude += freqSignal[i] * frequencies[i];
            sumAmplitude += freqSignal[i];
        }

        miu = sumFreqAmplitude / sumAmplitude;

        return miu;
    }

    private double getSpectralEntropy(){
        double[] w = new double[freqSignal.length];
        double sum = 0;
        double SE = 0;
        for (double signal : freqSignal) {
            sum += signal;
        }
        for(int i = 0; i < w.length; i++){
            w[i] = freqSignal[i] / sum;
        }
        for (double signal : w) {
            if(signal != 0) {
                SE += signal * (Math.log(signal) / Math.log(2));
            }
        }

        this.w = w;

        return SE;
    }

    private double getSpectralIrregularity(){
        double sumSquaredDifferences = 0;
        double sumSquared = 0;
        for(int i = 0; i < freqSignal.length; i++){
            sumSquared += freqSignal[i] * freqSignal[i];
            if(i != freqSignal.length - 1){
                sumSquaredDifferences += (freqSignal[i+1] - freqSignal[i]) * (freqSignal[i+1] - freqSignal[i]);
            } else{
                sumSquaredDifferences += freqSignal[i] * freqSignal[i];
            }
        }
        return sumSquaredDifferences / sumSquared;
    }

    private double getSpectralSpread(){
        double sum = 0;
        for(int i = 0; i < freqSignal.length; i++){
            sum += Math.pow(frequencies[i] - miu, 2) * w[i];
        }
        sigma = Math.sqrt(sum);
        return sigma;
    }

    private double getSpectralSkewness(){
        double sum = 0;
        for(int i = 0; i < freqSignal.length; i++){
            sum += Math.pow(frequencies[i] - miu, 3) * w[i];
        }
        return sum / Math.pow(sigma, 3);
    }

    private double getSpectralKurtosis(){
        double sum = 0;
        for(int i = 0; i < freqSignal.length; i++){
            sum += Math.pow(frequencies[i] - miu, 4) * w[i];
        }
        return sum / Math.pow(sigma, 4);
    }

    private double getSpectralRolloff(){
        double sum = 0;
        for(double signal : freqSignal){
            sum += signal;
        }
        double standard = sum * 0.85;

        double sumPartial = sum;
        int SR = 0;
        for(int i = freqSignal.length-1; i >= 0; i--){
            sumPartial -= freqSignal[i];
            if(sumPartial < standard) {
                SR = i;
                break;
            }
        }
        return SR;
    }

    private double[] getMFCC(){
        return MFCC.computeMFCC(frequencies, freqSignal);
    }

}
