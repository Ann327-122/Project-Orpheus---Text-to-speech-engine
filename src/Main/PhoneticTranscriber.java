import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PhoneticTranscriber {
    private static final Map<String, String> rules = new HashMap<>();

    static {
        // Expanded Dictionary for accuracy
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

        // Fallback phonetic rules (less accurate)
        rules.put("tion", "sh schwa n");
        rules.put("sh", "sh"); rules.put("ch", "ch"); rules.put("th", "th"); rules.put("ph", "f");
        rules.put("qu", "k w"); rules.put("oo", "u_long"); rules.put("ee", "iy");
        rules.put("ou", "aw"); rules.put("ay", "ay"); rules.put("ai", "ay"); rules.put("oi", "oy");
        rules.put("b", "b"); rules.put("c", "k"); rules.put("d", "d");
        rules.put("e", "e_short"); rules.put("f", "f"); rules.put("g", "g"); rules.put("h", "h");
        rules.put("i", "i_short"); rules.put("j", "j"); rules.put("k", "k"); rules.put("l", "l");
        rules.put("m", "m"); rules.put("n", "n"); rules.put("o", "o_short"); rules.put("p", "p");
        rules.put("r", "r"); rules.put("s", "s"); rules.put("t", "t"); rules.put("u", "u_short");
        rules.put("v", "v"); rules.put("w", "w"); rules.put("x", "k s"); rules.put("y", "iy"); rules.put("z", "z");
    }

    public List<String> transcribe(String word) {
        word = word.toLowerCase();
        if (rules.containsKey(word)) return List.of(rules.get(word).split(" "));
        List<String> phonemes = new ArrayList<>(); int i = 0;
        while (i < word.length()) {
            boolean matched = false;
            for (int len = 4; len >= 1 && !matched; len--) {
                if (i + len <= word.length()) {
                    String sub = word.substring(i, i + len);
                    if (rules.containsKey(sub)) {
                        phonemes.addAll(List.of(rules.get(sub).split(" ")));
                        i += len; matched = true;
                    }
                }
            }
            if (!matched) i++;
        }
        if (word.endsWith("e") && phonemes.size() > 1 && !word.endsWith("ee")) {
            String p = phonemes.get(phonemes.size() - 2);
            if(p.equals("a_short")) { phonemes.set(phonemes.size()-2, "ay"); }
            else if(p.equals("i_short")) { phonemes.set(phonemes.size()-2, "ay"); }
            else if(p.equals("o_short")) { phonemes.set(phonemes.size()-2, "o_long"); }
        }
        return phonemes;
    }
}