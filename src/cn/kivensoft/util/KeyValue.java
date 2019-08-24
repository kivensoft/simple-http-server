package cn.kivensoft.util;

public final class KeyValue {
	private Integer key;
	private String value;

	public KeyValue of(Integer key, String value) {
		return new KeyValue(key, value);
	}
	
	public KeyValue() {
		super();
	}
	
	public KeyValue(Integer key, String value) {
		this.key = key;
		this.value = value;
	}
	
	public Integer getKey() {
		return key;
	}

	public void setKey(Integer key) {
		this.key = key;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((key == null) ? 0 : key.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		KeyValue other = (KeyValue) obj;
		if (key == null) {
			if (other.key != null) return false;
		} else if (!key.equals(other.key))
			return false;
		if (value == null) {
			if (other.value != null) return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return Fmt.toJson(this);
	}
	
}
