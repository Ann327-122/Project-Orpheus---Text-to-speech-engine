import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * ProjectOrpheusTTS v6.0 - Main Application Class
 * <p>
 * This class creates and manages the main graphical user interface (GUI) for the
 * text-to-speech engine. It provides a text area for input, a button to trigger
 * synthesis, and a real-time oscilloscope to visualize the audio being generated.
 *
 * v6.0 Changes:
 * - PRONUNCIATION OVERHAUL (DICTIONARY EXPANSION): Fixed gross mispronunciations like "tea", "synthesizer",
 *   and "advanced" by massively expanding the phonetic transcriber's dictionary with hundreds of common
 *   and complex words. This replaces the error-prone rule-based guessing with explicit, accurate pronunciations.
 * - ACOUSTIC VOICING MODEL: Fixed the "d sounds like t/p" issue. Voiced plosives ('d', 'b', 'g') now have their
 *   voiced (humming) component passed through a low-pass filter. This accurately models the acoustics of human
 *   voicing, creating a realistic murmur that is clearly distinct from unvoiced consonants.
 * - REFINED FRICATIVE ENVELOPES: Fixed unnatural hissing sounds like "esssisser". Fricatives ('s', 'sh', 'f')
 *   now have a much sharper and more controlled volume envelope, preventing them from sounding unnaturally long.
 * - FASTER PHONEME TRANSITIONS: Reduced the audio blending time between phonemes to create crisper, clearer
 *   transitions and prevent sound "smearing" artifacts (like "sue" sounding like "sooeey").
 * - DEFAULT LAYERS INCREASED TO 30: The default synthesis layer count is now 30, as requested, for
 *   maximum acoustic richness out-of-the-box.
 */
public class ProjectOrpheusTTS extends JFrame {

    /**
     * Adjust this value to control the complexity of synthesized noise sounds (like 's', 'sh', 'f').
     * Higher values create a richer, more realistic sound by layering more noise sources,
     * similar to layering Perlin noise for realistic terrain.
     * Default: 30. Recommended range: 8-50+.
     */
    public static final int SYNTHESIS_LAYERS = 30;

