package sarsa;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import rts.GameState;
import rts.PlayerAction;
import rts.units.Unit;

/**
 * Extract features from a microRTS {@link GameState}
 * @author anderson
 *
 */
public class FeatureExtractor {
	public FeatureExtractor(){
		
	}
	
	public Map<String, Float> getFeatureVector(GameState gs, int player) {
        
		
		Map<String, Float> features = new HashMap<>();
		
		//gets the opponent's index:
		int opponent = 1 - player;
        
        // divides the map in 9 quadrants
		int horizQuadLength = gs.getPhysicalGameState().getWidth() / 3;
		int vertQuadLength = gs.getPhysicalGameState().getHeight() / 3;
        
        // for each quadrant, counts the number of units of each type
        // also count the average cumulative health
		for (int horizQuad = 0; horizQuad < 3; horizQuad++){
			for (int vertQuad = 0; vertQuad < 3; vertQuad++){
				
				Collection<Unit> unitsInQuad = gs.getPhysicalGameState().getUnitsAround(
					horizQuad*horizQuadLength, vertQuad*vertQuadLength, horizQuadLength
				);
				
				// TODO group by player and type
				for(Unit u : unitsInQuad){
					// feature name: unit-quad-xquad-yquad-player-type
					String featureName = String.format(
						"unit-quad-%d-%d-%d-%s", 
						horizQuad, vertQuad, u.getPlayer(), u.getType().name
					);
					
					
				}
				
			}
			
		}
        
        // adds the resources
        features.put("resorces-own", (float)gs.getPlayer(player).getResources());
        features.put("resorces-opp", (float)gs.getPlayer(opponent).getResources());
        
        // adds game time
        features.put("time", (float)gs.getTime());
        
        // normalizes
        
        // adds the bias
        features.put("bias", 1.0f);
        
        
        return features;
        
        
    }   

}
