package uk.co.bithatch.bitzx;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public class Txt2NextBasicConverter {
	enum ParseState {
	    NORMAL,
	    QUOTE,
	    VERBATIM
	}


    private static final Map<String, Integer> TOKENS = new HashMap<>();
    private static final Map<String, Integer> CHARS = new HashMap<>();

    static {
        // ZX Spectrum / NextBASIC tokens
        TOKENS.put("RND", 0xA5);
        TOKENS.put("INKEY$", 0xA6);
        TOKENS.put("PI", 0xA7);
        TOKENS.put("FN", 0xA8);
        TOKENS.put("POINT", 0xA9);
        TOKENS.put("SCREEN$", 0xAA);
        TOKENS.put("ATTR", 0xAB);
        TOKENS.put("AT", 0xAC);
        TOKENS.put("TAB", 0xAD);
        TOKENS.put("VAL$", 0xAE);
        TOKENS.put("CODE", 0xAF);
        TOKENS.put("VAL", 0xB0);
        TOKENS.put("LEN", 0xB1);
        TOKENS.put("SIN", 0xB2);
        TOKENS.put("COS", 0xB3);
        TOKENS.put("TAN", 0xB4);
        TOKENS.put("ASN", 0xB5);
        TOKENS.put("ACS", 0xB6);
        TOKENS.put("ATN", 0xB7);
        TOKENS.put("LN", 0xB8);
        TOKENS.put("EXP", 0xB9);
        TOKENS.put("INT", 0xBA);
        TOKENS.put("SQR", 0xBB);
        TOKENS.put("SGN", 0xBC);
        TOKENS.put("ABS", 0xBD);
        TOKENS.put("PEEK", 0xBE);
        TOKENS.put("IN", 0xBF);
        TOKENS.put("USR", 0xC0);
        TOKENS.put("STR$", 0xC1);
        TOKENS.put("CHR$", 0xC2);
        TOKENS.put("NOT", 0xC3);
        TOKENS.put("BIN", 0xC4);
        TOKENS.put("OR", 0xC5);
        TOKENS.put("AND", 0xC6);
        TOKENS.put("<=", 0xC7);
        TOKENS.put(">=", 0xC8);
        TOKENS.put("<>", 0xC9);
        TOKENS.put("LINE", 0xCA);
        TOKENS.put("THEN", 0xCB);
        TOKENS.put("TO", 0xCC);
        TOKENS.put("STEP", 0xCD);
        TOKENS.put("DEF", 0xCE);
        TOKENS.put("CAT", 0xCF);
        TOKENS.put("FORMAT", 0xD0);
        TOKENS.put("MOVE", 0xD1);
        TOKENS.put("ERASE", 0xD2);
        TOKENS.put("OPEN", 0xD3);
        TOKENS.put("CLOSE", 0xD4);
        TOKENS.put("MERGE", 0xD5);
        TOKENS.put("VERIFY", 0xD6);
        TOKENS.put("BEEP", 0xD7);
        TOKENS.put("CIRCLE", 0xD8);
        TOKENS.put("INK", 0xD9);
        TOKENS.put("PAPER", 0xDA);
        TOKENS.put("FLASH", 0xDB);
        TOKENS.put("BRIGHT", 0xDC);
        TOKENS.put("INVERSE", 0xDD);
        TOKENS.put("OVER", 0xDE);
        TOKENS.put("OUT", 0xDF);
        TOKENS.put("LPRINT", 0xE0);
        TOKENS.put("LLIST", 0xE1);
        TOKENS.put("STOP", 0xE2);
        TOKENS.put("READ", 0xE3);
        TOKENS.put("DATA", 0xE4);
        TOKENS.put("RESTORE", 0xE5);
        TOKENS.put("NEW", 0xE6);
        TOKENS.put("BORDER", 0xE7);
        TOKENS.put("CONTINUE", 0xE8);
        TOKENS.put("DIM", 0xE9);
        TOKENS.put("REM", 0xEA);
        TOKENS.put("FOR", 0xEB);
        TOKENS.put("GO TO", 0xEC);
        TOKENS.put("GO SUB", 0xED);
        TOKENS.put("INPUT", 0xEE);
        TOKENS.put("LOAD", 0xEF);
        TOKENS.put("LIST", 0xF0);
        TOKENS.put("LET", 0xF1);
        TOKENS.put("PAUSE", 0xF2);
        TOKENS.put("NEXT", 0xF3);
        TOKENS.put("POKE", 0xF4);
        TOKENS.put("PRINT", 0xF5);
        TOKENS.put("PLOT", 0xF6);
        TOKENS.put("RUN", 0xF7);
        TOKENS.put("SAVE", 0xF8);
        TOKENS.put("RANDOMIZE", 0xF9);
        TOKENS.put("IF", 0xFA);
        TOKENS.put("CLS", 0xFB);
        TOKENS.put("DRAW", 0xFC);
        TOKENS.put("CLEAR", 0xFD);
        TOKENS.put("RETURN", 0xFE);
        TOKENS.put("COPY", 0xFF);

        // NextBASIC extended tokens (single-byte, reusing previously unused values)
        TOKENS.put("CD", 0xA0);
        TOKENS.put("MKDIR", 0xA1);
        TOKENS.put("PWD", 0xA2);
        
        CHARS.put("£", 0xA3);
        CHARS.put("©", 0xA9);

        CHARS.put("\u259D", 0x81);  // Quadrant upper right
        CHARS.put("\u2598", 0x82);  // Quadrant upper left
        CHARS.put("\u2580", 0x83);  // Upper half block
        CHARS.put("\u2597", 0x84);  // Quadrant lower right
        CHARS.put("\u2590", 0x85);  // Right half block
        CHARS.put("\u259A", 0x86);  // Quadrant upper left and lower right
        CHARS.put("\u259C", 0x87);  // Quadrant upper left and upper right and lower right
        CHARS.put("\u2596", 0x88);  // Quadrant lower left
        CHARS.put("\u259E", 0x89);  // Quadrant upper right and lower left
        CHARS.put("\u258C", 0x8a);  // Left half block
        CHARS.put("\u259B", 0x8b);  // Quadrant upper left and upper right and lower left
        CHARS.put("\u2584", 0x8c);  // Lower half block
        CHARS.put("\u259F", 0x8d);  // Quadrant upper right and lower left and lower right
        CHARS.put("\u2599", 0x8e);  //Quadrant upper left and lower left and lower right
        CHARS.put("\u2588", 0x8f);  // Full block
    }

    public static void main(String[] args) throws IOException {
        Path input = Paths.get(args[0]);
        Path output = Paths.get(args[1]);

        List<String> lines = Files.readAllLines(input);
        byte[] basic = encodeProgram(lines);

        Files.write(output, basic);
        System.out.println("Wrote: " + output);
    }

    public static byte[] encodeProgram(String lines) throws IOException {
    	System.out.println("Encoding program ... " + lines);
    	return encodeProgram(Arrays.asList(lines.split("\\R")));
    }

    public static byte[] encodeProgram(Iterable<String> lines) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (String line : lines) {
            if (line.isBlank()) continue;
            byte[] encoded = encodeLine(line);
            out.writeBytes(encoded);
        }
        return out.toByteArray();
    }

    public static byte[] encodeLine(String line) throws IOException {
        String[] parts = line.trim().split("\\s+", 2);
        int lineNumber = Integer.parseInt(parts[0]);
        String code = parts.length > 1 ? parts[1] : "";

        ByteArrayOutputStream lineBytes = new ByteArrayOutputStream();
        ByteArrayOutputStream codeBytes = new ByteArrayOutputStream();

        tokenize(code, codeBytes);
        codeBytes.write(0x0D); // line end

        byte[] codeArray = codeBytes.toByteArray();

        ByteBuffer header = ByteBuffer.allocate(4);
        header.order(ByteOrder.BIG_ENDIAN).putShort((short) lineNumber);
        header.order(ByteOrder.LITTLE_ENDIAN).putShort((short) codeArray.length);

        lineBytes.writeBytes(header.array());
        lineBytes.writeBytes(codeArray);
        return lineBytes.toByteArray();
    }

    private static void tokenize(String code, OutputStream out) throws IOException {
        ParseState state = ParseState.NORMAL;
        Matcher m = Pattern.compile("\\s+|[A-Za-z_][A-Za-z0-9_]*|\\d+\\.\\d+|\\d+|[+\\-*/=(),]|\\\"|\\S")
                           .matcher(code);

        boolean lastWasQuote = false;

        while (m.find()) {
            String token = m.group();
            String upper = token.toUpperCase();

            switch (state) {
                case NORMAL -> {
                    if (upper.equals("REM") || upper.equals("DATA") || upper.equals("LINE") || upper.equals("DEF")) {
                        if (upper.equals("DEF")) {
                            if (peekNextToken(m).equalsIgnoreCase("FN")) {
                                outToken(out, TOKENS.get("DEF"));
                                outToken(out, TOKENS.get("FN"));
                                state = ParseState.VERBATIM;
                                continue;
                            }
                        }
                        outToken(out, TOKENS.get(upper));
                        state = ParseState.VERBATIM;
                    } else if (upper.equals("\"")) {
                        out.write('"');
                        state = ParseState.QUOTE;
                    } else if (TOKENS.containsKey(upper)) {
                        outToken(out, TOKENS.get(upper));
                    } else if (isNumeric(token)) {
                        out.write(ZxNumberEncoder.encode(token));
                    } else {
                        for (char c : token.toCharArray()) out.write((byte) c);
                    }
                }

                case VERBATIM -> {
                    for (char c : token.toCharArray()) out.write((byte) c);
                }

                case QUOTE -> {
                    out.write(token.getBytes(StandardCharsets.ISO_8859_1));
                    if (token.equals("\"")) {
                        state = ParseState.NORMAL;
                    }
                }
            }
        }

        if (state == ParseState.QUOTE) {
            // auto-close quote if unterminated
            out.write('"');
        }
    }
    
    private static String peekNextToken(Matcher m) {
        int oldPos = m.end();
        if (m.find()) {
            String next = m.group();
            m.reset(); // rewind
            m.region(oldPos, m.regionEnd());
            return next;
        }
        return "";
    }


    
    private static void emitQuotedString(String token, OutputStream out) {
        try {
            for (char c : token.toCharArray()) {
                out.write((byte) c);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    
    private static boolean isNumeric(String str) {
        try {
            if (str.contains(".") || str.contains("e") || str.contains("E")) {
                Double.parseDouble(str);
            } else {
                Integer.parseInt(str);
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }


    private static void outToken(OutputStream out, int token) {
        try {
            if (token > 0xFF) {
                // Multi-byte token: write high byte (prefix) then low byte (sub-token)
                out.write((token >> 8) & 0xFF);
                out.write(token & 0xFF);
            } else {
                out.write(token);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // TODO: Sinclair 5-byte float encoding
    public static byte[] encodeSinclairFloat(double value) {
        // Placeholder
        return new byte[]{0, 0, 0, 0, 0};
    }
}
