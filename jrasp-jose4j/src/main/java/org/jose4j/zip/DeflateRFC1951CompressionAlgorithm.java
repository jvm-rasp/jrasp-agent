/*
 * Copyright 2012-2013 Brian Campbell
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jose4j.zip;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import org.jose4j.keys.KeyPersuasion;
import org.jose4j.lang.JoseException;
import org.jose4j.lang.UncheckedJoseException;

/**
 */
public class DeflateRFC1951CompressionAlgorithm implements CompressionAlgorithm {

	public byte[] compress(byte[] data) {
		Deflater deflater = new Deflater(Deflater.DEFLATED, true);
		try {
			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			DeflaterOutputStream deflaterOutputStream = new DeflaterOutputStream(byteArrayOutputStream, deflater);
			deflaterOutputStream.write(data);
			deflaterOutputStream.finish();
			return byteArrayOutputStream.toByteArray();
		} catch (IOException e) {
			throw new UncheckedJoseException("Problem compressing data.", e);
		}
	}

	public byte[] decompress(byte[] compressedData) throws JoseException {
		Inflater inflater = new Inflater(true);
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

		try {
			InflaterInputStream iis = new InflaterInputStream(new ByteArrayInputStream(compressedData), inflater);
			int bytesRead;
			byte[] buff = new byte[256];
			while ((bytesRead = iis.read(buff)) != -1) {
				byteArrayOutputStream.write(buff, 0, bytesRead);
			}

			return byteArrayOutputStream.toByteArray();
		} catch (IOException e) {
			throw new JoseException("Problem decompressing data.", e);
		}
	}

	@Override
	public String getJavaAlgorithm() {
		return null;
	}

	@Override
	public String getAlgorithmIdentifier() {
		return CompressionAlgorithmIdentifiers.DEFLATE;
	}

	@Override
	public KeyPersuasion getKeyPersuasion() {
		return KeyPersuasion.NONE;
	}

	@Override
	public String getKeyType() {
		return null;
	}

	@Override
	public boolean isAvailable() {
		return true;
	}
}
