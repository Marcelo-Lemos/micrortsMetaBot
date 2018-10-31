package sarsa;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ai.core.AI;
import ai.core.AIWithComputationBudget;
import ai.core.ParameterSpecification;
import features.Feature;
import rts.GameState;
import rts.PlayerAction;
import rts.units.UnitTypeTable;

public class SarsaBot extends AIWithComputationBudget {
    UnitTypeTable myUnitTypeTable = null;
    
    private Map<String, Feature> features;
    
    /**
     * One weight 'vector' for each action/AI I can choose.
     * Each weight 'vector' is indexed by the corresponding feature name. 
     */
    private Map<AI, Map<String, Float>> weights; 

   /**
    * Initializes SarsaBot
    * @param utt
    */
    public SarsaBot(UnitTypeTable utt) {
        super(-1,-1);
        myUnitTypeTable = utt;
        
        //TODO initialize array of weight vector for each AI in the portfolio
        
        weights = new HashMap<>();
    }

    // This will be called by microRTS when it wants to create new instances of this bot (e.g., to play multiple games).
    public AI clone() {
    	//TODO copy features and weights
        return new SarsaBot(myUnitTypeTable);
    }
    
    // This will be called once at the beginning of each new game:    
    public void reset() {
    }
       
    // Called by microRTS at each game cycle.
    // Returns the action the bot wants to execute.
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
