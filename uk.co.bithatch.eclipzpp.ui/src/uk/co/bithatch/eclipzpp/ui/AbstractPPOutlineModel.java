package uk.co.bithatch.eclipzpp.ui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.eclipse.emf.common.util.EList;
import org.eclipse.jface.resource.ImageDescriptor;

public abstract class AbstractPPOutlineModel implements IPPOutlineModel {
	private final Map<Class<?>, PPOutlineItem> values = new HashMap<Class<?>, PPOutlineItem>();

	public void add(Class<?> type, String text, ImageDescriptor image) {
		add(type, text, image, null);
	}

	@SuppressWarnings("unchecked")
	public <E> void add(Class<E> type, String text, ImageDescriptor image, Function<E, EList<? extends Object>> mapper) {
		values.put(type, new PPOutlineItem(text, image, (Function<Object, EList<? extends Object>>)mapper));
	}

	public <E> void add(Class<E> type, Function<E, String> textMapper, Function<E, EList<? extends Object>> mapper) {
		add(type, textMapper, null, mapper);
	}
	

	public <E> void add(Class<E> type, Function<E, String> textMapper, ImageDescriptor image) {
		add(type, textMapper, image, null);
	}

	@SuppressWarnings("unchecked")
	public <E> void add(Class<E> type, Function<E, String> textMapper, ImageDescriptor image, Function<E, EList<? extends Object>> mapper) {
		values.put(type, new PPOutlineItem((Function<Object, String>)textMapper, image, (Function<Object, EList<? extends Object>>)mapper));
	}

	@Override
	public List<PPOutlineItem> items() {
		return values.values().stream().toList();
	}

	@Override
	public PPOutlineItem get(Class<?> clazz) {
		var val = values.get(clazz);
		if(val == null) {
			for(var ent : values.entrySet()) {
				if(clazz.isAssignableFrom(ent.getKey())) {
					return ent.getValue();
				}
			}
		}
		return val;
	}
}
