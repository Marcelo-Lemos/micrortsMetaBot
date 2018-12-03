package metabot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import ai.abstraction.HeavyRush;
import ai.abstraction.LightRush;
import ai.abstraction.RangedRush;
import ai.abstraction.WorkerRush;
import ai.core.AI;
import ai.core.ParameterSpecification;
import config.ConfigLoader;
import metabot.portfolio.BuildBarracks;
import metabot.portfolio.Expand;
import rl.Sarsa;
import rts.GameState;
import rts.PlayerAction;
import rts.units.UnitTypeTable;

public class MetaBot extends AI {
    UnitTypeTable myUnitTypeTable = null;
    
    /**
     * An array of AI's, which are used as 'sub-bots' to play the game.
     * In our academic wording, this is the portfolio of algorithms that play the game.
     */
    private Map<String,AI> portfolio;
    
    private Sarsa learningAgent;
    
    int myPlayerNumber;
    
    // BEGIN -- variables to feed the learning agent
    private GameState previousState;
    private GameState currentState;
    private AI choice;
    double reward;
    // END-- variables to feed the learning agent
    

   /**
    * Initializes MetaBot with default configurations
    * @param utt
    */
    public MetaBot(UnitTypeTable utt) {
    	// calls the other constructor, specifying the default config file
    	this(utt, "metabot.properties");
    }
    
    /**
     * Initializes MetaBot, loading the configurations from the specified file
     * @param utt
     * @param configPath
     */
    public MetaBot(UnitTypeTable utt, String configPath){
    	myUnitTypeTable = utt;
        
        // loads the configuration
        Properties config = null;
        String members;
		try {
			config = ConfigLoader.loadConfig(configPath);
			members = config.getProperty("portfolio.members");
		} catch (IOException e) {
			System.err.println("Error while loading configuration from " + configPath+ ". Using defaults.");
			e.printStackTrace();
			
			members = "WorkerRush, LightRush, RangedRush, HeavyRush, Expand, BuildBarracks";
		}
        
        String[] memberNames = members.split(",");
        
        //loads the portfolio according to the file specification
        portfolio = new HashMap<>();
        
        //TODO get rid of this for-switch and do something like https://stackoverflow.com/a/6094609/1251716
        for(Object nameObj : memberNames ){
        	String name = (String) nameObj;
        	
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
        
        // creates the learning agent with the specified portfolio
        //TODO customize by loading according to rl.agent in config
        learningAgent = new Sarsa(portfolio);
        
        reset();
    }
    
    
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
     * and my internal variables.
     */
    public void reset() {
    	for(AI ai : portfolio.values()){
    		ai.reset();
    	}
    	
    	choice = null;
    	previousState = null;
    	currentState = null;
    	myPlayerNumber = -1;
    	
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
        learningAgent.learn(previousState, choice, reward, currentState, state.gameover(), player);
    	
        // selected is the AI that will perform our action, let's try it:
    	choice = learningAgent.act(state, player);
    	
        try {
			return choice.getAction(player, state);
		} catch (Exception e) {
			System.err.println("Exception while getting action in frame #" + state.getTime() + " from " + choice.getClass().getSimpleName());
			System.err.println("Defaulting to empty action");
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
    }
    
    public AI clone() {
    	//FIXME copy features, weights and other attributes!
        return new MetaBot(myUnitTypeTable);
    }
    
    
    
    // This will be called by the microRTS GUI to get the
    // list of parameters that this bot wants exposed
    // in the GUI.
    public List<ParameterSpecification> getParameters()
    {
        return new ArrayList<>();
    }
    
}
