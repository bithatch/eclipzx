package uk.co.bithatch.zxbasic.ui.preparation;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;

import uk.co.bithatch.zxbasic.tools.Plus3DosFile;
import uk.co.bithatch.zxbasic.tools.Txt2NextBasicConverter;
import uk.co.bithatch.zxbasic.ui.api.IPreparationContext;
import uk.co.bithatch.zxbasic.ui.api.IPreparationSource;
import uk.co.bithatch.zxbasic.ui.language.BorielZXBasicOutputFormat;
import uk.co.bithatch.zxbasic.ui.util.FileItem;
import uk.co.bithatch.zxbasic.ui.util.FileSet;
import uk.co.bithatch.zxbasic.ui.util.FileSet.Purpose;

public class AutoexecBasPreparationSource implements IPreparationSource {
	private final static ILog LOG = ILog.of(AutoexecBasPreparationSource.class);

	@Override
	public void contribute(IPreparationContext ctx, List<FileSet> fileSets, IProgressMonitor monitor) throws CoreException {
		
		try {
			byte[] basicProg;
			
			switch(ctx.outputFormat()) {
			case BorielZXBasicOutputFormat.NEX:
				basicProg = Txt2NextBasicConverter.encodeProgram(replaceVariables(ctx, """
				10 CD "[binaryPath]"
				20 .NEXLOAD [binaryFile]
				"""));
				break;
			case BorielZXBasicOutputFormat.BIN:
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
			
			var plus3dos = Plus3DosFile.fromBasic("autoexec.bas", basicProg, 10, 0);
			var tf = Files.createTempFile("eclipzx", ".bas");
			try(var wtr = Files.newOutputStream(tf)) {
				wtr.write(plus3dos.toBytes());
			}
			
			LOG.info("Adding autoexec.bas to disk image");
			
			fileSets.add(new FileSet(Purpose.BOOT, "/", false, new FileItem(tf.toFile(), "autoexec.bas")));
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
