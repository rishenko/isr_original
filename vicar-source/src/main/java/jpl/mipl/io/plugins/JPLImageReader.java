package jpl.mipl.io.plugins;

import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;

import jpl.mipl.io.vicar.SystemLabel;

/**
 * Base abstract class for JPL ImageReader classes. Provides access to common objects, such
 * as <code>SystemLabel</code>.
 * 
 * @author kmcabee
 *
 */
public abstract class JPLImageReader extends ImageReader {

	private SystemLabel systemLabel;

	protected JPLImageReader(ImageReaderSpi originatingProvider) {
		super(originatingProvider);
	}

	public SystemLabel getSystemLabel() {
		return systemLabel;
	}

	public void setSystemLabel(SystemLabel sys) {
		this.systemLabel = sys;
	}
}
