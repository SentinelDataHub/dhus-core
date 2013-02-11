#! /bin/sh
cd "/home/$1/go-dhus-environment/dhus" \
&& ./start.sh > /dev/null 2>&1 \
& java -jar "/home/$1/go-dhus-environment/dhus-listener.jar" 300 "/home/$1/go-dhus-environment/dhus/dhus.log" "$2"
