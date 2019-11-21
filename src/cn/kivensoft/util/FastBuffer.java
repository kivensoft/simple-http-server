package cn.kivensoft.util;

import java.io.Serializable;

public class FastBuffer implements Serializable {

	private static final long serialVersionUID = 1L;

//	protected static final int B0 = 5; // Initial capacity in bits.
//	protected static final int C0 = 1 << B0; // Initial capacity (32)
	protected static final int B1 = 10; // Low array maximum capacity in bits.
	protected static final int C1 = 1 << B1; // Low array maximum capacity (1024).
	protected static final int M1 = C1 - 1; // Mask.

	// Resizes up to 1024 maximum (32, 64, 128, 256, 512, 1024).
	protected byte[] _low = new byte[C1];
	// For larger capacity use multi-dimensional array.
	protected byte[][] _high = new byte[1][];

	// Holds the current length.
	protected int _length;

	// Holds current capacity.
	protected int _capacity = C1;

	final protected void increaseCapacity() {
		int highIndex = _capacity >> B1;
		if (highIndex >= _high.length) { // Resizes _high.
			byte[][] tmp = new byte[_high.length << 1][];
			System.arraycopy(_high, 0, tmp, 0, _high.length);
			_high = tmp;
		}
		_high[highIndex] = new byte[C1];
		_capacity += C1;
	}

	public FastBuffer() {
		_high[0] = _low;
	}

	public FastBuffer(int capacity) {
		this();
		while (_capacity < capacity) _capacity <<= 1;
		if (_capacity > C1) {
			int highLen = _capacity >> B1;
			_low = new byte[C1];
			_high = new byte[highLen][];
			for (int i = 1; i < highLen; ++i)
				_high[i] = new byte[C1];
		}
		_high[0] = _low;
	}

	final public int length() {
		return _length;
	}

	final public int capacity() {
		return _capacity;
	}

	final public boolean isEmpty() {
		return _length == 0;
	}

	final public FastBuffer setLength(int newLength) {
		if (newLength <= _length) _length = newLength;
		else setLength(newLength, (byte)0);
		return this;
	}

	final public FastBuffer setLength(int newLength, byte fillByte) {
		if (newLength <= _length) _length = newLength;
		else {
			while (newLength > _capacity) increaseCapacity();
			for (int i = _length; i < newLength; ++i)
				_high[i >> B1][i & M1] = fillByte;
			_length = newLength;
		}
		return this;
	}

	public interface onForEach {
		boolean call(byte[] array, int start, int len);
	}

	final public void forEach(onForEach act) {
		forEach(0, _length, act);
	}

	final public void forEach(int begin, int end, onForEach act) {
		for (int i = begin; i < end;) {
			byte[] sub = _high[i >> B1];
			int off = i & M1;
			int x = C1 - off, y = end - i;
			int len = x < y ? x : y;
			if (!act.call(sub, off, len)) return;
			i += len;
		}
	}

	final public byte[] getBytes() {
		return getBytes(0, _length);
	}

	final public byte[] getBytes(int start, int end) {
		byte[] ret = new byte[end - start];
		int[] ret_pos = {0};
		forEach(0, _length, (va, off, len) -> {
			System.arraycopy(va, off, ret, ret_pos[0], len);
			ret_pos[0] += len;
			return true;
		});
		return ret;
	}

	final public FastBuffer append(byte value) {
		if (_length >= _capacity) increaseCapacity();
		_high[_length >> B1][_length & M1] = value;
		++_length;
		return this;
	}

