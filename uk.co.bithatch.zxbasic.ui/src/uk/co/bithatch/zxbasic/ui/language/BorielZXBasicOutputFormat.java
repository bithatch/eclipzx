package uk.co.bithatch.zxbasic.ui.language;

import java.util.Arrays;

import uk.co.bithatch.bitzx.FileNames;
import uk.co.bithatch.bitzx.IOutputFormat;

public enum BorielZXBasicOutputFormat implements IOutputFormat {
	ASM, BIN, IR, SNA, TAP, TZX, Z80, NEX;

	public boolean snapshot() {
		switch(this) {
		case SNA:
		case TAP:
		case TZX:
		case Z80:
			return true;
		default:
			return false;
		}
	}


	@Override
	public String fullDescription() {
		return  description() + " (*." + name().toLowerCase() + ")" ;
	}

	@Override
	public String description() {
		switch(this) {
		case ASM:
			return "Z80 Assembler";
		case SNA:
			return "Snapshot";
		case TAP:
			return "Raw Tape";
		case TZX:
			return "Compressed Tape";
		case Z80:
			return "Memory Snapshot";
		case IR:
			return "Intermediate";
		default:
			return name() + " Format";
		}
	}

	public String changeExtension(String filename) {
		return FileNames.changeExtension(filename, name().toLowerCase());
	}

//	public static String[][] asEditorArray() {
//		var vals = values();
//		return asEditorArray(vals);
//	}
//
//	public static String[][] asEditorArray(OutputFormat... vals) {
//		var arr = new String[vals.length][2];
//		for(int i = 0 ; i < vals.length; i++) {
//			arr[i][0] = vals[i].fullDescription();
//			arr[i][1] = vals[i].name();
//		}
//		return arr;
//	}

	public boolean requiresSecondPass() {
		return firstPass() != this;
	}
	
	public BorielZXBasicOutputFormat firstPass() {
		switch(this) {
		case NEX:
			return BIN;
		default:
			return this;
		}
	}

//	public static OutputFormat parse(String name) {
//		return parse(name, OutputFormat.BIN);
//	}
//	
//	public static OutputFormat parse(String name, OutputFormat defaultFmt) {
//		try {
//			return OutputFormat.valueOf(name);
//		}
//		catch(NullPointerException | IllegalArgumentException e) {
//			return defaultFmt;
//		}
//	}
//
//	public static String[] names() {
//		return Arrays.asList(values()).stream().map(OutputFormat::name).toList().toArray(new String[0]);
//	}
//	
//
	public static String[] snapshotNames() {
		return Arrays.asList(values()).stream().filter(o -> o.snapshot()).map(BorielZXBasicOutputFormat::name).toList().toArray(new String[0]);
	}
}