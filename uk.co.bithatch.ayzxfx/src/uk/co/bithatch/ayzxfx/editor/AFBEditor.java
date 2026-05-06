package uk.co.bithatch.ayzxfx.editor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.BorderData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;

import uk.co.bithatch.ayzxfx.AYFXUtil;
import uk.co.bithatch.ayzxfx.ay.AFB;
import uk.co.bithatch.ayzxfx.ay.AFX;
import uk.co.bithatch.ayzxfx.ay.NamedAFX;

public class AFBEditor extends AFXEditor {

	public static AFBEditor findOpenAFBEditorFor(IFile file) {
		var window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (window == null)
			return null;

		var page = window.getActivePage();
		if (page == null)
			return null;

		for (var ref : page.getEditorReferences()) {
			var editor = ref.getEditor(false); // don't force loading
			if (editor instanceof AFBEditor afbEditor) {
				IEditorInput input = editor.getEditorInput();
				if (input instanceof IFileEditorInput fileEditorInput) {
					IFile editorFile = fileEditorInput.getFile();
					if (editorFile.equals(file)) {
						return afbEditor;
					}
				}
			}
		}

		return null;
	}

	private AFB afb;
	private Text effectName;
	private Button prevButton;
	private Button nextButton;

	@Override
	protected AFX load(IPath loc) throws PartInitException {
		try (var in = Files.newByteChannel(loc.toPath())) {
			afb = AFB.load(in);
			return afb.get(0);
		} catch (IOException e) {
			throw new PartInitException(Status.error("Failed to load AFB file.", e));
		}
	}

	public void addEffect() {
		execute(new AddEffectOperation(this, AFX.create()));
	}

	public void addEffect(AFX effect) {
		execute(new AddEffectOperation(this, effect));
	}

	public void removeEffects(AFX... effects) {
		execute(new RemoveEffectsOperation(this, effects));
	}

	public void renameEffect(AFX effect, String newName) {
		execute(new UpdateEffectNameOperation(this, effect, newName));
	}

	public void afx(AFX afx) {
		if (!afb.contains(afx)) {
			throw new IllegalArgumentException("The AFX must be in the AFB");
		}
		if(afx instanceof NamedAFX nafx) {
			effectName.setText(nafx.name() == null ? "" : nafx.name());	
		}
		else {
			effectName.setText("");
		}
		super.afx(afx);
		AYFXUtil.refreshFileInExplorer(getFile());
		updateAvailable();
	}

	public AFB afb() {
		return afb;
	}

	@Override
	public void dispose() {
		super.dispose();
		AYFXUtil.refreshFileInExplorer(getFile());
	}
	
	@Override
	protected void onPartControl() {
		AYFXUtil.refreshFileInExplorer(getFile());
		
		var bars = getEditorSite().getActionBars();

		bars.setGlobalActionHandler(ActionFactory.COPY.getId(), new Action("Copy Effect") {
			@Override
			public void run() {
				var afx = model != null ? model.afx() : null;
				if (afx != null) {
					var clipboard = new Clipboard(PlatformUI.getWorkbench().getDisplay());
					try {
						clipboard.setContents(
							new Object[] { afx },
							new Transfer[] { AFXTransfer.getInstance() }
						);
					} finally {
						clipboard.dispose();
					}
				}
			}
		});

		bars.setGlobalActionHandler(ActionFactory.PASTE.getId(), new Action("Paste Effect") {
			@Override
			public void run() {
				var activeEditor = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
						.getActivePage().getActiveEditor();
				if (!(activeEditor instanceof AFBEditor target))
					return;
				var clipboard = new Clipboard(PlatformUI.getWorkbench().getDisplay());
				try {
					var contents = clipboard.getContents(AFXTransfer.getInstance());
					if (contents instanceof AFX afx) {
						var copy = AFX.create();
						copy.remove(0);
						for (var frame : afx.frames()) {
							copy.add(frame.copy());
						}
						if (afx instanceof NamedAFX nafx && nafx.name() != null) {
							var named = copy.named();
							named.name(nafx.name() + " (Copy)");
							target.addEffect(named);
						} else {
							target.addEffect(copy);
						}
					}
				} finally {
					clipboard.dispose();
				}
			}
		});

		bars.updateActionBars();
	}

