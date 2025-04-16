package com.example.DataSnatcher.collector.AudioInfo.Feature;

import java.util.Arrays;

public class MFCC {

    private static final int MEL_FILTER_BANK_SIZE = 40; // 例：梅尔滤波器组的大小
    private static final int MFCC_SIZE = 13; // 例：MFCC系数的大小

    // 计算梅尔频率
    private static double melFrequency(double frequency) {
        return 2595 * Math.log10(1 + frequency / 700);
    }

    // 计算频率到梅尔频率的转换
    private static double[] frequencyToMel(double[] frequencies) {
        return Arrays.stream(frequencies).map(MFCC::melFrequency).toArray();
    }

    // 创建梅尔滤波器组
    private static double[][] createMelFilterBank(int numFilters, double minFreq, double maxFreq, int numFFTPoints) {
        double[] melPoints = new double[numFilters + 2];
        double[] freqPoints = new double[numFilters + 2];
        double[][] filterBank = new double[numFilters][numFFTPoints / 2 + 1];

        for (int i = 0; i < melPoints.length; i++) {
            melPoints[i] = minFreq + i * (maxFreq - minFreq) / (melPoints.length - 1);
            freqPoints[i] = melFrequency(melPoints[i]);
        }

        for (int i = 0; i < numFilters; i++) {
            double left = freqPoints[i];
            double center = freqPoints[i + 1];
            double right = freqPoints[i + 2];

            for (int j = 0; j < numFFTPoints / 2 + 1; j++) {
                double freq = j * (maxFreq / (double)(numFFTPoints / 2));
                if (freq >= left && freq <= center) {
                    filterBank[i][j] = (freq - left) / (center - left);
                } else if (freq > center && freq <= right) {
                    filterBank[i][j] = (right - freq) / (right - center);
                } else {
                    filterBank[i][j] = 0;
                }
            }
        }

        return filterBank;
    }

    // 计算 MFCC
    public static double[] computeMFCC(int[] frequencies, double[] freqSignal) {
        int numFFTPoints = frequencies.length;
        double minFreq = 0;
        double maxFreq = (double) frequencies[numFFTPoints / 2];
        double[][] melFilterBank = createMelFilterBank(MEL_FILTER_BANK_SIZE, minFreq, maxFreq, numFFTPoints);

        // 应用梅尔滤波器
        double[] melSpectrogram = new double[MEL_FILTER_BANK_SIZE];
        for (int i = 0; i < MEL_FILTER_BANK_SIZE; i++) {
            for (int j = 0; j < numFFTPoints / 2 + 1; j++) {
                melSpectrogram[i] += freqSignal[j] * melFilterBank[i][j];
            }
        }

        // 对数处理
        for (int i = 0; i < MEL_FILTER_BANK_SIZE; i++) {
            melSpectrogram[i] = Math.log(melSpectrogram[i] + 1e-10); // 加1e-10避免对数0
        }

        // 离散余弦变换（DCT）
        double[] mfcc = new double[MFCC_SIZE];
        for (int i = 0; i < MFCC_SIZE; i++) {
            double sum = 0;
            for (int j = 0; j < MEL_FILTER_BANK_SIZE; j++) {
                sum += melSpectrogram[j] * Math.cos(Math.PI * i / MEL_FILTER_BANK_SIZE * (j + 0.5));
            }
            mfcc[i] = sum;
        }

        return mfcc;
    }

}
