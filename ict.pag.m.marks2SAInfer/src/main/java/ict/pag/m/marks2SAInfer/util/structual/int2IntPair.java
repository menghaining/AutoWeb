package ict.pag.m.marks2SAInfer.util.structual;

public class int2IntPair {
	int name;
	int value;

	public int getName() {
		return name;
	}

	public void setName(int name) {
		this.name = name;
	}

	public int getValue() {
		return value;
	}

	public void setValue(int value) {
		this.value = value;
	}

	public int2IntPair(int name, int value) {
		this.name = name;
		this.value = value;
	}

	public void increaseValue() {
		this.value++;
	}

}
