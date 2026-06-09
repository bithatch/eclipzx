package uk.co.bithatch.eclipzpp;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

final class ConstExpressionEvaluator {

	private final Map<String, String> defines;
	private final BiConsumer<Warning, String> onWarning;

	ConstExpressionEvaluator(Map<String, String> defines, BiConsumer<Warning, String> onWarning) {
		this.defines = defines;
		this.onWarning = onWarning;
	}

	long evaluate(String expr) {
		if(expr == null || expr.isBlank()) {
			throw new IllegalArgumentException("Constant expression is empty.");
		}
		var parser = new Parser(expr, new HashSet<>());
		var value = parser.parseConditional();
		parser.expectEnd();
		return value;
	}

	private long resolveSymbol(String symbol, Set<String> resolving) {
		if("ASMPC".equalsIgnoreCase(symbol)) {
			throw new IllegalArgumentException("ASMPC is not supported in preprocessor constant expressions.");
		}
		if(!defines.containsKey(symbol)) {
			onWarning.accept(Warning.UNDEFINED_EXPRESSION_SYMBOL,
					String.format("Undefined symbol '%s' in constant expression. Using 0.", symbol));
			return 0L;
		}
		if(resolving.contains(symbol)) {
			onWarning.accept(Warning.CIRCULAR_EXPRESSION_SYMBOL,
					String.format("Circular symbol reference for '%s' in constant expression. Using 0.", symbol));
			return 0L;
		}
		var expression = defines.get(symbol);
		if(expression == null || expression.isBlank()) {
			return 0L;
		}
		resolving.add(symbol);
		try {
			var parser = new Parser(expression, resolving);
			var value = parser.parseConditional();
			parser.expectEnd();
			return value;
		}
		catch(IllegalArgumentException iae) {
			throw new IllegalArgumentException(String.format("Symbol '%s' does not resolve to a constant expression: %s", symbol,
					iae.getMessage()), iae);
		}
		finally {
			resolving.remove(symbol);
		}
	}

	private final class Parser {
		private final String expr;
		private final Set<String> resolving;
		private int idx;
		private Token token;

		Parser(String expr, Set<String> resolving) {
			this.expr = expr;
			this.resolving = resolving;
		}

		long parseConditional() {
			var condition = parseLogicalOr();
			if(match(TokenType.QUESTION)) {
				var whenTrue = parseConditional();
				expect(TokenType.COLON, ":");
				var whenFalse = parseConditional();
				return condition != 0L ? whenTrue : whenFalse;
			}
			return condition;
		}

		void expectEnd() {
			if(peek().type != TokenType.END) {
				throw parseError("Unexpected token '" + peek().text + "'.");
			}
		}

		private long parseLogicalOr() {
			var left = parseLogicalAnd();
			while(matchOp("||")) {
				left = (left != 0L || parseLogicalAnd() != 0L) ? 1L : 0L;
			}
			return left;
		}

		private long parseLogicalAnd() {
			var left = parseBitOrXor();
			while(matchOp("&&")) {
				left = (left != 0L && parseBitOrXor() != 0L) ? 1L : 0L;
			}
			return left;
		}

		private long parseBitOrXor() {
			var left = parseBitAnd();
			while(true) {
				if(matchOp("|")) {
					left |= parseBitAnd();
				}
				else if(matchOp("^")) {
					left ^= parseBitAnd();
				}
				else {
					break;
				}
			}
			return left;
		}

		private long parseBitAnd() {
			var left = parseEqualsAndRelational();
			while(matchOp("&")) {
				left &= parseEqualsAndRelational();
			}
			return left;
		}

		private long parseEqualsAndRelational() {
			var left = parseShift();
			while(true) {
				if(matchOp("=") || matchOp("==")) {
					left = left == parseShift() ? 1L : 0L;
				}
				else if(matchOp("!=") || matchOp("<>")) {
					left = left != parseShift() ? 1L : 0L;
				}
				else if(matchOp("<=")) {
					left = left <= parseShift() ? 1L : 0L;
				}
				else if(matchOp(">=")) {
					left = left >= parseShift() ? 1L : 0L;
				}
				else if(matchOp("<")) {
					left = left < parseShift() ? 1L : 0L;
				}
				else if(matchOp(">")) {
					left = left > parseShift() ? 1L : 0L;
				}
				else {
					break;
				}
			}
			return left;
		}

