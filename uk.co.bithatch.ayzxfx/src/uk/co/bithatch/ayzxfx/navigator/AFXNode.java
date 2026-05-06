package uk.co.bithatch.ayzxfx.navigator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IAdaptable;

import uk.co.bithatch.ayzxfx.ay.AFB;
import uk.co.bithatch.ayzxfx.ay.AFX;

public class AFXNode implements IAdaptable, Comparable<AFXNode> {

	private IFile file;
	private AFB afb;
	private AFX afx;
	private int index;

	public AFXNode(IFile file, AFB afb, AFX afx, int index) {
		super();
		this.file = file;
		this.afb = afb;
		this.afx = afx;
		this.index = index;
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

	public int getIndex() {
		return index;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAdapter(Class<T> adapter) {
//		if(adapter.equals(IFile.class)) {
//			return (T)file;
//		}
		return null;
	}

	@Override
	public int compareTo(AFXNode o) {
		return Integer.compare(this.index, o.index);
	}

}
