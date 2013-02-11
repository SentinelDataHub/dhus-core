#! /bin/sh
cd /home/$GO_DHUS_ACCOUNT/go-dhus-environment \
&& rm -rf ./dhus \
&& mkdir ./dhus \
&& unzip -o dhus-software-distribution.zip -d ./dhus \
&& sed -i "s|local_dhus|/home/$GO_DHUS_ACCOUNT/go-dhus-environment/dhus/local_dhus|" dhus/etc/dhus.xml \
&& cp -r /home/$GO_DHUS_ACCOUNT/go-dhus-data/local_dhus ./dhus \
&& chmod +x ./dhus/start.sh ./dhus/stop.sh ./start-listen.sh
