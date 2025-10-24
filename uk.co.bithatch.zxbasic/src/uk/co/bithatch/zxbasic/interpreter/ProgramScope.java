package uk.co.bithatch.zxbasic.interpreter;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Stack;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;

import uk.co.bithatch.zxbasic.basic.Group;
import uk.co.bithatch.zxbasic.interpreter.ZXBasicInterpreter.GotoEscape;

final class ProgramScope implements Iterator<Group> {
	private static boolean CASE_SENSITIVE= Boolean.getBoolean("zxbasic.caseSensitveVariableAndConstants");
	
	private final class ScopeIterator implements Iterator<Group> {
		private EList<Group> lines;
		private int index;
		

		private ScopeIterator(EList<Group> lines, int index) {
			super();
			this.lines = lines;
			this.index = index;
		}
		
		@Override
		public boolean hasNext() {
			return index + 1 < lines.size();
		}

		@Override
		public Group next() {
			if(!hasNext())
				throw new NoSuchElementException();
			return lines.get(++index);
		}
	}

	public static final String RETURN_VALUE = "%retVal%";
	
	private Stack<ScopeIterator> stack = new Stack<ScopeIterator>();
	private ScopeIterator current = null;
    private final Map<String, Var> variables = new HashMap<>();
    private final Map<String, Var> constants = new HashMap<>();
	private final Optional<ProgramScope> parent;
	private final EObject part;

	public ProgramScope(EObject part, EList<Group> lines) {
		this(part, lines, Optional.empty());
	}
	
	private ProgramScope(EObject part, EList<Group> lines, Optional<ProgramScope> parent) {
		stack.push(new ScopeIterator(lines, -1));
		this.parent = parent;
		this.part = part;
	}

	public void removeVar(String varName) {
		variables.remove(processKey(varName));
	}

	public Var popVar(String varName) {
		var val = getConst(varName);
		if(val == null) {
			val = variables.remove(processKey(varName));
			if(val == null && parent.isPresent()) {
				val = parent.get().popVar(varName);
			}
		}
		return val;
	}

	public Var getVar(String varName, int[] arrayIndexes) {
		/* TODO get from array indexes */
		var val = getConst(varName);
		if(val == null) {
			val = variables.get(processKey(varName));
			if(val == null && parent.isPresent()) {
				val = parent.get().getVar(varName, arrayIndexes);
			}
		}
		return val;
	}

	public void pop() {
		stack.pop();
		current = null;
	}
	
	public void putVar(String name, Var value, int... arrayIndexes) {
		checkConstPut(name);
		/* TODO put into correct array index */
		variables.put(processKey(name), value);	
	}

	protected void checkConstPut(String name) {
		if(isConst(name)) {
			throw new IllegalStateException(name + " is constant.");
		}
	}
	
	public void putConst(String name, Var value) {
		constants.put(processKey(name), value);
	}

	@Override
	public boolean hasNext() {
		checkNext();
		return current != null && current.hasNext();
	}

	public ProgramScope newScope(EObject part, EList<Group> lines) {
		return new ProgramScope(part, lines, Optional.of(this));
	}

	@Override
	public Group next() {
		checkNext();
		if(current == null)
			throw new NoSuchElementException();
		try {
			return current.next();
		}
		finally {
			current = null;
		}
	}
	
	public boolean isConst(String key) {
		return constants.containsKey(processKey(key)) || parent.map(p -> p.isConst(processKey(key))).orElse(false);
	}

	public void exit() {
		if(stack.size() > 1) {
			pop();
		}
	}
	
	private Var getConst(String key) {
		var v = constants.get(processKey(key));
		return v == null ? parent.map(p -> p.getConst(key)).orElse(null) : v;
	}
	
	void push(EList<Group> lines) {
		stack.push(new ScopeIterator(lines, -1));
	}

	void gosub(Group target) {
		for(var i = stack.size() - 1 ; i >= 0 ; i--) {
			var top = stack.get(i);
			var idx = top.lines.indexOf(target);
			if(idx != -1) {
				stack.push(new ScopeIterator(top.lines, idx));
				return;
			}
		}
		throw new IllegalArgumentException("GOSUB target not found.");
	}

	void gotoLine(Group target) {
		while(stack.size() > 0) {
			var top = stack.peek();
			var idx = top.lines.indexOf(target);
			if(idx == -1) {
				pop();
			}
			else {
				top.index = idx;
				return;
			}
		}
		throw new GotoEscape();
	}
	
	private void checkNext() {
		if(current == null) {
			while(stack.size() > 0) {
				var top = stack.peek();
				if(top.hasNext()) {
					current = top;
				}
				return;
			}
		}
	}

	public void returnScope(Var retVal) {
		putVar(RETURN_VALUE, retVal);
		returnScope();
	}

	public void returnScope() {
		while(!stack.isEmpty())
			pop();
	}

	public EObject part() {
		return part;
	}
	
	private String processKey(String key) {
		if(CASE_SENSITIVE) {
			return key;
		}
		else {
			return key.toLowerCase();
		}
	}
}