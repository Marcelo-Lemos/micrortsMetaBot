#!/bin/bash

SCRIPT_PATH=rlexperiment.sh

DEFAULT_DIR="experiments"
#DEFAULT_BIN_PREFIX="binWeights"
#DEFAULT_HUMAN_PREFIX="humanWeights"

# Argument Parser
POSITIONAL=()
while [[ $# -gt 0 ]]
do
key="$1"

case $key in
    -n|--number-experiments)
    N_EXPERIMENTS=$2
    shift # past argument
    shift # past value
    ;;
    -d|--directory)
    DIR="$2"
    shift # past argument
    ;;
    -trc|--train-config)
    TRAIN_CONFIG="-c $2"
    shift # past argument
    shift # past value
    ;;
    -tsc|--test-config)
    TEST_CONFIG="-c $2"
    shift # past argument
    shift # past value
    ;;
    *)    # unknown option
    POSITIONAL+=("$1") # save it in an array for later
    shift # past argument
    ;;
esac
done
set -- "${POSITIONAL[@]}" # restore positional parameters

echo "Preparing to launch $N_EXPERIMENTS repetition(s)."
echo

# Check if the number of repetitions was set
if [ -z "$N_EXPERIMENTS" ]
then
    echo "Error: Number of repetitions not set. Use -n to set the number of repetitions."
    exit 1
fi

# Check if the directory was set
if [ -z "$DIR" ]
then
    echo "Warning: Directory not set. Using default directory \"$DEFAULT_DIR\". Use -d to set the directory."
    DIR=$DEFAULT_DIR
fi

# Check if the batch size was set
if [ -z "$BATCH_SIZE" ]
then
    echo "Batch size not set. Repetitions will be run serially."
    BATCH_SIZE=1
fi

if [ -z "$TRAIN_CONFIG" ]
then
    echo "Train config file not set."
    exit 1
fi

if [ -z "$TEST_CONFIG" ]
then
    echo "Test config file not set."
    exit 1
fi

echo "Creating directories..."
mkdir -p "$DIR"
for i in $(seq 0 $(($N_EXPERIMENTS-1))); do
    mkdir -p "$DIR"/rep_"$i"
done
echo "Done."
echo

echo "Lauching $N_EXPERIMENTS repetition(s)..."
echo
for i in $(seq 0 $(($N_EXPERIMENTS-1))); do
    WORKING_DIR="rep_$i"
    echo "Lauching train $i."
    echo "$SCRIPT_PATH" -s1 $i -b1 -d1 "$DIR"/"$WORKING_DIR" "$TRAIN_CONFIG" -o "$DIR"/"$WORKING_DIR"/results.txt ## For debugging
    . "$SCRIPT_PATH" -s1 $i -b1 -d1 "$DIR"/"$WORKING_DIR" "$TRAIN_CONFIG" -o "$DIR"/"$WORKING_DIR"/results.txt > "$DIR"/"$WORKING_DIR"/"$WORKING_DIR"-train.log
    echo
    echo "Launchin test $i."
    echo "$SCRIPT_PATH" -s1 $i "$TEST_CONFIG" -o "$DIR"/"$WORKING_DIR"/results.txt -bi1 "$DIR"/"$WORKING_DIR"/weights_0.bin ## For debugging
    . "$SCRIPT_PATH" -s1 $i "$TEST_CONFIG" -o "$DIR"/"$WORKING_DIR"/results.txt -bi1 "$DIR"/"$WORKING_DIR"/weights_0.bin > "$DIR"/"$WORKING_DIR"/"$WORKING_DIR"-test.log
    echo
done
echo "All repetitions done."
