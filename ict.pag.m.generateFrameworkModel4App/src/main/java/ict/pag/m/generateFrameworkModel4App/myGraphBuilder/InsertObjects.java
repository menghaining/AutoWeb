package ict.pag.m.generateFrameworkModel4App.myGraphBuilder;

import java.util.HashSet;
import java.util.Set;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.ShrikeBTMethod;
import com.ibm.wala.classLoader.ShrikeCTMethod;
import com.ibm.wala.classLoader.ShrikeClass;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrikeBT.ConstantInstruction;
import com.ibm.wala.shrikeBT.Constants;
import com.ibm.wala.shrikeBT.DupInstruction;
import com.ibm.wala.shrikeBT.LoadInstruction;
import com.ibm.wala.shrikeBT.MethodData;
import com.ibm.wala.shrikeBT.NewInstruction;
import com.ibm.wala.shrikeBT.StoreInstruction;
import com.ibm.wala.shrikeBT.info.LocalAllocator;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeName;

import ict.pag.m.generateFrameworkModel4App.appInfoParsers.entity.FieldPoints2Entity;
import ict.pag.m.generateFrameworkModel4App.util.MethodEditorForBean;
import ict.pag.m.generateFrameworkModel4App.util.ShrikeInstructionUtil;
import ict.pag.m.generateFrameworkModel4App.util.SpecialHelper;
import ict.pag.m.generateFrameworkModel4App.util.printUtil;

public class InsertObjects {

	public static int fieldInertNumber = 0;

	/** highlight: need to change the node on this given iclh !!! */
	public static void insert(IClassHierarchy iclh, HashSet<FieldPoints2Entity> fieldPoints2targetSet) {
		for (FieldPoints2Entity field2target : fieldPoints2targetSet) {
			boolean insertSuccess = false;

			boolean declare = field2target.isDeclare();
			IField field0 = field2target.getField();
			IClass target0 = field2target.getTarget();

//			if (field0.getReference().toString().contains("DiagnosisServiceImpl"))
//				System.out.println();

			if (field0.isStatic())
				System.out.println();
			// the class need to insert field object
			IClass class1 = iclh.getLoader(ClassLoaderReference.Application)
					.lookupClass(field0.getDeclaringClass().getName());
			// the field target is class2 or target0 depends on the value of 'declare'
			TypeName T = field0.getFieldTypeReference().getName();
			IClass class2 = iclh.getLoader(ClassLoaderReference.Application).lookupClass(T);
			if (class2 == null)
				class2 = iclh.getLoader(ClassLoaderReference.Primordial).lookupClass(T);
//			if (class2 == null && !declare)
//				continue;

//			if (!(class1 instanceof ShrikeClass) || !(class2 instanceof ShrikeClass))
			if (!(class1 instanceof ShrikeClass))
				continue;

			IField field = calculateField(field0, class1);
			if (field == null)
				continue;

			ShrikeClass shirkeclass1 = (ShrikeClass) class1;
			Set<IMethod> initMethods = shirkeclass1.getInitMethod();
			for (IMethod method : initMethods) {
				if (method instanceof ShrikeCTMethod) {

					/* calculate the target */
					if (declare) {
						// 1. is field declared type
						// Generate the object that is field declared
						if (class2 == null || !(class2 instanceof ShrikeClass))
							continue;

						if (class2.isInterface()) {
							Set<IClass> implers = iclh.getImplementors(class2.getReference());
							for (IClass c1 : implers) {
								if (c1 != null && c1 instanceof ShrikeClass) {
									insertSuccess = addFieldIntoConstructor(c1, (ShrikeBTMethod) method, field, iclh);
								}
							}
						} else {
							insertSuccess = addFieldIntoConstructor(class2, (ShrikeBTMethod) method, field, iclh);
						}

					} else {
						if (target0 != null) {
							// 2. is specified type
							// Generate the object that is target class
							IClass target = iclh.getLoader(ClassLoaderReference.Application)
									.lookupClass(target0.getName());

							insertSuccess = addFieldIntoConstructor(target, (ShrikeBTMethod) method, field, iclh);

						} else {
							if (class2 == null || !(class2 instanceof ShrikeClass))
								continue;
							if (!class2.isInterface() && !class2.isAbstract()) {
								// 3. declared is not abstract
								insertSuccess = addFieldIntoConstructor(class2, (ShrikeBTMethod) method, field, iclh);
							} else {
								// If declare class is not abstract, generate declare;
								// else, generate all sub-type of this class
								if (class2.isAbstract()) {
									// 4. delcare is abstract
									// add all sub classes
									for (IClass subc : iclh.computeSubClasses(class2.getReference())) {
										if (!subc.isAbstract()) {
											insertSuccess = addFieldIntoConstructor(subc, (ShrikeBTMethod) method,
													field, iclh);
										}
									}
								} else {
									Set<IClass> implers = iclh.getImplementors(class2.getReference());
									for (IClass c1 : implers) {
										if (c1 != null && c1 instanceof ShrikeClass) {
											insertSuccess = addFieldIntoConstructor(c1, (ShrikeBTMethod) method, field,
													iclh);
										}
									}
								}

							}

						}
					}
				}
			}
			if (printUtil.printSpecific) {
				if (!insertSuccess) {
					System.err.println("[info][insert fail!] " + field0.getReference());
				}
			}
		}
		if (printUtil.printSpecific) {
			System.out.println("[info][to be inserted total]" + fieldPoints2targetSet.size());
		}
		System.out.println("[RESULT][actually insert fields total] : " + fieldInertNumber);

	}

