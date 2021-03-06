package com.bclymer.bcliteorm;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.bclymer.bcliteorm.annotations.PersistantField;
import com.bclymer.bcliteorm.annotations.PersistantObject;

class BcCache {

	private static Map<Class<?>, CachedClass> mCache = new HashMap<Class<?>, CachedClass>();
	
	public static CachedClass cacheClass(Class<?> cls) {
		CachedClass cachedClass = mCache.get(cls);
		if (cachedClass != null) {
			return cachedClass;
		}
		cachedClass = new CachedClass();
		cachedClass.cls = cls;
		if (!cls.isAnnotationPresent(PersistantObject.class)) {
			throw new IllegalArgumentException("Class " + cls.getSimpleName() + " is not persistant");
		}
		cachedClass.persistantObject = cls.getAnnotation(PersistantObject.class);
		
		Field[] fields = cls.getDeclaredFields();
		boolean hasIdField = false;
		boolean hasPersistantField = false;
		cachedClass.fieldAnnotations = new HashMap<Field, PersistantField>();
		for (Field field : fields) {
			if (field.isAnnotationPresent(PersistantField.class)) {
				hasPersistantField = true;
				PersistantField persistantField = field.getAnnotation(PersistantField.class);
				cachedClass.fieldAnnotations.put(field, persistantField);
				if (persistantField.id()) {
					if (hasIdField) {
						throw new IllegalArgumentException("Class " + cls.getSimpleName() + " has more than 1 id fields");
					}
					hasIdField = true;
					cachedClass.idColName = cachedClass.columnName(field);
				} else if (persistantField.generatedId()) {
					if (hasIdField) {
						throw new IllegalArgumentException("Class " + cls.getSimpleName() + " has more than 1 id fields");
					}
					hasIdField = true;
					cachedClass.idColName = cachedClass.columnName(field);
				}
			}
		}
		cachedClass.columns = new String[cachedClass.fieldAnnotations.size()];
		int i = 0;
		for (Entry<Field,PersistantField> entry : cachedClass.fieldAnnotations.entrySet()) {
			cachedClass.columns[i++] = BcOrm.tickAndCombine(cachedClass.columnName(entry.getKey()));
			if (entry.getValue().id()) {
				cachedClass.idEntry = entry;
			}
		}
		if (!hasIdField) {
			throw new IllegalArgumentException("Class " + cls.getSimpleName() + " didn't have an id field");
		}
		if (!hasPersistantField) {
			throw new IllegalArgumentException("Class " + cls.getSimpleName() + " has no persistant field");
		}
		return cachedClass;
	}
	
	static class CachedClass {
		Map<Field, PersistantField> fieldAnnotations;
		Entry<Field, PersistantField> idEntry;
		PersistantObject persistantObject;
		Class<?> cls;
		String idColName;
		String[] columns;
		
		String columnName(Field field) {
			PersistantField persistantField = fieldAnnotations.get(field);
			String columnName = persistantField.columnName().equals("") ? field.getName() : persistantField.columnName();
			return BcOrm.tickAndCombine(columnName);
		}
		
		String tableName() {
			String tableName = persistantObject.tableName().equals("") ? cls.getSimpleName() : persistantObject.tableName();
			return BcOrm.tickAndCombine(tableName);
		}
	}
	
}
