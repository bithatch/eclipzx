package uk.co.bithatch.drawzx.editor;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;

public class PersistentPropertyWatcher {

	private final IFile file;
	private final Map<QualifiedName, String> lastKnownValues = new HashMap<>();
	private final IResourceChangeListener listener;
	private final BiConsumer<QualifiedName, String> onChange;

	public PersistentPropertyWatcher(IFile file, BiConsumer<QualifiedName, String> onChange, QualifiedName... keys) {
		this.file = file;
		this.onChange = onChange;

		for (var key : keys) {
			addKey(key);
		}

		this.listener = this::handleChange;
		ResourcesPlugin.getWorkspace().addResourceChangeListener(listener, IResourceChangeEvent.POST_CHANGE);
	}

	public void addKey(QualifiedName key) {
		try {
			lastKnownValues.put(key, file.getPersistentProperty(key));
		} catch (CoreException e) {
			lastKnownValues.put(key, null);
		}
	}

	public void dispose() {
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(listener);
	}

	private void handleChange(IResourceChangeEvent event) {
		try {
			event.getDelta().accept(delta -> {
				if (delta.getResource() instanceof IFile changed && changed.getFullPath().equals(file.getFullPath())) {

					try {
						var currentProps = changed.getPersistentProperties();

						for (var key : currentProps.keySet()) {
							var oldValue = lastKnownValues.get(key);
							var newValue = currentProps.get(key);

							if (!Objects.equals(oldValue, newValue)) {
								lastKnownValues.put(key, newValue);
								onChange.accept(key, newValue);
							}
						}

						for (var key : new HashMap<>(lastKnownValues).keySet()) {
							if (!currentProps.containsKey(key)) {
								lastKnownValues.remove(key);
								onChange.accept(key, null);
							}
						}

					} catch (CoreException e) {
						throw new IllegalStateException(e);
					}
				}
				return true;
			});
		} catch (CoreException e) {
			throw new IllegalStateException(e);
		}
	}

}
