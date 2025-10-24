package uk.co.bithatch.jspeccy.preferences;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class JSpeccyPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    public JSpeccyPreferencePage() {
        super(GRID);
        setDescription("Select a child category to show options.");
    }

    @Override
    public void createFieldEditors() {
    }

    @Override
    public void init(IWorkbench workbench) {}
}
