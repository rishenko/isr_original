package nom.tam.fits.io.plugins;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import javax.imageio.stream.ImageInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FitsImageInputStreamWrapper extends InputStream {

	Logger logger = LoggerFactory.getLogger(FitsImageInputStreamWrapper.class);
	private final ImageInputStream stream;
	private ByteBuffer byteBuffer;
	private int streamLength = 0;
	private byte[] buff = null;
	private int count = 0;

	public FitsImageInputStreamWrapper(ImageInputStream stream) {
		this.stream = stream;
	}

	public int read() {
		try {
			return stream.readUnsignedByte();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return -1;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		return stream.read(b, off, len);
	}

	@Override
	public void close() throws IOException {
		super.close();
		stream.close();
	}

	public boolean markSupported() {
		return false;
	}

}
