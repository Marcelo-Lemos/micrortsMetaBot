package metabot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import ai.abstraction.HeavyRush;
import ai.abstraction.LightRush;
import ai.abstraction.RangedRush;
import ai.abstraction.WorkerRush;
import ai.core.AI;
import ai.core.ParameterSpecification;
import features.Feature;
import features.FeatureExtractor;
import features.FeatureNames;
import features.QuadrantModelFeatureExtractor;
import metabot.portfolio.BuildBarracks;
import metabot.portfolio.Expand;
import rts.GameState;
import rts.PlayerAction;
import rts.units.UnitType;
import rts.units.UnitTypeTable;

public class MetaBot extends AI {
    UnitTypeTable myUnitTypeTable = null;
    
    /**
     * For feature calculation, the map will be divided in numQuadrants x numQuadrants
     */
    int numQuadrants;
    
    
    /**
     * Will return the feature values according to state
     */
    private FeatureExtractor featureExtractor;
    
    
    /**
     * Feature 'vector' encoded as a map: feature name -> feature value
     */
    private Map<String, Feature> features;
    
    /**
     * The weight 'vector' is the 'internal' Map from string (feature name) to float (weight value)
     * There's a weight vector per AI, hence the map from string (AI name) to weight vectors
     */
    private Map<String, Map<String, Float>> weights; 
    
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
        myUnitTypeTable = utt;
        
        // if we want to use a different featureExtractor, must customize this call
        //TODO customize numQuadrants 
        featureExtractor = new QuadrantModelFeatureExtractor(3);
        
        //weights are initialized in the first call to {@link #getAction} because we require the game map
        weights = null;
        
        portfolio = new HashMap<>();
        portfolio.put("WorkerRush", new WorkerRush (utt));
        portfolio.put("LightRush", new LightRush (utt));
        portfolio.put("RangedRush", new RangedRush (utt));
        portfolio.put("HeavyRush", new HeavyRush (utt));
        portfolio.put("Expand", new Expand (utt));
        portfolio.put("BuildBarracks", new BuildBarracks (utt));
    }
    
    /* TODO create a method to initialize the portfolio
    private MetaBot(UnitTypeTable utt, Map<String, Map<String, Float>> weights){
    	super(-1, -1);
    	myUnitTypeTable = utt;
    }*/

    
    public void preGameAnalysis(GameState gs, long milliseconds) throws Exception {
    }

    public void preGameAnalysis(GameState gs, long milliseconds, String readWriteFolder) throws Exception {
    	
    }
    
    /**
     * Initializes the weight vector (to be called at the first game frame)
     * Requires the game state because some features depend on map size
     * @param gs
     */
    public void initializeWeights(GameState state){
    	Random random = new Random();
    	weights = new HashMap<>();
    	
    	for(String aiName : portfolio.keySet()){
	    	Map<String, Float> aiWeights = new HashMap<>();
	    	
	    	for (String featureName : featureExtractor.getFeatureNames(state)){
	    		
	    		// weights are initialized randomly within [-1, 1]
	    		aiWeights.put(featureName, random.nextFloat()*2 - 1);
	    	}
	    	
	    	weights.put(aiName, aiWeights);
    	}
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
       
    public PlayerAction getAction(int player, GameState state) {
    	if(weights == null) initializeWeights(state);
    	
        PlayerAction pa = new PlayerAction();
        pa.fillWithNones(state, player, 10);
        return pa;
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
