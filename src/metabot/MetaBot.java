package metabot;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ai.abstraction.HeavyRush;
import ai.abstraction.LightRush;
import ai.abstraction.RangedRush;
import ai.abstraction.WorkerRush;
import ai.core.AI;
import ai.core.ParameterSpecification;
import config.ConfigManager;
import metabot.portfolio.BuildBarracks;
import metabot.portfolio.Expand;
import rl.Sarsa;
import rts.GameState;
import rts.PlayerAction;
import rts.units.UnitTypeTable;
import utils.FileNameUtil;

public class MetaBot extends AI {
    UnitTypeTable myUnitTypeTable = null;
    
    Logger logger;
    
    /**
     * Stores MetaBot-related configurations loaded from a .property file
     */
    private Properties config;
    
    /**
     * An array of AI's, which are used as 'sub-bots' to play the game.
     * In our academic wording, this is the portfolio of algorithms that play the game.
     */
    private Map<String,AI> portfolio;
    
    private Sarsa learningAgent;
    
    /**
     * Stores the choices made for debugging purposes
     */
    private List<String> choices;
    
    /**
     * Stores the player number to retrieve actions and determine match outcome
     */
    int myPlayerNumber;
    
    // BEGIN -- variables to feed the learning agent
    private GameState previousState;
    private GameState currentState;
    private AI choice;
    double reward;
    // END-- variables to feed the learning agent
    
    private int expNumber;
    

   /**
    * Initializes MetaBot with default configurations
    * @param utt
    */
    public MetaBot(UnitTypeTable utt, int expNumber) {
    	// calls the other constructor, specifying the default config file
    	this(utt, "metabot.properties", expNumber);
    }
    
    /**
     * Initializes MetaBot, loading the configurations from the specified file
     * @param utt
     * @param configPath
     */
    public MetaBot(UnitTypeTable utt, String configPath, int expNumber){
    	myUnitTypeTable = utt;
    	
    	logger = LogManager.getLogger(MetaBot.class);
    	
        // loads the configuration
        String members;
		try {
			config = ConfigManager.loadConfig(configPath);
			members = config.getProperty("portfolio.members");
		} catch (IOException e) {
			logger.error("Error while loading configuration from '" + configPath+ "'. Using defaults.", e);
			
			members = "WorkerRush, LightRush, RangedRush, HeavyRush, Expand, BuildBarracks";
		}
        
        String[] memberNames = members.split(",");
        logger.trace("Portfolio members: ", String.join(",", memberNames));
        
        //loads the portfolio according to the file specification
        portfolio = new HashMap<>();
        
        //TODO get rid of this for-switch and do something like https://stackoverflow.com/a/6094609/1251716
        for(String name : memberNames ){
        	name = name.trim();
        	
        	if(name.equalsIgnoreCase("WorkerRush")){
        		portfolio.put("WorkerRush", new WorkerRush (utt));
        	}
        	else if(name.equalsIgnoreCase("LightRush")){
        		portfolio.put("LightRush", new LightRush (utt));
        	}
        	else if(name.equalsIgnoreCase("RangedRush")){
        		portfolio.put("RangedRush", new RangedRush (utt));
        	}
        	else if(name.equalsIgnoreCase("HeavyRush")){
        		portfolio.put("HeavyRush", new HeavyRush (utt));
        	}
        	else if(name.equalsIgnoreCase("Expand")){
        		portfolio.put("Expand", new Expand (utt));
        	}
        	else if(name.equalsIgnoreCase("BuildBarracks")){
        		portfolio.put("BuildBarracks", new BuildBarracks (utt));
        	}
        	
        	else throw new RuntimeException("Unknown portfolio member '" + name +"'");
        }
        
        // creates the learning agent with the specified portfolio and loaded parameters
        learningAgent = new Sarsa(portfolio, config);
        
        this.expNumber = expNumber;
        
        if (expNumber == 0) {
	        if (config.containsKey("rl.bin_input")){
	        	try {
					learningAgent.loadBin(config.getProperty("rl.bin_input"));
				} catch (IOException e) {
					logger.error("Error while loading weights from " + config.getProperty("rl.input"), e);
					logger.error("Weights initialized randomly.");
					e.printStackTrace();
				}
	        }	
        }
        else {
        	if (config.containsKey("rl.workingdir")){     		
	    		String dir = config.getProperty("rl.workingdir");
	    		if (dir.charAt(dir.length()-1) != '/') {
	    			dir = dir + "/";
	    		}
	    		try {	
	    			learningAgent.loadBin(dir + "weights_" + myPlayerNumber + ".bin");
	    		} catch (IOException e) {
	    			logger.error("Error while loading weights from " + dir + "weights_" + myPlayerNumber + ".bin", e);
					logger.error("Weights initialized randomly.");
					e.printStackTrace();	    			
	    		}
        	}
        }        
        
        reset();
    }

//   /**
//     * Saves the weight 'vector' to a file in the specified path
//     * by serializing the weights HashMap.
//     * The file is overridden if already exists.
//     * @param path
//     * @throws IOException
//     */
//    public void saveBin(String path) throws IOException{
//    	learningAgent.saveBin(path);
//    }
//
//   /**
//     * Loads the weight 'vector' from a file in the specified path
//     * by de-serializing the weights HashMap
//     * @param path
//     * @throws IOException
//     */
//    public void loadBin(String path) throws IOException{
//    	learningAgent.loadBin(path);
//    }
//
//    /**
//     * Saves the weights in human-readable (csv) format. Creates one file
//     * for each portfolio member and appends a line with the weights separated by comma.
//     * The order of weights is as given by weights.get(portfolioMember).values()
//     * 
//     * @param prefix
//     * @throws IOException
//     */
//    public void saveHuman(String prefix) throws IOException{
//    	learningAgent.saveHuman(prefix);
//    }

