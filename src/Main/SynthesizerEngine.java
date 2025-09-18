import javax.sound.sampled.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class SynthesizerEngine {
    private static final int SAMPLE_RATE = 44100;
    private static final AudioFormat AUDIO_FORMAT = new AudioFormat(SAMPLE_RATE, 16, 1, true, true);
    private static final double MASTER_VOLUME = 0.9;
    private final java.util.function.Consumer<byte[]> onAudioChunkGenerated;
    private final PhoneticTranscriber transcriber = new PhoneticTranscriber();
    private static final Map<String, Phoneme> phonemePrototypes = new HashMap<>();
    private static final Random random = new Random();

    static {
        phonemePrototypes.put("iy", new Phoneme(200, new double[]{270, 2290, 3010}));
        phonemePrototypes.put("i_short", new Phoneme(150, new double[]{390, 1990, 2550}));
        phonemePrototypes.put("e_short", new Phoneme(150, new double[]{530, 1840, 2480}));
        phonemePrototypes.put("a_short", new Phoneme(180, new double[]{660, 1720, 2410}));
        phonemePrototypes.put("schwa", new Phoneme(80, new double[]{500, 1500, 2450}));
        phonemePrototypes.put("u_short", new Phoneme(150, new double[]{440, 1020, 2240}));
        phonemePrototypes.put("u_long", new Phoneme(250, new double[]{300, 870, 2240}));
        phonemePrototypes.put("o_short", new Phoneme(150, new double[]{570, 840, 2410}));
        phonemePrototypes.put("o_long", new Phoneme(250, new double[]{570, 840, 2410}, new double[]{400, 800, 2200}));
        phonemePrototypes.put("aw", new Phoneme(250, new double[]{660, 1720, 2410}, new double[]{300, 870, 2240}));
        phonemePrototypes.put("oy", new Phoneme(250, new double[]{400, 850, 2300}, new double[]{390, 1990, 2550}));
        phonemePrototypes.put("ay", new Phoneme(250, new double[]{750, 1720, 2410}, new double[]{390, 1990, 2550}));
        phonemePrototypes.put("ar", new Phoneme(220, new double[]{660, 1220, 2410}, new double[]{490, 1350, 1690}));
        phonemePrototypes.put("er", new Phoneme(180, new double[]{490, 1350, 1690}));
        phonemePrototypes.put("r", new Phoneme(120, new double[]{500, 1500, 2450}, new double[]{490, 1350, 1690}));
        phonemePrototypes.put("l", new Phoneme(100, new double[]{360, 1300, 2700}));
        phonemePrototypes.put("w", new Phoneme(80, new double[]{300, 600, 2240}));
        phonemePrototypes.put("y", new Phoneme(80, new double[]{270, 2000, 3010}));
        phonemePrototypes.put("m", new Phoneme(120, new double[]{300, 1100, 2300}));
        phonemePrototypes.put("n", new Phoneme(100, new double[]{300, 1400, 2500}));
        phonemePrototypes.put("h", new Phoneme(60, Phoneme.Type.FRICATIVE, 0.2, 500, 10000, 1.0, false));
        phonemePrototypes.put("f", new Phoneme(100, Phoneme.Type.FRICATIVE, 0.4, 4000, 9000, 1.5, false));
        phonemePrototypes.put("v", new Phoneme(100, Phoneme.Type.FRICATIVE, 0.4, 3000, 8000, 1.5, true));
        phonemePrototypes.put("s", new Phoneme(150, Phoneme.Type.FRICATIVE, 0.5, 6000, 10000, 2.0, false));
        phonemePrototypes.put("z", new Phoneme(150, Phoneme.Type.FRICATIVE, 0.5, 5500, 9500, 2.0, true));
        phonemePrototypes.put("sh", new Phoneme(150, Phoneme.Type.FRICATIVE, 0.3, 2500, 7000, 1.2, false));
        phonemePrototypes.put("zh", new Phoneme(150, Phoneme.Type.FRICATIVE, 0.3, 2000, 6000, 1.2, true));
        phonemePrototypes.put("th", new Phoneme(120, Phoneme.Type.FRICATIVE, 0.2, 5000, 9000, 1.8, false));
        phonemePrototypes.put("b", new Phoneme(40, Phoneme.Type.PLOSIVE, 0.8, 500, 1.0, true));
        phonemePrototypes.put("d", new Phoneme(40, Phoneme.Type.PLOSIVE, 0.9, 3500, 1.5, true));
        phonemePrototypes.put("g", new Phoneme(50, Phoneme.Type.PLOSIVE, 0.8, 1500, 1.2, true));
        phonemePrototypes.put("p", new Phoneme(40, Phoneme.Type.PLOSIVE, 0.8, 700, 1.0, false));
        phonemePrototypes.put("t", new Phoneme(40, Phoneme.Type.PLOSIVE, 0.9, 4500, 1.5, false));
        phonemePrototypes.put("k", new Phoneme(50, Phoneme.Type.PLOSIVE, 0.9, 1800, 1.2, false));
        phonemePrototypes.put("ch", new Phoneme(160, Phoneme.Type.AFFRICATE, false));
        phonemePrototypes.put("j", new Phoneme(160, Phoneme.Type.AFFRICATE, true));
        phonemePrototypes.put("_", new Phoneme(150, Phoneme.Type.SILENCE));
    }

    public SynthesizerEngine(java.util.function.Consumer<byte[]> onAudioChunk) { this.onAudioChunkGenerated = onAudioChunk; }

    public void speak(String text) {
        List<float[]> audioSequence = new ArrayList<>();
        List<String> phonemeCodes = new ArrayList<>();
        for (String word : text.toLowerCase().trim().split("[\\s\\p{Punct}]+")) {
            if(!word.isEmpty()) { phonemeCodes.addAll(transcriber.transcribe(word)); phonemeCodes.add("_"); }
        }
        if (phonemeCodes.isEmpty()) return;
        for(int i=0; i < phonemeCodes.size(); i++) {
            String code = phonemeCodes.get(i);
            Phoneme proto = phonemePrototypes.get(code);
            if(proto != null) {
                double ratio = Math.min(1.0, (double)i / Math.max(1, phonemeCodes.size() - 2));
                int pitch = (int)(110 - (30 * ratio));
                Phoneme nextPhoneme = (i + 1 < phonemeCodes.size()) ? phonemePrototypes.get(phonemeCodes.get(i + 1)) : null;
                audioSequence.add(proto.generate(pitch, nextPhoneme));
            }
        }
        float[] finalAudio = stitchAudio(audioSequence, 6);
        normalize(finalAudio, MASTER_VOLUME);
        byte[] finalAudioBytes = floatTo16BitByteArray(finalAudio);
        playAudio(finalAudioBytes);
    }

    private static float[] stitchAudio(List<float[]> seq, int ms) {
        int cs = (int)(SAMPLE_RATE * (ms / 1000.0));
        if (seq.isEmpty()) return new float[0];
        int total = seq.stream().mapToInt(a -> a.length).sum();
        float[] out = new float[total]; int pos = 0;
        for (float[] clip : seq) {
            int blendStart = Math.max(0, pos - cs);
            int blendEnd = Math.min(pos, blendStart + cs);
            for (int j = blendStart; j < blendEnd; j++) {
                float ratio = (float)(j - blendStart) / cs;
                int clipIndex = j - blendStart;
                if (clipIndex < clip.length) { out[j] = (out[j] * (1.0f - ratio) + clip[clipIndex] * ratio); }
            }
            int copyStart = blendEnd - blendStart;
            int remainingLength = clip.length - copyStart;
            if (remainingLength > 0 && pos + remainingLength <= out.length) { System.arraycopy(clip, copyStart, out, pos, remainingLength); }
            pos += remainingLength;
        }
        float[] trimmedOut = new float[pos];
        System.arraycopy(out, 0, trimmedOut, 0, pos);
        return trimmedOut;
    }

    private void playAudio(byte[] audioData) {
        try (SourceDataLine line = AudioSystem.getSourceDataLine(AUDIO_FORMAT)) {
            line.open(AUDIO_FORMAT); line.start();
            int chunkSize = 2048;
            for (int i = 0; i < audioData.length; i += chunkSize) {
                int len = Math.min(chunkSize, audioData.length - i);
                byte[] chunk = java.util.Arrays.copyOfRange(audioData, i, i + len);
                onAudioChunkGenerated.accept(chunk); line.write(chunk, 0, len);
            }
            line.drain();
        } catch (LineUnavailableException e) { e.printStackTrace(); }
    }

    private static class Phoneme {
        enum Type { VOWEL, PLOSIVE, FRICATIVE, AFFRICATE, SILENCE }
        Type type; int durationMs; double[] startFormants, endFormants;
        double amplitude, noiseFreq1, noiseFreq2, noiseQ; boolean voiced;
        Phoneme(int d, double[] f){this(d,f,f);}
        Phoneme(int d, double[] sf, double[] ef){this.type=Type.VOWEL; this.durationMs=d; this.startFormants=sf; this.endFormants=ef;}
        Phoneme(int d, Type t, boolean v){this.type=t; this.durationMs=d; this.voiced=v;}
        Phoneme(int d, Type t, double a, double nf1, double nf2, double q, boolean v){this.type=t; this.durationMs=d; this.amplitude=a; this.noiseFreq1=nf1; this.noiseFreq2=nf2; this.noiseQ=q; this.voiced=v;}
        Phoneme(int d, Type t, double a, double nf1, double q, boolean v){this.type=t; this.durationMs=d; this.amplitude=a; this.noiseFreq1=nf1; this.noiseQ=q; this.voiced=v;}
        Phoneme(int d, Type t){this.type=t; this.durationMs=d;}

        public float[] generate(int fundamental, Phoneme nextPhoneme) {
            int nS = (int)(SAMPLE_RATE * (durationMs / 1000.0));
            switch(type) {
                case VOWEL: return synthVowel(nS, fundamental, startFormants, endFormants);
                case PLOSIVE: return synthPlosive(nS, fundamental, amplitude, noiseFreq1, noiseQ, voiced, nextPhoneme);
                case FRICATIVE: return synthFricative(nS, fundamental, amplitude, noiseFreq1, noiseFreq2, noiseQ, voiced);
                case AFFRICATE:
                    Phoneme plosive = phonemePrototypes.get(voiced ? "d" : "t");
                    Phoneme fricative = phonemePrototypes.get(voiced ? "zh" : "sh");
                    return stitchAudio(List.of(plosive.generate(fundamental, fricative), fricative.generate(fundamental, null)), 2);
                default: return new float[nS];
            }
        }
    }

    private static float[] generateVoicedSource(int numSamples, int fundamental, double[] envelope) {
        float[] source = new float[numSamples]; double phase = 0.0;
        double period = (double) SAMPLE_RATE / fundamental;
        for (int i = 0; i < numSamples; i++) {
            phase = (phase + 1) % period;
            source[i] = (float) (((phase / period) * 2.0 - 1.0) * envelope[i]);
        }
        return source;
    }

    private static float[] synthVowel(int nS, int fund, double[] sF, double[] eF) {
        double[] envelope = createEnvelope(nS, 5, 10);
        float[] src = generateVoicedSource(nS, fund, envelope);
        List<BiquadFilter> filters = new ArrayList<>();
        for (double freq : sF) filters.add(new BiquadFilter(SAMPLE_RATE));
        float[] filt = new float[nS];
        for(int i=0; i<nS; i++) {
            double r = (double)i / nS; double sample = src[i];
            for(int j=0; j<filters.size(); j++) {
                double currentFreq = sF[j] + (eF[j] - sF[j]) * r;
                filters.get(j).recalculate(BiquadFilter.Type.BAND_PASS, currentFreq, 10.0);
                sample = filters.get(j).process(sample);
            }
            filt[i] = (float)sample;
        }
        normalize(filt, 1.0);
        return filt;
    }

    private static float[] synthFricative(int nS, int fund, double amp, double f1, double f2, double q, boolean voiced) {
        float[] mixedNoise = new float[nS];
        double[] envelope = createEnvelope(nS, 2, 50); // Sharp attack, faster decay
        for (int layer = 0; layer < ProjectOrpheusTTS.SYNTHESIS_LAYERS; layer++) {
            float[] noiseLayer = new float[nS];
            double centerFreq = f1 * (1.0 + (random.nextDouble() - 0.5) * 0.2 * layer);
            double layerQ = q * (1.0 + (random.nextDouble() - 0.5) * 0.3 * layer);
            BiquadFilter filter = new BiquadFilter(SAMPLE_RATE);
            filter.recalculate(BiquadFilter.Type.BAND_PASS, centerFreq, layerQ);
            for (int i = 0; i < nS; i++) noiseLayer[i] = (float) (filter.process(random.nextDouble() * 2 - 1) * envelope[i]);
            double gain = (layer == 0) ? 1.0 : 1.0 / (layer * 1.5);
            normalize(noiseLayer, gain);
            for (int i = 0; i < nS; i++) mixedNoise[i] += noiseLayer[i];
        }
        normalize(mixedNoise, amp);

        if (voiced) {
            float[] voicedLayer = generateVoicedSource(nS, fund, envelope);
            for (int i = 0; i < nS; i++) mixedNoise[i] = mixedNoise[i] * 0.7f + voicedLayer[i] * 0.3f;
        }
        return mixedNoise;
    }

    private static float[] synthPlosive(int nS, int fund, double amp, double burstFreq, double burstQ, boolean voiced, Phoneme nextPhoneme) {
        float[] out = new float[nS];
        int silenceDuration = nS / 3;
        int burstDuration = (int) (SAMPLE_RATE * 0.015);
        if (burstDuration > nS - silenceDuration) burstDuration = nS - silenceDuration;

        float[] clickLayer = new float[burstDuration];
        BiquadFilter clickFilter = new BiquadFilter(SAMPLE_RATE);
        clickFilter.recalculate(BiquadFilter.Type.BAND_PASS, burstFreq, burstQ);
        for (int i = 0; i < burstDuration; i++) clickLayer[i] = (float) clickFilter.process(random.nextDouble() * 2 - 1);
        normalize(clickLayer, 1.0);

        float[] aspirationLayer = new float[burstDuration];
        if (nextPhoneme != null && nextPhoneme.type == Phoneme.Type.VOWEL) {
            List<BiquadFilter> formantFilters = new ArrayList<>();
            for (double formant : nextPhoneme.startFormants) formantFilters.add(new BiquadFilter(SAMPLE_RATE));
            for (int i = 0; i < burstDuration; i++) {
                double noiseSample = random.nextDouble() * 2 - 1, filteredSample = 0;
                for(int j=0; j<formantFilters.size(); j++) {
                    formantFilters.get(j).recalculate(BiquadFilter.Type.BAND_PASS, nextPhoneme.startFormants[j], 10.0);
                    filteredSample += formantFilters.get(j).process(noiseSample);
                }
                aspirationLayer[i] = (float) (filteredSample / formantFilters.size());
            }
        }
        normalize(aspirationLayer, 1.0);

        for (int i = 0; i < burstDuration; i++) {
            double decay = Math.pow(1.0 - (double)i / burstDuration, 2);
            float mixedSample = (clickLayer[i] * 0.6f) + (aspirationLayer[i] * 0.4f);
            out[silenceDuration + i] = (float) (mixedSample * amp * decay);
        }

        if (voiced) {
            int voicedStart = silenceDuration - (int) (SAMPLE_RATE * 0.01);
            if(voicedStart < 0) voicedStart = 0;
            int voicedLength = nS - voicedStart;
            double[] voicedEnvelope = createEnvelope(voicedLength, 1, 5);
            float[] rawVoicedLayer = generateVoicedSource(voicedLength, fund, voicedEnvelope);

            BiquadFilter lowPass = new BiquadFilter(SAMPLE_RATE);
            lowPass.recalculate(BiquadFilter.Type.LOW_PASS, 400, 0.707);
            float[] voicedMurmur = new float[voicedLength];
            for (int i = 0; i < voicedLength; i++) voicedMurmur[i] = (float)lowPass.process(rawVoicedLayer[i]);

            normalize(voicedMurmur, 1.0);
            for (int i = 0; i < voicedLength; i++) {
                out[voicedStart + i] += voicedMurmur[i] * 0.8 * amp; // Stronger, bassy murmur
            }
        }
        return out;
    }

    private static void normalize(float[] audio, double targetPeak) {
        float max = 0; for (float s : audio) max = Math.max(max, Math.abs(s));
        if (max > 0) {
            double gain = targetPeak / max;
            for (int i = 0; i < audio.length; i++) audio[i] *= gain;
        }
    }
    
    private static double[] createEnvelope(int numSamples, double attackMs, double releaseMs) {
        double[] envelope = new double[numSamples];
        int attackSamples = (int)(SAMPLE_RATE * attackMs / 1000.0);
        int releaseSamples = (int)(SAMPLE_RATE * releaseMs / 1000.0);
        int sustainSamples = Math.max(0, numSamples - attackSamples - releaseSamples);
        for (int i = 0; i < attackSamples; i++) envelope[i] = (double)i / attackSamples;
        for (int i = 0; i < sustainSamples; i++) envelope[attackSamples + i] = 1.0;
        for (int i = 0; i < releaseSamples; i++) envelope[attackSamples + sustainSamples + i] = 1.0 - (double)i / releaseSamples;
        return envelope;
    }

    private static byte[] floatTo16BitByteArray(float[] audio) {
        byte[] bytes = new byte[audio.length * 2];
        for (int i = 0; i < audio.length; i++) {
            float sample = audio[i];
            if (sample > 1.0f) sample = 1.0f;
            if (sample < -1.0f) sample = -1.0f;
            short val = (short)(sample * 32767.0);
            bytes[i*2] = (byte)(val >> 8);
            bytes[i*2 + 1] = (byte)(val & 0xFF);
        }
        return bytes;
    }
}