package uk.co.bithatch.zxbasic.ui.language;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import uk.co.bithatch.bitzx.IArchitecture;
import uk.co.bithatch.bitzx.IOutputFormat;
import uk.co.bithatch.bitzx.WellKnownArchitecture;

public enum BorielZXBasicArchitecture implements IArchitecture {
	LEGACY, ZXNEXT;
	
	public String description() {
		switch(this) {
		case LEGACY:
			return "Legacy 48K and others";
		case ZXNEXT:
			return "ZX Next";
		default:
			return name();
		}
	}
	
	@Override
	public Optional<WellKnownArchitecture> wellKnown() {
		return Optional.of(WellKnownArchitecture.valueOf(name()));
	}

	@Override
	public List<IOutputFormat> supportedFormats() {
		return Arrays.asList(formats());
	}

	@Deprecated
	public BorielZXBasicOutputFormat[] formats() {
		switch(this) {
		case LEGACY:
			return new BorielZXBasicOutputFormat[] {  
					BorielZXBasicOutputFormat.Z80, 
					BorielZXBasicOutputFormat.BIN,  
					BorielZXBasicOutputFormat.SNA,  
					BorielZXBasicOutputFormat.TAP,  
					BorielZXBasicOutputFormat.TZX
			}; 
		case ZXNEXT:
			return new BorielZXBasicOutputFormat[] { 
					BorielZXBasicOutputFormat.NEX,  
					BorielZXBasicOutputFormat.BIN,  
					BorielZXBasicOutputFormat.SNA,  
					BorielZXBasicOutputFormat.TAP,  
					BorielZXBasicOutputFormat.TZX,  
					BorielZXBasicOutputFormat.Z80
			};
		default:
			throw new IllegalArgumentException();
		}
	}

//	public static String[] asComboArray() {
//		return Arrays.asList(values()).stream().map(Architecture::description).toList().toArray(new String[0]);
//	}
//
//	public static String[][] asEditorArray() {
//		var vals = values();
//		var arr = new String[vals.length][2];
//		for(int i = 0 ; i < vals.length; i++) {
//			arr[i][0] = vals[i].description();
//			arr[i][1] = vals[i].name();
//		}
//		return arr;
//	}
//
//	public static Architecture parse(String archName) {
//		try {
//			return valueOf(archName);
//		}
//		catch(IllegalArgumentException iae) {
//			return Architecture.LEGACY;
//		}
//	}
}