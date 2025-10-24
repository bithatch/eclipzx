package uk.co.bithatch.zxbasic.tools;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
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
        // Example subset – expand as needed
        TOKENS.put("PRINT", 0xF5);
        TOKENS.put("LET", 0xF0);
        TOKENS.put("IF", 0xF6);
        TOKENS.put("THEN", 0xF7);
        TOKENS.put("+", 0xA4);
        TOKENS.put("-", 0xA5);
        TOKENS.put("*", 0xA6);
        TOKENS.put("/", 0xA7);
        TOKENS.put("(", 0xA8);
        TOKENS.put(")", 0xA9);
        TOKENS.put("=", 0xB0);
        
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
    	return encodeProgram(Arrays.asList(lines.split(System.lineSeparator())));
    }

    public static byte[] encodeProgram(Iterable<String> lines) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (String line : lines) {
            byte[] encoded = encodeLine(line);
            out.writeBytes(encoded);
        }
        out.write(0); // program terminator
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

        ByteBuffer header = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
        header.putShort((short) lineNumber);
        header.putShort((short) codeArray.length);

        lineBytes.writeBytes(header.array());
        lineBytes.writeBytes(codeArray);
        return lineBytes.toByteArray();
    }

    private static void tokenize(String code, OutputStream out) throws IOException {
        ParseState state = ParseState.NORMAL;
        Matcher m = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*|\\d+\\.\\d+|\\d+|[+\\-*/=(),]|\\\"|\\S")
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
                    out.write(token.getBytes());
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
            out.write(token);
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
