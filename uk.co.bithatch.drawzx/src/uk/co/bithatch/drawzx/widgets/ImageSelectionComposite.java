package uk.co.bithatch.drawzx.widgets;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.OwnerDrawLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Text;
import org.osgi.framework.FrameworkUtil;

public class ImageSelectionComposite extends Composite {

	private final TableViewer viewer;
	private final List<ImageEntry> allImages = new ArrayList<>();
	private ImageEntry selectedImage = null;

	public ImageSelectionComposite(Composite parent, int style, String pathPrefix) {
		super(parent, style);

		setLayout(new GridLayout(1, false));

		Text filterText = new Text(this, SWT.SEARCH | SWT.CANCEL);
		filterText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		viewer = new TableViewer(this, SWT.SINGLE | SWT.BORDER | SWT.V_SCROLL);
		viewer.getTable().setLayoutData(new GridData(GridData.FILL_BOTH));
		viewer.setContentProvider(ArrayContentProvider.getInstance());
//        viewer.setLabelProvider(new ImageLabelProvider());
		viewer.setLabelProvider(new OwnerDrawLabelProvider() {
			@Override
			protected void measure(Event event, Object element) {
				event.width = 300;
				event.height = 32;
			}

			@Override
			protected void paint(Event event, Object element) {
				if (element instanceof ImageEntry entry) {
					Image image = entry.image;

					// Draw background behind whole row
					Color bg = Display.getCurrent().getSystemColor(SWT.COLOR_LIST_BACKGROUND);
					event.gc.setBackground(bg);
					event.gc.fillRectangle(event.x, event.y, event.width, event.height);

					// Draw background behind image
					bg = Display.getCurrent().getSystemColor(SWT.COLOR_WHITE);
					event.gc.setBackground(bg);
					event.gc.fillRectangle(event.x, event.y, image.getBounds().width + 8, event.height);

					// Draw image with margin
					event.gc.drawImage(image, event.x + 4, event.y + ((event.height - image.getBounds().height) / 2));

					// Draw filename text
					event.gc.drawText(entry.name, event.x + image.getBounds().width + 10, event.y + 6, true);
				}
			}
		});

		viewer.addSelectionChangedListener(event -> {
			IStructuredSelection sel = viewer.getStructuredSelection();
			if (!sel.isEmpty() && sel.getFirstElement() instanceof ImageEntry entry) {
				selectedImage = entry;
				
				var ue = new Event();
				ue.widget = this;
				ue.display = getDisplay();
				ue.doit = true;
				ue.data = entry;
				var e = new SelectionEvent(ue);
				getTypedListeners(SWT.Selection, SelectionListener.class).forEach(l -> l.widgetSelected(e));
			}
		});

		loadImagesFromBundle(pathPrefix);
		viewer.setInput(allImages);

		filterText.addModifyListener(e -> {
			String text = filterText.getText().toLowerCase();
			viewer.setInput(allImages.stream().filter(i -> i.name.toLowerCase().contains(text)).toList());
		});

		this.addDisposeListener(e -> allImages.forEach(entry -> entry.image.dispose()));
	}

	public void addSelectionListener(SelectionListener listener) {
		addTypedListener(listener, SWT.Selection, SWT.DefaultSelection);
	}

	public void removeSelectionListener(SelectionListener listener) {
		removeListener(SWT.Selection, listener);
		removeListener(SWT.DefaultSelection, listener);
	}

	public ImageEntry getSelectedImageEntry() {
		return selectedImage;
	}

	private void loadImagesFromBundle(String pathPrefix) {
		var bundle = FrameworkUtil.getBundle(getClass());
		if (bundle == null) {
			return;
		}

		try {
			var path = pathPrefix + "previews/";
			var entries = bundle.getEntryPaths(path);
			if (entries == null) {
				return;
			}

			while (entries.hasMoreElements()) {
				var entryPath = entries.nextElement();
				if (!entryPath.endsWith(".png") && !entryPath.endsWith(".gif") && !entryPath.endsWith(".jpg")) {
					continue;
				}

				var fileURL = FileLocator.find(bundle, new Path(entryPath), null);
				var base = entryPath.substring(path.length(), entryPath.lastIndexOf('.'));
				var realURL = FileLocator.find(bundle, new Path(pathPrefix + "ch8/" + base + ".ch8"), null);
				if (fileURL != null && realURL != null) {
					try (var stream = fileURL.openStream()) {

						var image = new Image(getDisplay(), stream);
						allImages.add(new ImageEntry(base, image, realURL));
					}
				}
			}

			Collections.sort(allImages, (i1, i2) -> i1.name.compareTo(i2.name));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static class ImageEntry {
		String name;
		Image image;
		URL fontFile;

		ImageEntry(String name, Image image, URL fontFile) {
			this.name = name;
			this.image = image;
			this.fontFile = fontFile;
		}

		public String getName() {
			return name;
		}

		public Image getImage() {
			return image;
		}

		public URL getFontFile() {
			return fontFile;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	private static class ImageLabelProvider extends LabelProvider implements ITableLabelProvider {
		@Override
		public Image getImage(Object element) {
			return (element instanceof ImageEntry e) ? e.image : null;
		}

		@Override
		public String getText(Object element) {
			return (element instanceof ImageEntry e) ? e.name : "";
		}

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return getImage(element);
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			return getText(element);
		}
	}
}
