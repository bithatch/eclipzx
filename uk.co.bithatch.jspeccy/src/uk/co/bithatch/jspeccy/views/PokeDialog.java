package uk.co.bithatch.jspeccy.views;

import java.util.function.BiConsumer;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;

import uk.co.bithatch.jspeccy.Activator;

public class PokeDialog extends Dialog {

    private Spinner addressSpinner;
    private Spinner valueSpinner;
    private final BiConsumer<Integer, Integer> onPoke;

    public static void open(Shell parent, BiConsumer<Integer, Integer> onPoke) {
        new PokeDialog(parent, onPoke).open();
    }

    public PokeDialog(Shell parentShell, BiConsumer<Integer, Integer> onPoke) {
        super(parentShell);
        this.onPoke = onPoke;
        setBlockOnOpen(true);
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText("Poke Address");
    }

    @Override
	protected void buttonPressed(int buttonId) {
		Activator.getDefault().settings().setPokeAddress(addressSpinner.getSelection());
		Activator.getDefault().settings().setPokeValue(valueSpinner.getSelection());
		super.buttonPressed(buttonId);
	}

	@Override
	protected void okPressed() {
		// TODO Auto-generated method stub
		super.okPressed();
	}

	@Override
    protected Control createDialogArea(Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);

        Composite container = new Composite(area, SWT.NONE);
        container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        GridLayout gl = new GridLayout(3, false);
        gl.marginWidth = 10;
        gl.marginHeight = 10;
        gl.horizontalSpacing = 10;
        gl.verticalSpacing = 8;
        container.setLayout(gl);

        // Address
        new Label(container, SWT.NONE).setText("Address:");
        addressSpinner = new Spinner(container, SWT.BORDER);
        addressSpinner.setMinimum(0);
        addressSpinner.setMaximum(65535);
        addressSpinner.setSelection(Activator.getDefault().settings().getPokeValue());
        addressSpinner.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        // Poke button (in the same row as Address for a compact layout)
        Button pokeBtn = new Button(container, SWT.PUSH);
        pokeBtn.setText("Poke");
        pokeBtn.setImage(Activator.getDefault().getImageRegistry().get(Activator.POKE_PATH));
        pokeBtn.setLayoutData(GridDataFactory.defaultsFor(pokeBtn).align(SWT.LEFT, SWT.CENTER).create());

        // Value (second row)
        new Label(container, SWT.NONE).setText("Value:");
        valueSpinner = new Spinner(container, SWT.BORDER);
        valueSpinner.setMinimum(0);
        valueSpinner.setMaximum(255);
        valueSpinner.setSelection(Activator.getDefault().settings().getPokeValue());
        GridData valGD = new GridData(SWT.FILL, SWT.CENTER, true, false);
        valueSpinner.setLayoutData(valGD);

        // filler to align grid (since Poke already took col 3 above)
        new Label(container, SWT.NONE).setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

        pokeBtn.addSelectionListener(new SelectionAdapter() {
            @Override public void widgetSelected(SelectionEvent e) {
                if (onPoke != null && !pokeBtn.isDisposed()) {
                    onPoke.accept(addressSpinner.getSelection(), valueSpinner.getSelection());
                }
            }
        });

        return area;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, "Close", true);
    }
}
