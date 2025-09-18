public class BiquadFilter {
    private double b0, b1, b2, a1, a2;
    private double z1, z2;
    private final int sampleRate;

    public enum Type { BAND_PASS, HIGH_PASS, LOW_PASS }

    public BiquadFilter(int sampleRate) {
        this.sampleRate = sampleRate;
    }

    public void recalculate(Type type, double centerFreq, double Q) {
        double w0 = 2 * Math.PI * Math.max(1.0, centerFreq) / sampleRate;
        double alpha = Math.sin(w0) / (2 * Math.max(0.01, Q));
        double cos_w0 = Math.cos(w0);
        double a0_norm;
        switch (type) {
            case BAND_PASS:
                a0_norm = 1 + alpha;
                b0 = alpha / a0_norm; b1 = 0; b2 = -alpha / a0_norm;
                a1 = -2 * cos_w0 / a0_norm; a2 = (1 - alpha) / a0_norm;
                break;
            case HIGH_PASS:
                a0_norm = 1 + alpha;
                b0 = (1 + cos_w0) / 2 / a0_norm; b1 = -(1 + cos_w0) / a0_norm; b2 = (1 + cos_w0) / 2 / a0_norm;
                a1 = -2 * cos_w0 / a0_norm; a2 = (1 - alpha) / a0_norm;
                break;
            case LOW_PASS:
                a0_norm = 1 + alpha;
                b0 = (1 - cos_w0) / 2 / a0_norm; b1 = (1 - cos_w0) / a0_norm; b2 = (1 - cos_w0) / 2 / a0_norm;
                a1 = -2 * cos_w0 / a0_norm; a2 = (1 - alpha) / a0_norm;
                break;
        }
    }

    public double process(double in) {
        double out = b0 * in + z1;
        z1 = b1 * in - a1 * out + z2;
        z2 = b2 * in - a2 * out;
        return out;
    }
}