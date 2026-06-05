package uk.co.bithatch.eclipzoxo.preferences;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import uk.co.bithatch.eclipzoxo.components.ZoxoComponentRegistry;
import uk.co.bithatch.zoxo.system.AddOn;
import uk.co.bithatch.zoxo.system.AddOn.Factory;
import uk.co.bithatch.zoxo.system.AddOnSettings;

/**
 * Property page for managing FAT disk image addOns on a project.
 * Shows only for IProject resources.
 */
public class AddOnsPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private record AddOnDefinition(String name, 
			Factory<AddOn<AddOnSettings, ?>, AddOnSettings> af, boolean enabled) {
	}
	
	private TableViewer tableViewer;
	private List<AddOnDefinition> addOns;
	private final Set<AddOnDefinition> pendingEnables = new HashSet<>();
	private final Set<AddOnDefinition> pendingDisables = new HashSet<>();

	public AddOnsPreferencePage() {
		super("Add-Ons");
		setDescription("Plug in or unplug emulated components. Built-in components cannot be unplugged.");
	}

    @Override
    public void init(IWorkbench workbench) {
    	reloadAddOns();
    }

    @Override
    protected Control createContents(Composite parent) {

		var composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		// Table
		tableViewer = new TableViewer(composite, SWT.BORDER | SWT.FULL_SELECTION | SWT.SINGLE);
		var table = tableViewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 7));

		var enabledCol = new TableViewerColumn(tableViewer, SWT.CENTER);
		enabledCol.getColumn().setText("Enabled");
		enabledCol.getColumn().setWidth(70);
		enabledCol.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				var ael = (AddOnDefinition) element;
				var en = ael.enabled;
				if (pendingDisables.contains(ael)) en = false;
				if (pendingEnables.contains(ael)) en = true;
				return en ? "\u2713" : "";
			}
		});

		var nameCol = new TableViewerColumn(tableViewer, SWT.NONE);
		nameCol.getColumn().setText("Name");
		nameCol.getColumn().setWidth(120);
		nameCol.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return ((AddOnDefinition) element).name();
			}
		});

		tableViewer.setContentProvider(ArrayContentProvider.getInstance());
		tableViewer.setInput(addOns);

		// Separator
		var sep = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
		sep.setLayoutData(GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).create());

		var enableButton = new Button(composite, SWT.PUSH);
		enableButton.setText("Enable");
		enableButton.setLayoutData(GridDataFactory.swtDefaults().align(SWT.FILL, SWT.TOP).create());
		enableButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				var sel = (IStructuredSelection) tableViewer.getSelection();
				if (sel.isEmpty()) return;
				var selected = (AddOnDefinition) sel.getFirstElement();
				pendingDisables.remove(selected);
				pendingEnables.add(selected);
				tableViewer.refresh();
			}
		});

		var disableButton = new Button(composite, SWT.PUSH);
		disableButton.setText("Disable");
		disableButton.setLayoutData(GridDataFactory.swtDefaults().align(SWT.FILL, SWT.TOP).create());
		disableButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				var sel = (IStructuredSelection) tableViewer.getSelection();
				if (sel.isEmpty()) return;
				var selected = (AddOnDefinition) sel.getFirstElement();
				pendingEnables.remove(selected);
				pendingDisables.add(selected);
				tableViewer.refresh();
			}
		});

		return composite;
	}

	@Override
	public boolean performOk() {
		pendingDisables.forEach(a -> ZoxoPreferencesAccess.get().setEnabled(a.af.type(), false));
		pendingEnables.forEach(a -> ZoxoPreferencesAccess.get().setEnabled(a.af.type(), true));
		return true;
	}

	@Override
	protected void performDefaults() {
		pendingDisables.clear();
		pendingEnables.clear();
		ZoxoComponentRegistry.addOns(null).forEach(af -> {
			pendingEnables.add(createAddOn(af));
		});
		addOns.clear();
		addOns.addAll(pendingEnables);
		tableViewer.refresh();
		super.performDefaults();
	}

	private void reloadAddOns() {
		addOns = ZoxoComponentRegistry.addOns(null).map(af -> {
    		return createAddOn(af);
    	}).toList();
	}

	private AddOnDefinition createAddOn(Factory<AddOn<AddOnSettings, ?>, AddOnSettings> af) {
		return new AddOnDefinition(
			af.name(), 
			af, 
			ZoxoPreferencesAccess.get().isEnabled(af.type())
		);
	}

}
