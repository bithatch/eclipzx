package uk.co.bithatch.drawzx.editor;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.part.EditorPart;

import uk.co.bithatch.drawzx.views.IColourPicker;
import uk.co.bithatch.drawzx.widgets.DrawSurface;
import uk.co.bithatch.zyxy.graphics.Palette;
import uk.co.bithatch.zyxy.graphics.VideoMemory;
import uk.co.bithatch.zyxy.graphics.VideoMode;
import uk.co.bithatch.zyxy.lib.Lang;

public abstract class AbstractScreenEditor extends EditorPart implements IColouredEditor, IPartListener {

    private ScrolledComposite scrolledComposite;
    private Canvas canvas;
    private Composite root;
    private Composite canvasContainer;
    private float zoomLevel = 1.0f;
    private DrawSurface surface;
    private ScheduledExecutorService exec;
	private VideoMemory buffer;
	private Palette palette;

    @Override
    public void createPartControl(Composite parent) {
        root = new Composite(parent, SWT.NONE);
        root.setLayout(new FillLayout());

        createCanvasArea(root);

		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getPartService().addPartListener(this);
		
        exec = Executors.newSingleThreadScheduledExecutor();
        exec.scheduleAtFixedRate(() -> {
        	surface.flashActiveOn(!surface.flashActive());
        }, 750, 750, TimeUnit.MILLISECONDS);
    }

    @Override
	public void dispose() {
		super.dispose();
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getPartService().removePartListener(this);
		exec.shutdown();
	}

    private void createCanvasArea(Composite parent) {
        scrolledComposite = new ScrolledComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
//        scrolledComposite.setLayoutData(new BorderData(SWT.CENTER));
        scrolledComposite.setLayout(new FillLayout());
//        scrolledComposite.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_RED));
        
        canvasContainer = new Composite(scrolledComposite, SWT.NONE);
//        GridLayout glayout = new GridLayout();
//		canvasContainer.setLayout(glayout);
//		canvasContainer.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_GREEN));
//        canvasContainer.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
//        canvasContainer.setLayout(new GridLayout(1, false));

        canvas = new Canvas(canvasContainer, SWT.DOUBLE_BUFFERED);
        canvas.setLayoutData(GridDataFactory.swtDefaults().align(SWT.CENTER, SWT.CENTER).hint(256, 192).create());
        canvas.addPaintListener(new ZoomablePaintListener());

        Point initialSize = scale(canvasSize());
        canvasContainer.setSize(initialSize);
        canvas.setSize(initialSize);

        scrolledComposite.setContent(canvasContainer);
        scrolledComposite.setExpandHorizontal(false);
        scrolledComposite.setExpandVertical(false);
        scrolledComposite.setMinSize(initialSize);
        
        
    }

	protected Point canvasSize() {
		return new Point(buffer.mode().width(), buffer.mode().height());
	}

    private void zoomCanvas(float scaleFactor) {
        zoomLevel *= scaleFactor;
        Point newSize = scale(canvasSize());
        System.out.println("newSize " + newSize);
        
        canvas.setSize(newSize);
        canvasContainer.setSize(newSize);
        scrolledComposite.requestLayout();
//        canvas.redraw();
    }

    private Point scale(Point base) {
        return new Point(scale(base.x), scale(base.y));
    }

	protected int scale(int x) {
		return Math.round(x * zoomLevel);
	}
	
	protected int unscale(int x) {
		return Math.round(x / zoomLevel);
	}

    private class ZoomablePaintListener implements PaintListener {
        @Override
        public void paintControl(PaintEvent e) {
            Transform transform = new Transform(e.gc.getDevice());
            transform.scale(zoomLevel, zoomLevel);
            e.gc.setTransform(transform);

            // Example content
            surface.paint( e.gc, unscale(e.x), unscale(e.y), unscale(e.width), unscale(e.height));
//            e.gc.drawRectangle(50, 50, 100, 100);
//            e.gc.drawText("Zoom Level: " + String.format("%.2f", zoomLevel), 60, 160);

            transform.dispose();
        }
    }

    @Override
    public void setFocus() {
        canvas.setFocus();
    }

    @Override
    public void doSave(IProgressMonitor monitor) {
        // Save logic here
    }

    @Override
    public void doSaveAs() {
        // Optional
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

		var file = getFile();
		var loc = file.getLocation();
		buffer = VideoMode.STANDARD.createBuffer(Lang.readFully(loc.toPath()));
		
		palette = Palette.rgb16();
        surface = new DrawSurface(palette, buffer);
        surface.addDirtyListener(dl -> {
        	System.out.println("Dirty " + dl.x + " " + dl.y + " " + dl.width + " " + dl.height);
        	
        	if(dl.x == 0 && dl.y == 0 && dl.width == surface.width() && dl.height == surface.height()) {
            	canvas.redraw(dl.x, dl.y, dl.width, dl.height, true);	
        	}
        	else {
            	canvas.redraw(dl.x, dl.y, dl.width, dl.height, false);	
        	}
        });
    }

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public IFile getFile() {
		return getEditorInput().getAdapter(IFile.class);
	}

	public void zoomIn() {
		zoomCanvas(1.5f);
	}

	public void zoomOut() {
		zoomCanvas(0.75f);
	}

	@Override
	public void colorSelected(int data, boolean b) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void partActivated(IWorkbenchPart part) {
		if (part.equals(this)) {
			var cmdService = PlatformUI.getWorkbench().getService(ICommandService.class);
			if (cmdService != null) {
				cmdService.refreshElements("uk.co.bithatch.drawzx.screens.commands.mode", null);
			}
		}
	}

	@Override
	public void partBroughtToTop(IWorkbenchPart part) {
	}

	@Override
	public void partClosed(IWorkbenchPart part) {
	}

	@Override
	public void partDeactivated(IWorkbenchPart part) {
	}

	@Override
	public void partOpened(IWorkbenchPart part) {
	}

	@Override
	public int maxPaletteHistorySize() {
		return 8;
	}

	@Override
	public boolean isPalettedChangeAllowed() {
		return true;
	}

	@Override
	public Palette palette() {
		return palette;
	}

	@Override
	public boolean isPaletteResettable() {
		return true;
	}

	@Override
	public void setDefaultPalette() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("TODO");
	}

	@Override
	public void setDefaultTransPalette() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("TODO");
	}

	@Override
	public boolean isPaletteOffsetUsed() {
		return false;
	}

	@Override
	public boolean isPaletteHistoryUsed() {
		return true;
	}

	@Override
	public void currentPaletteUpdate() {
		// TODO Auto-generated method stub
	}


	@Override
	public void picker(IColourPicker picker) {
		// TODO Auto-generated method stub
		
	}
}
