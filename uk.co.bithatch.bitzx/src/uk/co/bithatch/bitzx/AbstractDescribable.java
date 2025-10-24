package uk.co.bithatch.bitzx;

import java.util.Objects;

public abstract class AbstractDescribable implements IDescribed {
	
	private final String name;

	protected AbstractDescribable(String name) {
		this.name = name;
	}
	
	public final String name() {
		return name;
	}

	@Override
	public final int hashCode() {
		return Objects.hash(name());
	}

	@Override
	public final boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!getClass().isAssignableFrom(obj.getClass()) && !obj.getClass().isAssignableFrom(getClass()))
			return false;
		AbstractDescribable other = (AbstractDescribable) obj;
		return Objects.equals(name(), other.name());
	}
	
	
}
