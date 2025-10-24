package uk.co.bithatch.fatexplorer.variables;

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.variables.IDynamicVariable;
import org.eclipse.core.variables.IDynamicVariableResolver;

import uk.co.bithatch.fatexplorer.vfs.FATImageFileSystem;

public class FATImagePathVariableResolver implements IDynamicVariableResolver {
	
	@Override
	public String resolveValue(IDynamicVariable variable, String argument) throws CoreException {
		if(variable.getName().equals(FATImageContext.IMAGE)) {
			var disk = argument == null ?  FATImageContext.get(variable.getName(), "") : argument;
			if(disk == null)
				return null;
			else if(new File(disk).isAbsolute())  {
				return disk; 
			}
			else {
				while(disk.startsWith("/")) {
					disk = disk.substring(1);
				}
				
				return FATImageFileSystem.toDiskFile(disk).getAbsolutePath();
			}
		}
		else 
			return FATImageContext.get(variable.getName(), "");
	}

}