    public void preGameAnalysis(GameState gs, long milliseconds) throws Exception {
    }

    public void preGameAnalysis(GameState gs, long milliseconds, String readWriteFolder) throws Exception {
    	
    }
    
    /**
     * Resets the portfolio with the new unit type table
     */
    public void reset(UnitTypeTable utt) {
    	myUnitTypeTable = utt;
    	
    	for(AI ai : portfolio.values()){
    		ai.reset(utt);
    	}
    	
    	reset();
    	
    }
    
    /**
     * Is called at the beginning of every game. Resets all AIs in the portfolio
     * and my internal variables. It does not reset the weight vector
     */
    public void reset() {
    	for(AI ai : portfolio.values()){
    		ai.reset();
    	}
    	
    	choice = null;
    	previousState = null;
    	currentState = null;
    	myPlayerNumber = -1;
    	
    	choices = new ArrayList<>(3000);
    	
    }
       
    public PlayerAction getAction(int player, GameState state) {
    	
    	// sets to a valid number on the first call
    	if(myPlayerNumber == -1){
    		myPlayerNumber = player;
    	}
    	
    	// verifies if the number I set previously holds
    	if(myPlayerNumber != player){
    		throw new RuntimeException(
				"Called with wrong player number " + player + ". Was expecting: " + myPlayerNumber
			);
    	}
    	
    	// makes the learning agent learn
    	previousState = currentState;
    	currentState = state;
    	reward = 0;
        if (state.gameover()){
        	if(state.winner() == player) reward = 1;
        	if(state.winner() == 1-player) reward = -1;
        	else reward = 0;
        }
        learningAgent.learn(previousState, choice, reward, currentState, currentState.gameover(), player);
    	
        // selected is the AI that will perform our action, let's try it:
    	choice = learningAgent.act(state, player);
    	
    	choices.add(choice.getClass().getSimpleName());
    	
        try {
			return choice.getAction(player, state);
		} catch (Exception e) {
			logger.error("Exception while getting action in frame #" + state.getTime() + " from " + choice.getClass().getSimpleName(), e);
			logger.error("Defaulting to empty action");
			e.printStackTrace();
			
			PlayerAction pa = new PlayerAction();
			pa.fillWithNones(state, player, 1);
			return pa;
		}
    }   
    
    public void gameOver(int winner) throws Exception {
    	if (winner == -1) reward = 0; //game not finished (timeout) or draw
    	else if (winner == myPlayerNumber) reward = 1; //I won
    	else reward = -1; //I lost
    	
    	learningAgent.learn(previousState, choice, reward, currentState, true, myPlayerNumber);
    	
    	//tests whether the output prefix has been specified to save the weights (binary)
    	/*if (config.containsKey("rl.output.binprefix")){
    		String filename = FileNameUtil.nextAvailableFileName(
				config.getProperty("rl.output.binprefix"), "weights"
			);
    		learningAgent.saveBin(filename); 
    	}*/
    	
    	//tests whether the output prefix has been specified to save the weights (human-readable)
    	/*if (config.containsKey("rl.output.humanprefix")){    		
    		learningAgent.saveHuman(config.getProperty("rl.output.humanprefix"));
    	}*/

    	
    	if (config.containsKey("rl.save_weights_bin")){
    		if (config.getProperty("rl.save_weights_bin").equalsIgnoreCase("True")) {     		
	    		String dir = config.getProperty("rl.workingdir","weights/");
	    		if (dir.charAt(dir.length()-1) != '/') {
	    			dir = dir + "/";
	    		}
	    			
	    		learningAgent.saveBin(dir + "weights_" + myPlayerNumber + ".bin");
    		}
    	}
    	
    	if (config.containsKey("rl.save_weights_human")){
    		if (config.getProperty("rl.save_weights_human").equalsIgnoreCase("True")) {     		
	    		String dir = config.getProperty("rl.workingdir","weights/");
	    		if (dir.charAt(dir.length()-1) != '/') {
	    			dir = dir + "/";
	    		}
	    			
	    		learningAgent.saveHuman(dir + "weights_" + myPlayerNumber);
    		}
    	}
    	
    	    	
    	// check if it needs to save the choices
    	if (config.containsKey("output.choices_prefix")){
    		
    		// finds the file name
    		String filename = FileNameUtil.nextAvailableFileName(
				config.getProperty("output.choices_prefix"), "choices"
			);
        		
    		// saves the weights
    		FileWriter writer = new FileWriter(filename);
    		writer.write(String.join("\n", choices));
    		writer.close();
    	}
    	
    	
    }
    
    public AI clone() {
    	//FIXME copy features, weights and other attributes!
        return new MetaBot(myUnitTypeTable, expNumber);
    }
    
    
    
    // This will be called by the microRTS GUI to get the
    // list of parameters that this bot wants exposed
    // in the GUI.
    public List<ParameterSpecification> getParameters()
    {
        return new ArrayList<>();
    }
    
}
