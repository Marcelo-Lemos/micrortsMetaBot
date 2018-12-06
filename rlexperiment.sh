#!/bin/bash

classpath=.:bin:lib/:../starcraft/microrts/bin*

echo "Launching experiment..."

java -classpath $classpath -Djava.library.path=lib/ rl.Runner "$@" 

echo "Done."
