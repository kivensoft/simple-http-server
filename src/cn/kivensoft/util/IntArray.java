package cn.kivensoft.util;

import java.io.Serializable;
import java.util.Iterator;

final public class IntArray implements Serializable, Iterable<Integer> {

	private static final long serialVersionUID = 1L;

	protected static final int B0 = 5; // Initial capacity in bits.
	protected static final int C0 = 1 << B0; // Initial capacity (32)
	protected static final int B1 = 10; // Low array maximum capacity in bits.
	protected static final int C1 = 1 << B1; // Low array maximum capacity (1024).
	protected static final int M1 = C1 - 1; // Mask.

	// Resizes up to 1024 maximum (32, 64, 128, 256, 512, 1024).
	protected int[] _low = new int[C0];
	// For larger capacity use multi-dimensional array.
	protected int[][] _high = new int[1][];

	// Holds the current length.
	protected int _length;

	// Holds current capacity.
	protected int _capacity = C0;

	final protected void increaseCapacity() {
		if (_capacity < C1) { // For small capacity, resize.
			_capacity <<= 1;
			int[] tmp = new int[_capacity];
			System.arraycopy(_low, 0, tmp, 0, _length);
			_low = tmp;
			_high[0] = tmp;
		} else { // Add a new low block of 1024 elements.
			int highIndex = _capacity >> B1;
			if (highIndex >= _high.length) { // Resizes _high.
				int[][] tmp = new int[_high.length * 2][];
				System.arraycopy(_high, 0, tmp, 0, _high.length);
				_high = tmp;
			}
			_high[highIndex] = new int[C1];
			_capacity += C1;
		}
	}

	public IntArray() {
		_high[0] = _low;
	}

	public IntArray(int capacity) {
		this();
		while (_capacity < capacity) _capacity <<= 1;
		if (capacity <= C1) {
			_low = new int[_capacity];
		} else {
			int highLen = _capacity >> B1;
			_low = new int[C1];
			_high = new int[highLen][];
			for (int i = 1; i < highLen; ++i)
				_high[i] = new int[C1];
		}
		_high[0] = _low;
	}

	final public int size() {
		return _length;
	}

	final public int length() {
		return _length;
	}

	final public int capacity() {
		return _capacity;
	}

	public boolean isEmpty() {
		return _length == 0;
	}

	public interface onForEach {
		boolean call(int[] array, int start, int len);
	}

	public final void forEach(onForEach act) {
		forEach(0, _length, act);
	}

	public final void forEach(int begin, int end, onForEach act) {
		for (int i = begin; i < end;) {
			int[] sub = _high[i >> B1];
			int start = i & M1;
			int x = C1 - start, y = end - i;
			int length = x < y ? x : y;
			if (!act.call(sub, start, length)) return;
			i += length;
		}
	}

	public boolean contains(int value) {
		return indexOf(value) != -1;
	}

	@Override
	public int hashCode() {
		int result = 1;
		result = 31 * result + _capacity;
		result = 31 * result + _length;
		int[] elementHash = {1};
		forEach(0, _length, (va, start, len) -> {
			for (int i = 0, imax = start + len; i < imax; ++i)
				elementHash[0] = 31 * elementHash[0] + va[i];
			return true;
		});
		result = 31 * result + elementHash[0];
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		IntArray other = (IntArray) obj;
		if (_capacity != other._capacity) return false;
		if (_length != other._length) return false;
		for (int i = 0; i < _length;) {
			int[] sub1 = _high[i >> B1], sub2 = other._high[i >> B1];
			int start = i & M1;
			int x = C1 - start, y = _length - i;
			int len = x < y ? x : y;
			for (int j = start, jmax = start + len; j < jmax; ++j)
				if (sub1[j] != sub2[j]) return false;
			i += len;
		}
		return true;
	}

	@Override
	public Iterator<Integer> iterator() {
		return new Iterator<Integer>() {
			int index = 0;
			@Override public boolean hasNext() { return index < _length; }
			@Override public Integer next() { return get(index++); }
		};
	}

	public int[] toArray() {
		int[] ret = new int[_length];
		int[] ret_pos = {0};
		forEach(0, _length, (va, start, len) -> {
			System.arraycopy(va, start, ret, ret_pos[0], len);
			ret_pos[0] += len;
			return true;
		});
		return ret;
	}

