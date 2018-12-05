package rl;

import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;

import ai.core.AI;
import metabot.MetaBot;
import rts.GameSettings;
import rts.GameState;
import rts.PartiallyObservableGameState;
import rts.PhysicalGameState;
import rts.PlayerAction;
import rts.units.UnitTypeTable;

/**
 * A class to run microRTS games to train and test MetaBot
 * @author anderson
 * TODO use the methods to save and load weights
 */
public class Runner {

	public static void main(String[] args) throws Exception {
		Properties prop = new Properties();
		InputStream is;
		if(args.length > 1){
			is = new FileInputStream(args[1]);
		}
		else {
			is = new FileInputStream("config/microrts.properties");
		}
        prop.load(is);
		GameSettings settings = GameSettings.loadFromConfig(prop);
		System.out.println(settings);
		is.close();
		
		UnitTypeTable utt = new UnitTypeTable(settings.getUTTVersion(), settings.getConflictPolicy());
        AI ai1 = loadAI(settings.getAI1(), utt, prop);
        AI ai2 = loadAI(settings.getAI2(), utt, prop);
        
        int numGames = Integer.parseInt(prop.getProperty("num_games", "1"));
        
        for(int i = 0; i < numGames; i++){
        	headlessMatch(ai1, ai2, settings.getMapLocation(), settings.getMaxCycles(), utt, settings.isPartiallyObservable());
        	System.out.print(String.format("\rMatch %8d finished.", i+1));
        }
        
        System.out.println("\nExecuted " + numGames + " matches.");
	}
	
	/**
	 * Runs a match between two AIs, in the specified map and parameters without the GUI.
	 * @param ai1
	 * @param ai2
	 * @param mapFile
	 * @param timeLimit
	 * @param types
	 * @param partiallyObservable
	 * @throws Exception
	 */
    public static void headlessMatch(AI ai1, AI ai2, String mapFile, int timeLimit, UnitTypeTable types, boolean partiallyObservable) throws Exception{
        PhysicalGameState pgs;
		try {
			pgs = PhysicalGameState.load(mapFile, types);
		} catch (Exception e) {
			System.err.println("Error while loading map from file: " + mapFile);
			e.printStackTrace();
			System.err.println("Aborting match execution...");
			return;
		}

        GameState state = new GameState(pgs, types);
        
        boolean gameover = false;
    	
        while (!gameover && state.getTime() < timeLimit) {
        	// initializes state equally for the players 
        	GameState player1State = state; 
        	GameState player2State = state; 
        	
        	// places the fog of war if the state is partially observable
        	if (partiallyObservable) {
        		player1State = new PartiallyObservableGameState(state, 0);
        		player2State = new PartiallyObservableGameState(state, 0);
        	}
        	
        	// retrieves the players' actions
        	PlayerAction player1Action = ai1.getAction(0, player1State);
        	PlayerAction player2Action = ai2.getAction(1, player2State);
			
        	// issues the players' actions
			state.issueSafe(player1Action);
			state.issueSafe(player2Action);

			// runs one cycle of the game
			gameover = state.cycle();
		} 
		ai1.gameOver(state.winner());
		ai2.gameOver(state.winner());
    }

	/**
	 * Loads an {@link AI} according to its name, using the provided UnitTypeTable.
	 * If the AI is {@link MetaBot}, loads it with the configuration file specified in 
	 * entry 'metabot.config' of the received {@link Properties} 
	 * @param aiName
	 * @param utt
	 * @param properties
	 * @return
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 * @throws ClassNotFoundException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 */
	public static AI loadAI(String aiName, UnitTypeTable utt, Properties properties) throws NoSuchMethodException, SecurityException, ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException{
		AI ai;
		
		// (custom) loads MetaBot with its configuration file
		if(aiName.equalsIgnoreCase("metabot.MetaBot")) {
			if(properties.containsKey("metabot.config")){
				ai = new MetaBot(utt, properties.getProperty("metabot.config"));
			}
			else {
				ai = new MetaBot(utt);
			}
			
		}
		else { // (default) loads the AI according to its name
			Constructor<?> cons1 = Class.forName(aiName).getConstructor(UnitTypeTable.class);
			ai = (AI)cons1.newInstance(utt);
		}
		return ai;
	}
}
