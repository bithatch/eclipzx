package uk.co.bithatch.fatexplorer.vfs;

public interface FileOverwritePolicy {
	enum Decision {
		REPLACE, SKIP, CANCEL
	}

	public final static class Defaults {
		private final static FileOverwritePolicy ALWAYS = new FileOverwritePolicy() {
			@Override
			public Decision queryOverwrite(String path, boolean isMove) {
				return Decision.REPLACE;
			}
		};
	}

	static FileOverwritePolicy always() {
		return Defaults.ALWAYS;
	}

	Decision queryOverwrite(String path, boolean isMove);
}
