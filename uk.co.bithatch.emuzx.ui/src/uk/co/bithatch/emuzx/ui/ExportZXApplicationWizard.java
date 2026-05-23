package uk.co.bithatch.emuzx.ui;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ListViewer;

/**
 * Export Wizard that runs a "Bundle Application" launch configuration to
 * compile and prepare the ZX application (directory or FAT image), and
 * optionally archives the result as a zip file.
 */
public class ExportZXApplicationWizard extends Wizard implements IExportWizard {

	private static final ILog LOG = ILog.of(ExportZXApplicationWizard.class);
	private static final String BUNDLE_LAUNCH_TYPE_ID = "uk.co.bithatch.emuzx.ui.bundleLaunch";

	private BundleSelectionPage selectionPage;

	public ExportZXApplicationWizard() {
		setWindowTitle("Export ZX Application");
		setNeedsProgressMonitor(true);
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		/* nothing specific needed */
	}

	@Override
	public void addPages() {
		selectionPage = new BundleSelectionPage();
		addPage(selectionPage);
	}

	@Override
	public boolean performFinish() {
		var config = selectionPage.getSelectedConfiguration();
		if (config == null) {
			MessageDialog.openError(getShell(), "Export ZX Application",
					"No Bundle Application launch configuration selected.");
			return false;
		}

		var zipEnabled = selectionPage.isZipEnabled();
		var zipDest = selectionPage.getZipDestination();

		try {
			getContainer().run(true, true, monitor -> {
				var sub = SubMonitor.convert(monitor, "Exporting ZX Application", 100);
				try {
					/* Run the bundle launch synchronously */
					var launch = config.launch(ILaunchManager.RUN_MODE, sub.split(80));

					/* Optionally zip */
					if (zipEnabled && zipDest != null && !zipDest.isEmpty()) {
						var outputPath = launch.getAttribute(BundleLaunchConfiguration.ATTR_BUNDLE_OUTPUT_PATH);
						if (outputPath != null) {
							sub.subTask("Creating zip archive...");
							BundleLaunchConfiguration.zipPath(
									new File(outputPath),
									new File(zipDest),
									sub.split(20));
						}
					}
					sub.done();
				} catch (Exception e) {
					throw new InvocationTargetException(e);
				}
			});
		} catch (InvocationTargetException e) {
			LOG.error("Export failed", e.getTargetException());
			MessageDialog.openError(getShell(), "Export ZX Application",
					"Export failed: " + e.getTargetException().getMessage());
			return false;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		}

		return true;
	}

	// ----------------------------------------------------------------
	// Wizard page
	// ----------------------------------------------------------------

	static class BundleSelectionPage extends WizardPage {

		private ListViewer configViewer;
		private List<ILaunchConfiguration> configs = new ArrayList<>();
		private Button zipCheck;
		private Text zipDestText;
		private Button zipBrowse;

		protected BundleSelectionPage() {
			super("bundleSelection");
			setTitle("Export ZX Application");
			setDescription("Select a Bundle Application launch configuration, then optionally archive the output as a zip.");
		}

		@Override
		public void createControl(Composite parent) {
			var comp = new Composite(parent, SWT.NONE);
			comp.setLayout(new GridLayout(3, false));
			setControl(comp);

			/* Config list */
			var listLabel = new Label(comp, SWT.NONE);
			listLabel.setText("Bundle configurations:");
			listLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));

			configViewer = new ListViewer(comp, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL);
			configViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
			configViewer.setContentProvider(ArrayContentProvider.getInstance());
			configViewer.setLabelProvider(new LabelProvider() {
				@Override
				public String getText(Object element) {
					return ((ILaunchConfiguration) element).getName();
				}
			});
			configViewer.addSelectionChangedListener(e -> validatePage());

			/* Button to open launch config dialog to create a new one */
			var newButton = new Button(comp, SWT.PUSH);
			newButton.setText("New...");
			newButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
			newButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
				DebugUITools.openLaunchConfigurationDialogOnGroup(getShell(),
						new org.eclipse.jface.viewers.StructuredSelection(),
						"org.eclipse.debug.ui.launchGroup.run");
				refreshConfigs();
			}));

			/* Separator */
			var sep = new Label(comp, SWT.SEPARATOR | SWT.HORIZONTAL);
			sep.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));

			/* Zip option */
			zipCheck = new Button(comp, SWT.CHECK);
			zipCheck.setText("Archive output as zip");
			zipCheck.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));
			zipCheck.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
				updateZipControls();
				validatePage();
			}));

			var zipLabel = new Label(comp, SWT.NONE);
			zipLabel.setText("Zip file:");

			zipDestText = new Text(comp, SWT.BORDER);
			zipDestText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			zipDestText.addModifyListener(e -> validatePage());

			zipBrowse = new Button(comp, SWT.PUSH);
			zipBrowse.setText("Browse...");
			zipBrowse.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
				var dlg = new FileDialog(getShell(), SWT.SAVE);
				dlg.setFilterExtensions(new String[] { "*.zip" });
				dlg.setFilterNames(new String[] { "Zip Archives (*.zip)" });
				var result = dlg.open();
				if (result != null) {
					zipDestText.setText(result);
				}
			}));

			updateZipControls();
			refreshConfigs();
			validatePage();
		}

		private void refreshConfigs() {
			configs.clear();
			try {
				var mgr = DebugPlugin.getDefault().getLaunchManager();
				var type = mgr.getLaunchConfigurationType(BUNDLE_LAUNCH_TYPE_ID);
				if (type != null) {
					for (var cfg : mgr.getLaunchConfigurations(type)) {
						configs.add(cfg);
					}
				}
			} catch (Exception e) {
				LOG.error("Failed to load bundle launch configurations", e);
			}
			configViewer.setInput(configs);
			if (!configs.isEmpty()) {
				configViewer.getList().select(0);
			}
			validatePage();
		}

		private void updateZipControls() {
			var enabled = zipCheck.getSelection();
			zipDestText.setEnabled(enabled);
			zipBrowse.setEnabled(enabled);
		}

		private void validatePage() {
			setErrorMessage(null);
			if (configs.isEmpty()) {
				setErrorMessage("No Bundle Application launch configurations found. Create one using 'New...'.");
				setPageComplete(false);
				return;
			}
			if (getSelectedConfiguration() == null) {
				setErrorMessage("Please select a launch configuration.");
				setPageComplete(false);
				return;
			}
			if (zipCheck.getSelection() && zipDestText.getText().trim().isEmpty()) {
				setErrorMessage("Please specify a zip file destination.");
				setPageComplete(false);
				return;
			}
			setPageComplete(true);
		}

		ILaunchConfiguration getSelectedConfiguration() {
			var sel = configViewer.getStructuredSelection();
			return sel.isEmpty() ? null : (ILaunchConfiguration) sel.getFirstElement();
		}

		boolean isZipEnabled() {
			return zipCheck.getSelection();
		}

		String getZipDestination() {
			return zipDestText.getText().trim();
		}
	}
}
