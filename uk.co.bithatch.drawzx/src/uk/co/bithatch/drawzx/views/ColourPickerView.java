package uk.co.bithatch.drawzx.views;

import static uk.co.bithatch.drawzx.editor.EditorFileProperties.setProperty;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.part.ViewPart;

import uk.co.bithatch.drawzx.Activator;
import uk.co.bithatch.drawzx.editor.EditorFileProperties;
import uk.co.bithatch.drawzx.editor.EditorFileProperties.PaletteSource;
import uk.co.bithatch.drawzx.editor.IColouredEditor;
import uk.co.bithatch.drawzx.widgets.PaletteGrid;
import uk.co.bithatch.zyxy.graphics.Palette;

public class ColourPickerView extends ViewPart implements IPartListener2, IColourPicker {
	public static final String ID = "uk.co.bithatch.drawzx.views.colourPickerView";

	private Composite contentArea;
	private IColouredEditor editor;
	private PaletteGrid history;
	private GridData historyGridData;
	private Label historyLabel;
	private GridData historyLabelGridData;
	private PaletteGrid palette;
	private Label paletteInfo;
	private GridData paletteInfoGridData;
	private GridData paletteLayoutData;
	private Link paletteLink;
	private Composite paletteLinkButtons;
	private GridData paletteLinkButtonsGridData;
	private GridData paletteLinkGridData;
	private Spinner paletteOffset;
	private GridData paletteOffsetGridData;
	private Label paletteOffsetLabel;
	private GridData paletteOffsetLabelGridData;
	private Composite parent;
	private Composite pickerArea;
	private GridData pickerAreaGridData;
	private GridData unsupportedGridData;
	private Label unsupportedLabel;
	private Label paletteInfoLabel;
	private ResetPaletteAction resetPaletteAction;
	private ResetPaletteTransAction resetPaletteTransAction;

	private Label index;

	private Label rgb;

	private Label encoded;

	private Label encodedHex;

	private Label encodedBin;


	@Override
	public void createPartControl(Composite parent) {
		this.parent = parent;

		contentArea = new Composite(parent, SWT.NONE);
        contentArea.setLayout(new GridLayout(1, true));
		
		pickerArea = new Composite(contentArea, SWT.NONE);
		pickerArea.setLayout(new GridLayout(1, true));
		pickerAreaGridData = GridDataFactory.fillDefaults().grab(true, true).create();
		pickerArea.setLayoutData(pickerAreaGridData);
		
		createPicker(pickerArea);

		unsupportedLabel = new Label(contentArea, SWT.WRAP | SWT.CENTER);
		unsupportedGridData = GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.CENTER).grab(true, true).create();
		unsupportedGridData.exclude = true;
		unsupportedLabel.setLayoutData(unsupportedGridData);
		unsupportedLabel.setText("Open a graphics editor such as UDG, Sprite or Screen");
		unsupportedLabel.setVisible(false);
		
		contributeToActionBars();

		getSite().getWorkbenchWindow().getPartService().addPartListener(this);

		IEditorPart activeEditor = getSite().getPage().getActiveEditor();
		updateView(activeEditor);

