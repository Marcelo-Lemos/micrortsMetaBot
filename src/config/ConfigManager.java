package config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * A singleton configuration manager
 * @author anderson
 *
 */
public class ConfigManager {
	
	private static Map<String,Properties> configurations;
	private static ConfigManager instance = null;
	
	/**
	 * Returns the singleton instance of this class
	 * @return
	 */
	public static ConfigManager getInstance(){
		if (instance == null){
			instance = new ConfigManager();
		}
		return instance;
	}
	
	/**
	 * Hidden constructor (cannot instantiate)
	 */
	private ConfigManager(){
		configurations = new HashMap<>();
	}
	
	/**
	 * Returns the Configuration object. loadConfig must have been called prior to this method.
	 * @return
	 */
	public Properties getConfig(String context){
		//log("retrieving config for context");
		if(!configurations.containsKey(context)){
			throw new IllegalArgumentException("Context '" + context + "' not present in configurations.");
		}
		
		return configurations.get(context);
	}
	
	/**
	 * Creates a configuration (a Properties object) for a given context, 
	 * loading it from a configuration file.
	 * The configuration is stored within the specified context and returned.
	 * If a configuration already exists for that context, it is overridden.
	 *  
	 * @param context The name of the context to create the configuration.
	 * @param filename
	 * @return
	 * @throws IOException 
	 */
	public Properties newConfig(String context, String filename) throws IOException{
		
		Properties config = new Properties();
		InputStream is = new FileInputStream(filename.trim());
		config.load(is);
		is.close();
		
		configurations.put(context, config);
		
        return config;
	}
}
