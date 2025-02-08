/*
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.classLoader;

import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;

import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.util.shrike.ShrikeClassReaderHandle;
import com.ibm.wala.util.strings.ImmutableByteArray;

public class ClassURLModule extends AbstractURLModule implements SourceModule {
	private final String className;

	public ClassURLModule(URL url) throws InvalidClassFileException {
		super(url);
		ShrikeClassReaderHandle reader = new ShrikeClassReaderHandle(this);
		ImmutableByteArray name = ImmutableByteArray.make(reader.get().getName());
		className = name.toString();
	}

	@Override
	public String getClassName() {
		return className;
	}

	@Override
	public boolean isClassFile() {
		return true;
	}

	@Override
	public boolean isSourceFile() {
		return false;
	}

	@Override
	public Reader getInputReader() {
		return new InputStreamReader(getInputStream());
	}
}
