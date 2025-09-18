import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A simple, rule-based phonetic transcriber.
 * It converts English words into a custom list of phoneme strings.
 * The strategy is:
 * 1. Check for a full-word match in a dictionary.
 * 2. If not found, break the word down using a "longest-match-first" rule system.
 * 3. Apply a final "magic e" rule to adjust vowel sounds.
 */
public class PhoneticTranscriber {

    /**
     * The dictionary of transcription rules. Maps a letter or group of letters (grapheme)
     * to its corresponding phonetic representation (phoneme).
     *
     * The map contains three types of rules:
     * - Complete words for common exceptions (e.g., "the", "of").
     * - Multi-letter combinations (digraphs like "sh", "tion").
     * - Single letters as a final fallback.
     */
    private static final Map<String, String> rules = new HashMap<>();

    // Static initializer block. This code runs once when the class is first loaded,
    // efficiently populating our rules map.
    static {
        // --- Full Word Rules (handle common exceptions first) ---
        rules.put("a", "schwa"); rules.put("is", "i_short z"); rules.put("of", "schwa v"); rules.put("the", "th schwa");
        rules.put("to", "t u_long"); rules.put("and", "a_short n d"); rules.put("in", "i_short n"); rules.put("that", "th a_short t");
        rules.put("it", "i_short t"); rules.put("with", "w i_short th"); rules.put("for", "f o_short r"); rules.put("was", "w schwa z");
        rules.put("on", "o_short n"); rules.put("as", "a_short z"); rules.put("are", "ar"); rules.put("be", "b iy");
        rules.put("this", "th i_short s"); rules.put("hello", "h e_short l o_long");
        rules.put("world", "w er l d"); rules.put("robot", "r o_long b o_short t");
        rules.put("java", "j a_short v schwa"); rules.put("engine", "e_short n j i_short n");
        rules.put("synthesizer", "s i_short n th schwa s ay z er");
        rules.put("advanced", "schwa d v a_short n s t");
        rules.put("data", "d ay t schwa");
        rules.put("accurate", "a_short k y er schwa t");
        rules.put("listen", "l i_short s schwa n");
        rules.put("difference", "d i_short f r schwa n s");
        rules.put("between", "b schwa t w iy n");
        rules.put("tea", "t iy");
        rules.put("two", "t u_long");
        rules.put("see", "s iy");
        rules.put("sue", "s u_long");
        rules.put("version", "v er zh schwa n");
        rules.put("much", "m schwa ch");
        rules.put("more", "m o_long r");
        rules.put("test", "t e_short s t");

        // --- Multi-Letter Rules (digraphs, trigraphs, etc.) ---
        // These are crucial to check before single letters.
        rules.put("tion", "sh schwa n");
        rules.put("sh", "sh"); rules.put("ch", "ch"); rules.put("th", "th"); rules.put("ph", "f");
        rules.put("qu", "k w"); rules.put("oo", "u_long"); rules.put("ee", "iy");
        rules.put("ou", "aw"); rules.put("ay", "ay"); rules.put("ai", "ay"); rules.put("oi", "oy");

        // --- Single-Letter Fallback Rules ---
        rules.put("b", "b"); rules.put("c", "k"); rules.put("d", "d");
        rules.put("e", "e_short"); rules.put("f", "f"); rules.put("g", "g"); rules.put("h", "h");
        rules.put("i", "i_short"); rules.put("j", "j"); rules.put("k", "k"); rules.put("l", "l");
        rules.put("m", "m"); rules.put("n", "n"); rules.put("o", "o_short"); rules.put("p", "p");
        rules.put("r", "r"); rules.put("s", "s"); rules.put("t", "t"); rules.put("u", "u_short");
        rules.put("v", "v"); rules.put("w", "w"); rules.put("x", "k s"); rules.put("y", "iy"); rules.put("z", "z");
    }

    /**
     * Transcribes a single word into a list of phonemes.
     *
     * @param word The word to transcribe. Case-insensitive.
     * @return A list of phoneme strings.
     */
    public List<String> transcribe(String word) {
        // Normalize the input to lower case to match our rules.
        word = word.toLowerCase();

        // Fast path: If the entire word is in our dictionary, use that translation directly.
        if (rules.containsKey(word)) {
            return List.of(rules.get(word).split(" "));
        }

        List<String> phonemes = new ArrayList<>();
        int i = 0; // Our current position in the word.

        // Iterate through the word, consuming chunks that match our rules.
        while (i < word.length()) {
            boolean matched = false;
            // This is the key to the algorithm: Greedily check for the longest possible match first.
            // We check for 4-letter combos ("tion"), then 3, then 2 ("sh"), then finally 1.
            for (int len = 4; len >= 1 && !matched; len--) {
                // Make sure we don't look past the end of the word.
                if (i + len <= word.length()) {
                    String sub = word.substring(i, i + len);
                    if (rules.containsKey(sub)) {
                        // Found a rule! Add the resulting phoneme(s) to our list.
                        phonemes.addAll(List.of(rules.get(sub).split(" ")));
                        // Advance our position in the word by the length of the matched chunk.
                        i += len;
                        matched = true;
                    }
                }
            }
            // If no rule matched at the current position (e.g., a number or punctuation),
            // just skip this character and move on.
            if (!matched) {
                i++;
            }
        }

        // Post-processing: Apply the "magic e" rule.
        // If a word ends in 'e', it often makes the preceding vowel long.
        // e.g., "hat" (a_short) -> "hate" (ay)
        if (word.endsWith("e") && phonemes.size() > 1 && !word.endsWith("ee")) {
            // Get the phoneme corresponding to the letter before the final consonant.
            String p = phonemes.get(phonemes.size() - 2);
            if(p.equals("a_short")) { phonemes.set(phonemes.size()-2, "ay"); }
            else if(p.equals("i_short")) { phonemes.set(phonemes.size()-2, "ay"); } // Note: "i" -> "ay" is like "bit" -> "bite"
            else if(p.equals("o_short")) { phonemes.set(phonemes.size()-2, "o_long"); } // e.g., "rot" -> "rote"
        }

        return phonemes;
    }
}