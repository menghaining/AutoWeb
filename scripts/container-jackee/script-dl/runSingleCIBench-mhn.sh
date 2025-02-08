#!/usr/bin/env bash                                                             

if [ -f "${DOOP_HOME}/build/install/doop/bin/doop" ]; then
    DOOP="${DOOP_HOME}/build/install/doop/bin/doop"
else if [ -f "${DOOP_HOME}/doopOffline" ]; then
         DOOP="${DOOP_HOME}/doopOffline"
     else
         echo "Please set environment variable DOOP_HOME."
         exit
     fi
fi
echo "Using Doop: ${DOOP}"

function bench() {
    local BENCHMARK="$1"
    local ANALYSIS="context-insensitive"
    # local ID="$BENCHMARK-$ANALYSIS"
    ID="$BENCHMARK-mhn-$ANALYSIS"
    local FACTS="$BENCHMARK-facts"
    pushd ${DOOP_HOME}
    set -x
    ${DOOP} --Xstart-after-facts "/home/pldi2020/benchmark-facts/${FACTS}" -a ${ANALYSIS} --id ${ID} --souffle-jobs 16 --open-programs mhn0822  --Xsymlink-cached-facts --timeout 100000|& tee ${ID}.log
    #${DOOP} --Xstart-after-facts "/home/pldi2020/benchmark-facts/${FACTS}" -a ${ANALYSIS} --id ${ID} --souffle-jobs 16 --open-programs web-application-mhn-3frameworks-with-heuristic --Xsymlink-cached-facts --timeout 100000|& tee ${ID}.log
    #${DOOP} --Xstart-after-facts "/home/pldi2020/benchmark-facts/${FACTS}" -a ${ANALYSIS} --id ${ID} --souffle-jobs 16 --open-programs web-application --timeout 100000|& tee ${ID}.log
    set +x
    popd
    if [ -f "doop/out/context-insensitive/${ID}/database/Stats_Metrics.csv" ]; then
    	python print_metrics.py |& tee ${ID}-metrics.txt    
    else 
	echo "Error - Analysis failed!"
    fi
}

bench $1
