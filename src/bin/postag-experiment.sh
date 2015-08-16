#!/bin/bash

set -e

export JAR=target/sectionextractor-1.0-SNAPSHOT-jar-with-dependencies.jar
export BASE=/Users/alessio/Documents/Resources/postag-ita/crfsuite

export TRAINING_FILE=$BASE/training.txt
export TEST_FILE=$BASE/test.txt
export MODEL_FILE=$BASE/tmp-model.txt
export RES_FILE=$BASE/tmp-res.txt

export OUTPUT_FILE=$BASE/results.txt

ALGORITHMS=(lbfgs l2sgd ap pa arow)

> $OUTPUT_FILE

for i in "${ALGORITHMS[@]}"
do
    echo $i >> $OUTPUT_FILE
    crfsuite learn -a $i -m $MODEL_FILE $TRAINING_FILE
    crfsuite tag -r -m $MODEL_FILE $TEST_FILE > $RES_FILE
    java -cp $JAR eu.fbk.dkm.sectionextractor.itapos.SimpleEvaluation -i $RES_FILE >> $OUTPUT_FILE
done
