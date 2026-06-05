package uk.co.bithatch.eclipzoxo.components;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;

import uk.co.bithatch.zoxo.system.AddOn;
import uk.co.bithatch.zoxo.system.AddOn.Factory;
import uk.co.bithatch.zoxo.system.AddOnSettings;
import uk.co.bithatch.zoxo.system.Machine;
import uk.co.bithatch.zoxo.system.Machine.MachineFactory;
import uk.co.bithatch.zoxo.system.Model;
import uk.co.bithatch.zoxo.system.Session;

public class ZoxoComponentRegistry {

	public static final String MACHINE_EXTENSION_POINT_ID = "uk.co.bithatch.eclipzoxo.machine";
	public static final String ADD_ON_EXTENSION_POINT_ID = "uk.co.bithatch.eclipzoxo.addOn";

	public static List<MachineFactory<?,?>> machineFactories() {
		var registry = Platform.getExtensionRegistry();
		var point = registry.getExtensionPoint(MACHINE_EXTENSION_POINT_ID);
		if (point == null)
			return Collections.emptyList();
		else {
			Stream<MachineFactory<?,?>> map = Arrays.asList(point.getConfigurationElements()).stream()
					.filter(c -> c.getName().equals("machine"))
					.map(c -> {
						try {
							return (MachineFactory<?,?>)(c.createExecutableExtension("class"));
						} catch (CoreException e) {
							throw new IllegalStateException("Failed to create a machine extension.", e);
						}
					});
			return map.toList();
		}
	}

	public static Machine<?> get(Session platform, Model hardwareModel) {
		return machineFactories().stream()
				.filter(f -> f.model().equals(hardwareModel)).findFirst().map(f -> f.create(platform, platform.getSettingsForMachineFactory(f)))
				.orElseThrow(() -> new IllegalArgumentException("No machine of type " + hardwareModel + " found."));
	}

	@SuppressWarnings("unchecked")
	public static Stream<Factory<AddOn<AddOnSettings, ?>, AddOnSettings>> addOns(Machine<?> m) {
		var registry = Platform.getExtensionRegistry();
		var point = registry.getExtensionPoint(ADD_ON_EXTENSION_POINT_ID);
		if (point == null)
			return Stream.empty();
		else {
			return Arrays.asList(point.getConfigurationElements()).stream()
				.filter(c -> c.getName().equals("addOn"))
				.map(c -> {
					try {
						return (Factory<AddOn<AddOnSettings, ?>, AddOnSettings>)(c.createExecutableExtension("class"));
					} catch (CoreException e) {
						throw new IllegalStateException("Failed to create a machine extension.", e);
					}
				})
				.filter(ao -> m == null || ao.isActive(m))
				.sorted((a1,a2) -> Integer.valueOf(a1.priority()).compareTo(a2.priority()));
		}
	}


}
