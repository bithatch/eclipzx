package uk.co.bithatch.emuzx.debug;

import org.eclipse.debug.core.model.IStackFrame;

public interface ILocatableStackFrame extends IStackFrame {

	/**
	 * @return the source file name, or null if unknown
	 */
	String getSourceName();

}
