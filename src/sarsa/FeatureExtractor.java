package sarsa;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import rts.GameState;
import rts.PlayerAction;
import rts.units.Unit;
import rts.units.UnitType;

/**
 * Extract features from a microRTS {@link GameState}
 * @author anderson
 *
 */
public class FeatureExtractor {
	public FeatureExtractor(){
		
	}
	
	public Map<String, Float> getFeatureVector(GameState state, int player) {
        
		
		Map<String, Float> features = new HashMap<>();
		
		//gets the opponent's index:
		int opponent = 1 - player;
        
        // divides the map in 9 quadrants
		int horizQuadLength = state.getPhysicalGameState().getWidth() / 3;
		int vertQuadLength = state.getPhysicalGameState().getHeight() / 3;
        
        // for each quadrant, counts the number of units of each type per player
        // TODO: also count the average health of units owned by each player
		for (int horizQuad = 0; horizQuad < 3; horizQuad++){
			for (int vertQuad = 0; vertQuad < 3; vertQuad++){
				
				Collection<Unit> unitsInQuad = state.getPhysicalGameState().getUnitsAround(
					horizQuad*horizQuadLength, vertQuad*vertQuadLength, horizQuadLength
				);
				
				//Map<Unit, int> unitCounter
				
				// initializes the unit count of each type and player as zero
				// also initializes the average health of units owned per player as zero
				for(int p = 0; p < 2; p++){ // p for each player
					for(UnitType type : state.getUnitTypeTable().getUnitTypes()){ //type for each unit type
						features.put(unitCountFeatureName(horizQuad, vertQuad, p, type), 0.0f);
						features.put(avgHealthFeatureName(horizQuad, vertQuad, p), 0.0f);
					}
				}
				
				// traverses the list of units in quadrant, incrementing their feature count
				for(Unit u : unitsInQuad){
					String featureName = unitCountFeatureName(horizQuad, vertQuad, u.getPlayer(), u.getType());
					
					// counts and increment the number of the given unit in the current quadrant
					features.put(featureName, features.get(featureName) + 1 );
				}
				
			}
		}	
        
        // adds the resources
        features.put("resorces-own", (float)state.getPlayer(player).getResources());
        features.put("resorces-opp", (float)state.getPlayer(opponent).getResources());
        
        // adds game time
        features.put("time", (float)state.getTime());
        
        // normalizes
        
        // adds the bias
        features.put("bias", 1.0f);
        
        
        return features;
        
        
    }   
	
	/**
	 * Returns the feature name for unit count, given 
	 * the quadrant, unit owner and unit type
	 * @param xQuad
	 * @param yQuad
	 * @param owner
	 * @param type
	 * @return
	 */
	private String unitCountFeatureName(int xQuad, int yQuad, int owner, UnitType type){
		// feature name: unit_quad-x-y-owner-type
		return String.format(
			StateFeatures.UNIT_COUNT + "-%d-%d-%d-%s", 
			xQuad, yQuad, owner, type.name
		);
	}
	
	/**
	 * Returns the corresponding feature name for average unit health, given
	 * the quadrant and player
	 * @param xQuad
	 * @param yQuad
	 * @param player
	 * @return
	 */
	private String avgHealthFeatureName(int xQuad, int yQuad, int player){
		return String.format(
			StateFeatures.AVG_HEALTH + "-%d-%d-%d", 
			xQuad, yQuad, player
		);
	}

}
