package uk.co.bithatch.drawzx.editor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.file.Files;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.IOperationHistory;
import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.core.commands.operations.IUndoableOperation;
import org.eclipse.core.commands.operations.ObjectUndoContext;
import org.eclipse.core.commands.operations.OperationHistoryFactory;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.operations.RedoActionHandler;
import org.eclipse.ui.operations.UndoActionHandler;
import org.eclipse.ui.part.EditorPart;

import uk.co.bithatch.drawzx.Colors;
import uk.co.bithatch.drawzx.widgets.ColorCell;
import uk.co.bithatch.drawzx.widgets.PaletteGrid;
import uk.co.bithatch.zyxy.graphics.Palette;
import uk.co.bithatch.zyxy.graphics.Palette.Entry;

public class PaletteEditor extends EditorPart {

    private Composite root;
    private PaletteGrid paletteGrid;
	private Palette palette;
	private Label size;
	private Label bits;
	private Label transparency;
	private Label index;
	private Spinner red;
	private Spinner green;
	private Spinner blue;
	private Label webValue;
	private Label decValue;
	private Label hexValue;
	private Label binaryValue;
	private Button priority;
	private boolean dirty;
	private int selected;
	private ColorCell previewCell;
	private PaletteGrid swatchGrid;
	private final IUndoContext undoContext = new ObjectUndoContext(this);
	protected IOperationHistory history;

    @Override
    public void createPartControl(Composite parent) {
		setupUndo();
		
        root = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(2, false);
        layout.marginWidth = 8;
        layout.marginHeight = 8;
		root.setLayout(layout);
		

        createPaletteGrid(root);
        createDetails(root);

        updatePaletteInfo();

    	paletteGrid.select(0);
    }

