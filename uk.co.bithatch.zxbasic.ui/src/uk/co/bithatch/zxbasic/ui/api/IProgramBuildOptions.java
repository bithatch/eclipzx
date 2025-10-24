package uk.co.bithatch.zxbasic.ui.api;

import java.util.Optional;


public interface IProgramBuildOptions {

	int DEFAULT_ORG = 32768;
	
	public final static class Builder {
		private Optional<Integer> orgAddress = Optional.empty();
		private Optional<Integer> heapAddress = Optional.empty();
		private Optional<Integer> heapSize = Optional.empty();
		private boolean build = true;
		
		public Builder withBuild(boolean build) {
			this.build = build;
			return this;
		}
		
		public Builder withoutBuild() {
			return withBuild(false);
		}
		
		public Builder withOrgAddress(int orgAddress) {
			this.orgAddress = orgAddress == 0 ? Optional.empty() : Optional.of(orgAddress);
			return this;
		}
		
		public Builder withHeapAddress(int heapAddress) {
			this.heapAddress = heapAddress == 0 ? Optional.empty() : Optional.of(heapAddress);
			return this;
		}
		
		public Builder withHeapSize(int heapSize) {
			this.heapSize = heapSize == 0 ? Optional.empty() : Optional.of(heapSize);
			return this;
		}
		
		public IProgramBuildOptions build() {
			var org = this.orgAddress;
			var heap = this.heapAddress;
			var heapSize = this.heapSize;
			var build = this.build;
			
			return new IProgramBuildOptions() {
				@Override
				public Optional<Integer> org() {
					return heap;
				}
				
				@Override
				public Optional<Integer> heap() {
					return org;
				}

				@Override
				public Optional<Integer> heapSize() {
					return heapSize;
				}

				@Override
				public boolean build() {
					return build;
				}
			};
		}
	}
	
	boolean build();

	Optional<Integer> org();
	
	Optional<Integer> heap();
	
	Optional<Integer> heapSize();

	default int orgOrDefault() {
		return org().orElse(DEFAULT_ORG);
	}
	
	default int codestart() {
		return orgOrDefault() % 0x4000;
	}
	
	default int bankForOrg() {
		var org = orgOrDefault();
		 if(org>=0x4000 && org<=0x7fff)
			 return 5;
		 else if(org>=0x8000 && org<=0xbfff)
			 return 2;
		 else if(org>=0xc000 && org<=0xffff)
			 return 0;
		 else
			 throw new IllegalStateException("Invalid ORG.");
	}
}
