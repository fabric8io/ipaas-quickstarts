#!/bin/bash
for i in `seq 1 100`;
        do
            curl http://wildfly-camel-cdi-default.vagrant.f8
            sleep 5
            echo
        done
