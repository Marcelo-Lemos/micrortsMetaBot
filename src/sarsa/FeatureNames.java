package sarsa;

public class FeatureNames {
	
	public static final String UNIT_COUNT = "unit_count";	//to be concatenated with quadrant, player and unit type
	public static final String AVG_HEALTH = "avg_health"; 	//to be concatenated with quadrant and player
	
	public static final String RESOURCES_OWN = "resorces_own";
	public static final String RESOURCES_OPP = "resources_opp";
	public static final String GAME_TIME = "game_time";
	public static final String BIAS = "bias";	//the 'independent term' whose value is always 1

}
