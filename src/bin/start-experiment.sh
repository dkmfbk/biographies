#!/bin/bash

set -e

EXPERIMENT=$1
JAR=target/fssa-0.1-SNAPSHOT-jar-with-dependencies.jar

FOLDER=/Users/alessio/Documents/Resources/pantheon/en

#TRAINING_FILE=$FOLDER/dataset/train-dataset.txt
#TRAINING_FILE=$FOLDER/dataset/train-10k-dataset.txt
TRAINING_FILE=$FOLDER/dataset/train-15k-dataset.txt
#TRAINING_FILE=$FOLDER/dataset/train-20k-dataset.txt
#TRAINING_FILE=$FOLDER/dataset/train-25k-dataset.txt
#TRAINING_FILE=$FOLDER/dataset/train-30k-dataset.txt
#TRAINING_FILE=$FOLDER/dataset/train-50k-dataset.txt
#TRAINING_FILE=$FOLDER/dataset/train-75k-dataset.txt
#TRAINING_FILE=$FOLDER/dataset/train-100k-dataset.txt
#TRAINING_FILE=$FOLDER/dataset/train-150k-dataset.txt
#TRAINING_FILE=$FOLDER/dataset/train-200k-dataset.txt
#TRAINING_FILE=$FOLDER/en-sections.txt
#GOLD_FILE=$FOLDER/dataset/dev-dataset-gold.csv
#GOLD_FILE=$FOLDER/dataset/dev-dataset-gold-pl.csv
GOLD_FILE=$FOLDER/dataset/test-dataset-gold-pl.csv
#DATASET=$FOLDER/dataset/dev-dataset.txt
DATASET=$FOLDER/dataset/test-dataset.txt

mkdir -p $FOLDER/dataset/experiment$EXPERIMENT
#mvn package

### PROCESS

#    -d $FOLDER/en-birthdeath.txt \

java -cp $JAR eu.fbk.fssa.simple.ParseTraining \
    -i $TRAINING_FILE \
    -o $FOLDER/dataset/experiment$EXPERIMENT/crfsuite-train.txt \
    -s 1
java -cp $JAR eu.fbk.fssa.simple.ParseTraining \
    -i $DATASET \
    -o $FOLDER/dataset/experiment$EXPERIMENT/crfsuite-dev.txt \
    -g $GOLD_FILE \
    -s 1 -t

### TRAINING

cd $FOLDER/dataset/experiment$EXPERIMENT/
crfsuite learn -a lbfgs -m model crfsuite-train.txt
crfsuite tag -r -m model crfsuite-dev.txt > dev-results.txt
cd -

### POST-PROCESS

java -cp $JAR eu.fbk.fssa.simple.Postprocess \
    -i $DATASET \
    -o $FOLDER/dataset/experiment$EXPERIMENT/dev-results-final.txt \
    -r $FOLDER/dataset/experiment$EXPERIMENT/dev-results.txt

### BASELINE

java -cp $JAR eu.fbk.fssa.simple.Baseline \
    -i $DATASET \
    -o $FOLDER/dataset/experiment$EXPERIMENT/baseline.txt \
    -g $GOLD_FILE \
    -s contains

sed '/^$/d' $FOLDER/dataset/experiment$EXPERIMENT/dev-results.txt > $FOLDER/dataset/experiment$EXPERIMENT/dev-results-noblanks.txt
paste $FOLDER/dataset/experiment$EXPERIMENT/dev-results-noblanks.txt $DATASET > $FOLDER/dataset/experiment$EXPERIMENT/dev-results-errors.txt
sed '/^$/d' $FOLDER/dataset/experiment$EXPERIMENT/dev-results-final.txt > $FOLDER/dataset/experiment$EXPERIMENT/dev-results-final-noblanks.txt
paste $FOLDER/dataset/experiment$EXPERIMENT/dev-results-final-noblanks.txt $DATASET > $FOLDER/dataset/experiment$EXPERIMENT/dev-results-final-errors.txt
sed '/^$/d' $FOLDER/dataset/experiment$EXPERIMENT/baseline.txt > $FOLDER/dataset/experiment$EXPERIMENT/baseline-noblanks.txt
paste $FOLDER/dataset/experiment$EXPERIMENT/baseline-noblanks.txt $DATASET > $FOLDER/dataset/experiment$EXPERIMENT/baseline-errors.txt

echo "---"

rm -f $FOLDER/dataset/experiment$EXPERIMENT/log.txt
touch $FOLDER/dataset/experiment$EXPERIMENT/log.txt

echo "RESULTS" | tee -a $FOLDER/dataset/experiment$EXPERIMENT/log.txt
java -cp $JAR eu.fbk.fssa.yamcha.Evaluation \
    -i $FOLDER/dataset/experiment$EXPERIMENT/dev-results.txt | tee -a $FOLDER/dataset/experiment$EXPERIMENT/log.txt
echo "AFTER POSTPROCESSING" | tee -a $FOLDER/dataset/experiment$EXPERIMENT/log.txt
java -cp $JAR eu.fbk.fssa.yamcha.Evaluation \
    -i $FOLDER/dataset/experiment$EXPERIMENT/dev-results-final.txt | tee -a $FOLDER/dataset/experiment$EXPERIMENT/log.txt
echo "BASELINE" | tee -a $FOLDER/dataset/experiment$EXPERIMENT/log.txt
java -cp $JAR eu.fbk.fssa.yamcha.Evaluation \
    -i $FOLDER/dataset/experiment$EXPERIMENT/baseline.txt | tee -a $FOLDER/dataset/experiment$EXPERIMENT/log.txt
