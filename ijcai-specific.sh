#!/bin/bash

EXP_DIR="ijcai-experiments/specific"
TRAIN_TEST_PATH=train-test.sh
N_REP=5

echo "Launching IJCAI Specific experiments."
echo

echo "Launching AHTN Specific experiments."
echo

echo $TRAIN_TEST_PATH -n $N_REP -d $EXP_DIR/AHTN/results -trc $EXP_DIR/AHTN/train.properties -tsc $EXP_DIR/AHTN/test.properties
. $TRAIN_TEST_PATH -n $N_REP -d $EXP_DIR/AHTN/results -trc $EXP_DIR/AHTN/train.properties -tsc $EXP_DIR/AHTN/test.properties

echo "Launching AHTN Specific experiments."
echo

echo "Launching NaiveMCTS Specific experiments."
echo

echo $TRAIN_TEST_PATH -n $N_REP -d $EXP_DIR/NaiveMCTS/results -trc $EXP_DIR/NaiveMCTS/train.properties -tsc $EXP_DIR/NaiveMCTS/test.properties
. $TRAIN_TEST_PATH -n $N_REP -d $EXP_DIR/NaiveMCTS/results -trc $EXP_DIR/NaiveMCTS/train.properties -tsc $EXP_DIR/NaiveMCTS/test.properties

echo "Launching NaiveMCTS Specific experiments."
echo

echo "Launching PuppetAB Specific experiments."
echo

echo $TRAIN_TEST_PATH -n $N_REP -d $EXP_DIR/PuppetAB/results -trc $EXP_DIR/PuppetAB/train.properties -tsc $EXP_DIR/PuppetAB/test.properties
. $TRAIN_TEST_PATH -n $N_REP -d $EXP_DIR/PuppetAB/results -trc $EXP_DIR/PuppetAB/train.properties -tsc $EXP_DIR/PuppetAB/test.properties

echo "Launching PuppetAB Specific experiments."
echo

echo "Launching PuppetMCTS Specific experiments."
echo

echo $TRAIN_TEST_PATH -n $N_REP -d $EXP_DIR/PuppetMCTS/results -trc $EXP_DIR/PuppetMCTS/train.properties -tsc $EXP_DIR/PuppetMCTS/test.properties
. $TRAIN_TEST_PATH -n $N_REP -d $EXP_DIR/PuppetMCTS/results -trc $EXP_DIR/PuppetMCTS/train.properties -tsc $EXP_DIR/PuppetMCTS/test.properties

echo "Launching PuppetMCTS Specific experiments."
echo

echo "Launching StrategyTactics Specific experiments."
echo

echo $TRAIN_TEST_PATH -n $N_REP -d $EXP_DIR/StrategyTactics/results -trc $EXP_DIR/StrategyTactics/train.properties -tsc $EXP_DIR/StrategyTactics/test.properties
. $TRAIN_TEST_PATH -n $N_REP -d $EXP_DIR/StrategyTactics/results -trc $EXP_DIR/StrategyTactics/train.properties -tsc $EXP_DIR/StrategyTactics/test.properties

echo "Launching StrategyTactics Specific experiments."
echo

echo "IJCAI Specific experiments done."
echo
