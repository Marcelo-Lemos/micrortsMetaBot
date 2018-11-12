package config;

import java.io.File;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;

/**
 * Emulates a singleton version of {@link Configuration}.
 * @author anderson
 *
 */
public class ConfigLoader {
	
	private static Configuration configInstance;
	
	/**
	 * Hidden constructor (cannot instantiate)
	 */
	private ConfigLoader(){
		
	}
	
	/**
	 * Returns the Configuration object. loadConfig must have been called prior to this method.
	 * @return
	 */
	public static Configuration getConfiguration(){
		if(configInstance == null){
			throw new RuntimeException("Must load config before getting the config instance.");
		}
		
		return configInstance;
	}
	
	/**
	 * Loads a configuration file from the specified path and returns the corresponding
	 * Configuration object.
	 * @param path
	 * @return
	 */
	public static Configuration loadConfig(String path){
		Configurations configs = new Configurations();
        try {
        	configInstance = configs.properties(new File(path));
        }
        catch (ConfigurationException cex) {
            System.err.println("Error while loading configuration " + path);
            cex.printStackTrace();
        }
        
        return configInstance;
	}
}
