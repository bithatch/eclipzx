package uk.co.bithatch.zxbasic.scoping;

import java.util.LinkedHashSet;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.resource.IResourceDescription;
import org.eclipse.xtext.scoping.impl.ImportUriGlobalScopeProvider;
import org.eclipse.xtext.util.IResourceScopeCache;

import com.google.common.base.Splitter;
import com.google.inject.Inject;
import com.google.inject.Provider;

import uk.co.bithatch.zxbasic.basic.BasicPackage;

public class ZXBasicGlobalScopeProvider extends ImportUriGlobalScopeProvider {

	private static final Splitter SPLITTER = Splitter.on(',');

	@Inject
	private IResourceDescription.Manager descriptionManager;

	@Inject
	private IResourceScopeCache cache;

	@Override
	public LinkedHashSet<URI> getImportedUris(Resource resource) {
		return cache.get(ZXBasicGlobalScopeProvider.class.getSimpleName(), resource,
				new Provider<LinkedHashSet<URI>>() {
					@Override
					public LinkedHashSet<URI> get() {
						var uniqueImportURIs = collectImportUris(resource, new LinkedHashSet<URI>(5));

						var uriIter = uniqueImportURIs.iterator();
						while (uriIter.hasNext()) {
							if (!EcoreUtil2.isValidUri(resource, uriIter.next()))
								uriIter.remove();
						}
						return uniqueImportURIs;
					}

					public LinkedHashSet<URI> collectImportUris(Resource resource,
							LinkedHashSet<URI> uniqueImportURIs) {
						var resourceDescription = descriptionManager.getResourceDescription(resource);
						var models = resourceDescription.getExportedObjectsByType(BasicPackage.Literals.PROGRAM);
						models.forEach(mdl -> {
							var userData = mdl.getUserData(ZXBasicResourceDescriptionStrategy.INCLUDES);
							if (userData != null) {
								SPLITTER.split(userData).forEach(uri -> {
									
									var includedUri = URI.createURI(uri);
									includedUri = includedUri.resolve(resource.getURI());
									if (uniqueImportURIs.add(includedUri)) {
										collectImportUris(resource.getResourceSet().getResource(includedUri, true),
												uniqueImportURIs);
									}
								});
							}
						});
						return uniqueImportURIs;
					}
				});
	}
}
