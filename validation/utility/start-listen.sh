#! /bin/sh
cd /home/dhus/go-dhus-environment/dhus \
&& ./start.sh > /dev/null 2>&1 \
& java -jar /home/dhus/go-dhus-environment/dhus-listener.jar 300 /home/dhus/go-dhus-environment/dhus/dhus.log