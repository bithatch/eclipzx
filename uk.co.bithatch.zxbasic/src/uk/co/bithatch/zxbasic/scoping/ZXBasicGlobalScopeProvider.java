package uk.co.bithatch.zxbasic.scoping;

import java.util.LinkedHashSet;

import org.eclipse.core.runtime.ILog;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.resource.IResourceDescription;
import org.eclipse.xtext.scoping.impl.ImportUriGlobalScopeProvider;
import org.eclipse.xtext.util.IResourceScopeCache;

import com.google.common.base.Splitter;
import com.google.inject.Inject;
import com.google.inject.Provider;

import uk.co.bithatch.eclipzpp.IMappedResource;
import uk.co.bithatch.eclipzpp.ResourceResolver;
import uk.co.bithatch.zxbasic.ZxBasicIncludeSource;
import uk.co.bithatch.zxbasic.basic.BasicPackage;

public class ZXBasicGlobalScopeProvider extends ImportUriGlobalScopeProvider {
	public final static ILog LOG = ILog.of(ZXBasicGlobalScopeProvider.class);

	private static final Splitter SPLITTER = Splitter.on(',');

	@Inject
	private IResourceDescription.Manager descriptionManager;

	@Inject
	private IResourceScopeCache cache;
	
	@Inject(optional = true)
	private ZxBasicIncludeSource includeSource;

	@Override
	public LinkedHashSet<URI> getImportedUris(Resource resource) {
		System.out.println("XXXXX getImportedUris " + resource);
		return cache.get(ZXBasicGlobalScopeProvider.class.getSimpleName(), resource,
				new Provider<LinkedHashSet<URI>>() {
					@Override
					public LinkedHashSet<URI> get() {
						var uniqueImportURIs = new LinkedHashSet<URI>(5);
						if(includeSource != null) {
							uniqueImportURIs.addAll(includeSource.importUris(resource));
						}
						collectImportUris(resource, uniqueImportURIs);
						var uriIter = uniqueImportURIs.iterator();
						while (uriIter.hasNext()) {
							var uri = uriIter.next();
							if (!EcoreUtil2.isValidUri(resource, uri)) {
								System.out.println("    Removing " + uri);
								uriIter.remove();
							}
						}
						
						uniqueImportURIs.forEach(u -> {
							System.out.println("    = " + u);
						});
						
						return uniqueImportURIs;
					}
				});
	}

	private void collectImportUris(Resource resource,
			LinkedHashSet<URI> uniqueImportURIs) {
		var resourceDescription = descriptionManager.getResourceDescription(resource);
		var models = resourceDescription.getExportedObjectsByType(BasicPackage.Literals.PROGRAM);
		models.forEach(mdl -> {
			var userData = mdl.getUserData(ZXBasicResourceDescriptionStrategy.INCLUDES);
			if (userData != null) {
				SPLITTER.split(userData).forEach(uri -> {
					var includedUri =  ResourceResolver.makePlatformURI(((IMappedResource)resource).getFile().orElseThrow(), uri);
					includedUri = includedUri.resolve(resource.getURI());
					if (uniqueImportURIs.add(includedUri)) {
						try {
							var impResource = resource.getResourceSet().getResource(includedUri, true);
							collectImportUris(impResource,
									uniqueImportURIs);
						}
						catch(Exception e) {
							LOG.error("Failed to collect import URIs", e);
						}
					}
				});
			}
		});
	}
}