		private long parseShift() {
			var left = parseAddSub();
			while(true) {
				if(matchOp("<<")) {
					left = left << (int)parseAddSub();
				}
				else if(matchOp(">>")) {
					left = left >> (int)parseAddSub();
				}
				else {
					break;
				}
			}
			return left;
		}

		private long parseAddSub() {
			var left = parseMulDivMod();
			while(true) {
				if(matchOp("+")) {
					left += parseMulDivMod();
				}
				else if(matchOp("-")) {
					left -= parseMulDivMod();
				}
				else {
					break;
				}
			}
			return left;
		}

		private long parseMulDivMod() {
			var left = parsePower();
			while(true) {
				if(matchOp("*")) {
					left *= parsePower();
				}
				else if(matchOp("/")) {
					var right = parsePower();
					if(right == 0L) {
						throw parseError("Division by zero.");
					}
					left /= right;
				}
				else if(matchOp("%")) {
					var right = parsePower();
					if(right == 0L) {
						throw parseError("Division by zero.");
					}
					left %= right;
				}
				else {
					break;
				}
			}
			return left;
		}

		private long parsePower() {
			var left = parseUnary();
			if(matchOp("**")) {
				var exponent = parsePower();
				left = pow(left, exponent);
			}
			return left;
		}

		private long parseUnary() {
			if(matchOp("+")) {
				return parseUnary();
			}
			if(matchOp("-")) {
				return -parseUnary();
			}
			if(matchOp("!")) {
				return parseUnary() == 0L ? 1L : 0L;
			}
			if(matchOp("~")) {
				return ~parseUnary();
			}
			return parsePrimary();
		}

		private long parsePrimary() {
			var tk = peek();
			if(match(TokenType.NUMBER)) {
				return tk.number;
			}
			if(match(TokenType.IDENT)) {
				return resolveSymbol(tk.text, resolving);
			}
			if(match(TokenType.LPAREN)) {
				var val = parseConditional();
				expect(TokenType.RPAREN, ")");
				return val;
			}
			if(match(TokenType.LBRACKET)) {
				var val = parseConditional();
				expect(TokenType.RBRACKET, "]");
				return val;
			}
			throw parseError("Expected number, symbol or parenthesized expression.");
		}

		private long pow(long base, long exponent) {
			if(exponent < 0L) {
				throw parseError("Negative exponents are not supported in constant expressions.");
			}
			long result = 1L;
			long b = base;
			long e = exponent;
			while(e > 0L) {
				if((e & 1L) != 0L) {
					result *= b;
				}
				b *= b;
				e >>= 1;
			}
			return result;
		}

		private boolean matchOp(String op) {
			var tk = peek();
			if(tk.type == TokenType.OP && op.equals(tk.text)) {
				nextToken();
				return true;
			}
			return false;
		}

		private boolean match(TokenType type) {
			if(peek().type == type) {
				nextToken();
				return true;
			}
			return false;
		}

		private void expect(TokenType type, String expected) {
			if(!match(type)) {
				throw parseError("Expected '" + expected + "'.");
			}
		}

		private Token peek() {
			if(token == null) {
				token = readToken();
			}
			return token;
		}

		private void nextToken() {
			token = null;
		}

