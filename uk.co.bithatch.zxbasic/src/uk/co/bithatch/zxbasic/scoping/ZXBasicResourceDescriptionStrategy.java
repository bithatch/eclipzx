package uk.co.bithatch.zxbasic.scoping;

import java.util.Map;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.resource.EObjectDescription;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.resource.impl.DefaultResourceDescriptionStrategy;
import org.eclipse.xtext.scoping.impl.ImportUriResolver;
import org.eclipse.xtext.util.IAcceptor;

import com.google.inject.Inject;

import uk.co.bithatch.zxbasic.IIncludeSource;
import uk.co.bithatch.zxbasic.ILanguageSettings;
import uk.co.bithatch.zxbasic.basic.Group;
import uk.co.bithatch.zxbasic.basic.PPInclude;
import uk.co.bithatch.zxbasic.basic.Program;
import uk.co.bithatch.zxbasic.basic.Referable;

public class ZXBasicResourceDescriptionStrategy  extends DefaultResourceDescriptionStrategy {
	public static final String INCLUDES = "includes";
	
	@Inject
	private ImportUriResolver uriResolver;
	@Inject
	private ILanguageSettings languageSettings;
	@Inject(optional = true)
	private IIncludeSource includeSource;

	@Override
	public boolean createEObjectDescriptions(EObject eObject, IAcceptor<IEObjectDescription> acceptor) {
		if(eObject instanceof Program pgm) {
			createEObjectDescriptionForModel(pgm, acceptor);
			return true;
		}  else if (eObject instanceof Referable ref && ref.getName() != null) {
			var name = ScopingUtils.normalizeIdentifier(ref.getName(), languageSettings, ref);
	        acceptor.accept(EObjectDescription.create(QualifiedName.create(name), ref));
	        
	        if(!ref.getName().equals(name)) {
		        acceptor.accept(EObjectDescription.create(QualifiedName.create(ref.getName()), ref));
	        }
	        return true;
	    }  
		else if (eObject instanceof Group group && ScopingUtils.hasLabel(group)) {
	        var name = QualifiedName.create(ScopingUtils.numberOrLabel(group));
	        acceptor.accept(EObjectDescription.create(name, group));
	        return true;
	    }
		else {
			return super.createEObjectDescriptions(eObject, acceptor);
		}
	}
	
	public void createEObjectDescriptionForModel(Program model, IAcceptor<IEObjectDescription> acceptor) {
		var includeList = model.getProgram().getGroups().
				stream().
				filter(g -> g instanceof PPInclude).
				map(g -> uriResolver.apply(((PPInclude)g))).
				map(u -> {
					 if(u.startsWith("<") && u.endsWith(">")) {
						u = u.substring(1, u.length() - 1);
						var path = includeSource.find(model.eResource(), u);
						if(path != null && path.toFile().exists()) {
							return path.toUri().toString();
						}
					}
				    return u;
				}).
				filter(l -> l != null).
				toList();
		if(includeList.isEmpty()) {
			return;
		}
		var includeUris = String.join(",", includeList);
		var modelUri = QualifiedName.create(model.eResource().getURI().toString());
		acceptor.accept(EObjectDescription.create(modelUri, model, Map.of(
				INCLUDES, 
			includeUris
		)));
	}
}
