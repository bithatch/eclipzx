package uk.co.bithatch.zxbasic.interpreter;

import java.util.List;

/**
 * Encapsulate an array and the type of its contents.
 */
public record ArrayValue(VarType contentType, Object[] data, List<Integer> lowerBounds, List<Integer> upperBounds) { 
	
	public ArrayValue {
		if(lowerBounds.size() != upperBounds.size())
			throw new IllegalArgumentException();
	}
	
	public int dimensions() {
		return lowerBounds.size();
	}
}