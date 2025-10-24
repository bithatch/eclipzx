package uk.co.bithatch.zxbasic.interpreter;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;

import uk.co.bithatch.zxbasic.basic.DataStmt;
import uk.co.bithatch.zxbasic.basic.Expr;
import uk.co.bithatch.zxbasic.basic.Program;

public class DataIterator implements Iterator<Expr> {
	
	private TreeIterator<EObject> allContents;
	private Iterator<Expr> exprIt;
	private Expr next;

	DataIterator(Program  program) {
		allContents = program.eAllContents();
	}

	@Override
	public boolean hasNext() {
		checkNext();
		return next != null;
	}

	@Override
	public Expr next() {
		checkNext();
		if(next == null)
			throw new NoSuchElementException();
		else {
			try {
				return next;
			}
			finally {
				next = null;
			}
		}
	}
	
	private void checkNext() {
		if(next == null) {
			
			if(exprIt != null && !exprIt.hasNext()) {
				exprIt = null;
			}
			
			if(exprIt == null) {
				while(allContents.hasNext()) {
					var next = allContents.next();
					if(next  instanceof DataStmt ds) {
						var it = ds.getData().iterator();
						if(it.hasNext()) {
							exprIt = it;
							break;
						}
					}
				}
			}
			
			if(exprIt != null) {
				next = exprIt.next();
			}
		}
	}

}
