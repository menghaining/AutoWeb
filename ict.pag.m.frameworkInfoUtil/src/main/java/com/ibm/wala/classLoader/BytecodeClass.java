/*
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.classLoader;

import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyWarning;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.types.annotations.Annotation;
import com.ibm.wala.types.annotations.TypeAnnotation;
import com.ibm.wala.types.generics.TypeSignature;
import com.ibm.wala.util.collections.BimodalMap;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.strings.Atom;
import com.ibm.wala.util.strings.ImmutableByteArray;
import com.ibm.wala.util.warnings.Warning;
import com.ibm.wala.util.warnings.Warnings;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A class representing which originates in some form of bytecode.
 *
 * @param <T> type of classloader which loads this format of class.
 */
public abstract class BytecodeClass<T extends IClassLoader> implements IClass {

	protected BytecodeClass(T loader, IClassHierarchy cha) {
		this.loader = loader;
		this.cha = cha;
	}

	/**
	 * An Atom which holds the name of the super class. We cache this for efficiency
	 * reasons.
	 */
	protected ImmutableByteArray superName;

	/**
	 * The names of interfaces for this class. We cache this for efficiency reasons.
	 */
	protected ImmutableByteArray[] interfaceNames;

	/** The object that loaded this class. */
	protected final T loader;

	/** Governing class hierarchy for this class */
	protected final IClassHierarchy cha;

	/**
	 * A mapping from Selector to IMethod
	 *
	 * <p>
	 * TODO: get rid of this for classes (though keep it for interfaces) instead ...
	 * use a VMT.
	 */
	protected volatile Map<Selector, IMethod> methodMap;

	protected Set<IMethod> initMethods = new HashSet<>();

	/**
	 * A mapping from Selector to IMethod used to cache method lookups from
	 * superclasses
	 */
	protected Map<Selector, IMethod> inheritCache;

	/** Canonical type representation */
	protected TypeReference typeReference;

	/** superclass */
	protected IClass superClass;

	/** Compute the superclass lazily. */
	protected boolean superclassComputed = false;

	/**
	 * The IClasses that represent all interfaces this class implements (if it's a
	 * class) or extends (it it's an interface)
	 */
	protected Collection<IClass> allInterfaces = null;

	/** The instance fields declared in this class. */
	protected IField[] instanceFields;

	/** The static fields declared in this class. */
	protected IField[] staticFields;

	/** hash code; cached here for efficiency */
	protected int hashCode;

	private final HashMap<Atom, IField> fieldMap = HashMapFactory.make(5);

	/** A warning for when we get a class not found exception */
	private static class ClassNotFoundWarning extends Warning {

		final ImmutableByteArray className;

		ClassNotFoundWarning(ImmutableByteArray className) {
			super(Warning.SEVERE);
			this.className = className;
		}

		@Override
		public String getMsg() {
			return getClass().toString() + " : " + className;
		}

		public static ClassNotFoundWarning create(ImmutableByteArray className) {
			return new ClassNotFoundWarning(className);
		}
	}

	public abstract Module getContainer();

	@Override
	public IClassLoader getClassLoader() {
		return loader;
	}

	protected abstract IMethod[] computeDeclaredMethods() throws InvalidClassFileException;

	@Override
	public TypeReference getReference() {
		return typeReference;
	}

	@Override
	public String getSourceFileName() {
		return loader.getSourceFileName(this);
	}

