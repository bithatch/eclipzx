package uk.co.bithatch.zxbasic.ui.builder;

import static uk.co.bithatch.zxbasic.ui.ZXBasicUiActivator.PLUGIN_ID;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.QualifiedName;

import uk.co.bithatch.bitzx.AbstractResourceProperties;
import uk.co.bithatch.zxbasic.ui.ZXBasicUiActivator;

public class ResourceProperties extends  AbstractResourceProperties {
	
	public enum SysVarSource {
		NONE, BUNDLED, LOCATION
	}

	public static QualifiedName DISK_IMAGE_INCLUDE_IN_PREPARATION = new QualifiedName(PLUGIN_ID,
			"diskImage.includeInPreparation");
	public static QualifiedName DISK_IMAGE_PREPARATION_FOLDER = new QualifiedName(PLUGIN_ID,
			"diskImage.preparationFolder");
	public static QualifiedName DISK_IMAGE_FLATTEN_PREPARATION = new QualifiedName(PLUGIN_ID,
			"diskImage.flattenPreparation");
	public static QualifiedName DISK_IMAGE_TRIGGER_PROGRAMS_IN_THIS_FOLDER = new QualifiedName(PLUGIN_ID,
			"diskImage.triggerProgramsInThisFolder");
	public static QualifiedName DISK_IMAGE_OTHER_TRIGGER_PROGRAMS = new QualifiedName(PLUGIN_ID,
			"diskImage.otherTriggerPrograms");

	public static QualifiedName BUILD = new QualifiedName(ZXBasicUiActivator.PLUGIN_ID, "program.build");
	public static QualifiedName ORG_ADDRESS = new QualifiedName(ZXBasicUiActivator.PLUGIN_ID, "program.orgAddress");
	public static QualifiedName HEAP_ADDRESS = new QualifiedName(ZXBasicUiActivator.PLUGIN_ID, "program.heapAddress");
	public static QualifiedName HEAP_SIZE = new QualifiedName(ZXBasicUiActivator.PLUGIN_ID, "program.heapSize");

	public static QualifiedName NEX_OVERRIDE_PROJECT = new QualifiedName(PLUGIN_ID, "nex.overrideProject");
	public static QualifiedName NEX_SYSVARS = new QualifiedName(PLUGIN_ID, "nex.sysvars");
	public static QualifiedName NEX_SYSVARS_LOCATION = new QualifiedName(PLUGIN_ID, "nex.sysvars.location");
	public static QualifiedName NEX_CORE = new QualifiedName(PLUGIN_ID, "nex.core");
	public static QualifiedName NEX_SP = new QualifiedName(PLUGIN_ID, "nex.sp");
	public static QualifiedName NEX_PC = new QualifiedName(PLUGIN_ID, "nex.pc");
	public static QualifiedName NEX_ENTRY_BANK = new QualifiedName(PLUGIN_ID, "nex.entryBank");
	
	public static QualifiedName NEX_BUNDLE = new QualifiedName(PLUGIN_ID, "nex.includeInPreparation");
	public static QualifiedName NEX_BUNDLE_TYPE = new QualifiedName(PLUGIN_ID, "nex.bundleType");
	public static QualifiedName NEX_BANK = new QualifiedName(PLUGIN_ID, "nex.bank");
	public static QualifiedName NEX_ADDRESS = new QualifiedName(PLUGIN_ID, "nex.address");
	public static QualifiedName NEX_TRIGGER_PROGRAMS_IN_THIS_FOLDER = new QualifiedName(ZXBasicUiActivator.PLUGIN_ID,
			"nex.triggerProgramsInThisFolder");
	public static QualifiedName NEX_OTHER_TRIGGER_PROGRAMS = new QualifiedName(PLUGIN_ID, "nex.otherTriggerPrograms");
	public static QualifiedName NEX_BMP_DO_NOT_SAVE_PALETTE = new QualifiedName(PLUGIN_ID, "nex.bmp.doNotSavePalette");
	public static QualifiedName NEX_BMP_USE_8_BIT_PALETTE = new QualifiedName(PLUGIN_ID, "nex.bmp.use8BitPalette");
	public static QualifiedName NEX_BMP_BORDER = new QualifiedName(PLUGIN_ID, "nex.bmp.border");
	public static QualifiedName NEX_BMP_BAR_1 = new QualifiedName(PLUGIN_ID, "nex.bmp.bar1");
	public static QualifiedName NEX_BMP_BAR_2 = new QualifiedName(PLUGIN_ID, "nex.bmp.bar2");
	public static QualifiedName NEX_BMP_DELAY_1 = new QualifiedName(PLUGIN_ID, "nex.bmp.delay1");
	public static QualifiedName NEX_BMP_DELAY_2 = new QualifiedName(PLUGIN_ID, "nex.bmp.delay2");
	
	

}