		updatePaletteInfo();
		updateIndexInfo();
	}
	
	@Override
	public void updatePaletteInfo() {
		if(editor == null) {
			paletteLink.setText("");
			paletteInfo.setText("");
		}
		else {
			paletteInfo.setText(paletteText());
			var selectedPaletteFile = editor.getPaletteFile();
			if (selectedPaletteFile == null) {
				paletteLink.setToolTipText("Click to create and associate palette with this file.");
				paletteLink.setText(String.format("<a>Create</a>", paletteText()));
			}
			else {
				paletteLink.setToolTipText("Click to open current palette.");
				paletteLink.setText(String.format("<a>%s</a>", selectedPaletteFile.getName()));
			}
		}
	}

	@Override
	public void dispose() {
		if(this.editor != null) {
			this.editor.picker(null);
		}
		getSite().getWorkbenchWindow().getPartService().removePartListener(this);
		super.dispose();
	}

	@Override
	public void palette(Palette palette) {
		this.palette.palette(palette);		
	}

	@Override
	public int paletteOffset() {
		return paletteOffset.getSelection();
	}

	@Override
	public void partActivated(IWorkbenchPartReference partRef) {
		if (partRef instanceof IEditorReference) {
			IWorkbenchPart part = partRef.getPart(false);
			updateView(part);
		}
	}

	public void partBroughtToTop(IWorkbenchPartReference partRef) {
	}

	// Other IPartListener2 methods can be empty
	public void partClosed(IWorkbenchPartReference partRef) {
		if (partRef instanceof IEditorReference) {
			IWorkbenchPart part = partRef.getPart(false);
			if(part.equals(editor)) {
				updateView(null);		
			}
		}
	}

	public void partDeactivated(IWorkbenchPartReference partRef) {
	}

	public void partHidden(IWorkbenchPartReference partRef) {
	}

	public void partInputChanged(IWorkbenchPartReference partRef) {
	}

	public void partOpened(IWorkbenchPartReference partRef) {
	}

	public void partVisible(IWorkbenchPartReference partRef) {
	}

	@Override
	public void setFocus() {
		pickerArea.setFocus();
	}

	protected void colorSelected(int index, boolean priority) {
		editor.colorSelected(index, priority);
		if(editor.isPaletteHistoryUsed()) {
			if(!paletteHistory().contains(index)) {
				EditorFileProperties.addToSetProperty(editor.getFile(), EditorFileProperties.PALETTE_HISTORY_PROPERTY,
						String.valueOf(index), true, editor.maxPaletteHistorySize());
			}
			updatePaletteHistory(index);
		}
		
		if(editor.isPaletteOffsetUsed()) {
			paletteOffset.setSelection(index / 16);
			palette.paletteOffset(paletteOffset.getSelection());
		}
		
		updateIndexInfo();
	}
	
	protected void resetPaletteTrans() {
		EditorFileProperties.setProperty(editor.getFile(), EditorFileProperties.PALETTE_SOURCE_PROPERTY,
				PaletteSource.DEFAULT_TRANSPARENT.name());
		EditorFileProperties.setProperty(editor.getFile(), EditorFileProperties.PALETTE_PROPERTY, "");
		editor.setDefaultTransPalette();		
	}

	protected void resetPalette() {
		EditorFileProperties.setProperty(editor.getFile(), EditorFileProperties.PALETTE_SOURCE_PROPERTY,
				PaletteSource.DEFAULT.name());
		EditorFileProperties.setProperty(editor.getFile(), EditorFileProperties.PALETTE_PROPERTY, "");
		editor.setDefaultPalette();
	}

	protected void choosePalette(Shell shell) {
		var dialog = new ElementTreeSelectionDialog(shell, new WorkbenchLabelProvider(),
				new WorkbenchContentProvider());

		dialog.setTitle("Select Palette or Pattern File");
		dialog.setMessage("Select a .npl or .pal file from the workspace:");
		dialog.setInput(ResourcesPlugin.getWorkspace().getRoot());
		dialog.setAllowMultiple(false);

		dialog.addFilter(new ViewerFilter() {
			@Override
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				if (element instanceof IFile file) {
					var ext = file.getFileExtension();
					return ext != null && (ext.equalsIgnoreCase("npl") || ext.equalsIgnoreCase("pal"));
				} else if (element instanceof IContainer) {
					return true; // allow folders
				}
				return false;
			}
		});

		if (dialog.open() == Window.OK) {
			EditorFileProperties.setProperty(editor.getFile(), EditorFileProperties.PALETTE_SOURCE_PROPERTY,
					PaletteSource.FILE.name());
			EditorFileProperties.setProperty(editor.getFile(), EditorFileProperties.PALETTE_PROPERTY,
					((IFile) dialog.getFirstResult()).getFullPath().toString());
		}
	}
	
	protected void createColorAccessories(Composite colorInfo) {
		historyLabel = new Label(colorInfo, SWT.NONE);
		historyLabel.setText("History:");
		historyLabelGridData = GridDataFactory.swtDefaults().create();
		historyLabel.setLayoutData(historyLabelGridData);
		history = new PaletteGrid(colorInfo, Palette.empty(), 22, SWT.NONE); 
		historyGridData = GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).span(3, 1).hint(200, 22).create();
		history.setLayoutData(historyGridData);
		history.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			colorSelected(
					editor.palette().indexOf(history.palette().color((Integer) e.data)),
					( e.stateMask & SWT.CONTROL ) != 0);
		}));
		
		paletteOffsetLabel = new Label(colorInfo, SWT.NONE);
		paletteOffsetLabel.setText("Offset:");
		paletteOffsetLabelGridData = GridDataFactory.swtDefaults().create();
		paletteOffsetLabel.setLayoutData(paletteOffsetLabelGridData);
		
		paletteOffset =  new Spinner(colorInfo, SWT.NONE);
		paletteOffset.setValues(0, 0, 15, 0, 1, 4);
		paletteOffsetGridData = GridDataFactory.fillDefaults().span(3, 1).create();
		paletteOffset.setLayoutData(paletteOffsetGridData);
		paletteOffset.addSelectionListener(SelectionListener.widgetSelectedAdapter(evt -> {
			palette.paletteOffset(paletteOffset.getSelection());
		}));
	}

	protected GridLayout createGroupLayout(int cols, boolean equal) {
		var l = new GridLayout(cols, equal);
		l.verticalSpacing = 8;
//		l.marginWidth = 8;
//		l.marginHeight = 8;
		return l;
	}

	protected void createPaletteGrid(Composite parent) {
		palette = new PaletteGrid(parent, Palette.rgb333(), SWT.NONE);
		paletteLayoutData = GridDataFactory.fillDefaults().grab(true, true).span(4, 1).align(SWT.CENTER, SWT.CENTER).create();
		palette.setLayoutData(paletteLayoutData);
	}

	protected void createPicker(Composite groups) {
		var colorInfo = new Composite(groups, SWT.NONE);
		var colorLayout = createGroupLayout(4, false);
		colorInfo.setLayout(colorLayout);
		colorInfo.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());

		var paletteLabel = new Label(colorInfo, SWT.NONE);
		paletteLabel.setText("Palette:");

		paletteLink = new Link(colorInfo, SWT.NONE);
		paletteLinkGridData = GridDataFactory.fillDefaults().grab(true, false).hint(142, SWT.DEFAULT)
				.align(SWT.FILL, SWT.CENTER).create();
		paletteLink.setLayoutData(paletteLinkGridData);
		paletteLink.setText("<a>Default</a>");
		paletteLink.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			var selectedPaletteFile = editor.getPaletteFile();
			if (selectedPaletteFile == null) {
				selectedPaletteFile = editor.getFile().getParent().getFile(IPath.fromPortableString(
						editor.getFile().getName() + (editor.palette().transparency().isPresent() ? ".npl" : ".pal")));
				if (!selectedPaletteFile.exists()) {
					try {
						selectedPaletteFile.create(editor.palette().asBytes(), 0, null);
					} catch (CoreException e1) {
						throw new IllegalStateException(e1);
					}
				}
			}

			try {
				setProperty(editor.getFile(), EditorFileProperties.PALETTE_SOURCE_PROPERTY, PaletteSource.FILE.name());
				setProperty(editor.getFile(), EditorFileProperties.PALETTE_PROPERTY,
						selectedPaletteFile.getFullPath().toPortableString());
				editor.currentPaletteUpdate();
				IDE.openEditor(editor.getEditorSite().getPage(), selectedPaletteFile);
			} catch (Exception e1) {
				throw new IllegalStateException(e1);
			}

		}));

		paletteLinkButtons = new Composite(colorInfo, SWT.NONE);
		paletteLinkButtons.setLayout(new RowLayout());
		paletteLinkButtonsGridData = GridDataFactory.swtDefaults().align(SWT.CENTER, SWT.CENTER).span(2, 1).create();
		paletteLinkButtons.setLayoutData(paletteLinkButtonsGridData);

		paletteInfoLabel = new Label(colorInfo, SWT.BOLD);
		paletteInfoLabel.setText("Type:");

		paletteInfo = new Label(colorInfo, SWT.BOLD);
		paletteInfoGridData = GridDataFactory.fillDefaults().span(3, 1).create();
		paletteInfo.setLayoutData(paletteInfoGridData);

		createColorAccessories(colorInfo);

		createPaletteGrid(colorInfo);
		palette.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			colorSelected((Integer) e.data, ( e.stateMask & SWT.CTRL ) == 0);
		}));

		var indexLabel = new Label(colorInfo, SWT.NONE);
		indexLabel.setText("Index:");

		index = new Label(colorInfo, SWT.NONE);
		index.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).span(3, 1)
				.align(SWT.FILL, SWT.CENTER).create());
		index.setText("0");
		
		var rgbLabel = new Label(colorInfo, SWT.NONE);
		rgbLabel.setText("RRRGGGBBB:");

		rgb = new Label(colorInfo, SWT.NONE);
		rgb.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).span(3, 1)
				.align(SWT.FILL, SWT.CENTER).create());
		rgb.setText("0 0 0");
		
		var encodedLabel = new Label(colorInfo, SWT.NONE);
		encodedLabel.setText("Encoded:");

		encoded = new Label(colorInfo, SWT.NONE);
		encoded.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).span(3, 1)
				.align(SWT.FILL, SWT.CENTER).create());
		encoded.setText("0");
		
		new Label(colorInfo, SWT.NONE);

		encodedHex = new Label(colorInfo, SWT.NONE);
		encodedHex.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).span(3, 1)
				.align(SWT.FILL, SWT.CENTER).create());
		encodedHex.setText("0");
		
		new Label(colorInfo, SWT.NONE);

		encodedBin = new Label(colorInfo, SWT.NONE);
		encodedBin.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).span(3, 1)
				.align(SWT.FILL, SWT.CENTER).create());
		encodedBin.setText("0");
	}

	protected String paletteText() {
		if(editor == null) {
			return "";
		}
		else if (editor.palette().transparency().isPresent())
			return String.format("%d+1 Colour %d bit", editor.palette().size(), editor.palette().bits());
		else
			return String.format("%d Colour %d bit", editor.palette().size(), editor.palette().bits());
	}

	protected Set<Integer> paletteHistory() {
		return EditorFileProperties.getSetProperty(editor.getFile(), EditorFileProperties.PALETTE_HISTORY_PROPERTY,
				Set.of(String.valueOf(editor.defaultPaletteIndex()))).stream().map(s -> Integer.parseInt(s)).collect(Collectors.toCollection(() -> new LinkedHashSet<>()));
	}

	protected void setupPaletteHistory() {
		var primary = EditorFileProperties.getIntProperty(editor.getFile(), EditorFileProperties.PALETTE_PRIMARY_PROPERTY, editor.defaultPaletteIndex());
		var secondary = EditorFileProperties.getIntProperty(editor.getFile(), EditorFileProperties.PALETTE_SECONDARY_PROPERTY, editor.defaultSecondaryPaletteIndex());
		if (secondary == -1) {
			palette.select(primary);

		} else {
			palette.select(primary, secondary);
		}
	}

	protected void updatePaletteHistory(int index) {
		var historyPalette = Palette.of(paletteHistory().stream().map(idx ->
			editor.palette().color(idx)
		).toList());
		
		history.palette(historyPalette);
		history.columns(historyPalette.size());
		history.select(historyPalette.indexOf(editor.palette().color(index)));
	}

    private void contributeToActionBars() {
        IActionBars bars = getViewSite().getActionBars();
        IToolBarManager toolbar = bars.getToolBarManager();

        toolbar.add(new OpenWorkspacePaletteAction());
        toolbar.add(resetPaletteAction = new ResetPaletteAction());
        toolbar.add(resetPaletteTransAction = new ResetPaletteTransAction());
    }

	private void updateView(IWorkbenchPart part) {
		System.out.println("updateView " + part);
		
		if (part instanceof IColouredEditor editor) {
			pickerArea.setVisible(true);
			unsupportedGridData.exclude = true;
			unsupportedLabel.setVisible(false);
			pickerAreaGridData.exclude = false;
			contentArea.pack();
			
			palette.palette(editor.palette());

			if (editor.isPalettedChangeAllowed()) {
				updateVisibility(paletteLink, paletteLinkGridData, true);
				updateVisibility(paletteInfo, paletteInfoGridData, true);
				updateVisibility(paletteLinkButtons, paletteLinkButtonsGridData, true);
				
				resetPaletteAction.setEnabled(editor.isPaletteResettable());
				resetPaletteTransAction.setEnabled(editor.isPaletteResettable());
			}
			else {
				updateVisibility(paletteLink, paletteLinkGridData, false);
				updateVisibility(paletteInfo, paletteInfoGridData, true);
				updateVisibility(paletteLinkButtons, paletteLinkButtonsGridData, true);
				
				resetPaletteAction.setEnabled(false);
				resetPaletteTransAction.setEnabled(false);
			}
			
			palette.showOffsets(editor.isPaletteOffsetUsed());
			palette.cellSize(editor.paletteCellSize());
			paletteLayoutData.widthHint = palette.getSize().x;
			paletteLayoutData.heightHint = palette.getSize().y;
			
			updateVisibility(historyLabel, historyLabelGridData, editor.isPaletteHistoryUsed());
			updateVisibility(history, historyGridData, editor.isPaletteHistoryUsed());

			updateVisibility(paletteOffsetLabel, paletteOffsetLabelGridData, editor.isPaletteOffsetUsed());
			updateVisibility(paletteOffset, paletteOffsetGridData, editor.isPaletteOffsetUsed());
			
			this.editor = editor;
			this.editor.picker(this);
			
			setupPaletteHistory();
			
		} else {
			if(this.editor != null) {
				this.editor.picker(null);
			}
			
			pickerArea.setVisible(false);
			unsupportedLabel.setVisible(true);
			unsupportedGridData.exclude = false;
			pickerAreaGridData.exclude = true;

			updateVisibility(paletteLink, paletteLinkGridData, false);
			updateVisibility(paletteInfo, paletteInfoGridData, false);
			updateVisibility(paletteLinkButtons, paletteLinkButtonsGridData, false);
			
			updateVisibility(historyLabel, historyLabelGridData, false);
			updateVisibility(history, historyGridData, false);
			updateVisibility(paletteOffsetLabel, paletteOffsetLabelGridData, false);
			updateVisibility(paletteOffset, paletteOffsetGridData, false);
			
			resetPaletteAction.setEnabled(false);
			resetPaletteTransAction.setEnabled(false);
			contentArea.pack();
			this.editor = null;
		}
		parent.layout();

		updatePaletteInfo();
		updateIndexInfo();
	}

	private void updateVisibility(Control ctrl, GridData gdata, boolean visible) {
		ctrl.setVisible(visible);
		gdata.exclude = !visible;
	}
	
	private class ResetPaletteAction extends Action {
	    public ResetPaletteAction() {
	        super("Reset Palette", IAction.AS_PUSH_BUTTON);
	        setToolTipText("Reset to default palette (no transparency).");
			setImageDescriptor(Activator.getDefault().getImageRegistry().getDescriptor(Activator.PAL));
	    }

	    @Override
	    public void run() {
	    	resetPalette();
	    }
	}
		
	private class ResetPaletteTransAction extends Action {
	    public ResetPaletteTransAction() {
	        super("Reset Palette (transparent)", IAction.AS_PUSH_BUTTON);
	        setToolTipText("Reset to default palette (with transparency).");
			setImageDescriptor(Activator.getDefault().getImageRegistry().getDescriptor(Activator.PAL_TRANS));
	    }

	    @Override
	    public void run() {
	    	resetPaletteTrans();
	    }
	}
	
	private class OpenWorkspacePaletteAction extends Action {
	    public OpenWorkspacePaletteAction() {
	        super("Open Workspace Palette", IAction.AS_PUSH_BUTTON);
	        setToolTipText("Click to select a palette from your workspace.");
			setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJ_FOLDER));
	    }

	    @Override
	    public void run() {
			choosePalette(parent.getShell());
	    }
	}
	
	private void updateIndexInfo() {
		var idx = palette.selectedIndex();
		var entry = idx == -1 ? null : palette.palette().color(idx);
		index.setText(entry == null ? "" : String.valueOf(idx));
		rgb.setText(entry == null ? "" : entry.toValues());
		encoded.setText(entry == null ? "" : String.valueOf(entry.encoded()));
		encodedHex.setText(entry == null ? "" : entry.toEncodedHex());
		encodedBin.setText(entry == null ? "" : entry.toEncodedBinary());
	}
}
