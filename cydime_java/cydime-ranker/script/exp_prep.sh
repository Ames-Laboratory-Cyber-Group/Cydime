#!/bin/bash
java -Xmx30g -cp Cydime.jar:. gov.ameslab.cydime.aggregate.CydimeRankerAggExpHostMerge 2015/07/31 20 2015/07/ipHostMap.csv
find 2015/07 -mindepth 1 -maxdepth 1 -type d -exec cp 2015/07/ipHostMap.csv {}/preprocess/ \;

java -Xmx30g -cp Cydime.jar:. gov.ameslab.cydime.preprocess.CydimePreprocessor 2015/07/06
java -Xmx30g -cp Cydime.jar:. gov.ameslab.cydime.preprocess.CydimePreprocessor 2015/07/07
java -Xmx30g -cp Cydime.jar:. gov.ameslab.cydime.preprocess.CydimePreprocessor 2015/07/08
java -Xmx30g -cp Cydime.jar:. gov.ameslab.cydime.preprocess.CydimePreprocessor 2015/07/09
java -Xmx30g -cp Cydime.jar:. gov.ameslab.cydime.preprocess.CydimePreprocessor 2015/07/10
java -Xmx30g -cp Cydime.jar:. gov.ameslab.cydime.preprocess.CydimePreprocessor 2015/07/13
java -Xmx30g -cp Cydime.jar:. gov.ameslab.cydime.preprocess.CydimePreprocessor 2015/07/14
java -Xmx30g -cp Cydime.jar:. gov.ameslab.cydime.preprocess.CydimePreprocessor 2015/07/15
java -Xmx30g -cp Cydime.jar:. gov.ameslab.cydime.preprocess.CydimePreprocessor 2015/07/16
java -Xmx30g -cp Cydime.jar:. gov.ameslab.cydime.preprocess.CydimePreprocessor 2015/07/17
java -Xmx30g -cp Cydime.jar:. gov.ameslab.cydime.preprocess.CydimePreprocessor 2015/07/20
java -Xmx30g -cp Cydime.jar:. gov.ameslab.cydime.preprocess.CydimePreprocessor 2015/07/21
java -Xmx30g -cp Cydime.jar:. gov.ameslab.cydime.preprocess.CydimePreprocessor 2015/07/22
java -Xmx30g -cp Cydime.jar:. gov.ameslab.cydime.preprocess.CydimePreprocessor 2015/07/23
java -Xmx30g -cp Cydime.jar:. gov.ameslab.cydime.preprocess.CydimePreprocessor 2015/07/24
java -Xmx30g -cp Cydime.jar:. gov.ameslab.cydime.preprocess.CydimePreprocessor 2015/07/27
java -Xmx30g -cp Cydime.jar:. gov.ameslab.cydime.preprocess.CydimePreprocessor 2015/07/28
java -Xmx30g -cp Cydime.jar:. gov.ameslab.cydime.preprocess.CydimePreprocessor 2015/07/29
java -Xmx30g -cp Cydime.jar:. gov.ameslab.cydime.preprocess.CydimePreprocessor 2015/07/30
java -Xmx30g -cp Cydime.jar:. gov.ameslab.cydime.preprocess.CydimePreprocessor 2015/07/31
