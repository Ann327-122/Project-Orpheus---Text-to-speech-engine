import javax.sound.sampled.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;

/**
 * A formant-based speech synthesizer engine.
 *
 * This engine converts text into audio by:
 * 1. Transcribing the text into a sequence of phonemes using PhoneticTranscriber.
 * 2. Looking up the acoustic properties for each phoneme (formants, noise profiles, etc.).
 * 3. Synthesizing an audio waveform for each phoneme from scratch.
 * 4. Applying a basic intonation (prosody) model to the sequence.
 * 5. Stitching the individual phoneme audio clips together with cross-fading.
 * 6. Playing the final audio through the system's sound hardware.
 */
public class SynthesizerEngine {
    // --- Core Audio Constants ---
    private static final int SAMPLE_RATE = 44100; // CD-quality sample rate, a standard for audio processing.
    private static final AudioFormat AUDIO_FORMAT = new AudioFormat(SAMPLE_RATE, 16, 1, true, true); // 16-bit, mono, signed, big-endian byte order.
    private static final double MASTER_VOLUME = 0.9; // Final output volume is scaled to 90% to leave headroom and prevent clipping.

    // A callback function to pass generated audio chunks to an external component (like a UI).
    // This decouples the synthesis logic from the audio playback/streaming logic.
    private final Consumer<byte[]> onAudioChunkGenerated;

    private final PhoneticTranscriber transcriber = new PhoneticTranscriber();
    private static final Map<String, Phoneme> phonemePrototypes = new HashMap<>();
    private static final Random random = new Random(); // Used for generating white noise for fricatives and plosives.

