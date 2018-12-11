package rl;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Date;
import java.util.Properties;

import ai.core.AI;
import config.ConfigLoader;
import metabot.MetaBot;
import rts.GameSettings;
import rts.GameState;
import rts.PartiallyObservableGameState;
import rts.PhysicalGameState;
import rts.PlayerAction;
import rts.Trace;
import rts.TraceEntry;
import rts.units.UnitTypeTable;
import util.XMLWriter;
import utils.FileNameUtil;

/**
 * A class to run microRTS games to train and test MetaBot
 * TODO make an utilitary to grab the next available file number 
 * @author anderson
 */
public class Runner {

	public static final int MATCH_ERROR = 2;
	public static final int DRAW = -1;
	public static final int P1_WINS = 0;
	public static final int P2_WINS = 1;
	
	public static void main(String[] args) throws Exception {
		Properties prop = new Properties();
		String configFile;
		if(args.length > 0){
			configFile = args[0];
		}
		else {
			System.out.println("Input not specified, reading from 'config/microrts.properties'");
			System.out.println("args: " + Arrays.toString(args));
			configFile = "config/microrts.properties";
		}
		
		// loads the two forms of configuration object
        prop = ConfigLoader.loadConfig(configFile);
		GameSettings settings = GameSettings.loadFromConfig(prop);
		System.out.println(settings);
		
		UnitTypeTable utt = new UnitTypeTable(settings.getUTTVersion(), settings.getConflictPolicy());
        AI ai1 = loadAI(settings.getAI1(), utt, prop);
        AI ai2 = loadAI(settings.getAI2(), utt, prop);
        
        int numGames = Integer.parseInt(prop.getProperty("runner.num_games", "1"));
        
        for(int i = 0; i < numGames; i++){
        	Date begin = new Date(System.currentTimeMillis());
        	int result = headlessMatch(ai1, ai2, settings.getMapLocation(), settings.getMaxCycles(), utt, settings.isPartiallyObservable());
        	Date end = new Date(System.currentTimeMillis());
        	
        	System.out.print(String.format("\rMatch %8d finished.", i+1));
        	
        	long duration = end.getTime() - begin.getTime();
        	
        	if (prop.containsKey("runner.output")){
        		try{
        			outputSummary(prop.getProperty("runner.output"), result, duration, begin, end);
        		}
        		catch(IOException ioe){
        			System.err.println("Error while trying to write summary to '" + prop.getProperty("runner.output") + "'");
        			ioe.printStackTrace();
        		}
        		
        	}
        	
        	
        	ai1.reset();
        	ai2.reset();
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
    public static int headlessMatch(AI ai1, AI ai2, String mapFile, int timeLimit, UnitTypeTable types, boolean partiallyObservable) throws Exception{
        PhysicalGameState pgs;
		try {
			pgs = PhysicalGameState.load(mapFile, types);
		} catch (Exception e) {
			System.err.println("Error while loading map from file: " + mapFile);
			e.printStackTrace();
			System.err.println("Aborting match execution...");
			return MATCH_ERROR;
		}

		GameState state = new GameState(pgs, types);
		
		// creates the trace logger
		Trace replay = new Trace(types);
        
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
        	
        	// creates a new trace entry, fills the actions and stores it
        	TraceEntry thisFrame = new TraceEntry(state.getPhysicalGameState().clone(), state.getTime());
        	if (!player1Action.isEmpty()){
        		thisFrame.addPlayerAction(player1Action.clone());
        	}
        	if (!player2Action.isEmpty()) {
                thisFrame.addPlayerAction(player2Action.clone());
            }
        	replay.addEntry(thisFrame);

			
        	// issues the players' actions
			state.issueSafe(player1Action);
			state.issueSafe(player2Action);

			// runs one cycle of the game
			gameover = state.cycle();
		} 
		ai1.gameOver(state.winner());
		ai2.gameOver(state.winner());
		
		//traces the final state
		replay.addEntry(new TraceEntry(state.getPhysicalGameState().clone(), state.getTime()));
		
		//writes the trace
		String output = "/dev/null";
		Properties prop = ConfigLoader.getConfiguration();
		
		if(prop.containsKey("runner.trace_prefix")){
			// finds the file name
    		output = FileNameUtil.nextAvailableFileName(
				prop.getProperty("runner.trace_prefix"), "trace"
			);
    		
		}
		XMLWriter xml = new XMLWriter(new FileWriter(output));
        replay.toxml(xml);
        xml.flush();
		
		return state.winner();
    }

    
    public static void outputSummary(String path, int result, long duration, Date start, Date finish) throws IOException{
    	File f = new File(path);
		FileWriter writer; 
		
    	if(!f.exists()){ // creates a new file and writes the header
    		writer = new FileWriter(f, false); //must be after the test, because it creates the file upon instantiation
    		writer.write("#result,duration(ms),initial_time,final_time\n");
    		writer.close();
    	}
    	
    	// appends one line with each weight value separated by a comma
    	writer = new FileWriter(f, true); 
    	writer.write(String.format("%d,%d,%s,%s\n", result, duration, start, finish));
    	
    	writer.close();
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
