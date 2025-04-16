package com.example.DataSnatcher.collector.AudioInfo;

import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

public class SignalRecord{
    //麦克风接受信号

    private final int sampleRate;
    private AudioRecord audioRecord;
    private int bufferSize;
    private short[] audioData;

    private boolean isRecording = false;

    @SuppressLint("MissingPermission")
    public SignalRecord(int sampleRate) {
        this.sampleRate = sampleRate;

        bufferSize = AudioRecord.getMinBufferSize(this.sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                this.sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize);
    }

    public void recordAudio(int durationSeconds) {
        audioData = new short[durationSeconds * sampleRate];
        audioRecord.startRecording();
        //audioRecord.read(audioData, 0, audioData.length);

        Thread recordingThread;
        if (isRecording) {
            return;
        }
        isRecording = true;
        recordingThread = new Thread(() -> {
            while (isRecording) {
                int num = audioRecord.read(audioData, 0, audioData.length);
                if(num != 0) {
                    System.out.println("num: " + num);
                }
            }
        });
        recordingThread.start();

        //audioRecord.stop();

        System.out.println("record: down\n");
    }

    public void release() {
        isRecording = false;
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
    }

    public short[] getAudioData() {
        return audioData;
    }

}
