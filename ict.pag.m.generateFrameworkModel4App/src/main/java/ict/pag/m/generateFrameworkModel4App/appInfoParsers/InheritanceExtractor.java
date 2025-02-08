package ict.pag.m.generateFrameworkModel4App.appInfoParsers;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import com.ibm.wala.classLoader.BytecodeClass;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.ShrikeClass;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;

import ict.pag.m.frameworkInfoUtil.customize.Util;

public class InheritanceExtractor {

	/** Answers: marks2Classes */
	HashMap<String, HashSet<IClass>> mark2Class = new HashMap<>();

	public void extract(ClassHierarchy cha) {
		cha.getLoader(ClassLoaderReference.Application).iterateAllClasses().forEachRemaining(clazz -> {

			// Interfaces
			if (clazz instanceof BytecodeClass) {
				Collection<String> allInterf = ((BytecodeClass) clazz).getAllInterfacesSigs();
				Collection<IClass> alls = clazz.getAllImplementedInterfaces();

				/** "Lcom/example/controller/LoginControllerInterface" */
				if (allInterf != null && !allInterf.isEmpty()) {

					if (alls == null || alls.isEmpty()) {
						for (String i : allInterf) {
							HashSet<IClass> tmp = new HashSet<IClass>();
							tmp.add(clazz);
							cha.computeSubClasses(clazz.getReference()).forEach(sub -> {
								tmp.add(sub);
							});
							cha.getImplementors(clazz.getReference()).forEach(im -> {
								tmp.add(im);
							});

							String interf = Util.format(i);
							if (mark2Class.containsKey(interf)) {
								mark2Class.get(interf).addAll(tmp);
							} else {
								mark2Class.put(interf, tmp);
							}
						}
					} else {
						// implements framework interfaces and application interfaces
						Collection<String> appsInterf = new HashSet<>();
						for (IClass c1 : alls) {
							appsInterf.add(c1.getName().toString());
						}
						for (String i : allInterf) {
							boolean has = false;
							for (String s : appsInterf) {
								if (s.equals(i)) {
									has = true;
									break;
								}
							}
							if (!has) {
								HashSet<IClass> tmp = new HashSet<IClass>();
								tmp.add(clazz);
								cha.computeSubClasses(clazz.getReference()).forEach(sub -> {
									tmp.add(sub);
								});
								cha.getImplementors(clazz.getReference()).forEach(im -> {
									tmp.add(im);
								});

								String interf = Util.format(i);
								if (mark2Class.containsKey(interf)) {
									mark2Class.get(interf).addAll(tmp);
								} else {
									mark2Class.put(interf, tmp);
								}
							}
						}
					}

				}

			}
			// superClass
			if (clazz.getSuperclass() != null) {
				if (clazz instanceof ShrikeClass) {
					IClass superc = clazz.getSuperclass();
					String superClazz = ((ShrikeClass) clazz).getSuperName().toString();

					if (superClazz != null && !superClazz.toString().equals("Ljava/lang/Object")) {
						if (superc != null
								&& superc.getClassLoader().getReference().equals(ClassLoaderReference.Application)) {
							// do nothing
						} else {
							HashSet<IClass> tmp = new HashSet<IClass>();
							tmp.add(clazz);
							cha.computeSubClasses(clazz.getReference()).forEach(sub -> {
								tmp.add(sub);
							});
							cha.getImplementors(clazz.getReference()).forEach(im -> {
								tmp.add(im);
							});

							String sp = Util.format(superClazz);
							if (mark2Class.containsKey(sp)) {
								mark2Class.get(sp).addAll(tmp);
							} else {
								mark2Class.put(sp, tmp);
							}
						}

					}

				}

			}
		});
	}

	public HashMap<String, HashSet<IClass>> getMark2Class() {
		return mark2Class;
	}

}
