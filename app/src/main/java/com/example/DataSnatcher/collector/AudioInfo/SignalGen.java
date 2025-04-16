package com.example.DataSnatcher.collector.AudioInfo;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

public class SignalGen{
    //扬声器播放信号

    private final int sampleRate; //采样率
    private AudioTrack audioTrack; //用于生成
    private final int[] frequencies; //频率范围：14kHz~21kHz，间隔100Hz

    private boolean isRecording = false;

    public SignalGen(int sampleRate, int[] frequencies) {
        this.sampleRate = sampleRate;
        this.frequencies = frequencies;
    }

    public void playTone(int durationSeconds) {
        int numSamples = durationSeconds * sampleRate;
        double[] sample = new double[numSamples];
        byte[] generatedSound = new byte[2 * numSamples];

        for (int frequency : frequencies) {
            for (int i = 0; i < numSamples; ++i) {
                sample[i] += Math.cos(2 * Math.PI * i / ((double)sampleRate / (double)frequency));
            }
        }
        for (int i = 0; i < numSamples; ++i) {
            sample[i] /= frequencies.length; //归一化
        }

        // 余弦波转为PCM格式数据
        int idx = 0;
        for (final double dVal : sample) {
            final short val = (short) ((dVal * 32767));
            generatedSound[idx++] = (byte) (val & 0x00ff);
            generatedSound[idx++] = (byte) ((val & 0xff00) >>> 8);
        }

        audioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                generatedSound.length,
                AudioTrack.MODE_STATIC
        );

        audioTrack.write(generatedSound, 0, generatedSound.length);
        audioTrack.play();

        System.out.println("play: down\n");
    }

    public void stopAudio() {
        if (audioTrack != null) {
            audioTrack.stop();
            audioTrack.release();
            audioTrack = null;
        }
    }

}
