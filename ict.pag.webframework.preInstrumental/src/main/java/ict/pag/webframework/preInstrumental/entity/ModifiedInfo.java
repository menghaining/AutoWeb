package ict.pag.webframework.preInstrumental.entity;

import java.util.ArrayList;

public class ModifiedInfo {
	private String statement;/* modify on which statement */
//	private String configuration;/* modify which configuration */
	private Integer position;/* 0 is class, 1 is method,-1 is field */
	private ModifiedSemantic type;/* entry? field? indirect call? */

	private Modification way;/* remove? update? add? */
	private ArrayList<String> configurationContent;/* in fieldPointsTo size is 2<field,class>, other is 1 */

	private ArrayList<String> checkBody;/* in fieldPointsTo size is 2<field,class>, other is 1 */
	private boolean appear;/* under what situation find the key-configuration */

	/**
	 * @param statement:     modify on which statement
	 * @param configuration: modify which configuration
	 * @param posistion:     0 is class, 1 is method,-1 is field
	 * @param typr:          entry? field? indirect call?
	 */
	public ModifiedInfo(String statement, Integer position, ModifiedSemantic type, Modification way, ArrayList<String> configurationContent,
			ArrayList<String> checkBody, boolean appear) {
		this.statement = statement;
		this.position = position;
		this.type = type;

		this.way = way;
		this.configurationContent = configurationContent;

		this.checkBody = checkBody;
		this.appear = appear;
	}

	public String getStatement() {
		return statement;
	}

	public Integer getPosition() {
		return position;
	}

	public ModifiedSemantic getType() {
		return type;
	}

	public ArrayList<String> getConfigurationContent() {
		return configurationContent;
	}

	public ArrayList<String> getCheckBody() {
		return checkBody;
	}

	public boolean isAppear() {
		return appear;
	}

	public String toString() {
		return "[stmt]" + statement + "[type]" + type + "[configuration]" + configurationContent;
	}

	public Modification getWay() {
		return way;
	}

}
