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
    private int numQuadrants;
    
    /**
     * Probability of exploration
     */
    private float epsilon;
    
    /**
     * Learning rate
     */
    private float alpha;
    
    /**
     * Discount factor
     */
    private float gamma;
    
    /**
     * Eligibility trace
     */
    private float lambda;
    
    /**
     * Will return the feature values according to state
     */
    private FeatureExtractor featureExtractor;
    
    //--- variables to perform SARSA weight update:
    GameState state, prevState;	//state and previous state (rather than nextState and state)
    String choice, prevChoice; //choice and previous choice (rather than nextChoice and choice)
    
    
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
        
        state = prevState = null;
        choice = prevChoice = null;
        
        //TODO customize random seed
        random = new Random();
        
        //TODO customize numQuadrants
        numQuadrants = 3;
        
        //TODO customize epsilon
        epsilon = 0.1f;
        
        //TODO customize alpha
        alpha = 0.1f;
        
        //TODO customize gamma
        gamma = 0.9f;
        
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
       
    public PlayerAction getAction(int player, GameState gameState) {
    	//initializes weights on first frame
    	if(weights == null) initializeWeights(gameState);
    	
    	// perceives the current state (will be used in learning)
    	prevState = state;
    	state = gameState;
    	prevChoice = choice;
    	
    	/**
         * Feature 'vector' encoded as a map: feature name -> feature value
         */
        Map<String, Feature> features = featureExtractor.getFeatures(state, player);
        
        // will choose the action for this state
        choice = null;
        
        // epsilon-greedy:
        if(random.nextFloat() < epsilon){ //random choice
        	//trick to randomly select from HashMap adapted from: https://stackoverflow.com/a/9919827/1251716
        	List<String> keys = new ArrayList<String>(portfolio.keySet());
        	choice = keys.get(random.nextInt(keys.size()));
        }
        else { //greedy choice
        	float maxQ = Float.MIN_VALUE;
        	
        	for(String aiName: weights.keySet()){
        		float q = qValue(features, aiName);
        		if (q > maxQ){
        			maxQ = q;
        			choice = aiName;
        		}
        	}
        }
        // learning (i.e. weight vector update)
        // reward is zero for all states but terminals
        float reward = 0;
        if (state.gameover()){
        	if(state.winner() == player) reward = 1;
        	if(state.winner() == 1-player) reward = -1;
        	else reward = 0; //draw (added here to make it explicit)
        }
        // uses sarsa to update the weights regarding the PREVIOUS state and choice
        // this is necessary because sarsa needs the 'future' state and choice which I got to know now
        sarsaLearning(prevState, prevChoice, reward, state, choice, player);
        
        
        //now, 'selected' contains the AI that will perform our action, let's try it:
        try {
        	AI selected = portfolio.get(choice);
			return selected.getAction(player, gameState);
		} catch (Exception e) {
			System.err.println("Exception while getting action in frame #" + gameState.getTime() + " from " + choice);
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
    
    /**
     * Updates the weight vector of the current action (choice) using the Sarsa rule:
     * delta = r + gamma * Q(s',a') - Q(s,a)
     * w_i <- w_i + alpha*delta*f_i (where w_i is the i-th weight and f_i the i-th feature)
     * 
     * @param state s in Sarsa equation
     * @param choice a in Sarsa equation
     * @param reward r in Sarsa equation
     * @param nextState s' in Sarsa equation
     * @param nextChoice a' in Sarsa equation
     * @param player required to extract the features for the states
     */
    private void sarsaLearning(GameState state, String choice, float reward, GameState nextState, String nextChoice, int player){
    	// checks if s' and a' are ok (s and a will always be ok, we hope)
    	if(nextState == null || nextChoice == null) return;
    	
    	Map<String, Feature> stateFeatures = featureExtractor.getFeatures(state, player);
    	Map<String, Feature> nextStateFeatures = featureExtractor.getFeatures(nextState, player);
    	
    	float q = qValue(stateFeatures, choice);
    	float futureQ = qValue(nextStateFeatures, nextChoice);
    	
    	//the temporal-difference error (delta in Sarsa equation)
    	float delta = reward + gamma * futureQ - q;
    	
    	for(String featureName : stateFeatures.keySet()){
    		//retrieves the weight value, updates it and stores the updated value
    		float weightValue = weights.get(choice).get(featureName);
    		weightValue += alpha * delta * stateFeatures.get(featureName).getValue();
    		weights.get(choice).put(featureName, weightValue);
    	}
    	
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
    
    /**
     * Returns the Q-value of a choice (action), for a given set of features
     * @param features
     * @param choice
     * @return
     */
    private float qValue(Map<String,Feature> features, String choice){
    	return dotProduct(features, weights.get(choice));
    }
}
