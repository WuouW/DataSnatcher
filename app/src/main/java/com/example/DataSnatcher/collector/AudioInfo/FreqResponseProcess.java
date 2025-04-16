package com.example.DataSnatcher.collector.AudioInfo;

import org.jtransforms.fft.DoubleFFT_1D;

public class FreqResponseProcess {
    //计算频率响应，提取特征

    private final int sampleRate;

    private final int[] frequencies;
    private double[] stimulusAmplitude;

    public FreqResponseProcess(int sampleRate, int[] frequencies) {
        this.sampleRate = sampleRate;
        this.frequencies = frequencies;

        // 初始化刺激信号的频谱
        int numFrequencies = this.frequencies.length;
        stimulusAmplitude = new double[numFrequencies];
        for (int i = 0; i < numFrequencies; i++) {
            stimulusAmplitude[i] = 1.0; // 刺激信号幅度恒定为1
        }
    }

    public double[] process(short[] audioData) {
        double[] recordedAmplitude = computeFFT(audioData);
        double[] response = computeResponse(recordedAmplitude);
        double[] features = extractFeatures(response);
        return normalize(features);
    }

    private double[] computeFFT(short[] audioData) {
        int n = audioData.length;
        double[] fftData = new double[n * 2];

        //FFT结果为复数
        for (int i = 0; i < n; i++) {
            fftData[2 * i] = audioData[i];
            fftData[2 * i + 1] = 0;
        }

        DoubleFFT_1D fft = new DoubleFFT_1D(n);
        fft.realForwardFull(fftData);

        //只需要前半部分的正频率成分（后半部分为镜像）
        double[] amplitude = new double[n / 2];
        for (int i = 0; i < n / 2; i++) {
            amplitude[i] = Math.sqrt(fftData[2 * i] * fftData[2 * i] + fftData[2 * i + 1] * fftData[2 * i + 1]);
        }

        return amplitude;
    }

    private double[] computeResponse(double[] recordedAmplitude) {
        double[] response = new double[frequencies.length];
        for (int i = 0; i < frequencies.length; i++) {
            //频率对应的索引 = （频率 * 时域信号长度） / 采样率。此处*2是因为时域信号只取了前半部分
            int freqIndex = frequencies[i] * recordedAmplitude.length*2 / sampleRate;
            //response[i] = recordedAmplitude[freqIndex] / stimulusAmplitude[freqIndex];
            response[i] = recordedAmplitude[freqIndex] / stimulusAmplitude[i];
        }
        return response;
    }

    private double[] extractFeatures(double[] frequencyResponse) {
        //有效点就是这些对应频率部分的点
        return frequencyResponse;
    }

    private double[] normalize(double[] features) {
        double max = Double.NEGATIVE_INFINITY;
        double min = Double.POSITIVE_INFINITY;

        for (double feature : features) {
            if (feature > max) {
                max = feature;
            }
            if (feature < min) {
                min = feature;
            }
        }

        double[] normalized = new double[features.length];
        for (int i = 0; i < features.length; i++) {
            normalized[i] = (features[i] - min) / (max - min);
        }

        return normalized;
    }
}
