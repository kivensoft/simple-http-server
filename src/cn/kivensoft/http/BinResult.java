package cn.kivensoft.http;

public class BinResult {
	private String contentType;
	private byte[] data;

	public BinResult() {
		super();
	}

	public BinResult(String contentType, byte[] data) {
		super();
		this.contentType = contentType;
		this.data = data;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}
}
