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
 *
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
		
		UnitTypeTable utt = new UnitTypeTable(settings.getUTTVersion(), settings.getConflictPolicy());
        PhysicalGameState pgs = null;
		pgs = PhysicalGameState.load(settings.getMapLocation(), utt);

        GameState gs = new GameState(pgs, utt);
        //int PERIOD = 20;
        boolean gameover = false;
        
        AI ai1 = loadAI(settings.getAI1(), utt, prop);
        AI ai2 = loadAI(settings.getAI2(), utt, prop);

        //JFrame w = PhysicalGameStatePanel.newVisualizer(gs,640,640,settings.isPartiallyObservable(),
        //                                                PhysicalGameStatePanel.COLORSCHEME_BLACK);

        //long nextTimeToUpdate = System.currentTimeMillis() + PERIOD;
        do{
            //if (System.currentTimeMillis()>=nextTimeToUpdate) {
                if (settings.isPartiallyObservable()) {
                    PlayerAction pa1 = ai1.getAction(0, new PartiallyObservableGameState(gs,0));
                    PlayerAction pa2 = ai2.getAction(1, new PartiallyObservableGameState(gs,1));            
                    gs.issueSafe(pa1);
                    gs.issueSafe(pa2);
                } else {
                    PlayerAction pa1 = ai1.getAction(0, gs);
                    PlayerAction pa2 = ai2.getAction(1, gs);
                    gs.issueSafe(pa1);
                    gs.issueSafe(pa2);
                }

                // simulate:
                gameover = gs.cycle();
           //     w.repaint();
           //     nextTimeToUpdate+=PERIOD;
           /* } else {
                try {
                    Thread.sleep(1);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }*/
        }while(!gameover && gs.getTime()<settings.getMaxCycles());
        ai1.gameOver(gs.winner());
        ai2.gameOver(gs.winner());    

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
