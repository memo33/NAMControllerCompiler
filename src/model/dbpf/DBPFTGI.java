package model.dbpf;

/**
 * Representation of DBPF TGI (type, group, instance).
 */
public class DBPFTGI {
	
	private int type, group, instance;
	
	public DBPFTGI(int type, int group, int instance) {
		this.type = type;
		this.group = group;
		this.instance = instance;
	}

	public int getType() {
		return type;
	}

	public int getGroup() {
		return group;
	}

	public int getInstance() {
		return instance;
	}
}
