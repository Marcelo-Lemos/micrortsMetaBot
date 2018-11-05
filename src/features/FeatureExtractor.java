package features;

import java.util.Map;
import java.util.Set;

import rts.GameState;

public abstract class FeatureExtractor {
	
	/**
	 * Returns the features associated with a {@link GameState} from the point
	 * of view of a player (0 or 1)
	 * @param state
	 * @param player
	 * @return
	 */
	public abstract Map<String, Feature> getFeatures(GameState state, int player); 
	
	/**
	 * Returns the set of feature names for this model.
	 * Requires the game state because some features depend on specific map
	 * characteristics.
	 * 
	 * @param state
	 * @return
	 */
	public abstract Set<String> getFeatureNames(GameState state);
	
	/**
	 * Returns the features associated with a {@link GameState} from the point
	 * of view of a player (0 or 1).
	 * 
	 * The features are normalized to the range [0, 1] via min-max scaling.
	 * @param state
	 * @param player
	 * @return
	 */
	public Map<String, Feature> getNormalizedFeatures(GameState state, int player) {
		Map<String, Feature> features = getFeatures(state, player);
		
        for(Feature f: features.values()){
        	f.minMaxScaling();
        }
        return features;
	}

}
