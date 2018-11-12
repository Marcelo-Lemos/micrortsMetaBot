package rl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.configuration2.Configuration;

import ai.core.AI;
import config.ConfigLoader;
import features.Feature;
import features.FeatureExtractor;
import features.QuadrantModelFeatureExtractor;
import rts.GameState;

/**
 * Implements Sarsa(0)
 * 
 * TODO implement Sarsa(lambda)
 * TODO make its interface more gym-like
 * @author anderson
 *
 */
public class Sarsa {
	/**
     * Random number generator
     */
    Random random;
    
    /**
     * For feature calculation, the map will be divided in quadrantDivision x quadrantDivision
     */
    private int quadrantDivision;
    
    /**
     * Probability of exploration
     */
    private double epsilon;
    
    /**
     * Decay rate of epsilon
     */
    private double epsilonDecayRate;
    
    /**
     * Learning rate
     */
    private double alpha;
    
    /**
     * Decay rate of alpha
     */
    private double alphaDecayRate;
    
    /**
     * Discount factor
     */
    private double gamma;
    
    /**
     * Eligibility trace
     */
    private double lambda;
    
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
    
    public Sarsa(Map<String,AI> portfolio){
    	state = prevState = null;
        choice = prevChoice = null;
        
        Configuration config = ConfigLoader.getConfiguration();
        
        random = new Random(config.getInt("rl.random.seed"));
        
        epsilon = config.getDouble("rl.epsilon.initial", 0.1);
        epsilonDecayRate = config.getDouble("rl.epsilon.decay", 1.0);
        
        alpha = config.getDouble("rl.alpha.initial", 0.1);
        alphaDecayRate = config.getDouble("rl.alpha.decay", 1.0);
        
        gamma = config.getDouble("rl.gamma", 0.9);
        
        lambda = config.getDouble("rl.lambda", 0.0);
        
        quadrantDivision = config.getInt("rl.feature.extractor.quadrant_division", 3);
        
        // if we want to use a different featureExtractor, must customize this call
        featureExtractor = new QuadrantModelFeatureExtractor(quadrantDivision);
            
        //weights are initialized in the first call to {@link #getAction} because we require the game map
        weights = null;
        
        this.portfolio = portfolio;
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
     * Returns the AI for the given state and player. 
     * Also performs learning from the previous choice, state, perceived reward, current state and choice
     * @param gameState
     * @param player
     * @return
     */
    public AI act(GameState gameState, int player){
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
        	
        	//decays alpha and epsilon
        	alpha *= alphaDecayRate;
        	epsilon *= epsilonDecayRate;
        	
        }
        // uses sarsa to update the weights for the PREVIOUS state and choice
        // using current state and choice as Sarsa's future ones
        sarsaLearning(prevState, prevChoice, reward, state, choice, player);
        
        return portfolio.get(choice);
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
    	double delta = reward + gamma * futureQ - q;
    	
    	for(String featureName : stateFeatures.keySet()){
    		//retrieves the weight value, updates it and stores the updated value
    		float weightValue = weights.get(choice).get(featureName);
    		weightValue += alpha * delta * stateFeatures.get(featureName).getValue();
    		weights.get(choice).put(featureName, weightValue);
    	}
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
