package uk.co.bithatch.zxbasic.ui.debug;

import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.IDebugTarget;

public class DelegatingDebugElement implements IDebugElement {
		private IDebugElement delegate;

		DelegatingDebugElement(IDebugElement delegate) {
			this.delegate = delegate;
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T> T getAdapter(Class<T> adapter) {
//			if(adapter.equals(delegate.getClass())) {
//				return (T)delegate;
//			}
//			return delegate.getAdapter(adapter);
			return null;
		}

		@Override
		public String getModelIdentifier() {
			return delegate.getModelIdentifier();
		}

		@Override
		public IDebugTarget getDebugTarget() {
			return delegate.getDebugTarget();
		}

		@Override
		public ILaunch getLaunch() {
			return delegate.getLaunch();
		}
		
		protected IDebugElement delegate() {
			return delegate;
		}
	}