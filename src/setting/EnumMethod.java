package setting;

public enum EnumMethod {
	CCE("CCE"),
	SCE("SCE"),
	WSCE("WSCE"),
	WPSCE("WPSCE");
	
	private String methodName;
	
	private EnumMethod(String methodName){
		this.methodName = methodName;
	}
	
	public String getMethodName() {
		return this.methodName;
	}
}