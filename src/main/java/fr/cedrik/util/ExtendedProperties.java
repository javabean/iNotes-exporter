/*
 * Licensed to Digitas France under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
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
package fr.cedrik.util;

import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Loads a properties file from filesystem or classpath.<br>
 * System properties override properties values.<br>
 * Convenience type conversion methods.
 *
 * @author C&eacute;drik LIME
 * @see Properties
 * @see "http://commons.apache.org/configuration/"
 * @see PropertyEditor
 */
@SuppressWarnings("serial")
public class ExtendedProperties extends Properties {

	/**
	 * Creates an empty property list with no default values.
	 */
	public ExtendedProperties() {
		super();
	}

	/**
	 * Creates an empty property list with the specified defaults.
	 *
	 * @param   defaults   the defaults.
	 */
	public ExtendedProperties(Properties defaults) {
		super(defaults);
	}

	/**
	 * Loads the given file from the filesystem or the classpath.
	 *
	 * @see Properties#load(InputStream)
	 */
	public void load(String file) throws IOException {
		InputStream in = null;
		// load from filesystem
		try {
			in = new FileInputStream(new File(file));
		} catch (FileNotFoundException ignore) {
		}
		if (in != null) {
			try {
				load(in);
//			} catch (IOException e) {
//				throw new IllegalStateException("Error while loading file from filesystem: " + file, e);
			} finally {
				IOUtils.closeQuietly(in);
			}
		} else {
			if (Thread.currentThread().getContextClassLoader() != null) {
				in = Thread.currentThread().getContextClassLoader().getResourceAsStream(file);
			}
			if (in == null) {
				in = this.getClass().getResourceAsStream(file);
			}
			if (in != null) {
				// load from classpath
				try {
					load(in);
//				} catch (IOException e) {
//					throw new IllegalStateException("Error while loadind file from classpath: " + file, e);
				} finally {
					IOUtils.closeQuietly(in);
				}
			} else {
				// error
				throw new FileNotFoundException("Can not find file neither in filesystem nor classpath: " + file);
			}
		}
	}

	/************************************************************************/

	/**
	 * {@inheritDoc}
	 * <p>
	 * Looks up in System properties before looking up in this property list.
	 */
	@Override
	public String getProperty(String key) {
		String value = System.getProperty(key);
		if (value == null) {
			value = super.getProperty(key);
		}
		return value;
	}

	public <T> T get(String key, Class<T> targetType) {
		String valueStr = getProperty(key);
		return convert(valueStr, targetType);
	}

	public <T> T get(String key, T defaultValueNotNull) {
		if (defaultValueNotNull == null) {
			throw new NullPointerException("defaultValue can not be null");
		}
		String val = getProperty(key);
		return (val == null) ? defaultValueNotNull : convert(val, (Class<T>)defaultValueNotNull.getClass());
	}

	/**
	 * convert via a {@link java.beans.PropertyEditor}
	 */
	// see also org.springframework.beans.propertyeditors
	// http://docs.spring.io/spring/docs/3.2.x/spring-framework-reference/html/validation.html
	// see also org.apache.commons.configuration.PropertyConverter
	protected <T> T convert(String valueStr, Class<T> targetType) {
		PropertyEditor propertyEditor = PropertyEditorManager.findEditor(targetType);
		if (propertyEditor == null) {
			throw new IllegalArgumentException("Can not find a PropertyEditor for class " + targetType);
		}
		propertyEditor.setAsText(valueStr);
		T value = (T) propertyEditor.getValue();
		return value;
	}

	public String getString(String key) {
		return getProperty(key);
	}
	public String getString(String key, String defaultValue) {
		return getProperty(key, defaultValue);
	}

	public boolean getBoolean(String key) {
		return Boolean.parseBoolean(getProperty(key));
	}
	public boolean getBoolean(String key, boolean defaultValue) {
		String val = getProperty(key);
		return (val == null) ? defaultValue : Boolean.parseBoolean(val);
	}

	public byte getByte(String key) {
		return Byte.parseByte(getProperty(key));
	}
	public byte getByte(String key, byte defaultValue) {
		String val = getProperty(key);
		return (val == null) ? defaultValue : Byte.parseByte(val);
	}

	public short getShort(String key) {
		return Short.parseShort(getProperty(key));
	}
	public short getShort(String key, short defaultValue) {
		String val = getProperty(key);
		return (val == null) ? defaultValue : Short.parseShort(val);
	}

	public int getInt(String key) {
		return Integer.parseInt(getProperty(key));
	}
	public int getInt(String key, int defaultValue) {
		String val = getProperty(key);
		return (val == null) ? defaultValue : Integer.parseInt(val);
	}

	public long getLong(String key) {
		return Long.parseLong(getProperty(key));
	}
	public long getLong(String key, long defaultValue) {
		String val = getProperty(key);
		return (val == null) ? defaultValue : Long.parseLong(val);
	}

	public float getFloat(String key) {
		return Float.parseFloat(getProperty(key));
	}
	public float getFloat(String key, float defaultValue) {
		String val = getProperty(key);
		return (val == null) ? defaultValue : Float.parseFloat(val);
	}

	public double getDouble(String key) {
		return Double.parseDouble(getProperty(key));
	}
	public double getDouble(String key, double defaultValue) {
		String val = getProperty(key);
		return (val == null) ? defaultValue : Double.parseDouble(val);
	}

	public String[] getStringArray(String key) {
		return getStringArray(key, ',');
	}
	public String[] getStringArray(String key, char separatorChar) {
		String val = getProperty(key);
		return StringUtils.split(val, separatorChar);
	}

	//TODO need to pass a convertor to convert from List<String> to List<T>
//	public List<String> getList(String key) {
//		return getList(key, ',');
//	}
//	public List<String> getStringList(String key, char separatorChar) {
//		return Arrays.asList(getStringArray(key, separatorChar));
//	}

	/**
	 * Performance note: this is a convenience method, not intended for intensive usage.
	 * Please cache your own {@link MessageFormat} instance for performance.
	 */
	public String getMessage(String key, Object...arguments) {
		return MessageFormat.format(getString(key), arguments);
	}
}