	@Override
	protected void createAccessory(Composite root) {

		var effectDetails = new Composite(root, SWT.NONE);
		effectDetails.setLayoutData(new BorderData(SWT.TOP));
		effectDetails.setLayout(new GridLayout(4, false));

		var lbl = new Label(effectDetails, SWT.NONE);
		lbl.setText("Effect Name:");
		lbl.setLayoutData(GridDataFactory.defaultsFor(lbl).create());

		effectName = new Text(effectDetails, SWT.NONE);
		effectName.addTraverseListener(event -> {
			if (event.detail == SWT.TRAVERSE_RETURN) {
				setFocus();
				execute(new UpdateEffectNameOperation(AFBEditor.this, model.afx(), effectName.getText()));
			}
		});
		effectName.setMessage("<unnamed>");
		effectName.setLayoutData(GridDataFactory.defaultsFor(effectName).grab(true, false).create());
		effectName.addFocusListener(FocusListener.focusLostAdapter(e -> {
			execute(new UpdateEffectNameOperation(this, model.afx(), effectName.getText()));
		}));

		prevButton = new Button(effectDetails, SWT.PUSH);
		prevButton.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_BACK));
		prevButton.setLayoutData(GridDataFactory.defaultsFor(prevButton).create());
		prevButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			afx(afb.get(afb.indexOf(model.afx()) - 1));
		}));

		nextButton = new Button(effectDetails, SWT.PUSH);
		nextButton.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_FORWARD));
		nextButton.setLayoutData(GridDataFactory.defaultsFor(nextButton).create());
		nextButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			afx(afb.get(afb.indexOf(model.afx()) + 1));
		}));

		updateAvailable();

		if(afb != null && afb.size() > 0) {
			afx(afb.get(0));
		}
	}

	String setName(AFX effect, String newName) {
		// Ensure we have a NamedAFX, replacing in the AFB if needed
		NamedAFX namedAfx;
		if (effect instanceof NamedAFX nafx) {
			namedAfx = nafx;
		} else {
			namedAfx = effect.named();
			var idx = afb.indexOf(effect);
			if (idx >= 0) {
				afb.remove(effect);
				afb.add(idx, namedAfx);
			}
		}
		var was = namedAfx.name();
		if (!Objects.equals(was, newName)) {
			namedAfx.name(newName);
			// Only update the UI text field if this is the currently displayed effect
			if (model.afx() == effect || model.afx() == namedAfx) {
				effectName.setText(newName == null ? "" : newName);
			}
			markDirty();
			AYFXUtil.refreshFileInExplorer(getFile());
		}
		return was;
	}

	private void updateAvailable() {
		var afx = model.afx();
		if (afx != null) {
			var idx = afb.indexOf(afx);
			nextButton.setEnabled(idx < afb.size() - 1);
			prevButton.setEnabled(idx > 0);
		}
	}

	@Override
	protected void doSave(IFile file) throws IOException {
		try (var wtr = Files.newByteChannel(file.getLocation().toPath(), StandardOpenOption.WRITE,
				StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)) {
			afb.save(wtr);
		}
	}

	void doAddEffect(AFX effect) {
		doAddEffect(afb.size(), effect);
	}

	void doAddEffect(int index, AFX effect) {
		afb.add(index, effect);
		afx(effect);
		markDirty();
		AYFXUtil.refreshFileInExplorer(getFile());
	}

	public void moveEffect(int fromIndex, int toIndex) {
		if (fromIndex != toIndex) {
			execute(new MoveEffectOperation(this, fromIndex, toIndex));
		}
	}

	void doMoveEffect(int fromIndex, int toIndex) {
		afb.move(fromIndex, toIndex);
		markDirty();
		AYFXUtil.refreshFileInExplorer(getFile());
	}

	void doRemoveEffects(AFX... effects) {

		for (var effect : effects) {
			var idx = afb.indexOf(effect);
			afb.remove(effect);
			if (afb.size() > 0)
				afx(afb.get(Math.max(0, Math.min(afb.size() - 1, idx))));
			else
				afx(null);
		}
		AYFXUtil.refreshFileInExplorer(getFile());
	}
}
