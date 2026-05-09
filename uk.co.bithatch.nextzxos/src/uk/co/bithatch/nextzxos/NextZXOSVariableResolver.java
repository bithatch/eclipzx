package uk.co.bithatch.nextzxos;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.variables.IDynamicVariable;
import org.eclipse.core.variables.IDynamicVariableResolver;

import uk.co.bithatch.bitzx.LaunchContext;

public class NextZXOSVariableResolver implements IDynamicVariableResolver {
	
	public final static String NEXT_ZXOS_IMAGE = "next_zxos_image";
	
	@Override
	public String resolveValue(IDynamicVariable variable, String argument) throws CoreException {
		if(variable.getName().equals(NEXT_ZXOS_IMAGE)) {
			var lc = LaunchContext.get();
			// TODO
		}
		return "";
	}

}
