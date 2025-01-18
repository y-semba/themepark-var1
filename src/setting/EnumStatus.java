package setting;

public enum EnumStatus {
	INACTIVE(0),
	WAITING(1),
	SERVED(2),
	TERMINATED(3);
	
	private int code;
	
	private EnumStatus(int code){
		this.code = code;
	}
	int getCode() {
		return this.code;
	}
}