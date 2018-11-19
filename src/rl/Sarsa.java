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
 * TODO allow loading and saving of weights
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
    
    /**
     * The 'action' that this learning agent returns, i.e. an AI to perform the 
     * game action on behalf of the plaer
     */
    AI nextChoice;
    
    
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
     * @param state
     * @param player
     * @return
     */
    public AI act(GameState state, int player){
    	//nextChoice is null on the first call to this function, afterwards, it is determined as a side-effect of 'learn'
    	if(nextChoice == null){
    		nextChoice = epsilonGreedy(state, player);
    	}
    	
        return nextChoice;
    }
    
    /**
     * Returns an action using epsilon-greedy for the given state
     * (i.e., a random action with probability epsilon, and the greedy action otherwise)
     * @param state
     * @param player
     * @return
     */
    private AI epsilonGreedy(GameState state, int player){
    	//initializes weights on first frame
    	if(weights == null) initializeWeights(state);
    	
    	// will choose the action for this state
        String choiceName = null;
        
        //Feature 'vector' encoded as a map: feature name -> feature value
        Map<String, Feature> features = featureExtractor.getFeatures(state, player);
        
        // epsilon-greedy:
        if(random.nextFloat() < epsilon){ //random choice
        	//trick to randomly select from HashMap adapted from: https://stackoverflow.com/a/9919827/1251716
        	List<String> keys = new ArrayList<String>(portfolio.keySet());
        	choiceName = keys.get(random.nextInt(keys.size()));
        }
        else { //greedy choice
        	float maxQ = Float.MIN_VALUE;
        	
        	for(String aiName: weights.keySet()){
        		float q = qValue(features, aiName);
        		if (q > maxQ){
        			maxQ = q;
        			choiceName = aiName;
        		}
        	}
        }
        
        return portfolio.get(choiceName);
    }
    
    /**
     * Receives an experience tuple (s, a, r, s') and updates the action-value function
     * As a side effect of Sarsa, the next action a' is chosen here.
     * @param state s
     * @param choice a
     * @param reward r
     * @param nextState s'
     * @param done whether this is the end of the episode
     * @param player required to extract the features of this state
     */
    public void learn(GameState state, AI choice, double reward, GameState nextState, boolean done, int player){
        
    	// ensures all variables are valid (they won't be in the initial state)
    	if (state == null || nextState == null || choice == null) {
    		return; 
    	}
    	
    	// determines the next choice
    	nextChoice = epsilonGreedy(nextState, player);
    	
    	// applies the update rule with s, a, r, s', a'
        sarsaLearning(
    		state, choice.getClass().getSimpleName(), reward, 
    		nextState, nextChoice.getClass().getSimpleName(), player
    	);
        
        
        if (done){
        	//decays alpha and epsilon
        	alpha *= alphaDecayRate;
        	epsilon *= epsilonDecayRate;
        	
        }
        
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
    private void sarsaLearning(GameState state, String choice, double reward, GameState nextState, String nextChoice, int player){
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
