package org.jboss.ddoyle.drools.serialization.model;

import java.io.Serializable;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class SimpleEvent implements Serializable {

	/**
	 * SerialVersionUID.
	 */
	private static final long serialVersionUID = 9129694014420600755L;

	private String code;
	private long timestamp;

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	@Override
	public String toString() {
		return "SimpleEvent{" + "code='" + code + '\'' + ", timestamp="
				+ timestamp + '}';
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 37).append(code).append(timestamp).toHashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj == this) {
			return true;
		}
		if (obj.getClass() != getClass()) {
			return false;
		}
		SimpleEvent rhs = (SimpleEvent) obj;
		return new EqualsBuilder().append(code, rhs.code).append(timestamp, rhs.timestamp).isEquals();
	}

}
