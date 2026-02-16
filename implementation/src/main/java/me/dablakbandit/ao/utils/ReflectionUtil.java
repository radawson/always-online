package me.dablakbandit.ao.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

/**
 * Minimal reflection helpers for NMS field injection. Used only where direct access is not possible (private/final fields).
 */
public final class ReflectionUtil {

	public static Field setAccessible(Field field) throws ReflectiveOperationException {
		field.setAccessible(true);
		if (Modifier.isFinal(field.getModifiers())) {
			removeFinal(field);
		}
		return field;
	}

	private static void removeFinal(Field field) throws ReflectiveOperationException {
		int mods = field.getModifiers();
		if (!Modifier.isFinal(mods)) return;
		try {
			Field modifiersField = Field.class.getDeclaredField("modifiers");
			modifiersField.setAccessible(true);
			modifiersField.setInt(field, mods & ~Modifier.FINAL);
		} catch (Exception e1) {
			try {
				removeFinalVarHandle(field, mods);
			} catch (Exception e2) {
				removeFinalViaGetDeclaredFields0(field, mods);
			}
		}
	}

	private static void removeFinalVarHandle(Field field, int mods) throws Exception {
		Class<?> methodHandles = Class.forName("java.lang.invoke.MethodHandles");
		Object lookup = methodHandles.getMethod("lookup").invoke(null);
		Object privateLookupIn = methodHandles.getMethod("privateLookupIn", Class.class, lookup.getClass()).invoke(null, Field.class, lookup);
		Object varHandle = privateLookupIn.getClass().getMethod("findVarHandle", Class.class, String.class, Class.class).invoke(privateLookupIn, Field.class, "modifiers", int.class);
		int newMods = mods & ~Modifier.FINAL;
		varHandle.getClass().getMethod("set", Object[].class).invoke(varHandle, (Object) new Object[]{field, newMods});
	}

	private static void removeFinalViaGetDeclaredFields0(Field field, int mods) throws ReflectiveOperationException {
		Method getDeclaredFields0 = Class.class.getDeclaredMethod("getDeclaredFields0", boolean.class);
		getDeclaredFields0.setAccessible(true);
		Field[] fields = (Field[]) getDeclaredFields0.invoke(Field.class, false);
		for (Field f : fields) {
			if ("modifiers".equals(f.getName())) {
				f.setAccessible(true);
				f.set(field, mods & ~Modifier.FINAL);
				return;
			}
		}
		throw new ReflectiveOperationException("Could not remove final from field");
	}

	public static Field getFirstFieldOfType(Class<?> clazz, Class<?> type) throws ReflectiveOperationException {
		for (Field field : clazz.getDeclaredFields()) {
			if (type.equals(field.getType())) {
				return setAccessible(field);
			}
		}
		throw new NoSuchFieldException("No field of type " + type.getName() + " in " + clazz.getName());
	}

	public static Field getFirstFieldOfTypeSilent(Class<?> clazz, Class<?> type) {
		try {
			return getFirstFieldOfType(clazz, type);
		} catch (Exception e) {
			return null;
		}
	}

	public static Method getMethod(Class<?> clazz, String name, Class<?>... paramTypes) {
		for (Method m : clazz.getDeclaredMethods()) {
			if (m.getName().equals(name) && Arrays.equals(m.getParameterTypes(), paramTypes)) {
				m.setAccessible(true);
				return m;
			}
		}
		for (Method m : clazz.getMethods()) {
			if (m.getName().equals(name) && Arrays.equals(m.getParameterTypes(), paramTypes)) {
				m.setAccessible(true);
				return m;
			}
		}
		return null;
	}
}
