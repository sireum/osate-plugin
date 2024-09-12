package org.sireum.aadl.osate.util;

public class SlangUtil {

	public static org.sireum.Z toZ(int i) {
		return org.sireum.Z$.MODULE$.apply(i);
	}

	public static org.sireum.U32 toU32(int i) {
		return org.sireum.U32$.MODULE$.apply(Integer.toString(i)).get();
	}

	public static <T> org.sireum.None<T> toNone() {
		return org.sireum.None$.MODULE$.apply();
	}

	public static <T> org.sireum.Some<T> toSome(T t) {
		return org.sireum.Some$.MODULE$.apply(t);
	}
	
	public static org.sireum.String sireumString(String s) {
		return new org.sireum.String(s);
	}
}
