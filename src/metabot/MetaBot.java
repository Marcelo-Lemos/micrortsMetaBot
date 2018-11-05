package metabot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ai.abstraction.HeavyRush;
import ai.abstraction.LightRush;
import ai.abstraction.RangedRush;
import ai.abstraction.WorkerRush;
import ai.core.AI;
import ai.core.AIWithComputationBudget;
import ai.core.ParameterSpecification;
import features.Feature;
import metabot.portfolio.BuildBarracks;
import metabot.portfolio.Expand;
import rts.GameState;
import rts.PlayerAction;
import rts.units.UnitTypeTable;

public class MetaBot extends AIWithComputationBudget {
    UnitTypeTable myUnitTypeTable = null;
    
    private Map<String, Feature> features;
    
    /**
     * One weight 'vector' for each action/AI I can choose.
     * Each weight 'vector' is indexed by the corresponding feature name. 
     */
    private Map<AI, Map<String, Float>> weights; 
    
    /**
     * An array of AI's, which are used as 'sub-bots' to play the game.
     * In our academic wording, this is the portfolio of algorithms that play the game.
     */
    private Map<String,AI> portfolio;

   /**
    * Initializes MetaBot
    * @param utt
    */
    public MetaBot(UnitTypeTable utt) {
        super(-1,-1);
        myUnitTypeTable = utt;
        
        //weights are initialized in the first call to {@link #getAction}
        weights = null;
        
        portfolio = new HashMap<>();
        portfolio.put("WorkerRush", new WorkerRush (utt));
        portfolio.put("LightRush", new LightRush (utt));
        portfolio.put("RangedRush", new RangedRush (utt));
        portfolio.put("HeavyRush", new HeavyRush (utt));
        portfolio.put("Expand", new Expand (utt));
        portfolio.put("BuildBarracks", new BuildBarracks (utt));


        
    }

    // This will be called by microRTS when it wants to create new instances of this bot (e.g., to play multiple games).
    public AI clone() {
    	//TODO copy features and weights
        return new MetaBot(myUnitTypeTable);
    }
    
    // This will be called once at the beginning of each new game:    
    public void reset() {
    }
    
    public void preGameAnalysis(GameState gs, long milliseconds) throws Exception {
    }

    public void preGameAnalysis(GameState gs, long milliseconds, String readWriteFolder) throws Exception {
    	
    }
       
    public PlayerAction getAction(int player, GameState gs) {
        PlayerAction pa = new PlayerAction();
        pa.fillWithNones(gs, player, 10);
        return pa;
    }    
    
    // This will be called by the microRTS GUI to get the
    // list of parameters that this bot wants exposed
    // in the GUI.
    public List<ParameterSpecification> getParameters()
    {
        return new ArrayList<>();
    }
}
