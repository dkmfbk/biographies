#!/bin/bash

set -e

export JAR=target/sectionextractor-1.0-SNAPSHOT-jar-with-dependencies.jar
export BASE=/Users/alessio/Documents/Resources/postag-ita/crfsuite

export TRAINING_FILE=$BASE/training.txt
export TEST_FILE=$BASE/test.txt
export GAZZETTINI_FOLDER=/Users/alessio/Documents/Resources/postag-ita/gazzettini

export ORIG_TRAINING=/Users/alessio/Documents/Resources/postag-ita/TagPro_corpora/ITA/DataSet/Elsnet-Training
export ORIG_TEST=/Users/alessio/Documents/Resources/postag-ita/TagPro_corpora/ITA/DataSet/Elsnet-Test

java -cp $JAR eu.fbk.dkm.sectionextractor.itapos.CreateTraining \
    -i $ORIG_TRAINING \
    -o $TRAINING_FILE \
    -jobs $GAZZETTINI_FOLDER/PROFESSIONI.txt \
    -places $GAZZETTINI_FOLDER/place-names-it_normalized.txt \
    -orgs $GAZZETTINI_FOLDER/org-names-it_normalized.txt \
    -nam $GAZZETTINI_FOLDER/it-names.txt \
    -sur $GAZZETTINI_FOLDER/it-surnames.txt \
    -s 2

java -cp $JAR eu.fbk.dkm.sectionextractor.itapos.CreateTraining \
    -i $ORIG_TEST \
    -o $TEST_FILE \
    -jobs $GAZZETTINI_FOLDER/PROFESSIONI.txt \
    -places $GAZZETTINI_FOLDER/place-names-it_normalized.txt \
    -orgs $GAZZETTINI_FOLDER/org-names-it_normalized.txt \
    -nam $GAZZETTINI_FOLDER/it-names.txt \
    -sur $GAZZETTINI_FOLDER/it-surnames.txt \
    -s 2
