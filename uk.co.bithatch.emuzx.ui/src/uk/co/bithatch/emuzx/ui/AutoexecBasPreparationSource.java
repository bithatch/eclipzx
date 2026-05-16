package uk.co.bithatch.emuzx.ui;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;

import uk.co.bithatch.bitzx.FileItem;
import uk.co.bithatch.bitzx.FileSet;
import uk.co.bithatch.bitzx.FileSet.Purpose;
import uk.co.bithatch.bitzx.Plus3DosFile;
import uk.co.bithatch.bitzx.Txt2NextBasicConverter;
import uk.co.bithatch.emuzx.api.IPreparationContext;
import uk.co.bithatch.emuzx.api.IPreparationSource;

public class AutoexecBasPreparationSource implements IPreparationSource {
	private final static ILog LOG = ILog.of(AutoexecBasPreparationSource.class);

	@Override
	public void contribute(IPreparationContext ctx, List<FileSet> fileSets, IProgressMonitor monitor) throws CoreException {
		
		try {
			byte[] basicProg;
			
			switch(ctx.outputFormat().wellKnown().orElseThrow(() -> new CoreException(Status.error("Autoexec.bas is only appropriate whem outpput format is well known (e.g. NEX).")))) {
			case NEX:
				basicProg = Txt2NextBasicConverter.encodeProgram(replaceVariables(ctx, """
				10 CD "[binaryPath]"
				20 .NEXLOAD [binaryFile]
				"""));
				break;
			case BIN:
				basicProg = Txt2NextBasicConverter.encodeProgram(replaceVariables(ctx, """
				10 CD "[binaryPath]"
				20 CLEAR [clear]
				30 LOAD "[binaryFile]" CODE [org]
				40 RANDOMIZE USR [org]
				"""));
				break;
			default:
				throw new CoreException(Status.error("Autoexec.bas is only appropriate for BIN or NEX output types."));
				
			}
			
 			var plus3dos = Plus3DosFile.fromBasic("autoexec.bas", basicProg, 0);
			var tf = Files.createTempFile("eclipzx", ".bas");
			try(var wtr = Files.newOutputStream(tf)) {
				wtr.write(plus3dos.toBytes());
			}
			ctx.addCleanUpTask(() -> {
				try {
					Files.deleteIfExists(tf);
				} catch (IOException e) {
					LOG.warn("Failed to delete temporary autoexec.bas file.", e);
				}
			});
			LOG.info("Adding autoexec.bas to disk image");
			
			fileSets.add(new FileSet(Purpose.BOOT, "/NextZXOS", false, new FileItem(tf.toFile(), "autoexec.bas")));
		}
		catch(IOException ioe) {
			throw new CoreException(Status.error("Failed to generate autoexec.bas.", ioe));
		}
		
	}
	
	private String replaceVariables(IPreparationContext prepCtx, String str) {
		str = str.replace("[binaryPath]", prepCtx.preparedBinaryFilePath().map(s -> s.replace("/", "\\")).orElseThrow(
				() -> new IllegalStateException(
						"Cannot create autoexec.bas, the binary output file was not "
						+ "included in preparation, so there is nothing to execute.")));
		str= str.replace("[binaryFile]", prepCtx.binaryFile().getName());
		str = str.replace("[clear]", String.valueOf(prepCtx.buildOptions().orgOrDefault() - 1));
		str = str.replace("[org]", String.valueOf(prepCtx.buildOptions().orgOrDefault()));
		
		System.out.println(str);
		
		return str;
	}

}
