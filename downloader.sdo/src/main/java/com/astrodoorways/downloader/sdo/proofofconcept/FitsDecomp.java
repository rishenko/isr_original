package com.astrodoorways.downloader.sdo.proofofconcept;

import java.io.IOException;
import java.io.InputStream;

public class FitsDecomp {

	public int fits_rdecomp_short(InputStream c, /* input buffer                 */
			long clen, /* length of input              */
			short array[], /* output array                 */
			int nx, /* number of output pixels      */
			int nblock) /* coding block size            */
	throws IOException {

		nblock = 32; // according to blocksize in the header? shit if I know

		int i, imax;
		int bsize, k;
		int nbits, nzero, fs;
		int bytevalue;
		int b, diff, lastpix;
		int fsmax, fsbits, bbits;
		int[] nonzero_count = null;

		/*
		  * Original size of each pixel (bsize, bytes) and coding block
		  * size (nblock, pixels)
		  * Could make bsize a parameter to allow more efficient
		  * compression of short & byte images.
		  */

		bsize = 2;

		/*    nblock = 32; now an input parameter */
		/*
		 * From bsize derive:
		 * FSBITS = # bits required to store FS
		 * FSMAX = maximum value for FS
		 * BBITS = bits/pixel for direct coding
		 */

		/*
		   switch (bsize) {
		   case 1:
		       fsbits = 3;
		       fsmax = 6;
		       break;
		   case 2:
		       fsbits = 4;
		       fsmax = 14;
		       break;
		   case 4:
		       fsbits = 5;
		       fsmax = 25;
		       break;
		   default:
		       System.out.println("rdecomp: bsize must be 1, 2, or 4 bytes");
		       return 1;
		   }
		*/

		/* move out of switch block, to tweak performance */
		fsbits = 4;
		fsmax = 14;

		bbits = 1 << fsbits;

		if (nonzero_count == null) { //NULL
			/*
			 * nonzero_count is lookup table giving number of bits
			 * in 8-bit values not including leading zeros
			 */

			/*  NOTE!!!  This memory never gets freed  */

			//       nonzero_count = (int ) malloc(256*sizeof(int)); // ALLOCATE a bloc of memory equivalent to 256 times the size of integer and return a pointer to it

			nonzero_count = new int[256];

			nzero = 8;
			k = 128;
			for (i = 255; i >= 0;) {
				for (; i >= k; i--)
					nonzero_count[i] = nzero;
				k = k / 2;
				nzero--;
			}
		}
		/*
		 * Decode in blocks of nblock pixels
		 */

		/* first 2 bytes of input buffer contain the value of the first */
		/* 2 byte integer value, without any encoding */

		lastpix = 0;
		bytevalue = c.read();
		lastpix = lastpix | (bytevalue << 8);
		bytevalue = c.read();
		lastpix = lastpix | bytevalue;

		b = c.read(); /* bit buffer                       */
		nbits = 8; /* number of bits remaining in b    */
		for (i = 0; i < nx;) {
			/* get the FS value from first fsbits */
			nbits -= fsbits;
			while (nbits < 0) {
				b = (b << 8) | (c.read());
				nbits += 8;
			}
			fs = (b >> nbits) - 1;

			b &= (1 << nbits) - 1;
			/* loop over the next block */
			imax = i + nblock;
			if (imax > nx)
				imax = nx;
			if (fs < 0) {
				/* low-entropy case, all zero differences */
				for (; i < imax; i++)
					array[i] = (short) lastpix;
			} else if (fs == fsmax) {
				/* high-entropy case, directly coded pixel values */
				for (; i < imax; i++) {
					k = bbits - nbits;
					diff = b << k;
					for (k -= 8; k >= 0; k -= 8) {
						b = c.read();
						diff |= b << k;
					}
					if (nbits > 0) {
						b = c.read();
						diff |= b >> (-k);
						b &= (1 << nbits) - 1;
					} else {
						b = 0;
					}

					/*
					 * undo mapping and differencing
					 * Note that some of these operations will overflow the
					 * unsigned int arithmetic -- that's OK, it all works
					 * out to give the right answers in the output file.
					 */
					if ((diff & 1) == 0) {
						diff = diff >> 1;
					} else {
						diff = ~(diff >> 1);
					}
					array[i] = (short) (diff + lastpix);
					lastpix = array[i];
				}
			} else {
				/* normal case, Rice coding */
				for (; i < imax; i++) {
					/* count number of leading zeros */
					while (b == 0) {
						nbits += 8;
						b = c.read();
					}
					nzero = nbits - nonzero_count[b];
					nbits -= nzero + 1;
					/* flip the leading one-bit */
					b ^= 1 << nbits;
					/* get the FS trailing bits */
					nbits -= fs;
					while (nbits < 0) {
						b = (b << 8) | (c.read());
						nbits += 8;
					}
					diff = (nzero << fs) | (b >> nbits);
					b &= (1 << nbits) - 1;

					/* undo mapping and differencing */
					if ((diff & 1) == 0) {
						diff = diff >> 1;
					} else {
						diff = ~(diff >> 1);
					}
					array[i] = (short) (diff + lastpix);
					lastpix = array[i];
				}
			}
			//			if (c. > cend) {
			//				System.out.println("decompression error: hit end of compressed byte stream");
			//				return 1;
			//			}
		}
		//		if (c < cend) {
		//			System.out.println("decompression warning: unused bytes at end of compressed buffer");
		//		}
		return 0;
	}

}