    // --- Phoneme Acoustic Database ---
    // This static block is the "brain" of the synthesizer. It runs once when the class
    // is loaded, populating the map with the acoustic definitions for every sound.
    static {
        // Vowels and Diphthongs (defined by formant frequencies, which are resonant peaks in the vocal tract)
        phonemePrototypes.put("iy",      new Phoneme(200, new double[]{270, 2290, 3010}));                      // "ee" as in "see"
        phonemePrototypes.put("i_short", new Phoneme(150, new double[]{390, 1990, 2550}));                      // "i" as in "sit"
        phonemePrototypes.put("e_short", new Phoneme(150, new double[]{530, 1840, 2480}));                      // "e" as in "set"
        phonemePrototypes.put("a_short", new Phoneme(180, new double[]{660, 1720, 2410}));                      // "a" as in "cat"
        phonemePrototypes.put("schwa",   new Phoneme(80,  new double[]{500, 1500, 2450}));                      // "uh" as in "sofa"
        phonemePrototypes.put("u_short", new Phoneme(150, new double[]{440, 1020, 2240}));                      // "oo" as in "book"
        phonemePrototypes.put("u_long",  new Phoneme(250, new double[]{300, 870, 2240}));                       // "oo" as in "sue"
        phonemePrototypes.put("o_short", new Phoneme(150, new double[]{570, 840, 2410}));                      // "o" as in "cot"
        // Diphthongs are vowels that glide from one formant configuration to another.
        phonemePrototypes.put("o_long",  new Phoneme(250, new double[]{570, 840, 2410}, new double[]{400, 800, 2200})); // "oa" as in "boat"
        phonemePrototypes.put("aw",      new Phoneme(250, new double[]{660, 1720, 2410}, new double[]{300, 870, 2240})); // "ow" as in "cow"
        phonemePrototypes.put("oy",      new Phoneme(250, new double[]{400, 850, 2300}, new double[]{390, 1990, 2550})); // "oy" as in "boy"
        phonemePrototypes.put("ay",      new Phoneme(250, new double[]{750, 1720, 2410}, new double[]{390, 1990, 2550})); // "i" as in "sight"
        phonemePrototypes.put("ar",      new Phoneme(220, new double[]{660, 1220, 2410}, new double[]{490, 1350, 1690})); // "ar" as in "car"
        phonemePrototypes.put("er",      new Phoneme(180, new double[]{490, 1350, 1690}));                      // "er" as in "bird"

        // Sonorants (voiced, non-turbulent consonants)
        phonemePrototypes.put("r",       new Phoneme(120, new double[]{500, 1500, 2450}, new double[]{490, 1350, 1690}));
        phonemePrototypes.put("l",       new Phoneme(100, new double[]{360, 1300, 2700}));
        phonemePrototypes.put("w",       new Phoneme(80,  new double[]{300, 600, 2240}));
        phonemePrototypes.put("y",       new Phoneme(80,  new double[]{270, 2000, 3010}));
        phonemePrototypes.put("m",       new Phoneme(120, new double[]{300, 1100, 2300}));
        phonemePrototypes.put("n",       new Phoneme(100, new double[]{300, 1400, 2500}));

        // Fricatives (hissing sounds made by forcing air through a narrow channel)
        // Phoneme(duration, type, amplitude, noise_freq1, noise_freq2, Q, is_voiced)
        phonemePrototypes.put("h",  new Phoneme(60,  Phoneme.Type.FRICATIVE, 0.2, 500, 10000, 1.0, false));
        phonemePrototypes.put("f",  new Phoneme(100, Phoneme.Type.FRICATIVE, 0.4, 4000, 9000, 1.5, false));
        phonemePrototypes.put("v",  new Phoneme(100, Phoneme.Type.FRICATIVE, 0.4, 3000, 8000, 1.5, true));
        phonemePrototypes.put("s",  new Phoneme(150, Phoneme.Type.FRICATIVE, 0.5, 6000, 10000, 2.0, false));
        phonemePrototypes.put("z",  new Phoneme(150, Phoneme.Type.FRICATIVE, 0.5, 5500, 9500, 2.0, true));
        phonemePrototypes.put("sh", new Phoneme(150, Phoneme.Type.FRICATIVE, 0.3, 2500, 7000, 1.2, false));
        phonemePrototypes.put("zh", new Phoneme(150, Phoneme.Type.FRICATIVE, 0.3, 2000, 6000, 1.2, true)); // as in "measure"
        phonemePrototypes.put("th", new Phoneme(120, Phoneme.Type.FRICATIVE, 0.2, 5000, 9000, 1.8, false)); // as in "thin"

        // Plosives (sounds made by stopping airflow, then releasing it in a burst)
        // Phoneme(duration, type, amplitude, burst_freq, burst_Q, is_voiced)
        phonemePrototypes.put("b", new Phoneme(40, Phoneme.Type.PLOSIVE, 0.8, 500,  1.0, true));
        phonemePrototypes.put("d", new Phoneme(40, Phoneme.Type.PLOSIVE, 0.9, 3500, 1.5, true));
        phonemePrototypes.put("g", new Phoneme(50, Phoneme.Type.PLOSIVE, 0.8, 1500, 1.2, true));
        phonemePrototypes.put("p", new Phoneme(40, Phoneme.Type.PLOSIVE, 0.8, 700,  1.0, false));
        phonemePrototypes.put("t", new Phoneme(40, Phoneme.Type.PLOSIVE, 0.9, 4500, 1.5, false));
        phonemePrototypes.put("k", new Phoneme(50, Phoneme.Type.PLOSIVE, 0.9, 1800, 1.2, false));

        // Affricates (composite sounds: a plosive immediately followed by a fricative)
        phonemePrototypes.put("ch", new Phoneme(160, Phoneme.Type.AFFRICATE, false)); // composite of 't' + 'sh'
        phonemePrototypes.put("j",  new Phoneme(160, Phoneme.Type.AFFRICATE, true));  // composite of 'd' + 'zh'

        // Special: A silent phoneme to create gaps between words.
        phonemePrototypes.put("_", new Phoneme(150, Phoneme.Type.SILENCE));
    }

    public SynthesizerEngine(Consumer<byte[]> onAudioChunk) {
        this.onAudioChunkGenerated = onAudioChunk;
    }

