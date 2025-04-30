package com.example.DataSnatcher.collector.AudioInfo;

import android.content.Context;
import android.media.AudioManager;

import com.example.DataSnatcher.Util;
import com.example.DataSnatcher.collector.AudioInfo.Feature.AudioFeature;
import com.example.DataSnatcher.collector.IInfoCollector;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class AudioInfoCollector implements IInfoCollector {
    private final int sampleRate; //采样率

    private final int[] frequencies; //频率范围：14kHz~21kHz

    private Context context;

    public AudioInfoCollector(Context context) {
        this.context = context;

        sampleRate = 44100;

        //int startFrequency = 14000;
        int startFrequency = 14000;
        int endFrequency = 21000;
        int interval = 100;
        int numFrequencies = (endFrequency-startFrequency) / interval + 1;
        frequencies = new int[numFrequencies];
        int i = 0;
        for (int freq = startFrequency; freq <= endFrequency; freq += interval) {
            frequencies[i++] = freq;
        }

        System.out.println("init: down\n");
    }

    @Override
    public String getCategory() {
        return "音频信息";
    }

    @Override
    public JSONObject collect() {
        //TODO
        SignalGen signalGen = new SignalGen(sampleRate, frequencies);
        SignalRecord signalRecord = new SignalRecord(sampleRate);

        System.out.println("init gen and record: down\n");

        CyclicBarrier barrier = new CyclicBarrier(2); // 屏障数（用来保障同步）

        Thread threadPlay = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    barrier.await();
                } catch (BrokenBarrierException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
                signalGen.playTone(10);
            }
        });
        Thread threadRecord = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    barrier.await();
                } catch (BrokenBarrierException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
                signalRecord.recordAudio(1);
            }
        });

        threadPlay.start();
        threadRecord.start();

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        try {
            threadPlay.join();
            threadRecord.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        signalGen.stopAudio();
        signalRecord.release();

        //处理频率响应生成的其他部分
        short[] recordedAudio = signalRecord.getAudioData();
        FreqResponseProcess processor = new FreqResponseProcess(sampleRate, frequencies);
        double[] normalizedFeatures = processor.process(recordedAudio);

        /*
        for(int i = 0; i < recordedAudio.length; i++){
            if(i < 5000)
                System.out.println("audio " + i + " : " + recordedAudio[i]);
        }*/

        AudioFeature AF = new AudioFeature(recordedAudio, normalizedFeatures, frequencies);
        JSONObject audioFeature = AF.calFeature();

        //打印归一化后的特征向量
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        JSONObject audioInfo = new JSONObject();
        try {
            audioInfo.put("frequency", Arrays.toString(normalizedFeatures));
            audioInfo = Util.merge(audioInfo, audioFeature);
            audioInfo.put("max volumn", new int[] {
                    audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL),
                    audioManager.getStreamMaxVolume(AudioManager.STREAM_SYSTEM),
                    audioManager.getStreamMaxVolume(AudioManager.STREAM_RING),
                    audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
                    audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM),
                    audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION),
                    audioManager.getStreamMaxVolume(AudioManager.STREAM_DTMF)
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return audioInfo;
    }

}
