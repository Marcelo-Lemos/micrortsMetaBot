# micrortsMetaBot
microRTS bot implementing SARSA with Linear Function Approximation

This is somewhat a refactoring of https://github.com/SivaAnbalagan1/micrortsFA to provide a simpler codebase to a microRTS bot with
Sarsa + linear function approximation.

An experiment is executed by running `./rlexperiment.sh configfile` where configfile is a file formatted with Java .properties style.

The listing below shows an example of such a file with the parameters to configure (taken from config/metabot.properties, might be outdated):

```
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
