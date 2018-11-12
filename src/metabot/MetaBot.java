package metabot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration2.Configuration;

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
    

   /**
    * Initializes MetaBot
    * @param utt
    */
    public MetaBot(UnitTypeTable utt) {
        myUnitTypeTable = utt;
        
        // loads the configuration
        //TODO customize config. file
        Configuration config = ConfigLoader.loadConfig("metabot.properties");
        
        List<Object> memberList = config.getList("portfolio");
        
        //loads the portfolio according to the file specification
        portfolio = new HashMap<>();
        
        //TODO get rid of this for-switch and do something like https://stackoverflow.com/a/6094609/1251716
        for(Object nameObj : memberList ){
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
        	
        	else throw new RuntimeException("Unknown portfolio member " + name);
        	
        }
        
        // creates the learning agent with the specified portfolio
        learningAgent = new Sarsa(portfolio);
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
    	
    }
    
    /**
     * Is called at the beginning of every game. Resets all AIs in the portfolio
     */
    public void reset() {
    	for(AI ai : portfolio.values()){
    		ai.reset();
    	}
    	
    }
       
    public PlayerAction getAction(int player, GameState gameState) {
    	
        // selected is the AI that will perform our action, let's try it:
    	AI selected = learningAgent.act(gameState, player);
    	
        try {
			return selected.getAction(player, gameState);
		} catch (Exception e) {
			System.err.println("Exception while getting action in frame #" + gameState.getTime() + " from " + selected.getClass().getSimpleName());
			System.err.println("Defaulting to empyt action");
			e.printStackTrace();
			
			PlayerAction pa = new PlayerAction();
			pa.fillWithNones(gameState, player, 1);
			return pa;
		}
    }    
    
    public void gameOver(int winner) throws Exception {
    }
    
    public AI clone() {
    	//TODO copy features and weights
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
