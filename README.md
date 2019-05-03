# micrortsMetaBot
microRTS bot implementing SARSA with Linear Function Approximation

This is somewhat a refactoring of https://github.com/SivaAnbalagan1/micrortsFA to provide a simpler codebase to a microRTS bot with
Sarsa + linear function approximation.

An experiment is executed by running `./rlexperiment.sh configfile` where configfile is a file formatted with Java .properties style.

The listing below shows an example of such a file with the parameters to configure (taken from config/microrts.properties, might be outdated). The listing specifies MetaBot as player 1 and NaiveMCTS as player 2. Specific parameters of MetaBot are specified in a separate file. To see how to configure, check the Readme.md at `config/` in this project.

```
# STANDALONE = Starts MicroRTS as a standalone instance (No remote agents).
# GUI = Starts the microRTS GUI.
# SERVER = Starts MicroRTS as a server instance.
# CLIENT = Starts MicroRTS as a client instance.
launch_mode=STANDALONE

### NETWORKING ###
# Only needed if modes are SERVER/CLIENT
# server_address=127.0.0.1
# server_port=9898
# 1 = XML
# 2 = JSON
# serialization_type=2

# MAP
map_location=maps/24x24/basesWorkers24x24.xml

# number of games to play
num_games=1

#### GAME SETTINGS ###

# The max number of cycles the game will perform.
max_cycles=3000

# If false, the players have full vision of the map.
partially_observable=false

# Versions of the Unit Type Table (DEFAULT = 2)
# 1 = original
# 2 = original finetuned
# 3 = non-deterministic version of original finetuned (damages are random)
UTT_version=2

# Conflict policies (DEFAULT = 1)
# 1 = A conflict resolution policy where move conflicts cancel both moves
# 2 = A conflict resolution policy where move conflicts are solved randomly
# 3 = A conflict resolution policy where move conflicts are solved by alternating the units trying to move
conflict_policy=1

# a file to write match results
runner.output=summary.csv

### STANDALONE Settings ###
# Only needed if mode is STANDALONE
# Set which AIs will play
AI1=metabot.MetaBot
AI2=ai.mcts.naivemcts.NaiveMCTS

### metabot settings ###
player1.config=config/metabot.properties
```
