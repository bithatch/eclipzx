package uk.co.bithatch.squashzx;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.function.Consumer;

import org.eclipse.core.runtime.IProgressMonitor;

import uk.co.bithatch.zyxy.compress.Compressor;
import uk.co.bithatch.zyxy.compress.Decompressor;
import uk.co.bithatch.zyxy.compress.Optimizer;

public class ZX0 {

    public static final int MAX_OFFSET_ZX0 = 32640;
    public static final int MAX_OFFSET_ZX7 = 2176;
    public static final int DEFAULT_THREADS = 4;
    
    public static ByteBuffer zx0(ByteBuffer input, int skip, boolean backwardsMode, boolean classicMode, boolean quickMode, int threads, Consumer<Integer> progress, int delta[]) {
        return new Compressor().compress(
                new Optimizer().optimize(input, skip, quickMode ? MAX_OFFSET_ZX7 : MAX_OFFSET_ZX0, threads, progress),
                input, skip, backwardsMode, !classicMode && !backwardsMode, delta);
    }

    public static ByteBuffer dzx0(ByteBuffer input, boolean backwardsMode, boolean classicMode) {
        return new Decompressor().decompress(input, backwardsMode, !classicMode && !backwardsMode);
    }

    public static void reverse(ByteBuffer array) {
        int i = 0;
        int j = array.limit()-1;
        while (i < j) {
            byte k = array.get(i);
            array.put(i++, array.get(j));
            array.put(j--, k);
        }
    }


	public static void  zx0(
			IProgressMonitor monitor, 
			Path path, 
			int skip, 
			boolean forcedMode, 
			boolean backwardsMode, 
			Path outputPath,
			boolean decompress,
			boolean classicMode,
			boolean quickMode,
			int threads) throws IOException {
		
		// read input file
        // TODO could use a MappedByteBuffer here? will break backwardsMode
    	ByteBuffer input = ByteBuffer.allocateDirect((int)Files.size(path));
        try(SeekableByteChannel in = Files.newByteChannel(path, StandardOpenOption.READ)) {
        	while(in.read(input) > 0) {}
        } catch (Exception e) {
            throw new IOException("Cannot read input file " + path);
        }
        input.flip();

        // determine input size
        if (input.limit() == 0) {
            throw new IOException("Empty input file " + path);
        }

        // validate skip against input size
        if (skip >= input.limit()) {
        	throw new IOException("Skipping entire input file " + path);
        }

        // check output file
        if (!forcedMode && Files.exists(outputPath)) {
            throw new IOException("Already existing output file " + outputPath);
        }

        // conditionally reverse input file
        if (backwardsMode) {
            reverse(input);
        }

        // generate output file
        ByteBuffer output = null;
        int[] delta = { 0 };

        if (!decompress) {
            output = zx0(input, skip, backwardsMode, classicMode, quickMode, threads, new Consumer<Integer>() {
			
            	private int last = 0;
            	
				@Override
				public void accept(Integer t) {
					var worked = t - last;
					monitor.worked(worked);
					last = t;
				}
			}, delta);
        } else {
            try {
                output = dzx0(input, backwardsMode, classicMode);
            } catch (ArrayIndexOutOfBoundsException e) {
                throw new IOException("Invalid input file " + path);
            }
        }

        // conditionally reverse output file
        if (backwardsMode) {
            reverse(output);
        }

        // write output file
        try(SeekableByteChannel out = Files.newByteChannel(outputPath, WRITE, CREATE, forcedMode ? TRUNCATE_EXISTING : CREATE_NEW)) {
        	while(out.write(output) > 0);
        } catch (Exception e) {
            System.err.println("Error: Cannot write output file " + outputPath);
            System.exit(1);
        }

        // done!
        if (!decompress) {
            System.out.println("File " + (skip > 0 ? "partially " : "") + "compressed " + (backwardsMode ? "backwards " : "") + "from " + (input.limit()-skip) + " to " + output.limit() + " bytes! (delta " + delta[0] + ")");
        } else {
            System.out.println("File decompressed " + (backwardsMode ? "backwards " : "") + "from " + (input.limit()-skip) + " to " + output.limit() + " bytes!");
        }
	}
}