	public boolean removeValue(int value) {
		int index = indexOf(value);
		if (index != -1) remove(index);
		return true;
	}

	public boolean containsAll(int... values) {
		for (int i = 0, imax = values.length; i < imax; ++i)
			if (indexOf(values[i]) == -1) return false;
		return true;
	}

	public boolean addAll(int[] values) {
		int newLen = _length + values.length;
		while (newLen > _capacity) increaseCapacity();
		for (int i = _length, imax = _length + values.length, j = 0; i < imax; ++i,++j)
			_high[i >> B1][i & M1] = values[j];
		_length += values.length;
		return true;
	}

	public boolean addAll(int index, int... values) {
		int newLen = _length + values.length;
		while (newLen > _capacity) increaseCapacity();
		for (int i = _length - 1, j = newLen - 1; i >= index; --i, --j)
			_high[j >> B1][j & M1] = _high[i >> B1][i & M1];
		for (int i = index, imax = index + values.length, j = 0; i < imax; ++i, ++j)
			_high[i >> B1][i & M1] = values[j];
		_length = newLen;
		return true;
	}

	public boolean removeAll(int... values) {
		for (int i = 0, imax = values.length; i < imax; ++i) {
			int idx = indexOf(values[i]);
			while (idx != -1) {
				remove(idx);
				idx = indexOf(values[i], idx, _length);
			}
		}
		return true;
	}

	public void clear() {
		forEach(0, _length, (va, start, len) -> {
			for (int i = start, imax = start + len; i < imax; ++i)
				va[i] = 0;
			return true;
		});
		_length = 0;
	}

	public int get(int index) {
		return index < C1 ? _low[index] : _high[index >> B1][index & M1];
	}

	public int set(final int index, final int value) {
		int ret;
		if (index < C1) {
			ret = _low[index];
			_low[index] = value;
		} else {
			int[] sub = _high[index >> B1];
			int idx = index & M1;
			ret = sub[idx];
			sub[idx] = value;
		}
		return ret;
	}

	public void add(final int value) {
		if (_length >= _capacity) increaseCapacity();
		_high[_length >> B1][_length & M1] = value;
		++_length;
	}

	public void add(final int index, final int value) {
		if (_length >= _capacity) increaseCapacity();
		
		for (int i = _length; i > index;) {
			int[] sub = _high[i >> B1];
			int start = i & M1;
			int y = i - index;
			int stop = start < y ? 0 : (index + 1) & M1;
			for (int j = start; j > stop; --j)
				 sub[j] = sub[j - 1];
			i -= start - stop + 1;
			sub[stop] = _high[i >> B1][i & M1];
		}

		_high[index >> B1][index & M1] = value;
		++_length;
	}

	public int remove(int index) {
		int ret = _high[index >> B1][index & M1];
		for (int i = index, imax = _length - 1; i < imax;) {
			int[] sub = _high[i >> B1];
			int start = i & M1;
			int x = C1 - start, y = imax - i;
			int len = x < y ? x : y;
			for (int j = start, jmax = start + len - 1; j < jmax; ++j)
				sub[j] = sub[j + 1];
			i += len;
			sub[start + len - 1] = _high[i >> B1][i & M1];
		}
		--_length;
		return ret;
	}

	public int indexOf(int value) {
		return indexOf(value, 0, _length);
	}

	public int indexOf(int value, int begin, int end) {
		for (int i = begin; i < end;) {
			int[] sub = _high[i >> B1];
			int start = i & M1;
			int x = C1 - start, y = end - i;
			int subLen = x < y ? x : y;
			for (int j = start, jmax = start + subLen; j< jmax; ++j)
				if (sub[j] == value) return i + j;
			i += subLen;
		}
		return -1;
	}

	public int lastIndexOf(int value) {
		for (int i = _length - 1; i >= 0;) {
			int[] sub = _high[i >> B1];
			int start = i & M1;
			for (int j = start; j >= 0; --j)
				 if (value == sub[j]) return i + j - start;
			i -= start + 1;
		}
		return -1;
	}

}
