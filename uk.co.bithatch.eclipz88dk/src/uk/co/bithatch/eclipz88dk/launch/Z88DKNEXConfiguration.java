package uk.co.bithatch.eclipz88dk.launch;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.eclipse.core.runtime.ILog;

import uk.co.bithatch.emuzx.AbstractNEXConfiguration;

/**
 * A NEX configuration that is built up by the Z88DK launch configuration, and
 * then converted to command line arguments for the z88dk-nex tool.
 */
public class Z88DKNEXConfiguration extends AbstractNEXConfiguration {
	
	private final static ILog LOG = ILog.of(Z88DKNEXConfiguration.class);

	private Optional<Integer> pc = Optional.empty();
	private Optional<Integer> sp = Optional.empty();
	private Optional<Integer> bank = Optional.empty();
	private Optional<Integer> border = Optional.empty();
	private Optional<Integer> bar1 = Optional.empty();
	private Optional<Integer> bar2 = Optional.empty();
	private Optional<Integer> delay1 = Optional.empty();
	private Optional<Integer> delay2 = Optional.empty();
	private Optional<Path> screen1 = Optional.empty();
	private Optional<Path> screen2 = Optional.empty();
	private List<Path> extraFiles = new ArrayList<>();

	@Override
	public void extraFile(Path file) {
		extraFiles.add(file);
	}

	@Override
	public void scr(Path file) {
		if(screen1.isEmpty()) {
			screen1 = Optional.of(file);
		} else {
			LOG.warn("More than 1 loading screens of legacy format specified - ignoring " + file);
		}
	}

	@Override
	public void bmp(Path file, boolean use8bitPalette, boolean dontSavePalette) {
		if(screen1.isEmpty()) {
			screen1 = Optional.of(file);
		} else {
			LOG.warn("More than 1 loading screens of legacy format specified - ignoring " + file);
		}
		
	}

	@Override
	public void sl2(Path file) {
		if(screen2.isEmpty()) {
			screen2 = Optional.of(file);
		} else {
			LOG.warn("More than 1 L2 loading screen specified - ignoring " + file);
		}
	}

	@Override
	public void nxi(Path file) {
		if(screen2.isEmpty()) {
			screen2 = Optional.of(file);
		} else {
			LOG.warn("More than 1 L2 loading screen specified - ignoring " + file);
		}
		
	}

	@Override
	public void loading(int border, int bar1, int bar2, int delay1, int delay2) {
		this.border = Optional.of(border);
		this.bar1 = Optional.of(bar1);
		this.bar2 = Optional.of(bar2);
		this.delay1 = Optional.of(delay1);
		this.delay2 = Optional.of(delay2);
		
		this.bar2.ifPresent(v -> {
			LOG.warn("2nd loading bar colour specified - z88dk-nex only supports 1, ignoring " + v);
		});
		this.delay2.ifPresent(v -> {
			LOG.warn("2nd delay specified - z88dk-nex only supports 1, ignoring " + v);
		});
	}

	@Override
	public void slr(Path file) {
		if(screen1.isEmpty()) {
			screen1 = Optional.of(file);
		} else {
			LOG.warn("More than 1 loading screens of legacy format specified - ignoring " + file);
		}
		
	}

	@Override
	public void shr(Path file) {
		LOG.warn("SHR loading screens are not supported by z88dk-nex - ignoring " + file);
	}

	@Override
	public void shc(Path file) {
		if(screen1.isEmpty()) {
			screen1 = Optional.of(file);
		} else {
			LOG.warn("More than 1 loading screens of legacy format specified - ignoring " + file);
		}
	}

	@Override
	public void mmu(Path file, Optional<Integer> bank8k, Optional<Integer> address8k) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addFile(Path file, Optional<Integer> bank, Optional<Integer> address, int[] SNA_Bank) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void entryBank(int bank) {
		this.bank = Optional.of(bank);
	}

	@Override
	public void pcsp(int pc, Optional<Integer> sp, Optional<Integer> bank) {
		this.pc = Optional.of(pc);
		this.sp = sp;
		this.bank = bank;
	}

	@Override
	public void core(int... core) {
		LOG.warn("NEX core specified but z88dk-nex does not support this - ignoring core " + List.of(core));
	}

	public Collection<String> toCommandLineArgs() {

		// These are the actual options not what copilot hallucinated
//
//		 {  0,  "nex",          "Make .nex instead of .tap", OPT_BOOL, &nex },
//		    {  0,  "nex-2mb",      "Force setting of the 2MB flag", OPT_BOOL, &zxnex.mb },
//		    {  0,  "nex-norun",    "Return to basic after loading", OPT_BOOL, &zxnex.norun },
//		    {  0,  "nex-screen",   "File containing loading screen (layer 2)", OPT_STR, &zxnex.screen },
//		    {  0,  "nex-screen-no-palette", "No palette prepends loading screen", OPT_BOOL, &zxnex.nopalette },
//		    {  0,  "nex-screen-is-ula", "Loading screen is ula (6912 bytes)", OPT_BOOL, &zxnex.screen_ula },
//		    {  0,  "nex-screen-is-lores", "Loading screen is lores (12288 bytes)", OPT_BOOL, &zxnex.screen_lores },
//		    {  0,  "nex-screen-is-hires", "Loading screen is hires (12288 bytes)", OPT_BOOL, &zxnex.screen_hires },
//		    {  0,  "nex-screen-is-hicol", "Loading screen is hi-colour (12288 bytes)", OPT_BOOL, &zxnex.screen_hicol },
//		    {  0,  "nex-border",   "Initial border colour", OPT_INT, &zxnex.border },
//		    {  0,  "nex-loadbar",  "Load bar colour", OPT_INT, &zxnex.loadbar },
//		    {  0,  "nex-loaddel",  "Delay after loading a bank", OPT_INT, &zxnex.loaddelay },
//		    {  0,  "nex-startdel", "Delay before starting", OPT_INT, &zxnex.startdelay },
//		    {  0,  "nex-noreset",  "Do not reset nextreg state\n", OPT_BOOL, &zxnex.noreset },
		
		
		var args = new ArrayList<String>();
//		pc.ifPresent(v -> args.addAll(List.of("--org", String.valueOf(v))));
//		sp.ifPresent(v -> args.addAll(List.of("--sp", String.valueOf(v))));
//		bank.ifPresent(v -> args.addAll(List.of("--entry", String.valueOf(v))));
//		border.ifPresent(v -> args.addAll(List.of("--border", String.valueOf(v))));
//		bar1.ifPresent(v -> args.addAll(List.of("--loadbar", "--loadbar-color", String.valueOf(v))));
//		delay1.ifPresent(v -> args.addAll(List.of("--loaddelay", String.valueOf(v))));
//		screen1.ifPresent(v -> args.addAll(List.of("--screen", v.toAbsolutePath().toString())));
//		screen2.ifPresent(v -> args.addAll(List.of("--screen", v.toAbsolutePath().toString())));
//		
//		extraFiles.forEach(v -> args.addAll(List.of("-pragma-include", v.toAbsolutePath().toString())));
		
		LOG.warn("NEX configuration specified but z88dk-nex support is not yet implemented - ignoring " + this);

		return args;
	}

	@Override
	public boolean hasContent() {
		/* Z88dk NEX always has content, the program is always added. */
		return true;
	}


}
