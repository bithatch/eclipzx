package uk.co.bithatch.emuzx.ui;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.resource.ResourceLocator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import uk.co.bithatch.emuzx.api.EmulatorDescriptor;

public final class EmulatorLabelProvider extends LabelProvider {
	
	private Map<String, Image> images = new HashMap<>();
	
	@Override
	public Image getImage(Object element) {
		var lib = (EmulatorDescriptor) element;
		var icon = lib == null ? null : lib.getIcon();
		Image img = null;
		if(icon != null && !icon.equals("")) {
			var key =  lib.getPluginId() + ":" + icon;
			if(images.containsKey(key)) {
				img = images.get(key);
			}
			else {
				img = ResourceLocator.imageDescriptorFromBundle(lib.getPluginId(), icon).map(des -> des.createImage()).orElse(null);
				images.put(key, img);
			}
		}
		return img;
	}

	@Override
	public String getText(Object element) {
	    var lib = (EmulatorDescriptor) element;
		return lib == null ? "Unknown" : lib.getName();
	}

	@Override
	public void dispose() {
		super.dispose();
		images.values().forEach(Image::dispose);
	}
}