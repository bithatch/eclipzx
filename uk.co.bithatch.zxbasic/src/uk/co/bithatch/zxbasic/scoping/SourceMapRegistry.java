package uk.co.bithatch.zxbasic.scoping;

import java.util.Optional;

import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.impl.AdapterImpl;
import org.eclipse.emf.ecore.resource.Resource;

import uk.co.bithatch.zxbasic.preprocessor.SourceMap;

public class SourceMapRegistry {
	
	private final static ThreadLocal<SourceMap> current = new ThreadLocal<>();

    private static class SourceMapAdapter extends AdapterImpl {
        private final SourceMap sourceMap;

        public SourceMapAdapter(SourceMap map) {
            this.sourceMap = map;
        }

        @Override
        public boolean isAdapterForType(Object type) {
            return type == SourceMap.class;
        }

        public SourceMap getSourceMap() {
            return sourceMap;
        }
    }
    
    public static void set(SourceMap current) {
    	if(current == null)
        	SourceMapRegistry.current.remove();
    	else {
    		if(get() != null)
    			throw new IllegalStateException("Cannot set source map without unsetting previous. Attempt to re-enter, or map was never unset.");
    		SourceMapRegistry.current.set(current);
    	}
    }
    
    public static SourceMap get() {
    	return SourceMapRegistry.current.get();
    }

    public static void attach(Resource resource, SourceMap map) {
        remove(resource); // in case one already exists
        resource.eAdapters().add(new SourceMapAdapter(map));
    }

    public static Optional<SourceMap> get(Resource resource) {
        for (Adapter adapter : resource.eAdapters()) {
            if (adapter instanceof SourceMapAdapter mapAdapter) {
                return Optional.of(mapAdapter.getSourceMap());
            }
        }
        return Optional.empty();
    }

    public static void remove(Resource resource) {
        resource.eAdapters().removeIf(a -> a instanceof SourceMapAdapter);
    }

    // Convenience method: get SourceMap from any EObject
    public static Optional<SourceMap> get(org.eclipse.emf.ecore.EObject obj) {
        if (obj == null || obj.eResource() == null) return Optional.empty();
        return get(obj.eResource());
    }
}
