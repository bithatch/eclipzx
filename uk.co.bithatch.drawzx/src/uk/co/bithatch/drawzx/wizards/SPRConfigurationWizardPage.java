package uk.co.bithatch.drawzx.wizards;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.function.Supplier;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;

import uk.co.bithatch.bitzx.FileNames;
import uk.co.bithatch.zyxy.graphics.Palette;

public class SPRConfigurationWizardPage extends WizardPage {

	private Spinner numberOfSprites;
	private Combo bpp;
	private Combo cellSize;
	private Combo palette;
	private Button createPalette;
	private Button fillTransparency;
	private Text file;
	private final Supplier<String> filename;
	private final Supplier<IPath> container;
	private Label createPaletteLabel;

	public SPRConfigurationWizardPage(Supplier<String> filename, Supplier<IPath> container) {
		super("Configuration");
		this.filename = filename;
		this.container = container;
        setTitle("Spritesheet Configuration");
        setDescription("Configure the initial spritesheet. Some configuration can be changed later.");
	}

	@Override
	public void createControl(Composite parent) {


        var container = new Composite(parent, SWT.NONE);
        var layout = new GridLayout(2, false);
        layout.verticalSpacing = 8;
        layout.horizontalSpacing = 16;
		container.setLayout(layout);

        var label = new Label(container, SWT.NONE);
        label.setText("Number Of Sprites:");

        numberOfSprites = new Spinner(container, SWT.NONE);
        numberOfSprites.setValues(64, 1, 256, 0, 1, 10);
        numberOfSprites.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        label = new Label(container, SWT.NONE);
        label.setText("Cell Size:");

        cellSize = new Combo(container, SWT.NONE);
        cellSize.setItems("8", "16");
        cellSize.select(1);
        cellSize.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        label = new Label(container, SWT.NONE);
        label.setText("Bits per pixel:");

        bpp= new Combo(container, SWT.NONE);
        bpp.setItems("8", "4", "1");
        bpp.select(0);
        bpp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        label = new Label(container, SWT.NONE);
        label.setText("Palette:");

        palette  = new Combo(container, SWT.NONE);
        palette.setItems("RGB333-512", "RGB333", "RGB16", "Mono", "Select Palette");
        palette.select(0);
        palette.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        palette.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				updateAvailable();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				updateAvailable();
			}
        	
		});
        
        createPalette  = new Button(container, SWT.CHECK);
        var gd1 = new GridData();
        gd1.horizontalIndent = 16;
        gd1.verticalIndent = 16;
        createPalette.setLayoutData(gd1);
        createPalette.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				updateAvailable();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				updateAvailable();
			}
        	
		});
        
        createPaletteLabel = new Label(container, SWT.NONE);
        createPaletteLabel.setText("Create palette file");
        createPaletteLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));

        fillTransparency = new Button(container, SWT.CHECK);
        fillTransparency.setSelection(true);
        var gdFill = new GridData();
        gdFill.horizontalIndent = 16;
        gdFill.verticalIndent = 8;
        fillTransparency.setLayoutData(gdFill);

        var fillTransparencyLabel = new Label(container, SWT.NONE);
        fillTransparencyLabel.setText("Fill with transparency index");
        fillTransparencyLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));

        
        label = new Label(container, SWT.NONE);
        label.setText("File:");
        var gd2 = new GridData();
        gd2.horizontalIndent = 24;
        label.setLayoutData(gd2);

        file  = new Text(container, SWT.NONE);
        file.setText(FileNames.changeExtension(filename.get(), "pal"));
        file.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        file.addModifyListener(new ModifyListener() {
			
			@Override
			public void modifyText(ModifyEvent e) {
				checkFile();
			}
		});

		setControl(container);
		setMessage(null);
		setErrorMessage(null);

		updateAvailable();
	}

	public int getNumberOfSprites() {
		return numberOfSprites.getSelection();
	}

	public int getBPP() {
		return Integer.parseInt(bpp.getItems()[bpp.getSelectionIndex()]);
	}

	public int getCellSize() {
		return Integer.parseInt(cellSize.getItems()[cellSize.getSelectionIndex()]);
	}

	public boolean isFillTransparency() {
		return fillTransparency.getSelection();
	}

	public Palette getPalette() {
		Palette pal;
		switch(palette.getSelectionIndex()) {
		case 0:
			pal = Palette.rgb333with512();
			break;
		case 1:
			pal = Palette.rgb333();
			break;
		case 2:
			pal = Palette.rgb16();
			break;
		case 3:
			pal = Palette.mono();
			break;
		case 4:
			return Palette.load(container.get().append(file.getText()).toFile().getAbsolutePath());
		default:
			throw new IllegalStateException();
		}
		
		if(isFillTransparency()) {
			pal = pal.withTransparency();
		}
		
		if(createPalette.getSelection()) {
			var osfile = container.get().append(file.getText());
			if(osfile.toFile().exists()) {
				throw new IllegalStateException("Palette file already exists.");
			}
			try(var chnl = Files.newByteChannel(osfile.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
				pal.save(chnl);;
			}
			catch(IOException ioe) {
				throw new UncheckedIOException(ioe);
			}
		}
		return pal;
	}
	
	private void checkFile() {
		var osfile = container.get().append(file.getText());
		if(palette.getSelectionIndex() == 4) {
			if(osfile.toFile().exists()) {
				setErrorMessage(null);
			}
			else {
				setErrorMessage("Palette file does not exist");
			}
		}
		else if(createPalette.getSelection()) {
			if(osfile.toFile().exists()) {
				setErrorMessage("Palette file already exists");
			}
			else {
				setErrorMessage(null);
			}
		}
	}
	
	private void updateAvailable() {
		file.setEnabled(palette.getSelectionIndex() >3);
		createPalette.setEnabled(palette.getSelectionIndex() != 4);
		createPaletteLabel.setEnabled(createPalette.isEnabled());
		checkFile();
	}

}