		private Token readToken() {
			skipWhitespace();
			if(idx >= expr.length()) {
				return new Token(TokenType.END, "", 0L);
			}
			var c = expr.charAt(idx);
			if(isIdentStart(c)) {
				return readIdentifier();
			}
			if(Character.isDigit(c)) {
				return readNumberFromDigit();
			}
			if(c == '$') {
				return readHexPrefixed();
			}
			if(c == '\'') {
				return readCharLiteral();
			}
			if(c == '%' || c == '@') {
				if(idx + 1 < expr.length()) {
					var nc = expr.charAt(idx + 1);
					if(nc == '"') {
						return readBitmapBinary();
					}
					if(nc == '0' || nc == '1') {
						return readBinaryPrefixed();
					}
				}
			}
			if(matchText("**") || matchText("<<") || matchText(">>") || matchText("<=") || matchText(">=")
					|| matchText("==") || matchText("!=") || matchText("<>") || matchText("&&") || matchText("||")) {
				var op = expr.substring(idx, idx + 2);
				idx += 2;
				return new Token(TokenType.OP, op, 0L);
			}
			idx++;
			switch(c) {
			case '(':
				return new Token(TokenType.LPAREN, "(", 0L);
			case ')':
				return new Token(TokenType.RPAREN, ")", 0L);
			case '[':
				return new Token(TokenType.LBRACKET, "[", 0L);
			case ']':
				return new Token(TokenType.RBRACKET, "]", 0L);
			case '?':
				return new Token(TokenType.QUESTION, "?", 0L);
			case ':':
				return new Token(TokenType.COLON, ":", 0L);
			case '+':
			case '-':
			case '*':
			case '/':
			case '%':
			case '&':
			case '|':
			case '^':
			case '=':
			case '<':
			case '>':
			case '!':
			case '~':
				return new Token(TokenType.OP, String.valueOf(c), 0L);
			default:
				throw parseError("Unexpected character '" + c + "'.");
			}
		}

		private Token readIdentifier() {
			var start = idx;
			idx++;
			while(idx < expr.length() && isIdentPart(expr.charAt(idx))) {
				idx++;
			}
			return new Token(TokenType.IDENT, expr.substring(start, idx), 0L);
		}

		private Token readNumberFromDigit() {
			if(matchText("0x") || matchText("0X")) {
				idx += 2;
				var start = idx;
				while(idx < expr.length() && isHexDigit(expr.charAt(idx))) {
					idx++;
				}
				if(start == idx) {
					throw parseError("Expected hexadecimal digits after 0x.");
				}
				return new Token(TokenType.NUMBER, expr.substring(start - 2, idx), parseLong(expr.substring(start, idx), 16));
			}
			if(matchText("0b") || matchText("0B")) {
				idx += 2;
				var start = idx;
				while(idx < expr.length() && isBinaryDigit(expr.charAt(idx))) {
					idx++;
				}
				if(start == idx) {
					throw parseError("Expected binary digits after 0b.");
				}
				return new Token(TokenType.NUMBER, expr.substring(start - 2, idx), parseLong(expr.substring(start, idx), 2));
			}
			var start = idx;
			while(idx < expr.length() && Character.isLetterOrDigit(expr.charAt(idx))) {
				idx++;
			}
			var raw = expr.substring(start, idx);
			if(raw.endsWith("h") || raw.endsWith("H")) {
				var body = raw.substring(0, raw.length() - 1);
				if(body.isEmpty() || !isHexString(body)) {
					throw parseError("Invalid hexadecimal literal '" + raw + "'.");
				}
				return new Token(TokenType.NUMBER, raw, parseLong(body, 16));
			}
			if(raw.endsWith("b") || raw.endsWith("B")) {
				var body = raw.substring(0, raw.length() - 1);
				if(body.isEmpty() || !isBinaryString(body)) {
					throw parseError("Invalid binary literal '" + raw + "'.");
				}
				return new Token(TokenType.NUMBER, raw, parseLong(body, 2));
			}
			if(!isDecimalString(raw)) {
				throw parseError("Invalid number literal '" + raw + "'.");
			}
			return new Token(TokenType.NUMBER, raw, parseLong(raw, 10));
		}

		private Token readHexPrefixed() {
			idx++;
			var start = idx;
			while(idx < expr.length() && isHexDigit(expr.charAt(idx))) {
				idx++;
			}
			if(start == idx) {
				throw parseError("Expected hexadecimal digits after '$'.");
			}
			return new Token(TokenType.NUMBER, expr.substring(start - 1, idx), parseLong(expr.substring(start, idx), 16));
		}