	@Override
	public Reader getSource() {
		return loader.getSource(this);
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public String toString() {
		return getReference().toString();
	}

	@Override
	public boolean isArrayClass() {
		return false;
	}

	@Override
	public IClassHierarchy getClassHierarchy() {
		return cha;
	}

	@Override
	public TypeName getName() {
		return getReference().getName();
	}

	@Override
	public boolean isReferenceType() {
		return getReference().isReferenceType();
	}

	@Override
	public IField getField(Atom name) {
		if (fieldMap.containsKey(name)) {
			return fieldMap.get(name);
		} else {
			List<IField> fields = findDeclaredField(name);
			if (!fields.isEmpty()) {
				if (fields.size() == 1) {
					IField f = fields.iterator().next();
					fieldMap.put(name, f);
					return f;
				} else {
					throw new IllegalStateException("multiple fields with name " + name);
				}
			} else if ((superClass = getSuperclass()) != null) {
				IField f = superClass.getField(name);
				if (f != null) {
					fieldMap.put(name, f);
					return f;
				}
			}
			// try superinterfaces
			for (IClass i : getAllImplementedInterfaces()) {
				IField f = i.getField(name);
				if (f != null) {
					fieldMap.put(name, f);
					return f;
				}
			}
		}

		return null;
	}

	@Override
	public IField getField(Atom name, TypeName type) {
		boolean unresolved = false;
		try {
			// typically, there will be at most one field with the name
			IField field = getField(name);
			if (field != null && field.getFieldTypeReference().getName().equals(type)) {
				return field;
			} else {
				unresolved = true;
			}
		} catch (IllegalStateException e) {
			assert e.getMessage().startsWith("multiple fields with");
			unresolved = true;
		}

		if (unresolved) {
			// multiple fields. look through all of them and see if any have the appropriate
			// type
			List<IField> fields = findDeclaredField(name);
			for (IField f : fields) {
				if (f.getFieldTypeReference().getName().equals(type)) {
					return f;
				}
			}
			// check superclass
			if (getSuperclass() != null) {
				IField f = superClass.getField(name, type);
				if (f != null) {
					return f;
				}
			}
			// try superinterfaces
			for (IClass i : getAllImplementedInterfaces()) {
				IField f = i.getField(name, type);
				if (f != null) {
					return f;
				}
			}
		}
		return null;
	}

	private void computeSuperclass() {
		superclassComputed = true;

		if (superName == null) {
			if (!getReference().equals(loader.getLanguage().getRootType())) {
				superClass = loader.lookupClass(loader.getLanguage().getRootType().getName());
			}
			return;
		}

		superClass = loader.lookupClass(TypeName.findOrCreate(superName));
	}

	public void setSuperClass(IClass clazz) {
		if (superClass == null)
			superClass = clazz;
	}

	@Override
	public IClass getSuperclass() {
		if (!superclassComputed) {
			computeSuperclass();
		}
		if (superClass == null && !getReference().equals(TypeReference.JavaLangObject)) {
			// TODO MissingSuperClassHandling.Phantom needs to be implemented
			if (cha instanceof ClassHierarchy && ((ClassHierarchy) cha).getSuperClassHandling()
					.equals(ClassHierarchy.MissingSuperClassHandling.ROOT)) {
				superClass = loader.lookupClass(loader.getLanguage().getRootType().getName());
			} else {
				// modify by mhn
//    	  throw new NoSuperclassFoundException(
//    	            "No superclass found for " + this + " Superclass name " + superName);
				superClass = loader.lookupClass(TypeReference.JavaLangObject.getName());
			}

		}
		return superClass;
	}

	public TypeName getSuperName() {
		return TypeName.findOrCreate(superName);
	}

	/*
	 * @see com.ibm.wala.classLoader.IClass#getAllFields()
	 */
	@Override
	public Collection<IField> getAllFields() {
		Collection<IField> result = new LinkedList<>();
		result.addAll(getAllInstanceFields());
		result.addAll(getAllStaticFields());
		return result;
	}

	/*
	 * @see com.ibm.wala.classLoader.IClass#getAllImplementedInterfaces()
	 */
	@Override
	public Collection<IClass> getAllImplementedInterfaces() {
		if (allInterfaces == null) {
			Collection<IClass> C = computeAllInterfacesAsCollection();
			allInterfaces = Collections.unmodifiableCollection(C);
		}
		return allInterfaces;
	}

	/*
	 * @see com.ibm.wala.classLoader.IClass#getDeclaredFields()
	 */
	@Override
	public Collection<IField> getDeclaredInstanceFields() {
		if (instanceFields == null) {
			return Collections.emptySet();
		} else {
			return Collections.unmodifiableList(Arrays.asList(instanceFields));
		}
	}

	/*
	 * @see com.ibm.wala.classLoader.IClass#getDeclaredFields()
	 */
	@Override
	public Collection<IField> getDeclaredStaticFields() {
		return Collections.unmodifiableList(Arrays.asList(staticFields));
	}

	/*
	 * @see com.ibm.wala.classLoader.IClass#getDirectInterfaces()
	 */
	@Override
	public Collection<? extends IClass> getDirectInterfaces() {
		return array2IClassSet(interfaceNames);
	}

	/**
	 * added by mhn </br>
	 * for find all infos about extends/implements when that are not loaded into
	 * classloader</br>
	 * @Returns Collection of (IClass) interfaces this class implements, including
	 * all ancestors of interfaces immediately implemented. If this class is an
	 * interface, it returns allsuper-interfaces.
	 */
	public Collection<String> getAllInterfacesSigs() {
		ArrayList<String> result = new ArrayList<>(interfaceNames.length);
		for (ImmutableByteArray name : interfaceNames) {
			IClass klass = null;
			klass = loader.lookupClass(TypeName.findOrCreate(name));
			if (klass == null) {
				// get the class that has not loaded but declared
				result.add(name.toString());
			} else {
				result.add(name.toString());
				for (IClass i : klass.getAllImplementedInterfaces()) {
					result.add(i.getName().toString());
				}
			}
		}
		return result;
	}

	/*
	 * @see com.ibm.wala.classLoader.IClass#getAllInstanceFields()
	 */
	@Override
	public Collection<IField> getAllInstanceFields() {
		Collection<IField> result = new LinkedList<>(getDeclaredInstanceFields());
		IClass s = getSuperclass();
		while (s != null) {
			result.addAll(s.getDeclaredInstanceFields());
			s = s.getSuperclass();
		}
		return result;
	}

	/*
	 * @see com.ibm.wala.classLoader.IClass#getAllStaticFields()
	 */
	@Override
	public Collection<IField> getAllStaticFields() {
		Collection<IField> result = new LinkedList<>(getDeclaredStaticFields());
		IClass s = getSuperclass();
		while (s != null) {
			result.addAll(s.getDeclaredStaticFields());
			s = s.getSuperclass();
		}
		return result;
	}

	/*
	 * @see com.ibm.wala.classLoader.IClass#getAllMethods()
	 */
	@Override
	public Collection<IMethod> getAllMethods() {
		Collection<IMethod> result = new LinkedList<>(getDeclaredMethods());
		if (isInterface()) {
			for (IClass i : getDirectInterfaces()) {
				result.addAll(i.getAllMethods());
			}
		} else {
			// for non-interfaces, add default methods inherited from interfaces #219.
			for (IClass i : this.getAllImplementedInterfaces())
				for (IMethod m : i.getDeclaredMethods())
					if (!m.isAbstract())
						result.add(m);
		}
		IClass s = getSuperclass();
		while (s != null) {
			result.addAll(s.getDeclaredMethods());
			s = s.getSuperclass();
		}
		return result;
	}

	/*
	 * @see com.ibm.wala.classLoader.IClass#getDeclaredMethods()
	 */
	@Override
	public Collection<IMethod> getDeclaredMethods() {
		try {
			computeMethodMapIfNeeded();
		} catch (InvalidClassFileException e) {
			e.printStackTrace();
			Assertions.UNREACHABLE();
		}
		return Collections.unmodifiableCollection(methodMap.values());
	}

	public Set<IMethod> getInitMethod() {
		try {
			computeMethodMapIfNeeded();
		} catch (InvalidClassFileException e) {
			e.printStackTrace();
			Assertions.UNREACHABLE();
		}
		return initMethods;
	}

	/*
	 * @see com.ibm.wala.classLoader.IClass#getMethod(com.ibm.wala.types.Selector)
	 */
	@Override
	public IMethod getMethod(Selector selector) {
		try {
			computeMethodMapIfNeeded();
		} catch (InvalidClassFileException e1) {
			e1.printStackTrace();
			Assertions.UNREACHABLE();
		}

		// my methods + cached parent stuff
		IMethod result = methodMap.get(selector);
		if (result != null) {
			return result;
		}
		if (inheritCache != null) {
			result = inheritCache.get(selector);
			if (result != null) {
				return result;
			}
		}

		// check parent, caching if found
		if (!selector.equals(MethodReference.clinitSelector) && !selector.getName().equals(MethodReference.initAtom)) {
			IClass superclass = getSuperclass();
			if (superclass != null) {
				IMethod inherit = superclass.getMethod(selector);
				if (inherit != null) {
					if (inheritCache == null) {
						inheritCache = new BimodalMap<>(5);
					}
					inheritCache.put(selector, inherit);
					return inherit;
				}
			}
		}

		// check interfaces for Java 8 default implementation
		// Java seems to require a single default implementation, so take that on faith
		// here
		for (IClass iface : getAllImplementedInterfaces()) {
			for (IMethod m : iface.getDeclaredMethods()) {
				if (!m.isAbstract() && m.getSelector().equals(selector)) {
					if (inheritCache == null) {
						inheritCache = new BimodalMap<>(5);
					}
					inheritCache.put(selector, m);

					return m;
				}
			}
		}

		// no method found
		if (inheritCache == null) {
			inheritCache = new BimodalMap<>(5);
		}
		inheritCache.put(selector, null);
		return null;
	}

	/**
	 * @return Collection of IClasses, representing the interfaces this class
	 *         implements.
	 */
	protected Collection<IClass> computeAllInterfacesAsCollection() {
		Collection<? extends IClass> c = getDirectInterfaces();
		Set<IClass> result = HashSetFactory.make();
		for (IClass klass : c) {
			if (klass.isInterface()) {
				result.add(klass);
			} else {
				Warnings.add(ClassHierarchyWarning.create("expected an interface " + klass));
			}
		}

		// at this point result holds all interfaces the class directly extends.
		// now expand to a fixed point.
		Set<IClass> last = null;
		do {
			last = HashSetFactory.make(result);
			for (IClass i : last) {
				result.addAll(i.getDirectInterfaces());
			}
		} while (last.size() < result.size());

		// now add any interfaces implemented by the super class
		IClass sup = null;
		sup = getSuperclass();
		if (sup != null) {
			result.addAll(sup.getAllImplementedInterfaces());
		}
		return result;
	}

	/**
	 * @param interfaces a set of class names
	 * @return Set of all IClasses that can be loaded corresponding to the class
	 *         names in the interfaces array; raise warnings if classes can not be
	 *         loaded
	 */
	private Collection<IClass> array2IClassSet(ImmutableByteArray[] interfaces) {
		ArrayList<IClass> result = new ArrayList<>(interfaces.length);
		for (ImmutableByteArray name : interfaces) {
			IClass klass = null;
			klass = loader.lookupClass(TypeName.findOrCreate(name));
			if (klass == null) {
				Warnings.add(ClassNotFoundWarning.create(name));
				// get the class that has not loaded but declared
//				result.add(klass);
			} else {
				result.add(klass);
			}
		}
		return result;
	}

	protected List<IField> findDeclaredField(Atom name) {

		List<IField> result = new ArrayList<>(1);

		if (instanceFields != null) {
			for (IField instanceField : instanceFields) {
				if (instanceField.getName() == name) {
					result.add(instanceField);
				}
			}
		}

		if (staticFields != null) {
			for (IField staticField : staticFields) {
				if (staticField.getName() == name) {
					result.add(staticField);
				}
			}
		}

		return result;
	}

	protected void addFieldToList(List<FieldImpl> L, Atom name, ImmutableByteArray fieldType, int accessFlags,
			Collection<Annotation> annotations, Collection<TypeAnnotation> typeAnnotations, TypeSignature sig) {
		TypeName T = null;
		if (fieldType.get(fieldType.length() - 1) == ';') {
			T = TypeName.findOrCreate(fieldType, 0, fieldType.length() - 1);
		} else {
			T = TypeName.findOrCreate(fieldType);
		}
		TypeReference type = TypeReference.findOrCreate(getClassLoader().getReference(), T);
		FieldReference fr = FieldReference.findOrCreate(getReference(), name, type);
		FieldImpl f = new FieldImpl(this, fr, accessFlags, annotations, typeAnnotations, sig);
		L.add(f);
	}

	/** set up the methodMap mapping */
	protected void computeMethodMapIfNeeded() throws InvalidClassFileException {
		if (methodMap == null) {
			synchronized (this) {
				if (methodMap == null) {
					IMethod[] methods = computeDeclaredMethods();

					final Map<Selector, IMethod> tmpMethodMap;
					// if (methods.length > 5) {
					tmpMethodMap = HashMapFactory.make(methods.length);
					// } else {
					// tmpMethodMap = new SmallMap<>();
					// }
					for (IMethod m : methods) {
						tmpMethodMap.put(m.getReference().getSelector(), m);
						if (m.getReference().getName().equals(MethodReference.initAtom))
							initMethods.add(m);
					}

					methodMap = tmpMethodMap;
				}
			}
		}
	}

	public void updateMethodMap(MethodReference oldMR, MethodReference newMR) {
		Selector oldSelector = oldMR.getSelector();
		assert methodMap != null;
		assert methodMap.containsKey(oldSelector);
		IMethod method = methodMap.get(oldSelector);
		methodMap.remove(oldSelector);
		methodMap.put(newMR.getSelector(), method);
	}

	public abstract Collection<Annotation> getAnnotations(boolean runtimeVisible) throws InvalidClassFileException;
}
