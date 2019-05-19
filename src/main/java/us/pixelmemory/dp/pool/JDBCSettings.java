package us.pixelmemory.dp.pool;

import java.util.Map;

public class JDBCSettings {
	public String url;
	public String driverClass= null;
	public String user;
	public String pass;
	public Map<String, String> properties= null;
	public int validationTimeoutSeconds= 15;
	
	public JDBCSettings () {
		//Bean
	}

	public JDBCSettings(String url, String driverClass, String user, String pass, Map<String, String> properties, int validationTimeoutSeconds) {
		this.url = url;
		this.driverClass = driverClass;
		this.user = user;
		this.pass = pass;
		this.properties = properties;
		this.validationTimeoutSeconds = validationTimeoutSeconds;
	}
}
