#!/bin/bash
java -Xmx30g -cp Cydime.jar:. gov.ameslab.cydime.aggregate.CydimeAggregator 2015/07/31 2
cp 2015/07/31/model/ip/aggregated.norm.id 2015/07/31/2days.id

for DAYS in 02 03 04 05 06 07 08 09 10 11 12 13 14 15 16 17 18 19 20
do
    java -Xmx30g -cp Cydime.jar:. gov.ameslab.cydime.aggregate.CydimeAggregator 2015/07/31 ${DAYS} 2015/07/31/2days.id
    java -Xmx30g -cp Cydime.jar:. gov.ameslab.cydime.aggregate.CydimeRankerAggExp 2015/07/31 100
    cp 2015/07/31/report/ip/summary_00label100.csv 2015/07/summary_${DAYS}.csv
done

java -Xmx30g -cp Cydime.jar:. gov.ameslab.cydime.aggregate.CydimeRankerAggExpResult 1 > 2015/07/ap_d2.csv
java -Xmx30g -cp Cydime.jar:. gov.ameslab.cydime.aggregate.CydimeRankerAggExpResult 4 > 2015/07/ndcg_d2.csv
