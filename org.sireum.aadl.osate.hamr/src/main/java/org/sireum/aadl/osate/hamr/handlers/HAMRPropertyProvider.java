package org.sireum.aadl.osate.hamr.handlers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.osate.aadl2.EnumerationLiteral;
import org.osate.aadl2.NamedElement;
import org.osate.aadl2.NamedValue;
import org.osate.aadl2.Property;
import org.osate.aadl2.PropertyExpression;
import org.osate.aadl2.properties.PropertyLookupException;
import org.osate.xtext.aadl2.properties.util.GetProperties;
import org.osate.xtext.aadl2.properties.util.PropertyUtils;

public class HAMRPropertyProvider {

	final static String PROP_HAMR__PLATFORM = "HAMR::Platform";
	final static String PROP_HAMR__HW = "HAMR::HW";
	final static String PROP_HAMR__DEFAULT_BIT_WIDTH = "HAMR::Default_Bit_Width";
	final static String PROP_HAMR__DEFAULT_MAX_SEQUENCE_SIZE = "HAMR::Default_Max_Sequence_Size";
	final static String PROP_HAMR__MAX_STRING_SIZE = "HAMR::Max_String_Size";

	final static int DEFAULT_BIT_WIDTH = 64;
	final static int DEFAULT_MAX_SEQUENCE_SIZE = 16;
	final static int DEFAULT_MAX_STRING_SIZE = 256;

	// should match Platform in HAMR.aadl
	enum Platform {
		JVM, Linux, macOS, Cygwin, seL4
	}

	// should match HW in HAMR.aadl
	enum HW {
		ODROID_XU4, QEMU, x86, amd64
	}

	final static List<Integer> bitWidths = Arrays.asList(8, 16, 32, 64);

	public static List<HW> getHWsFromElement(NamedElement ne) {
		List<HW> r = new ArrayList<>();
		try {
			Property p = GetProperties.lookupPropertyDefinition(ne, PROP_HAMR__HW);

			List<? extends PropertyExpression> propertyValues = ne.getPropertyValueList(p);
			for (PropertyExpression pe : propertyValues) {
				String v = ((EnumerationLiteral) ((NamedValue) pe).getNamedValue()).getName();
				r.add(HW.valueOf(v));
			}
		} catch (PropertyLookupException e) {
		}
		return r;
	}

	public static List<Platform> getPlatformsFromElement(NamedElement ne) {
		List<Platform> r = new ArrayList<>();
		try {
			Property p = GetProperties.lookupPropertyDefinition(ne, PROP_HAMR__PLATFORM);

			List<? extends PropertyExpression> propertyValues = ne.getPropertyValueList(p);
			for (PropertyExpression pe : propertyValues) {
				String v = ((EnumerationLiteral) ((NamedValue) pe).getNamedValue()).getName();
				r.add(Platform.valueOf(v));
			}
		} catch (PropertyLookupException e) {
		}
		return r;
	}

	public static long getIntegerValue(NamedElement ne, String propertyName, long defaultValue) {
		try {
			Property p = GetProperties.lookupPropertyDefinition(ne, propertyName);
			return PropertyUtils.getIntegerValue(ne, p, defaultValue);
		} catch (PropertyLookupException e) {
			return defaultValue;
		}
	}

	public static int getDefaultBitWidthFromElement(NamedElement ne) {
		return new Long(HAMRPropertyProvider.getIntegerValue(ne, HAMRPropertyProvider.PROP_HAMR__DEFAULT_BIT_WIDTH,
				HAMRPropertyProvider.DEFAULT_BIT_WIDTH)).intValue();
	}

	public static int getDefaultMaxSequenceSizeFromElement(NamedElement ne) {
		return new Long(
				HAMRPropertyProvider.getIntegerValue(ne, HAMRPropertyProvider.PROP_HAMR__DEFAULT_MAX_SEQUENCE_SIZE,
						HAMRPropertyProvider.DEFAULT_MAX_SEQUENCE_SIZE)).intValue();
	}

	public static int getDefaultMaxStringSizeFromElement(NamedElement ne) {
		return new Long(HAMRPropertyProvider.getIntegerValue(ne, HAMRPropertyProvider.PROP_HAMR__MAX_STRING_SIZE,
				HAMRPropertyProvider.DEFAULT_MAX_STRING_SIZE)).intValue();
	}
}
