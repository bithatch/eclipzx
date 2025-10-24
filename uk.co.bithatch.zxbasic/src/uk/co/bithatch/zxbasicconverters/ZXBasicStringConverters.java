package uk.co.bithatch.zxbasicconverters;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.xtext.conversion.IValueConverter;
import org.eclipse.xtext.conversion.ValueConverter;
import org.eclipse.xtext.conversion.impl.AbstractDeclarativeValueConverterService;
import org.eclipse.xtext.nodemodel.INode;

/**
 * STRING converter that accepts Boriel ZX Basic escape sequences.
 */
public class ZXBasicStringConverters extends AbstractDeclarativeValueConverterService {

    // patterns for multi-char sequences
    private static final Pattern DECIMAL_CODE = Pattern.compile("\\\\#([0-9]{3})"); // \#000..255
    private static final Pattern COLOUR_CODE  = Pattern.compile("\\\\\\{([ipbf])([0-9])\\}"); // \{iN} etc
    private static final Pattern GRAPHICS_2   = Pattern.compile("\\\\([:\\.']){2}"); // \.. \.: \:' etc
    private static final Pattern UDG          = Pattern.compile("\\\\[A-Z]"); // \A..\Z

    @ValueConverter(rule = "STRING")
    public IValueConverter<String> STRING() {
        return new IValueConverter<>() {
            @Override
            public String toValue(String string, INode node) {
                if (string == null) return null;
                String raw = unquote(string); // keep original content between quotes

                StringBuilder out = new StringBuilder(raw.length());
                for (int i = 0; i < raw.length();) {
                    char c = raw.charAt(i);
                    if (c != '\\') {
                        out.append(c);
                        i++;
                        continue;
                    }
                    // At a backslash – try ZX-specific sequences in priority order
                    if (i + 1 < raw.length()) {
                        char n1 = raw.charAt(i + 1);

                        // 1) Java escapes (already valid): keep as in out but ensure they survive roundtrip
                        if ("btnfr\"'\\0".indexOf(n1) >= 0) {
                            // already handled by convertFromJavaString; but we preserve original char
                            // Append the decoded char from s at same logical position is messy; simplest is:
                            // Let Eclipse use the decoded 's' for model; we just accept syntax here.
                            // So copy as-is from the already-decoded 's' by recomputing later.
                            out.append('\\').append(n1);
                            i += 2;
                            continue;
                        }

                        // 2) Pound (backtick) -> £
                        if (n1 == '`') {
                            out.append('£');
                            i += 2;
                            continue;
                        }

                        // 3) Copyright -> ©
                        if (n1 == '*') {
                            out.append('©');
                            i += 2;
                            continue;
                        }

                        // 4) Decimal code \#nnn
                        Matcher mNum = DECIMAL_CODE.matcher(raw.substring(i));
                        if (mNum.lookingAt()) {
                            int val = Integer.parseInt(mNum.group(1));
                            if (val < 0 || val > 255) {
                                throw new IllegalArgumentException("Invalid \\# code (0..255): " + mNum.group(1));
                            }
                            // Option A: append as char (Latin-1); Option B: preserve literal.
                            out.append((char) (val & 0xFF));
                            i += mNum.end();
                            continue;
                        }

                        // 5) Graphics \.. etc (two of . : ')
                        Matcher mG = GRAPHICS_2.matcher(raw.substring(i));
                        if (mG.lookingAt()) {
                            // Leave as-is or map to a placeholder; we preserve as 2-char token here.
                            out.append(mG.group());
                            i += mG.end();
                            continue;
                        }

                        // 6) Colour control \{iN}, \{pN}, \{bN}, \{fN}
                        Matcher mC = COLOUR_CODE.matcher(raw.substring(i));
                        if (mC.lookingAt()) {
                            char kind = mC.group(1).toLowerCase(Locale.ROOT).charAt(0);
                            int N = mC.group(2).charAt(0) - '0';
                            boolean ok =
                                (kind == 'i' || kind == 'p') ? (N >= 0 && N <= 8) :
                                (kind == 'b' || kind == 'f') ? (N == 0 || N == 1) : false;
                            if (!ok) {
                                throw new IllegalArgumentException("Invalid colour control code: " + mC.group());
                            }
                            // Preserve literal; downstream can translate to bytes if desired.
                            out.append(mC.group());
                            i += mC.end();
                            continue;
                        }

                        // 7) UDG \A..\Z – preserve literal
                        Matcher mU = UDG.matcher(raw.substring(i));
                        if (mU.lookingAt()) {
                            out.append(mU.group());
                            i += mU.end();
                            continue;
                        }
                    }

                    // Fallback: accept a backslash followed by any single char (don’t reject)
                    // to be liberal in parsing ZX strings.
                    if (i + 1 < raw.length()) {
                        out.append('\\').append(raw.charAt(i + 1));
                        i += 2;
                    } else {
                        out.append('\\');
                        i++;
                    }
                    
                    throw new IllegalArgumentException("Invalid escape or control code at " + i);
                }

                // Return the *decoded* Java string where Java escapes were already handled,
                // but ZX-specific sequences are mostly preserved (except £/© and \#nnn).
                // If you’d rather preserve everything exactly as typed, return `raw` instead.
                return out.toString();
            }

            @Override
            public String toString(String value) {
                // Serialize back to a ZX-friendly literal.
                // We’ll escape quotes/backslashes and keep other ZX sequences as-is.
                String escaped = value
                        .replace("\\", "\\\\")
                        .replace("\"", "\\\"");
                return "\"" + escaped + "\"";
            }

            private String unquote(String s) {
                int n = s.length();
                if (n >= 2 && s.charAt(0) == '"' && s.charAt(n - 1) == '"') {
                    return s.substring(1, n - 1);
                }
                return s;
            }
        };
    }
}
