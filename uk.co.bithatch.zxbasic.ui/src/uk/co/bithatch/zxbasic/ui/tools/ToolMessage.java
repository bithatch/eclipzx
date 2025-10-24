package uk.co.bithatch.zxbasic.ui.tools;

public final class ToolMessage {
	private final String path;
	private final int line;
	private final ToolMessageLevel level;
	private final String message;
	
	public ToolMessage(String path, int line, ToolMessageLevel level, String message) {
		super();
		this.path = path;
		this.line = line;
		this.level = level;
		this.message = message;
	}
	
	public String getPath() {
		return path;
	}

	public int getLine() {
		return line;
	}

	public ToolMessageLevel getLevel() {
		return level;
	}

	public String getMessage() {
		return message;
	}

	public static ToolMessage parse(String output) {
		var parts = output.split(":");
		try {
			return new ToolMessage(parts[0], Integer.parseInt(parts[1]), ToolMessageLevel.valueOf(parts[2].trim().toUpperCase()), parts[3].trim());
		}
		catch(Exception e) {
			return new ToolMessage("/", 0, ToolMessageLevel.ERROR, "Failed toArgs: /usr/bin/python3 /home/SOUTHPARK/tanktarta/.eclipzx/zxb/zxbasic-1.18.1/zxbc.py -d -d -d -f z80 /home/SOUTHPARK/tanktarta/Desktop/Eclipsz/test/dimtest.bas\n"
					+ " parse output. " + e.getMessage());
		}
	}
	
}