		private Token readBinaryPrefixed() {
			idx++;
			var start = idx;
			while(idx < expr.length() && isBinaryDigit(expr.charAt(idx))) {
				idx++;
			}
			if(start == idx) {
				throw parseError("Expected binary digits after binary prefix.");
			}
			return new Token(TokenType.NUMBER, expr.substring(start - 1, idx), parseLong(expr.substring(start, idx), 2));
		}

		private Token readBitmapBinary() {
			idx += 2;
			var start = idx;
			while(idx < expr.length() && expr.charAt(idx) != '"') {
				var c = expr.charAt(idx);
				if(c != '#' && c != '-') {
					throw parseError("Binary bitmap only supports '#' and '-'.");
				}
				idx++;
			}
			if(idx >= expr.length() || expr.charAt(idx) != '"') {
				throw parseError("Unterminated bitmap literal.");
			}
			var raw = expr.substring(start, idx);
			idx++;
			var numeric = raw.replace('#', '1').replace('-', '0');
			if(numeric.isEmpty()) {
				throw parseError("Binary bitmap literal cannot be empty.");
			}
			return new Token(TokenType.NUMBER, raw, parseLong(numeric, 2));
		}

		private Token readCharLiteral() {
			idx++;
			if(idx >= expr.length()) {
				throw parseError("Unterminated character literal.");
			}
			char value;
			if(expr.charAt(idx) == '\\') {
				idx++;
				if(idx >= expr.length()) {
					throw parseError("Unterminated escape in character literal.");
				}
				value = switch(expr.charAt(idx)) {
				case 'n' -> '\n';
				case 'r' -> '\r';
				case 't' -> '\t';
				case '\\' -> '\\';
				case '\'' -> '\'';
				default -> expr.charAt(idx);
				};
				idx++;
			}
			else {
				value = expr.charAt(idx);
				idx++;
			}
			if(idx >= expr.length() || expr.charAt(idx) != '\'') {
				throw parseError("Character literal must contain exactly one character.");
			}
			idx++;
			return new Token(TokenType.NUMBER, "'" + value + "'", value);
		}

		private void skipWhitespace() {
			while(idx < expr.length() && Character.isWhitespace(expr.charAt(idx))) {
				idx++;
			}
		}

		private boolean matchText(String text) {
			return idx + text.length() <= expr.length() && expr.startsWith(text, idx);
		}

		private boolean isIdentStart(char c) {
			return Character.isLetter(c) || c == '_';
		}

		private boolean isIdentPart(char c) {
			return Character.isLetterOrDigit(c) || c == '_';
		}

		private boolean isBinaryDigit(char c) {
			return c == '0' || c == '1';
		}

		private boolean isHexDigit(char c) {
			return Character.isDigit(c) || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
		}

		private boolean isDecimalString(String str) {
			for(var i = 0; i < str.length(); i++) {
				if(!Character.isDigit(str.charAt(i))) {
					return false;
				}
			}
			return !str.isEmpty();
		}

		private boolean isBinaryString(String str) {
			for(var i = 0; i < str.length(); i++) {
				if(!isBinaryDigit(str.charAt(i))) {
					return false;
				}
			}
			return !str.isEmpty();
		}

		private boolean isHexString(String str) {
			for(var i = 0; i < str.length(); i++) {
				if(!isHexDigit(str.charAt(i))) {
					return false;
				}
			}
			return !str.isEmpty();
		}

		private long parseLong(String txt, int radix) {
			try {
				return Long.parseLong(txt, radix);
			}
			catch(NumberFormatException nfe) {
				throw parseError("Number out of range for long: '" + txt + "'.");
			}
		}

		private IllegalArgumentException parseError(String message) {
			return new IllegalArgumentException(message + " Expression: " + expr);
		}
	}

	private enum TokenType {
		NUMBER,
		IDENT,
		OP,
		LPAREN,
		RPAREN,
		LBRACKET,
		RBRACKET,
		QUESTION,
		COLON,
		END
	}

	private record Token(TokenType type, String text, long number) {
	}
}
