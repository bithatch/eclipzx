package uk.co.bithatch.zxbasic.ui.outline;

import org.eclipse.emf.common.notify.impl.AdapterImpl;

//@Deprecated
public class LabelAdapter extends AdapterImpl {
    private final String label;

    public LabelAdapter(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public boolean isAdapterForType(Object type) {
        return type == LabelAdapter.class;
    }
}
