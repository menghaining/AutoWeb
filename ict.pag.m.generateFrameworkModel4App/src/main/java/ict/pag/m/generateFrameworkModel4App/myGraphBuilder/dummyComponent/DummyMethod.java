package ict.pag.m.generateFrameworkModel4App.myGraphBuilder.dummyComponent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import com.ibm.wala.cfg.InducedCFG;
import com.ibm.wala.classLoader.ArrayClass;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.classLoader.SyntheticMethod;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.summaries.SyntheticIR;
import com.ibm.wala.shrikeBT.IConditionalBranchInstruction;
import com.ibm.wala.shrikeBT.IInvokeInstruction;
import com.ibm.wala.ssa.ConstantValue;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAArrayStoreInstruction;
import com.ibm.wala.ssa.SSAConditionalBranchInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SSAOptions;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashMapFactory;

/**
 * A synthetic method from the {@link DummyMainClass}
 */
public class DummyMethod extends SyntheticMethod {

	public DummyMethod(MethodReference mainMethod, final IClassHierarchy cha, IClass clazz, IAnalysisCacheView cache) {
		super(mainMethod, clazz, true, false);
		this.cha = cha;
		this.cache = cache;
		this.instFactory = clazz.getClassLoader().getInstructionFactory();
		if (cache == null) {
			throw new IllegalArgumentException("null cache");
		}
	}

	final protected ArrayList<SSAInstruction> instructions = new ArrayList<SSAInstruction>();

	private Map<ConstantValue, Integer> constant2ValueNumber = HashMapFactory.make();

	/**
	 * The number of the next local value number available for the fake root method.
	 * Note that we reserve value number 1 to represent the value "any exception
	 * caught by the root method"
	 */
	protected int nextLocal = 2;

	protected final IClassHierarchy cha;

	protected final IAnalysisCacheView cache;

	protected final SSAInstructionFactory instFactory;

	public SSAInstruction[] getInstructions() {
		return (SSAInstruction[]) instructions.toArray(new SSAInstruction[instructions.size()]);
	}

	@Override
	public SSAInstruction[] getStatements(SSAOptions options) {
		return getInstructions();
	}

	@Override
	public IR makeIR(Context context, SSAOptions options) {
		SSAInstruction instrs[] = getInstructions();
		Map<Integer, ConstantValue> constants = null;
		if (!constant2ValueNumber.isEmpty()) {
			constants = HashMapFactory.make(constant2ValueNumber.size());
			for (ConstantValue c : constant2ValueNumber.keySet()) {
				int vn = constant2ValueNumber.get(c);
				constants.put(vn, c);
			}
		}
		InducedCFG cfg = makeControlFlowGraph(instrs);
		return new SyntheticIR(this, Everywhere.EVERYWHERE, cfg, instrs, options, constants);
	}

	public int addLocal() {
		return nextLocal++;
	}

	/**
	 * @return the invoke instructions added by this operation
	 * @throws IllegalArgumentException if site is null
	 */
	public SSAAbstractInvokeInstruction addInvocation(int[] params, CallSiteReference site) {
		if (site == null) {
			throw new IllegalArgumentException("site is null");
		}
		CallSiteReference newSite = CallSiteReference.make(instructions.size(), site.getDeclaredTarget(),
				site.getInvocationCode());
		SSAAbstractInvokeInstruction s = null;
		if (newSite.getDeclaredTarget().getReturnType().equals(TypeReference.Void)) {
			s = instFactory.InvokeInstruction(instructions.size(), params, nextLocal++, newSite, null);
		} else {
			s = instFactory.InvokeInstruction(instructions.size(), nextLocal++, params, nextLocal++, newSite, null);
		}
		instructions.add(s);
		cache.invalidate(this, Everywhere.EVERYWHERE);
		return s;
	}

	/**
	 * Add a return statement
	 */
	public SSAReturnInstruction addReturn(int vn, boolean isPrimitive) {
		SSAReturnInstruction s = instFactory.ReturnInstruction(instructions.size(), vn, isPrimitive);
		instructions.add(s);
		cache.invalidate(this, Everywhere.EVERYWHERE);
		return s;
	}