    /**
     * The main entry point to synthesize speech from a string of text.
     * @param text The text to speak.
     */
    public void speak(String text) {
        List<float[]> audioSequence = new ArrayList<>();
        List<String> phonemeCodes = new ArrayList<>();

        // 1. Transcribe the entire text into a flat list of phoneme codes.
        // We split by whitespace and punctuation to get individual words.
        for (String word : text.toLowerCase().trim().split("[\\s\\p{Punct}]+")) {
            if(!word.isEmpty()) {
                phonemeCodes.addAll(transcriber.transcribe(word));
                phonemeCodes.add("_"); // Add a short silence after each word.
            }
        }
        if (phonemeCodes.isEmpty()) return; // Nothing to say.

        // 2. Synthesize each phoneme in the sequence.
        for(int i = 0; i < phonemeCodes.size(); i++) {
            String code = phonemeCodes.get(i);
            Phoneme proto = phonemePrototypes.get(code);

            if(proto != null) {
                // This is a simple but effective prosody (intonation) model.
                // It calculates the position in the sentence (0.0 at start, 1.0 at end).
                double ratio = Math.min(1.0, (double)i / Math.max(1, phonemeCodes.size() - 2));
                // The pitch starts at 110Hz and gently falls to 80Hz by the end of the sentence.
                int pitch = (int)(110 - (30 * ratio));

                // A key feature for realism: look ahead to the next phoneme.
                // This allows for "coarticulation," where the current sound is influenced
                // by the one that follows (e.g., the 'p' in 'pool' is different from 'peel').
                Phoneme nextPhoneme = (i + 1 < phonemeCodes.size()) ? phonemePrototypes.get(phonemeCodes.get(i + 1)) : null;
                audioSequence.add(proto.generate(pitch, nextPhoneme));
            }
        }

        // 3. Assemble the final audio clip.
        float[] finalAudio = stitchAudio(audioSequence, 6); // Stitch with a 6ms crossfade.
        normalize(finalAudio, MASTER_VOLUME); // Normalize to the master volume.
        byte[] finalAudioBytes = floatTo16BitByteArray(finalAudio); // Convert to 16-bit PCM bytes.
        playAudio(finalAudioBytes); // Play the sound.
    }

    /**
     * Stitches a sequence of audio clips together, using a short cross-fade to smooth the transitions.
     * @param seq A list of float arrays, each representing an audio clip.
     * @param ms The duration of the cross-fade in milliseconds.
     * @return A single float array containing the combined audio.
     */
    private static float[] stitchAudio(List<float[]> seq, int ms) {
        // Calculate the number of samples for the cross-fade duration.
        int cs = (int)(SAMPLE_RATE * (ms / 1000.0));
        if (seq.isEmpty()) return new float[0];
        
        // Calculate the total length of the final audio clip.
        int total = seq.stream().mapToInt(a -> a.length).sum();
        float[] out = new float[total];
        int pos = 0; // Current write position in the output array.

        for (float[] clip : seq) {
            // Determine the region in the 'out' array where we'll blend.
            int blendStart = Math.max(0, pos - cs);
            int blendEnd = Math.min(pos, blendStart + cs);

            // Perform the linear cross-fade.
            for (int j = blendStart; j < blendEnd; j++) {
                float ratio = (float)(j - blendStart) / cs; // 0.0 at start of blend, 1.0 at end.
                int clipIndex = j - blendStart;
                if (clipIndex < clip.length) {
                    // Mix the existing audio (fading out) with the new clip's audio (fading in).
                    out[j] = (out[j] * (1.0f - ratio) + clip[clipIndex] * ratio);
                }
            }
            
            // Copy the rest of the new clip directly after the blended section.
            int copyStart = blendEnd - blendStart;
            int remainingLength = clip.length - copyStart;
            if (remainingLength > 0 && pos + remainingLength <= out.length) {
                System.arraycopy(clip, copyStart, out, pos, remainingLength);
            }
            pos += remainingLength;
        }

        // Trim the output array to the actual length of audio written, in case of calculation drift.
        float[] trimmedOut = new float[pos];
        System.arraycopy(out, 0, trimmedOut, 0, pos);
        return trimmedOut;
    }

