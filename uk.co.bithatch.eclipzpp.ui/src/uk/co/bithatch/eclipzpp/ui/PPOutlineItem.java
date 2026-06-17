package uk.co.bithatch.eclipzpp.ui;

import java.util.function.Function;

import org.eclipse.emf.common.util.EList;
import org.eclipse.jface.resource.ImageDescriptor;

public record PPOutlineItem(Function<Object, String> textMapper, ImageDescriptor image, Function<Object, EList<? extends Object>> childMapper) {

	public PPOutlineItem(String text, ImageDescriptor image, Function<Object, EList<? extends Object>> mapper) {
		this(e -> text, image, mapper);
	}

	public PPOutlineItem(Function<Object, String> textMapper, Function<Object, EList<? extends Object>> mapper) {
		this(textMapper, null, mapper);
	}
	
	public PPOutlineItem(ImageDescriptor image, String text) {
		this(e -> text, image, null);
	}
}