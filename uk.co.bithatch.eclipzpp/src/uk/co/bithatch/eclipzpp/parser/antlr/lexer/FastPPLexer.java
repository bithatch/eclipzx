package uk.co.bithatch.eclipzpp.parser.antlr.lexer;

import org.antlr.runtime.CharStream;
import org.antlr.runtime.CommonToken;
import org.antlr.runtime.RecognizerSharedState;
import org.antlr.runtime.Token;

/**
 * Runtime lexer wrapper that coalesces consecutive RULE_ANY_OTHER tokens
 * into a single token to reduce token churn when parsing raw text blocks.
 */
public class FastPPLexer extends InternalPPLexer {

	private Token buffered;

	public FastPPLexer() {
		super();
	}

	public FastPPLexer(CharStream input) {
		super(input);
	}

	public FastPPLexer(CharStream input, RecognizerSharedState state) {
		super(input, state);
	}

	@Override
	public Token nextToken() {
		if (buffered != null) {
			Token t = buffered;
			buffered = null;
			return t;
		}

		Token first = super.nextToken();
		if (first == null || first.getType() != RULE_ANY_OTHER) {
			return first;
		}

		StringBuilder text = new StringBuilder(first.getText() == null ? "" : first.getText());
		int start = first instanceof CommonToken ? ((CommonToken) first).getStartIndex() : -1;
		int stop = first instanceof CommonToken ? ((CommonToken) first).getStopIndex() : -1;

		while (true) {
			Token next = super.nextToken();
			if (next == null) {
				break;
			}
			if (next.getType() == RULE_ANY_OTHER) {
				if (next.getText() != null) {
					text.append(next.getText());
				}
				if (next instanceof CommonToken) {
					stop = ((CommonToken) next).getStopIndex();
				}
				continue;
			}
			buffered = next;
			break;
		}

		CommonToken merged = new CommonToken(RULE_ANY_OTHER, text.toString());
		merged.setLine(first.getLine());
		merged.setCharPositionInLine(first.getCharPositionInLine());
		merged.setChannel(first.getChannel());
		merged.setStartIndex(start);
		merged.setStopIndex(stop);
		return merged;
	}
}
