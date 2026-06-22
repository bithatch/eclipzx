package uk.co.bithatch.eclipz80.assembler;

public final class Symbol {
	
	public final static class Builder {

		private final String name;
		private int address;
		private boolean isGlobal = false;
		private boolean isExternal = false;
		private boolean isPublic = false;
		private String section;
		private String module;
		
		public Builder(String name) {
			this.name = name;
		}
		
		public Builder withSection(String section) {
			this.section = section;
			return  this;
		}
		
		public Builder withModule(String module) {
			this.module = module;
			return  this;
		}

		public Builder asGlobal(boolean isGlobal) {
			this.isGlobal = isGlobal;
			return this;
		}

		public Builder asPublic(boolean isPublic) {
			this.isPublic = isPublic;
			return this;
		}

		public Builder asExternal(boolean isExternal) {
			this.isExternal = isExternal;
			return this;
		}
		
		public Builder withAddress(int address) {
			this.address = address;
			return this;
		}
		
		public Symbol build() {
			return new Symbol(this);
		}
	}
	
	final String name;
	int address;
	boolean isGlobal;
	boolean isExternal;
	boolean isPublic;
	String section;
	String module;
	
	Symbol(String name) {
		this.name = name;
	}	
	
	private Symbol(Builder builder) {
		this.name = builder.name;
		this.address = builder.address;
		this.isExternal = builder.isExternal;
		this.isPublic = builder.isPublic;
		this.isGlobal = builder.isGlobal;
		this.section = builder.section;
		this.module = builder.module;
	}

	public int address() {
		return address;
	}
	
	public boolean isPublic() {
		return isPublic;
	}
	
	public boolean isExternal() {
		return isExternal;
	}
	
	public boolean isGlobal() {
		return isGlobal;
	}
	
	public String name() {
		return name;
	}
	
	public String section() {
		return section;
	}
	
	public String module() {
		return module;
	}
}