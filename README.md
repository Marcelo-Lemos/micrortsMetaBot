# micrortsMetaBot
microRTS bot implementing SARSA with Linear Function Approximation for algorithm selection.

This is somewhat a refactoring of https://github.com/SivaAnbalagan1/micrortsFA to provide a simpler codebase to a microRTS bot with Sarsa + linear function approximation.

We call it MetaBot because it selects an algorithm to play in its behalf at each game frame (instead of searching in the huge action space).

## Running an experiment

An experiment is executed by running `./rlexperiment.sh configfile` where configfile is a file formatted with Java .properties style.

The listing below shows an example of such a file with the parameters to configure (taken from config/microrts.properties, might be outdated). The listing specifies MetaBot as player 1 and NaiveMCTS as player 2. Specific parameters of MetaBot are specified in a separate file. To see how to configure, check the section below. Note that you can configure MetaBot to play against itself (self-play) by setting both player 1 and 2 as MetaBot. In this case, make sure you specify two different configuration files for each instance of MetaBot so that they write their weights to different paths.

```properties
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
# Set which AIs will play (must put the full class name)
AI1=metabot.MetaBot
AI2=ai.mcts.naivemcts.NaiveMCTS

### metabot settings ###
player1.config=config/metabot.properties
```

## Configuring MetaBot

The listing below is an example of config. file for MetaBot:

```properties
# specifies the portfolio members
portfolio.members = WorkerRush, LightRush, RangedRush, HeavyRush, Expand, BuildBarracks

### the parameters below are related to the reinforcement learning algorithm ###
# specifies the type of learning agent
rl.agent = "sarsa"

# the initial value and decay of exploration rate (epsilon is multiplied by this decay factor after each episode)
rl.epsilon.initial = 0.1
rl.epsilon.decay = 1

# the initial value and decay of learning rate (alpha is multiplied by this decay factor after each episode)
rl.alpha.initial = 0.1
rl.alpha.decay = 1

# Note: setting the decay rates to 1 makes the parameters constant throughout all episodes

# the discount factor
rl.gamma = 0.9

# eligibility trace (not used yet)
rl.lambda = 0

# the feature extractor
rl.feature.extractor = quadrant_model

# the map is divided in quadrant_division x quadrant_division quadrants
# this parameter is specific of the quadrant_model
rl.feature.extractor.quadrant_division = 3

# the random seed (if not specified, it will load the default seed)
rl.random.seed = 1

# the prefix of the output file to save weights
rl.output.binprefix = training/binweights-dryrun

# the prefix of the output file to save weights in human-readable format
rl.output.humanprefix = training/weights-dryrun
```
