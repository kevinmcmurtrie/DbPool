package us.pixelmemory.dbPool;

import java.util.LinkedHashMap;
import java.util.Map;

public class JDBCConnectionSettings {
	String url;
	String driverClass= null;
	String user;
	String pass;
	Map<String, String> properties= null;
	int validationTimeoutSeconds= 15;
	
	public JDBCConnectionSettings () {
		//Bean
	}

	public JDBCConnectionSettings(String url, String driverClass, String user, String pass, Map<String, String> properties, int validationTimeoutSeconds) {
		this.url = url;
		this.driverClass = driverClass;
		this.user = user;
		this.pass = pass;
		this.properties = properties;
		this.validationTimeoutSeconds = validationTimeoutSeconds;
	}
	
	public JDBCConnectionSettings(JDBCConnectionSettings other) {
		this.url = other.url;
		this.driverClass = other.driverClass;
		this.user = other.user;
		this.pass = other.pass;
		this.properties = (other.properties != null) ? new LinkedHashMap<>(other.properties) : null;
		this.validationTimeoutSeconds = other.validationTimeoutSeconds;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getDriverClass() {
		return driverClass;
	}

	public void setDriverClass(String driverClass) {
		this.driverClass = driverClass;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPass() {
		return pass;
	}

	public void setPass(String pass) {
		this.pass = pass;
	}

	public Map<String, String> getProperties() {
		return properties;
	}

	public void setProperties(Map<String, String> properties) {
		this.properties = properties;
	}

	public int getValidationTimeoutSeconds() {
		return validationTimeoutSeconds;
	}

	public void setValidationTimeoutSeconds(int validationTimeoutSeconds) {
		this.validationTimeoutSeconds = validationTimeoutSeconds;
	}
}