    /**
     * Constructor for the main application window.
     */
    public ProjectOrpheusTTS() {
        setupNimbusLookAndFeel(); // Apply the custom dark theme.
        setTitle("Project Orpheus v6.0 - The Acoustic Accuracy Update");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10)); // Use a BorderLayout with 10px gaps.
        // Add padding around the entire content pane.
        ((JPanel) getContentPane()).setBorder(new EmptyBorder(10, 10, 10, 10));

        // Create the oscilloscope panel for waveform visualization.
        OscilloscopePanel oscilloscope = new OscilloscopePanel();
        add(oscilloscope, BorderLayout.NORTH); // Place it at the top of the window.

        // Create the text area for user input.
        JTextArea inputTextArea = new JTextArea("Test data for the advanced synthesizer. Listen to the difference between tea and two, or see and sue. This is much more accurate.");
        inputTextArea.setLineWrap(true);
        inputTextArea.setWrapStyleWord(true);
        inputTextArea.setFont(new Font("SansSerif", Font.PLAIN, 16));
        
        // Put the text area inside a scroll pane for handling long text.
        JScrollPane scrollPane = new JScrollPane(inputTextArea);
        scrollPane.setBorder(null); // Remove the default border for a cleaner look.
        scrollPane.getViewport().setBackground(new Color(18, 30, 49)); // Match viewport background to the theme.
        add(scrollPane, BorderLayout.CENTER); // Place it in the center.

        // Create the "Speak" button and its container panel.
        JButton speakButton = new JButton("Speak");
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT)); // Align button to the right.
        bottomPanel.add(speakButton);
        add(bottomPanel, BorderLayout.SOUTH); // Place it at the bottom.

        // Instantiate the synthesizer engine.
        // This is a key part of the design: we pass the oscilloscope's updateWaveform method
        // as a callback. The engine will call this method with chunks of audio data as it
        // generates them, completely decoupling the engine from the GUI.
        SynthesizerEngine synthesizer = new SynthesizerEngine(oscilloscope::updateWaveform);

        // Set up the action for the speak button.
        speakButton.addActionListener(e -> {
            String textToSpeak = inputTextArea.getText();
            if (textToSpeak.trim().isEmpty()) return; // Do nothing if the text is empty.

            speakButton.setEnabled(false); // Disable the button to prevent multiple clicks.

            // CRITICAL: Perform synthesis on a separate background thread.
            // This prevents the GUI from freezing while the CPU-intensive audio generation runs.
            new Thread(() -> {
                try {
                    synthesizer.speak(textToSpeak);
                } finally {
                    // After synthesis is complete (or if an error occurs),
                    // re-enable the button. This update MUST happen on the Event Dispatch Thread (EDT).
                    SwingUtilities.invokeLater(() -> speakButton.setEnabled(true));
                }
            }).start();
        });

        pack(); // Size the window to fit its components.
        setMinimumSize(new Dimension(500, 400));
        setLocationRelativeTo(null); // Center the window on the screen.
    }

    /**
     * Configures the application's look and feel to a custom "Nimbus" dark theme.
     */
    private void setupNimbusLookAndFeel() {
        try {
            // Find and set the Nimbus Look and Feel.
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
            // Override specific Nimbus color properties to create the desired dark theme.
            UIManager.put("control", new Color(68, 71, 74));
            UIManager.put("info", new Color(128, 128, 128));
            UIManager.put("nimbusBase", new Color(18, 30, 49));
            UIManager.put("nimbusLightBackground", new Color(18, 30, 49));
            UIManager.put("text", new Color(230, 230, 230));
            UIManager.put("nimbusSelectionBackground", new Color(104, 93, 156));
            // Customize scrollbar colors.
            UIManager.put("ScrollBar.background", new Color(0,0,0,0)); // Make background transparent.
            UIManager.put("ScrollBar.thumb", new Color(104, 93, 156));
            UIManager.put("ScrollBar.track", new Color(68, 71, 74));
            UIManager.put("ScrollBar.width", 12);
        } catch (Exception e) {
            // If Nimbus is not available, the application will fall back to the default L&F.
            System.err.println("Nimbus L&F not found: " + e.getMessage());
        }
    }

    /**
     * The main entry point for the application.
     * @param args Command line arguments (not used).
     */
    public static void main(String[] args) {
        // Schedule the creation and display of the GUI on the Event Dispatch Thread (EDT).
        // This is the standard and correct way to start a Swing application.
        SwingUtilities.invokeLater(() -> new ProjectOrpheusTTS().setVisible(true));
    }

    /**
     * A custom JPanel that visualizes audio waveforms in real-time.
     */
    static class OscilloscopePanel extends JPanel {
        private byte[] waveformChunk; // A buffer to hold the latest chunk of audio data.
        private final Color LIME_GREEN = new Color(50, 255, 50);

        public OscilloscopePanel() {
            setPreferredSize(new Dimension(0, 150)); // Set a default height.
            setBackground(Color.BLACK);
        }

        /**
         * The public method used as a callback by the SynthesizerEngine.
         * It receives a chunk of audio data and triggers a repaint of the panel.
         * @param chunk A byte array of 16-bit big-endian audio data.
         */
        public void updateWaveform(byte[] chunk) {
            this.waveformChunk = chunk;
            repaint(); // Request a repaint on the Event Dispatch Thread.
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g); // Standard practice: call the parent's paint method first.
            if (waveformChunk == null || waveformChunk.length == 0) return; // Don't draw if there's no data.

            g.setColor(LIME_GREEN);
            int midY = getHeight() / 2; // The horizontal center line (zero amplitude).
            int width = getWidth();
            int samples = waveformChunk.length / 2; // Each sample is 2 bytes.

            // Iterate through the panel's horizontal pixels to draw the waveform.
            for (int i = 0; i < width - 1; i++) {
                // Map the screen pixel 'i' to a sample in our audio chunk.
                int sampleIndex1 = (i * samples / width) * 2;
                int sampleIndex2 = ((i + 1) * samples / width) * 2;
                if (sampleIndex1 + 1 >= waveformChunk.length || sampleIndex2 + 1 >= waveformChunk.length) continue;

                // Reconstruct the 16-bit signed short from two big-endian bytes.
                // byte1 is shifted left by 8 bits, and byte2 is OR'd in.
                short val1 = (short) ((waveformChunk[sampleIndex1] << 8) | (waveformChunk[sampleIndex1 + 1] & 0xFF));
                short val2 = (short) ((waveformChunk[sampleIndex2] << 8) | (waveformChunk[sampleIndex2 + 1] & 0xFF));

                // Scale the sample value (from -32768 to 32767) to the panel's height.
                int y1 = midY - (int) (val1 * (midY / 32768.0));
                int y2 = midY - (int) (val2 * (midY / 32768.0));

                // Draw a line connecting this point to the next.
                g.drawLine(i, y1, i + 1, y2);
            }
        }
    }
}