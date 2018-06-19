package cn.kivensoft.sql;

import java.util.LinkedList;
import java.util.List;

public class DynParams {
	private List<String> columns = new LinkedList<>();
	private List<Object> values = new LinkedList<>();
	
	boolean isEmpty() {
		return columns.size() == 0;
	}
	
	public void add(String column, Object value) {
		columns.add(column);
		values.add(value);
	}
	
	public DynParams addIf(boolean predicate, String column, Object value) {
		if (predicate) {
			columns.add(column);
			values.add(value);
		}
		return this;
	}
	
	public DynParams addIfNotNull(String column, Object value) {
		if (value != null) {
			if (value.getClass() == String.class) {
				if (((String)value).length() > 0) {
					columns.add(column);
					values.add(value);
				}
			} else {
				columns.add(column);
				values.add(value);
			}
		}
		return this;
	}

	public List<String> getColumns() {
		return columns;
	}

	public List<Object> getValues() {
		return values;
	}
}
