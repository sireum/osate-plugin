package org.sireum.aadl.osate.util;

import org.osate.aadl2.instance.SystemInstance;
import org.sireum.IS;
import org.sireum.U8;
import org.sireum.Z;
import org.sireum.aadl.ir.Aadl;
import org.sireum.aadl.ir.JSON;
import org.sireum.aadl.ir.MsgPack;
import org.sireum.aadl.osate.PreferenceValues;
import org.sireum.aadl.osate.architecture.Visitor$;

public class Util {

	public enum Tool {
		ARSIT("org.sireum.aadl.arsit.Runner"), //
		ACT("org.sireum.aadl.act.Act"), //
		AWAS("org.sireum.awas.AADLBridge.AadlHandler");

		String className;
		Tool(String name) {
			className = name;
		}

		public Boolean exists() {
			return classExists(className);
		}

		public String className() {
			return className;
		}
	}

	public static String serialize(Aadl model, PreferenceValues.SerializerType t) {
		switch (t) {
		case JSON:
			return JSON.fromAadl(model, false);
		case JSON_COMPACT:
			return JSON.fromAadl(model, true);
		case MSG_PACK:
			IS<Z, U8> x = MsgPack.fromAadl(model, true);
			String ret = org.sireum.conversions.String.toBase64(x).toString();
			return ret;
		default:
			return null;
		}
	}

	public static String getAir(SystemInstance si) {
		Aadl ir = Visitor$.MODULE$.apply(si).get();
		return serialize(ir, PreferenceValues.SerializerType.JSON_COMPACT);
	}

	public static boolean classExists(String className) {
		try {
			Class.forName(className);
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}
}
