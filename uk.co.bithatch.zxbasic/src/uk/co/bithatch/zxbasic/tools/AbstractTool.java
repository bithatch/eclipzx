package uk.co.bithatch.zxbasic.tools;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.Callable;

public abstract class AbstractTool implements Closeable {
	/**
	 * Builder
	 */
	public abstract static class AbstractBuilder<BLDR extends AbstractBuilder<BLDR, TOOL>, TOOL> {
		
		public abstract TOOL build();
	}

	private Thread runThread;
	private boolean stop;
	private boolean debug = true;
	
    
    protected AbstractTool(AbstractBuilder<?, ?> bldr) {
    }

    protected <T> T runTerminable(Callable<T> callable) {
    	if(runThread == null) {
    		stop = false;
    		runThread = Thread.currentThread();
    		try {
    			return callable.call();
    		}
    		catch(RuntimeException re) {
    			throw re;
    		}
    		catch(IOException ioe) {
    			throw new UncheckedIOException(ioe);
    		}
    		catch(Exception e) {
    			throw new IllegalStateException(e);
    		}
    		finally {
    			runThread = null;
    		}
    	}
    	else {
    		throw new IllegalStateException("Already active.");
    	}
    }
    
    protected final boolean isStop() {
    	return stop;
    }

	public final void terminate() {
		var rt = runThread;
		if(rt == null)
			throw new IllegalStateException("Not active.");
		else {
			try {
				stop = true;
				rt.interrupt();
			}
			finally {
				onTerminate();
			}
		}
		
	}

    protected void onTerminate() {
	}

	@Override
	public final void close() {
    	if(runThread != null)
    		terminate();
    	onClose();
	}

    protected void onClose() {
	}


    protected void log(String message) {
        if (debug) {
            System.out.println("[DEBUG] " + message);
        }
    }

}
