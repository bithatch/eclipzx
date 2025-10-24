package uk.co.bithatch.zxbasic.scoping;

import org.eclipse.xtext.naming.IQualifiedNameConverter;
import org.eclipse.xtext.naming.QualifiedName;

import com.google.inject.Inject;

import uk.co.bithatch.zxbasic.ILanguageSettings;
import uk.co.bithatch.zxbasic.interpreter.ZXStdlib;

public class ZXBasicQualifiedNameConverter extends IQualifiedNameConverter.DefaultImpl {
	@Inject
	private ILanguageSettings languageSettings;

    @Override
    public QualifiedName toQualifiedName(String origName) {    	
		var name = origName;
		if(ZXStdlib.get().isDefined(origName)) {
			name = ScopingUtils.normalizeIdentifier(origName, languageSettings, null);
		}
//    	if(name.startsWith("@")) {
//    		name = origName.substring(1);
//    		System.out.println("tqn: " + origName + " -> " + name);
//    	}
//    	else {
////    		name = ScopingUtils.normalizeIdentifier(name); 
//    		if(!name.equals(origName)) 
//    			System.out.println("tqn: " + origName + " -> " + name);
//    	}
//		if(name.equals(origName)) {
//			System.out.println("nc: " + origName);
//		}
			
        return super.toQualifiedName(name);
    }

    @Override
    public String toString(QualifiedName qualifiedName) {
        return super.toString(qualifiedName);
    }
}