	private static boolean addFieldIntoConstructor(IClass c1, ShrikeBTMethod method, IField field,
			IClassHierarchy iclh) {
		boolean insertSuccess = false;
		if (field.isStatic()) {
			for (IMethod target : SpecialHelper.findAllClinitMethods(c1)) {
				try {
					insertFieldObjectIntoIntoConstructor((ShrikeBTMethod) method, field, target, iclh);
					insertSuccess = true;
				} catch (InvalidClassFileException e) {
					e.printStackTrace();
				}
			}
		} else {
			for (IMethod target : SpecialHelper.findAllInitMethods(c1)) {
				try {
					insertFieldObjectIntoIntoConstructor((ShrikeBTMethod) method, field, target, iclh);
					insertSuccess = true;
				} catch (InvalidClassFileException e) {
					e.printStackTrace();
				}
			}

		}

		return insertSuccess;

	}

	private static IField calculateField(IField field0, IClass class1) {
		for (IField f1 : class1.getDeclaredInstanceFields()) {
			if (f1.getFieldTypeReference().toString().equals(field0.getFieldTypeReference().toString()))
				return f1;
		}
		return null;
	}

	private static void insertFieldObjectIntoIntoConstructor(ShrikeBTMethod constructor, IField field,
			IMethod targetInitmethod, IClassHierarchy iclh) throws InvalidClassFileException {
		IClass constructorClass = field.getDeclaringClass();
		IClass targetClass = targetInitmethod.getDeclaringClass();

		MethodEditorForBean me = new MethodEditorForBean(constructor);
		me.beginPass();
		String seleString = constructor.getSelector().toString();
		String sub = seleString.substring(seleString.indexOf("("));
//		if(constructor.getSignature().contains("ProgramWorkflowServiceImpl"))
//			System.out.println();

		MethodData mData = new MethodData(Constants.ACC_PUBLIC, constructorClass.getName().toString(),
				constructor.getReference().getName().toString(), sub, constructor.getInstructions(),
				constructor.getHandlers(), constructor.getInstructionsToBytecodes());

		try {
			int position = constructor.getInstructions().length - 2;
			me.insertAfter(position, new ict.pag.m.generateFrameworkModel4App.util.MethodEditorForBean.Patch() {
				@Override
				public void emitTo(ict.pag.m.generateFrameworkModel4App.util.MethodEditorForBean.Output w) {
					// w.emit(DupInstruction.make(0));
					int tmpInt = LocalAllocator.allocate(mData, "Ljava/lang/Object");
					w.emit(LoadInstruction.make(Constants.TYPE_Object, 0));
					w.emit(NewInstruction.make(targetClass.getName().toString(), 0));
					w.emit(DupInstruction.make(0));

					if (targetInitmethod.getNumberOfParameters() == 1) {
						w.emit(ShrikeInstructionUtil.makeInvokeDefalutInit(targetClass));
					} else {
						for (int i = 0; i < targetInitmethod.getNumberOfParameters() - 1; i++) {
							w.emit(ConstantInstruction.makeString(null));
						}
						w.emit(ShrikeInstructionUtil.makeInvokeSpecialInit(targetInitmethod, targetClass));
					}

					w.emit(StoreInstruction.make(Constants.TYPE_Object, tmpInt));

					w.emit(LoadInstruction.make(Constants.TYPE_Object, 0));
					w.emit(LoadInstruction.make(Constants.TYPE_Object, tmpInt));

					w.emit(ShrikeInstructionUtil.makePutNoStatic(constructorClass, field));

				}
			});
		} catch (InvalidClassFileException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

//		System.out.println(" insert method:" + m.getSignature() + "\n insert field " + fieldName + "\n field class "
//				+ beanClass.getReference().getName().toString() + "\n bean name " + bdName + "\n");
		me.applyPatches();
		fieldInertNumber++;
		me.endPass();

		if (printUtil.printSpecific)
			System.out.println("[generate]" + targetClass.getReference().getName().toString() + " \n\t-for- "
					+ field.getName() + "   -in- " + constructor.getSignature());

	}

//	private static IMethod getInitMethodOfDefault(IClass clazz) {
//		IMethod targetInitmethod = null;
//		ShrikeClass targetShirkeclass = (ShrikeClass) clazz;
//		Set<IMethod> targetInitMethods = targetShirkeclass.getInitMethod();
//		for (IMethod initmethod : targetInitMethods) {
//			if (initmethod.toString().contains(" <init>()V")) {
//				targetInitmethod = initmethod;
//				break;
//			} else {
//				targetInitmethod = initmethod;
//			}
//		}
//		return targetInitmethod;
//	}
}
