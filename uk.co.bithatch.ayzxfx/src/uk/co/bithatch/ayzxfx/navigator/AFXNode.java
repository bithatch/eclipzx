package uk.co.bithatch.ayzxfx.navigator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IAdaptable;

import uk.co.bithatch.ayzxfx.ay.AFB;
import uk.co.bithatch.ayzxfx.ay.AFX;

public class AFXNode implements IAdaptable {

	private IFile file;
	private AFB afb;
	private AFX afx;

	public AFXNode(IFile file, AFB afb, AFX afx) {
		super();
		this.file = file;
		this.afb = afb;
		this.afx = afx;
	}

	public IFile getFile() {
		return file;
	}

	public AFB getAfb() {
		return afb;
	}

	public AFX getAfx() {
		return afx;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if(adapter.equals(IFile.class)) {
			return (T)file;
		}
		return null;
	}

}