    private void createDetails(Composite parent) {
    	var groups = new Composite(parent, SWT.NONE);
        groups.setLayout(new GridLayout(1, false));
        groups.setLayoutData(GridDataFactory.fillDefaults().grab(false, true).hint(256, SWT.DEFAULT).create());
        
        var paletteInfo = new Group(groups, SWT.TITLE);
        paletteInfo.setLayout(createGroupLayout(2, false));
        paletteInfo.setText("Palette");
        paletteInfo.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
        
        var sizeLabel = new Label(paletteInfo, SWT.NONE);
        sizeLabel.setText("Size:");
        size = new Label(paletteInfo, SWT.BOLD);
        
        var bitsLabel = new Label(paletteInfo, SWT.NONE);
        bitsLabel.setText("Bits:");
        bits = new Label(paletteInfo, SWT.BOLD);
        
        if(palette.transparency().isPresent()) {
        	

            var transparencyLabel = new Link(paletteInfo, SWT.NONE);
            transparencyLabel.setLayoutData(GridDataFactory.swtDefaults().create());
            transparencyLabel.setText("<a>Transparency</a>");
            transparencyLabel.setToolTipText("Click to select index to use as transparency.");
            transparencyLabel.addSelectionListener(new SelectionAdapter() {
            	
            	private Runnable pickModeHandle;

    			@Override
    			public void widgetSelected(SelectionEvent e) {
    				if(pickModeHandle != null) {
    					try {
    						pickModeHandle.run();
    					}
    					finally {
    						pickModeHandle = null;
    					}
    				}
    				else {
	    				pickModeHandle = paletteGrid.pickColor(c -> {
	    					changePalette(palette.withTransparency(c));
	    					pickModeHandle = null;
	    				});
    				}
    			}
            	
    		});
            
	        transparency = new Label(paletteInfo, SWT.BOLD);
	        transparency.setLayoutData(GridDataFactory.fillDefaults().create());
        }
        
        var entryInfo = new Group(groups, SWT.TITLE);
        entryInfo.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
        entryInfo.setLayout(createGroupLayout(2, false));
        entryInfo.setText("Entry");
        
        var indexLabel = new Label(entryInfo, SWT.NONE);
        indexLabel.setText("Index:");
        index = new Label(entryInfo, SWT.BOLD);
        index.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
        index.setText("Click to select");
        
        var redLabel = new Label(entryInfo, SWT.NONE);
        redLabel.setText("Red:");
        red = new Spinner(entryInfo, SWT.BOLD);
        red.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateSelected();
			}
		});
        red.setValues(0, 0, 7, 0, 1, 8);
        red.setEnabled(false);
        
        var greenLabel = new Label(entryInfo, SWT.NONE);
        greenLabel.setText("Green:");
        green = new Spinner(entryInfo, SWT.BOLD);
        green.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateSelected();
			}
		});
        green.setValues(0, 0, 7, 0, 1, 8);
        green.setEnabled(false);
        
        var blueLabel = new Label(entryInfo, SWT.NONE);
        blueLabel.setText("Blue:");
        blue = new Spinner(entryInfo, SWT.BOLD);
        blue.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateSelected();
			}
		});
        blue.setValues(0, 0, 7, 0, 1, 4);
        blue.setEnabled(false);
        
        priority = new Button(entryInfo, SWT.CHECK);
        priority.setText("Priority");
        priority.setEnabled(false);
        priority.setLayoutData(GridDataFactory.fillDefaults().span(2, 1).indent(0, 16).create());
        priority.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateSelected();
			}
		});
        

        var link = new Link(entryInfo, SWT.NONE);
        link.setLayoutData(GridDataFactory.fillDefaults().span(2, 1).indent(0, 16).grab(true, true).align(SWT.CENTER,SWT.END).create());
        link.setText("<a>Pick rough colour</a>");
        link.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
		        var dialog = new ColorDialog(getSite().getShell());
		        dialog.setText("Rough Colour");
		        var selected = dialog.open();
		        if(selected != null) {
					choose(Colors.toEntry(selected));
		        }
			}
        	
		});
        
        var colorInfo = new Group(groups, SWT.TITLE);
        colorInfo.setLayout(createGroupLayout(2, false));
        colorInfo.setText("Color");
        colorInfo.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
        
        previewCell = new ColorCell(colorInfo, 32);
        previewCell.setSelected(true);
        previewCell.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).indent(16, 16).span(2, 2).create());

        var valueLabel = new Label(colorInfo, SWT.NONE);
        valueLabel.setText("Value:");
        valueLabel.setLayoutData(GridDataFactory.fillDefaults().indent(0, 16).create());
        decValue = new Label(colorInfo, SWT.BOLD);
        decValue.setText("   ");
        decValue.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).indent(0, 16).create());
        new Label(colorInfo, SWT.NONE);
        hexValue = new Label(colorInfo, SWT.BOLD);
        hexValue.setText("   ");
        hexValue.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
        new Label(colorInfo, SWT.NONE);
        binaryValue = new Label(colorInfo, SWT.BOLD);
        binaryValue.setText("   ");
        binaryValue.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
        new Label(colorInfo, SWT.NONE);
        webValue = new Label(colorInfo, SWT.BOLD);
        webValue.setText("   ");
        webValue.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
    }

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == IUndoContext.class) {
			return (T) undoContext;
		}
		return super.getAdapter(adapter);
	}
	public IUndoContext getUndoContext() {
		return undoContext;
	}

	protected void execute(IUndoableOperation op) {
		System.out.println("UNDOABLE OP: " + op);
		op.addContext(getUndoContext());
		try {
			history.execute(op, null, null);
		} catch (ExecutionException e) {
			throw new IllegalStateException("Cannot add frame.", e);
		}
	}

	private void choose(Entry entry) {
		execute(new SetEntryOperation(this, selected, entry));
	}

    private GridLayout createGroupLayout(int cols, boolean equal) {
		var l = new GridLayout(cols, equal);
		l.verticalSpacing = 8;
		l.marginWidth = 8;
		l.marginHeight = 8;
		return l;
	}

	private void createPaletteGrid(Composite parent) {
		
		var palettes = new Composite(parent, SWT.NONE);
		var layout = new GridLayout();
		layout.verticalSpacing = 8;
		palettes.setLayout(layout);
		palettes.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
        
		if(palette.size() == 512)
	        paletteGrid = new PaletteGrid(palettes, palette, 4, 32, SWT.BORDER);
		else
			paletteGrid = new PaletteGrid(palettes, palette, SWT.BORDER);
		paletteGrid.asDragSource();
		paletteGrid.asDropTarget();
        paletteGrid.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
        paletteGrid.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				select((Integer)e.data);
			}
        	
		});
        paletteGrid.addModifyListener(t -> {

			Object[] data = (Object[])t.data;
			var idx = (Integer)data[0];
			var entry = (Entry)data[1];
			

//			setEntry(potentialDropTarget, entry);
//			palette.set(idx, entry);
			paletteGrid.select(idx);
//			select(idx);
			
//        	choose((Entry) t.data);
        	choose(entry);
//        	updatePaletteInfo();
        });
        
        
        var fullPal = Palette.rgb333with512();
		swatchGrid = new PaletteGrid(palettes, fullPal, 8, 32, SWT.BORDER);
		swatchGrid.asDragSource();
        swatchGrid.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).hint(SWT.DEFAULT, 200).create());
        swatchGrid.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				if(e.detail == 2) {
					choose(fullPal.color((Integer)e.data));
				}
			}
        	
		});
        
    }

	private void updatePaletteInfo() {
		size.setText(String.valueOf(palette.size()));
		bits.setText(String.valueOf(palette.bits()));
		if(transparency != null) {
			transparency.setText(palette.transparency().map(i -> String.valueOf(i)).orElse("None"));
		}
	}

    void select(int selected) {
    	if(this.selected != selected) {
	    	var entry = palette.color(selected);
	    	index.setText(String.valueOf(selected));
	    	red.setSelection(entry.r() >> 5);
	    	green.setSelection(entry.g() >> 5);
	    	blue.setSelection(entry.b() >> 5);
	        red.setEnabled(true);
	        green.setEnabled(true);
	        blue.setEnabled(true);
	        priority.setEnabled(true);
	        
	        this.selected = selected;
	        if(paletteGrid.selectedIndex() != selected)
	        	paletteGrid.select(selected);
	        updateValue();
    	}
	}
    
    private void updateValue() {
    	var entry = palette.color(selected);
    	var encoded = entry.encoded();
		decValue.setText(String.valueOf(encoded));
		hexValue.setText(entry.toEncodedHex());
		binaryValue.setText(entry.toEncodedBinary());
		webValue.setText(entry.toWeb());
		previewCell.setColor(Colors.toRGBA(entry));
    }

	@Override
    public void setFocus() {
    	paletteGrid.setFocus();
    }

    @Override
    public void doSave(IProgressMonitor monitor) {
    	var file = getEditorInput().getAdapter(IFile.class);
    	try {
    		var bytes = new ByteArrayOutputStream();
    		try(var wtr = Channels.newChannel(bytes)) {
        		palette.save(wtr);	
    		}
    		
			file.write(bytes.toByteArray(), false, false, true, monitor);
	    	markClean();
		} catch (CoreException | IOException e) {
			e.printStackTrace();
		}
    }

    @Override
    public void doSaveAs() {
    }

    @Override
    public boolean isSaveAsAllowed() {
        return false;
    }

    @Override
    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        setSite(site);
        setInput(input);
        setPartName(input.getName());
        
        var file = input.getAdapter(IFile.class);
        try(var in = Files.newByteChannel(file.getLocation().toFile().toPath())) {
    		palette = Palette.load(in,256);
        	if(palette.size()  == 0) {
        		palette = Palette.rgb333();
        		markDirty();
        	}
        	if(file.getFileExtension().equalsIgnoreCase("npl") && palette.transparency().isEmpty()) {
        		palette = palette.withTransparency(0xff);
        		markDirty();
        	}
        	else if(file.getFileExtension().equalsIgnoreCase("pal") && !palette.transparency().isEmpty()) {
        		palette = palette.withoutTransparency();
        		markDirty();
        	}
        } catch (IOException e) {
        	throw new PartInitException(Status.error("Failed to load palette.", e));
		}
    }

	@Override
	public boolean isDirty() {
		return dirty;
	}

	@Override
	public void dispose() {
		history.dispose(undoContext, dirty, dirty, dirty);
		super.dispose();
	}
	
	private void changePalette(Palette palette) {
		execute(new SetPaletteOperation(this, palette));
	}
	
	Palette setPalette(Palette palette) {
		var was = this.palette;
		if(was != palette) {
			this.palette = palette;
			paletteGrid.palette(palette);
			updatePaletteInfo();
			markDirty();
		}
		return was;
	}

	private void markClean() {
		if(dirty) {
			dirty = false;
			firePropertyChange(PROP_DIRTY);
		}
	}
	
	private void markDirty() {
		if(!dirty) {
			dirty = true;
			firePropertyChange(PROP_DIRTY);
		}
	}
	
	private void updateSelected() {
		choose(new Palette.Entry(red.getSelection() << 5, green.getSelection() << 5, blue.getSelection() << 5, priority.getSelection()));
	}

	Entry setEntry(Entry newEntry) {
		var was = palette.set(selected, newEntry);
		paletteGrid.entry(selected, newEntry);
		updateValue();
		markDirty();
		select(PaletteEditor.this.selected);
		return was;
	}

	private void setupUndo() {
		history = OperationHistoryFactory.getOperationHistory();
		history.setLimit(undoContext, 1000);
		var undoHandler = new UndoActionHandler(getSite(), undoContext);
		var redoHandler = new RedoActionHandler(getSite(), undoContext);
		var bars = getEditorSite().getActionBars();

		bars.setGlobalActionHandler(ActionFactory.UNDO.getId(), undoHandler);
		bars.setGlobalActionHandler(ActionFactory.REDO.getId(), redoHandler);
		bars.updateActionBars();
	}

}
