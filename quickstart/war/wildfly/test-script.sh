#!/bin/bash
for i in `seq 1 100`;
        do
            curl http://war-wildfly-default.vagrant.f8/api/greet/fabric8
            sleep 5
            echo
        done
