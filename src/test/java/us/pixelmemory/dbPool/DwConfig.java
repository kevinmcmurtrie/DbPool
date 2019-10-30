package us.pixelmemory.dbPool;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.dropwizard.Configuration;


public class DwConfig extends Configuration {
	private DwPooledDataSourceFactory dbf;
	
	@JsonProperty("database")
    public void setDataSourceFactory(DwPooledDataSourceFactory dataSourceFactory) {
		dbf = dataSourceFactory;
    }

	@JsonProperty("database")
	public DwPooledDataSourceFactory getDataSourceFactory() {
		return dbf;
	}
	
}