	/**
	 * Add a New statement of the given type
	 * 
	 * Side effect: adds call to default constructor of given type if one exists.
	 * 
	 * @return instruction added, or null
	 * @throws IllegalArgumentException if T is null
	 */
	public SSANewInstruction addAllocation(TypeReference T) {
		return addAllocation(T, true);
	}

	/**
	 * Add a New statement of the given type
	 */
	public SSANewInstruction addAllocationWithoutCtor(TypeReference T) {
		return addAllocation(T, false);
	}

	/**
	 * Add a New statement of the given type
	 * 
	 * @return instruction added, or null
	 * @throws IllegalArgumentException if T is null
	 */
	private SSANewInstruction addAllocation(TypeReference T, boolean invokeCtor) {
		if (T == null) {
			throw new IllegalArgumentException("T is null");
		}
		int instance = nextLocal++;
		SSANewInstruction result = null;

		if (T.isReferenceType()) {
			NewSiteReference ref = NewSiteReference.make(instructions.size(), T);
			if (T.isArrayType()) {
				int[] sizes = new int[ArrayClass.getArrayTypeDimensionality(T)];
				Arrays.fill(sizes, getValueNumberForIntConstant(1));
				result = instFactory.NewInstruction(instructions.size(), instance, ref, sizes);
			} else {
				result = instFactory.NewInstruction(instructions.size(), instance, ref);
			}
			instructions.add(result);

			IClass klass = cha.lookupClass(T);
			if (klass == null) {
				return null;
			}

			if (klass.isArrayClass()) {
				int arrayRef = result.getDef();
				TypeReference e = klass.getReference().getArrayElementType();
				while (e != null && !e.isPrimitiveType()) {
					// allocate an instance for the array contents
					NewSiteReference n = NewSiteReference.make(instructions.size(), e);
					int alloc = nextLocal++;
					SSANewInstruction ni = null;
					if (e.isArrayType()) {
						int[] sizes = new int[((ArrayClass) cha.lookupClass(T)).getDimensionality()];
						Arrays.fill(sizes, getValueNumberForIntConstant(1));
						ni = instFactory.NewInstruction(instructions.size(), alloc, n, sizes);
					} else {
						ni = instFactory.NewInstruction(instructions.size(), alloc, n);
					}
					instructions.add(ni);

					// emit an astore
					SSAArrayStoreInstruction store = instFactory.ArrayStoreInstruction(instructions.size(), arrayRef,
							getValueNumberForIntConstant(0), alloc, e);
					instructions.add(store);

					e = e.isArrayType() ? e.getArrayElementType() : null;
					arrayRef = alloc;
				}
			}
			if (invokeCtor) {
				IMethod ctor = cha.resolveMethod(klass, MethodReference.initSelector);
				if (ctor != null) {
					addInvocation(new int[] { instance }, CallSiteReference.make(instructions.size(),
							ctor.getReference(), IInvokeInstruction.Dispatch.SPECIAL));
				}
			}
		}
		cache.invalidate(this, Everywhere.EVERYWHERE);
		return result;
	}

	private int getValueNumberForIntConstant(int c) {
		ConstantValue v = new ConstantValue(c);
		Integer result = constant2ValueNumber.get(v);
		if (result == null) {
			result = nextLocal++;
			constant2ValueNumber.put(v, result);
		}
		return result;
	}

	public int addPhi(int[] values) {
		int result = nextLocal++;
		SSAPhiInstruction phi = instFactory.PhiInstruction(instructions.size(), result, values);
		instructions.add(phi);
		return result;
	}

	public int addNull() {
		instructions.add(null);
		return instructions.size() - 1;
	}

	public SSAConditionalBranchInstruction addConditionalBranch(int iindex, int target) {
		assert (target != -1 && iindex != -1);

		int val1 = nextLocal++;
		int val2 = nextLocal++;
		SSAConditionalBranchInstruction conditionalBranchInstruction = instFactory.ConditionalBranchInstruction(
				instructions.size(), IConditionalBranchInstruction.Operator.EQ, TypeReference.Null, val1, val2, target);
		instructions.add(iindex, conditionalBranchInstruction);
		instructions.remove(iindex + 1);
		return conditionalBranchInstruction;
	}
}
