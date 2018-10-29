package sarsa;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ai.core.AI;
import ai.core.AIWithComputationBudget;
import ai.core.ParameterSpecification;
import rts.GameState;
import rts.PlayerAction;
import rts.units.UnitTypeTable;

public class SarsaBot extends AIWithComputationBudget {
    UnitTypeTable m_utt = null;
    
    private Map<String, Float> features;
    private Map<AI,Map<String, Float>> weights; //one weight vector for each action/AI I can choose

   /**
    * Initializes SarsaBot
    * @param utt
    */
    public SarsaBot(UnitTypeTable utt) {
        super(-1,-1);
        m_utt = utt;
        
        features = new HashMap<>();
        weights = new HashMap<>();
    }

    // This will be called by microRTS when it wants to create new instances of this bot (e.g., to play multiple games).
    public AI clone() {
    	//TODO copy features and weights
        return new SarsaBot(m_utt);
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
