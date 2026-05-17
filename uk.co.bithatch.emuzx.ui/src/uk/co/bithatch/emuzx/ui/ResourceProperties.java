package uk.co.bithatch.emuzx.ui;

import static uk.co.bithatch.emuzx.ui.EmuZXUIActivator.PLUGIN_ID;

import org.eclipse.core.runtime.QualifiedName;

import uk.co.bithatch.emuzx.api.IResourceProperties;

public class ResourceProperties extends IResourceProperties {

	public static QualifiedName DISK_IMAGE_INCLUDE_IN_PREPARATION = new QualifiedName(PLUGIN_ID,
			"diskImage.includeInPreparation");
	public static QualifiedName DISK_IMAGE_PREPARATION_FOLDER = new QualifiedName(PLUGIN_ID,
			"diskImage.preparationFolder");
	public static QualifiedName DISK_IMAGE_FLATTEN_PREPARATION = new QualifiedName(PLUGIN_ID,
			"diskImage.flattenPreparation");
	public static QualifiedName DISK_IMAGE_TRIGGER_PROGRAMS_IN_THIS_FOLDER = new QualifiedName(PLUGIN_ID,
			"diskImage.triggerProgramsInThisFolder");
	public static QualifiedName DISK_IMAGE_TRIGGER_ALWAYS = new QualifiedName(PLUGIN_ID,
			"diskImage.triggerAlways");
	public static QualifiedName DISK_IMAGE_OTHER_TRIGGER_PROGRAMS = new QualifiedName(PLUGIN_ID,
			"diskImage.otherTriggerPrograms");
}
