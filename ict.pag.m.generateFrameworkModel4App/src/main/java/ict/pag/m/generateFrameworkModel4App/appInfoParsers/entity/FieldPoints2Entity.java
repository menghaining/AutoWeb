package ict.pag.m.generateFrameworkModel4App.appInfoParsers.entity;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;

public class FieldPoints2Entity {
	IField field;
	boolean declare = false;
	IClass target = null;

	/**
	 * @param field   the field to injected in
	 * @param declare whether the type of inject is declare of the field
	 * @param target  if not declare, the specific target
	 */
	public FieldPoints2Entity(IField field, boolean declare, IClass target) {
		this.field = field;
		this.declare = declare;
		this.target = target;
	}

	/**
	 * @param field   the field to injected in
	 * @param declare whether the type of inject is declare of the field
	 */
	public FieldPoints2Entity(IField field, boolean declare) {
		this.field = field;
		this.declare = declare;
	}

	public IField getField() {
		return field;
	}

	public boolean isDeclare() {
		return declare;
	}

	public IClass getTarget() {
		return target;
	}

}