    /**
     * Plays a byte array of audio data through the system's default audio output.
     * @param audioData The raw 16-bit PCM audio data.
     */
    private void playAudio(byte[] audioData) {
        // 'try-with-resources' ensures the audio line is closed automatically.
        try (SourceDataLine line = AudioSystem.getSourceDataLine(AUDIO_FORMAT)) {
            line.open(AUDIO_FORMAT);
            line.start();

            // Write the audio data in chunks to the line's buffer to stream it.
            // This prevents holding the entire audio clip in the sound card's memory at once.
            int chunkSize = 2048;
            for (int i = 0; i < audioData.length; i += chunkSize) {
                int len = Math.min(chunkSize, audioData.length - i);
                byte[] chunk = java.util.Arrays.copyOfRange(audioData, i, i + len);
                onAudioChunkGenerated.accept(chunk); // Fire the callback for external listeners.
                line.write(chunk, 0, len); // Write the chunk to the audio buffer.
            }
            
            // Blocks until the audio line's buffer has been emptied (all sound has finished playing).
            line.drain();
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    /**
     * A data class holding the acoustic parameters for a single phoneme.
     */
    private static class Phoneme {
        enum Type { VOWEL, PLOSIVE, FRICATIVE, AFFRICATE, SILENCE }

        Type type;
        int durationMs;
        // Vowel parameters
        double[] startFormants, endFormants;
        // Fricative/Plosive parameters
        double amplitude, noiseFreq1, noiseFreq2, noiseQ;
        boolean voiced;

        // --- Overloaded Constructors for convenience ---
        Phoneme(int d, double[] f){this(d,f,f);} // Constructor for static vowels.
        Phoneme(int d, double[] sf, double[] ef){this.type=Type.VOWEL; this.durationMs=d; this.startFormants=sf; this.endFormants=ef;} // For diphthongs (gliding vowels).
        Phoneme(int d, Type t, boolean v){this.type=t; this.durationMs=d; this.voiced=v;} // For affricates.
        Phoneme(int d, Type t, double a, double nf1, double nf2, double q, boolean v){this.type=t; this.durationMs=d; this.amplitude=a; this.noiseFreq1=nf1; this.noiseFreq2=nf2; this.noiseQ=q; this.voiced=v;} // For fricatives.
        Phoneme(int d, Type t, double a, double nf1, double q, boolean v){this.type=t; this.durationMs=d; this.amplitude=a; this.noiseFreq1=nf1; this.noiseQ=q; this.voiced=v;} // For plosives.
        Phoneme(int d, Type t){this.type=t; this.durationMs=d;} // For silence.

        /**
         * Generates the audio waveform for this phoneme. This is a factory method.
         * @param fundamental The fundamental pitch (F0) in Hz.
         * @param nextPhoneme The phoneme that will follow this one, for coarticulation.
         * @return A float array of audio samples.
         */
        public float[] generate(int fundamental, Phoneme nextPhoneme) {
            int nS = (int)(SAMPLE_RATE * (durationMs / 1000.0)); // Number of samples.
            switch(type) {
                case VOWEL:
                    return synthVowel(nS, fundamental, startFormants, endFormants);
                case PLOSIVE:
                    return synthPlosive(nS, fundamental, amplitude, noiseFreq1, noiseQ, voiced, nextPhoneme);
                case FRICATIVE:
                    return synthFricative(nS, fundamental, amplitude, noiseFreq1, noiseFreq2, noiseQ, voiced);
                case AFFRICATE:
                    // An affricate is a plosive followed by a fricative. We generate them separately and stitch them.
                    Phoneme plosive = phonemePrototypes.get(voiced ? "d" : "t");
                    Phoneme fricative = phonemePrototypes.get(voiced ? "zh" : "sh");
                    // Generate the two parts and stitch them with a very short 2ms blend.
                    return stitchAudio(List.of(plosive.generate(fundamental, fricative), fricative.generate(fundamental, null)), 2);
                default: // SILENCE
                    return new float[nS];
            }
        }
    }

    /**
     * Generates the "source" signal for a voiced sound - a sawtooth wave.
     * This represents the buzzing of the vocal cords before it's shaped by the mouth.
     * @param numSamples The length of the audio to generate.
     * @param fundamental The pitch (F0).
     * @param envelope An amplitude envelope to apply to the source.
     * @return The generated source signal.
     */
    private static float[] generateVoicedSource(int numSamples, int fundamental, double[] envelope) {
        float[] source = new float[numSamples];
        double phase = 0.0;
        double period = (double) SAMPLE_RATE / fundamental; // Samples per cycle.
        for (int i = 0; i < numSamples; i++) {
            phase = (phase + 1) % period; // Increment phase, wrap around at the end of a period.
            // This creates a sawtooth wave from 0.0 to 1.0, then maps it to -1.0 to 1.0.
            source[i] = (float) (((phase / period) * 2.0 - 1.0) * envelope[i]);
        }
        return source;
    }

    /**
     * Synthesizes a vowel by filtering a voiced source through formant filters.
     * @param nS Number of samples.
     * @param fund Fundamental frequency.
     * @param sF Start formant frequencies.
     * @param eF End formant frequencies (for diphthongs).
     * @return The vowel audio clip.
     */
    private static float[] synthVowel(int nS, int fund, double[] sF, double[] eF) {
        double[] envelope = createEnvelope(nS, 5, 10); // 5ms attack, 10ms release.
        float[] src = generateVoicedSource(nS, fund, envelope);

        // Create a bank of band-pass filters, one for each formant.
        List<BiquadFilter> filters = new ArrayList<>();
        for (double freq : sF) filters.add(new BiquadFilter(SAMPLE_RATE));

        float[] filt = new float[nS];
        for(int i=0; i<nS; i++) {
            double r = (double)i / nS; // Interpolation ratio (0.0 to 1.0).
            double sample = src[i];
            for(int j=0; j<filters.size(); j++) {
                // For diphthongs, glide the center frequency of the filter from start to end.
                double currentFreq = sF[j] + (eF[j] - sF[j]) * r;
                filters.get(j).recalculate(BiquadFilter.Type.BAND_PASS, currentFreq, 10.0);
                sample = filters.get(j).process(sample);
            }
            filt[i] = (float)sample;
        }
        normalize(filt, 1.0);
        return filt;
    }

    /**
     * Synthesizes a fricative sound by filtering white noise.
     * Multiple layers of slightly different filtered noise are used for a richer sound.
     */
    private static float[] synthFricative(int nS, int fund, double amp, double f1, double f2, double q, boolean voiced) {
        float[] mixedNoise = new float[nS];
        double[] envelope = createEnvelope(nS, 2, 50); // Sharp attack, faster decay.

        // This reference seems to point to a constant in another class. I'll hardcode it to 3 for now.
        final int SYNTHESIS_LAYERS = 3; 
        for (int layer = 0; layer < SYNTHESIS_LAYERS; layer++) {
            float[] noiseLayer = new float[nS];
            // Each layer gets a slightly randomized center frequency and Q for a thicker sound.
            double centerFreq = f1 * (1.0 + (random.nextDouble() - 0.5) * 0.2 * layer);
            double layerQ = q * (1.0 + (random.nextDouble() - 0.5) * 0.3 * layer);
            BiquadFilter filter = new BiquadFilter(SAMPLE_RATE);
            filter.recalculate(BiquadFilter.Type.BAND_PASS, centerFreq, layerQ);
            
            // Generate white noise and pass it through the filter.
            for (int i = 0; i < nS; i++) noiseLayer[i] = (float) (filter.process(random.nextDouble() * 2 - 1) * envelope[i]);
            
            // Subsequent layers are quieter.
            double gain = (layer == 0) ? 1.0 : 1.0 / (layer * 1.5);
            normalize(noiseLayer, gain);
            for (int i = 0; i < nS; i++) mixedNoise[i] += noiseLayer[i];
        }
        normalize(mixedNoise, amp);

        // If the fricative is voiced (like 'v' or 'z'), mix in a voiced source.
        if (voiced) {
            float[] voicedLayer = generateVoicedSource(nS, fund, envelope);
            for (int i = 0; i < nS; i++) mixedNoise[i] = mixedNoise[i] * 0.7f + voicedLayer[i] * 0.3f;
        }
        return mixedNoise;
    }

    /**
     * Synthesizes a plosive sound, which consists of silence, a noise burst, and aspiration.
     */
    private static float[] synthPlosive(int nS, int fund, double amp, double burstFreq, double burstQ, boolean voiced, Phoneme nextPhoneme) {
        float[] out = new float[nS];
        int silenceDuration = nS / 3; // The initial "stop" phase.
        int burstDuration = (int) (SAMPLE_RATE * 0.015); // A short 15ms burst of noise.
        if (burstDuration > nS - silenceDuration) burstDuration = nS - silenceDuration;

        // 1. Generate the main noise burst (the "click" of the 'p' or 't').
        float[] clickLayer = new float[burstDuration];
        BiquadFilter clickFilter = new BiquadFilter(SAMPLE_RATE);
        clickFilter.recalculate(BiquadFilter.Type.BAND_PASS, burstFreq, burstQ);
        for (int i = 0; i < burstDuration; i++) clickLayer[i] = (float) clickFilter.process(random.nextDouble() * 2 - 1);
        normalize(clickLayer, 1.0);

        // 2. Generate the aspiration (the "puff of air") which is shaped by the following vowel.
        float[] aspirationLayer = new float[burstDuration];
        if (nextPhoneme != null && nextPhonome.type == Phoneme.Type.VOWEL) {
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

        // 3. Mix the click and aspiration with a fast decay envelope.
        for (int i = 0; i < burstDuration; i++) {
            double decay = Math.pow(1.0 - (double)i / burstDuration, 2); // Exponential-like decay.
            float mixedSample = (clickLayer[i] * 0.6f) + (aspirationLayer[i] * 0.4f);
            out[silenceDuration + i] = (float) (mixedSample * amp * decay);
        }

        // 4. For voiced plosives ('b', 'd', 'g'), add a low-frequency murmur during the stop phase.
        if (voiced) {
            int voicedStart = silenceDuration - (int) (SAMPLE_RATE * 0.01); // Start murmur 10ms before burst.
            if(voicedStart < 0) voicedStart = 0;
            int voicedLength = nS - voicedStart;
            double[] voicedEnvelope = createEnvelope(voicedLength, 1, 5);
            float[] rawVoicedLayer = generateVoicedSource(voicedLength, fund, voicedEnvelope);

            // The murmur is low-pass filtered to sound muffled.
            BiquadFilter lowPass = new BiquadFilter(SAMPLE_RATE);
            lowPass.recalculate(BiquadFilter.Type.LOW_PASS, 400, 0.707);
            float[] voicedMurmur = new float[voicedLength];
            for (int i = 0; i < voicedLength; i++) voicedMurmur[i] = (float)lowPass.process(rawVoicedLayer[i]);

            normalize(voicedMurmur, 1.0);
            for (int i = 0; i < voicedLength; i++) {
                out[voicedStart + i] += voicedMurmur[i] * 0.8 * amp;
            }
        }
        return out;
    }

    /**
     * Normalizes an audio clip to a target peak amplitude.
     * @param audio The audio data to normalize.
     * @param targetPeak The desired peak level (e.g., 1.0).
     */
    private static void normalize(float[] audio, double targetPeak) {
        float max = 0;
        for (float s : audio) max = Math.max(max, Math.abs(s));
        if (max > 0) {
            double gain = targetPeak / max;
            for (int i = 0; i < audio.length; i++) audio[i] *= gain;
        }
    }
    
    /**
     * Creates a simple Attack-Sustain-Release (ASR) amplitude envelope.
     * @param numSamples Total length of the envelope.
     * @param attackMs Duration of the attack phase.
     * @param releaseMs Duration of the release phase.
     * @return An array of amplitude values from 0.0 to 1.0.
     */
    private static double[] createEnvelope(int numSamples, double attackMs, double releaseMs) {
        double[] envelope = new double[numSamples];
        int attackSamples = (int)(SAMPLE_RATE * attackMs / 1000.0);
        int releaseSamples = (int)(SAMPLE_RATE * releaseMs / 1000.0);
        int sustainSamples = Math.max(0, numSamples - attackSamples - releaseSamples);
        // Ramp up during attack.
        for (int i = 0; i < attackSamples; i++) envelope[i] = (double)i / attackSamples;
        // Hold at max during sustain.
        for (int i = 0; i < sustainSamples; i++) envelope[attackSamples + i] = 1.0;
        // Ramp down during release.
        for (int i = 0; i < releaseSamples; i++) envelope[attackSamples + sustainSamples + i] = 1.0 - (double)i / releaseSamples;
        return envelope;
    }

    /**
     * Converts a float audio array [-1.0, 1.0] to a 16-bit big-endian byte array.
     */
    private static byte[] floatTo16BitByteArray(float[] audio) {
        byte[] bytes = new byte[audio.length * 2];
        for (int i = 0; i < audio.length; i++) {
            float sample = audio[i];
            // Clip the sample to prevent overflow.
            if (sample > 1.0f) sample = 1.0f;
            if (sample < -1.0f) sample = -1.0f;
            // Scale to 16-bit integer range.
            short val = (short)(sample * 32767.0);
            // Convert short to two bytes (big-endian).
            bytes[i*2] = (byte)(val >> 8);      // Most significant byte.
            bytes[i*2 + 1] = (byte)(val & 0xFF); // Least significant byte.
        }
        return bytes;
    }
}