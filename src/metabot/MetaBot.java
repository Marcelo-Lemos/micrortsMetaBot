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
import features.QuadrantModelFeatureExtractor;
import metabot.portfolio.BuildBarracks;
import metabot.portfolio.Expand;
import rts.GameState;
import rts.PlayerAction;
import rts.units.UnitTypeTable;

public class MetaBot extends AI {
    UnitTypeTable myUnitTypeTable = null;
    
    /**
     * Random number generator
     */
    Random random;
    
    /**
     * For feature calculation, the map will be divided in numQuadrants x numQuadrants
     */
    int numQuadrants;
    
    /**
     * Probability of exploration
     */
    float epsilon;
    
    /**
     * Learning rate
     */
    float alpha;
    
    /**
     * Eligibility trace
     */
    float lambda;
    
    /**
     * Will return the feature values according to state
     */
    private FeatureExtractor featureExtractor;
    
    
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
        
        //TODO customize random seed
        random = new Random();
        
        //TODO customize numQuadrants
        numQuadrants = 3;
        
        //TODO customize epsilon
        epsilon = 0.1f;
        
        //TODO customize alpha
        alpha = 0.1f;
        
        //TODO customize lambda
        lambda = 0;
        
        // if we want to use a different featureExtractor, must customize this call
        featureExtractor = new QuadrantModelFeatureExtractor(numQuadrants);
        
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
    	//TODO perform learning
    	
    	if(weights == null) initializeWeights(state);
    	
    	/**
         * Feature 'vector' encoded as a map: feature name -> feature value
         */
        Map<String, Feature> features = featureExtractor.getFeatures(state, player);
        
        AI selected = null;
        
        // epsilon-greedy:
        if(random.nextFloat() < epsilon){ //random choice
        	//trick to randomly select from HashMap from: https://stackoverflow.com/a/9919827/1251716
        	Random       random    = new Random();
        	List<String> keys      = new ArrayList<String>(portfolio.keySet());
        	String       randomKey = keys.get( random.nextInt(keys.size()) );
        	selected 			   = portfolio.get(randomKey);
        }
        else { //greedy choice
        	float maxProduct = Float.MIN_VALUE;
        	
        	for(String aiName: weights.keySet()){
        		float product = dotProduct(features, weights.get(aiName));
        		if (product > maxProduct){
        			maxProduct = product;
        			selected = portfolio.get(aiName);
        		}
        	}
        }
        
        //now, 'selected' contains the AI that will perform our action, let's try it:
        try {
			return selected.getAction(player, state);
		} catch (Exception e) {
			System.err.println("Exception while getting action in frame #" + state.getTime() + " from " + selected.getClass().getSimpleName());
			System.err.println("Defaulting to empyt action");
			e.printStackTrace();
			
			PlayerAction pa = new PlayerAction();
			pa.fillWithNones(state, player, 1);
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
    
    /**
     * Returns the dot product of features and their respective weights 
     * @param features
     * @param weights
     * @return
     */
    private float dotProduct(Map<String,Feature> features, Map<String, Float> weights){
    	float product = 0.0f;
    	for(String featureName : features.keySet()){
    		product += features.get(featureName).getValue() * weights.get(featureName);
    	}
    	return product;
    }
}
