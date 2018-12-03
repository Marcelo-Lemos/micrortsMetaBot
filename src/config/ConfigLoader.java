package config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import rts.GameSettings;

/**
 * Emulates a singleton version of {@link Configuration}.
 * @author anderson
 *
 */
public class ConfigLoader {
	
	private static Properties configInstance;
	
	/**
	 * Hidden constructor (cannot instantiate)
	 */
	private ConfigLoader(){
		
	}
	
	/**
	 * Returns the Configuration object. loadConfig must have been called prior to this method.
	 * @return
	 */
	public static Properties getConfiguration(){
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
	 * @throws IOException 
	 */
	public static Properties loadConfig(String path) throws IOException{
		configInstance = new Properties();
		InputStream is = new FileInputStream(path);
		configInstance.load(is);
		is.close();
        return configInstance;
	}
}