	final public FastBuffer append(int index, byte value) {
		if (_length >= _capacity) increaseCapacity();

		for (int i = _length; i > index;) {
			byte[] sub = _high[i >> B1];
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
		return this;
	}

	final public FastBuffer append(byte[] value) {
		return value == null ? this : append(value, 0, value.length);
	}

	final public FastBuffer append(byte[] value, int off, int len) {
		if (value == null) return this;
		int newLen = _length + len;
		while (newLen > _capacity) increaseCapacity();
		int[] pos = new int[] { off };
		forEach(_length, newLen, (_bs, _start, _len) -> {
			for (int i = _start, imax = _start + _len, j = pos[0]; i < imax; ++i, ++j)
				_bs[i] = value[j];
			pos[0] += _len;
			return true;
		});
		_length = newLen;
		return this;
	}

	final public void append(int index, byte[] value) {
		append(index, value, 0, value.length);
	}

	final public FastBuffer append(int index, byte[] value, int off, int len) {
		if (value == null) return this;
		int newLen = _length + len;
		while (newLen > _capacity) increaseCapacity();
		for (int i = _length - 1, j = newLen - 1; i >= index; --i, --j)
			_high[j >> B1][j & M1] = _high[i >> B1][i & M1];
		for (int i = index, imax = index + len, j = off; i < imax; ++i, ++j)
			_high[i >> B1][i & M1] = value[j];
		_length = newLen;
		return this;
	}

	final public FastBuffer remove(int index) {
		for (int i = index, imax = _length - 1; i < imax;) {
			byte[] sub = _high[i >> B1];
			int start = i & M1;
			int x = C1 - start, y = imax - i;
			int len = x < y ? x : y;
			for (int j = start, jmax = start + len - 1; j < jmax; ++j)
				sub[j] = sub[j + 1];
			i += len;
			sub[start + len - 1] = _high[i >> B1][i & M1];
		}
		--_length;
		return this;
	}

	final public FastBuffer remove(int begin, int end) {
		for (int i = end, j = begin; i < _length; ++i,++j)
			_high[j >> B1][j & M1] = _high[i >> B1][i & M1];
		_length -= end - begin;
		return this;
	}

	final public FastBuffer clear() {
		_length = 0;
		return this;
	}

	final public byte get(int index) {
		return index < C1 ? _low[index] : _high[index >> B1][index & M1];
	}

	final public FastBuffer set(int index, byte value) {
		if (index < C1) _low[index] = value;
		else _high[index >> B1][index & M1] = value;
		return this;
	}

	final public int indexOf(byte b, int begin, int end) {
		for (int i = begin; i < end;) {
			byte[] sub = _high[i >> B1];
			int start = i & M1;
			int x = C1 - start, y = end - i;
			int len = x < y ? x : y;
			for (int j = start, jmax = start + len; j < jmax; ++j)
				if (sub[j] == b) return i + j;
			i += len;
		}
		return -1;
	}

	final public int indexOf(byte b) {
		return indexOf(b, 0, _length);
	}

	@Override
	public int hashCode() {
		int result = 1;
		result = 31 * result + _capacity;
		result = 31 * result + _length;
		for (int i = 0; i < _length;) {
			byte[] sub = _high[i >> B1];
			int start = i & M1;
			int x = C1 - start, y = _length - i;
			int len = x < y ? x : y;
			for (int j = start, jmax = start + len; j < jmax; ++j)
				result = 31 * result + sub[j];
			i += len;
		}
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		FastBuffer other = (FastBuffer) obj;
		if (_capacity != other._capacity) return false;
		if (_length != other._length) return false;
		for (int i = 0; i < _length;) {
			byte[] sub1 = _high[i >> B1], sub2 = other._high[i >> B1];
			int start = i & M1;
			int x = C1 - start, y = _length - i;
			int len = x < y ? x : y;
			for (int j = start, jmax = start + len; j < jmax; ++j)
				if (sub1[j] != sub2[j]) return false;
			i += len;
		}
		return true;
	}

	public final FastBuffer append(String text) {
		return text == null ? this : append(text, 0, text.length());
	}

	public final FastBuffer append(String text, int off, int len) {
		for (int i = off, imax = off + len; i < imax; ++i) {
			int c = ((int) text.charAt(i)) & 0xFFFF;
			if (c < 0x80) {
				append((byte) c);
				continue;
			}

			int bc;
			if (c < 0x800)        bc = 0;
			else if (c < 0x10000) bc = 1;
			else                  bc = 2;

			append((byte) ((c >> ((bc + 1) * 6)) | ((0xF0 << (2 - bc)) & 0xFF)));
			for (int j = bc * 6; j >= 0; j -= 6)
				append((byte) (((c >> j) & 0x3F) | 0x80));
		}
		return this;
	}
}
