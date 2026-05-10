package uk.co.bithatch.zxbasic.ui.preferences;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.resource.ResourceLocator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import uk.co.bithatch.zxbasic.ui.ZXBasicUiActivator;
import uk.co.bithatch.zxbasic.ui.library.ZXSDK;

public final class SDKLabelProvider extends LabelProvider {
	private Map<String, Image> images = new HashMap<>();
	
	@Override
	public Image getImage(Object element) {
		var sdk = (ZXSDK) element;
		var lib = sdk.libraries().get(0);
		var icon = lib == null ? null : lib.icon();
		Image img = null;
		if(icon != null && !icon.equals("")) {
			var key =  lib.plugin() + ":" + icon;
			if(images.containsKey(key)) {
				img = images.get(key);
			}
			else {
				img = ResourceLocator.imageDescriptorFromBundle(lib.plugin(), icon).map(des -> des.createImage()).orElse(null);
				images.put(key, img);
			}
		}
		else {
			img = ZXBasicUiActivator.getInstance().getImageRegistry().get(ZXBasicUiActivator.LIBRARY_PATH);
		}
		return img;
	}

	@Override
	public String getText(Object element) {
		ZXSDK sdk = (ZXSDK) element;
		return sdk == null ? "Unknown" : sdk.name();
	}

	@Override
	public void dispose() {
		super.dispose();
		images.values().forEach(img -> {
			if(img != null)
				img.dispose();
		});
	}
}