#! /bin/bash
core_pom_path="$1"
dist_pom_path="$2"

core_version=$(xmllint --xpath "/*[local-name()='project']/*[local-name()='version']/text()" $core_pom_path)
if [ $? -eq 0 ]
then
	echo "dhus-core version: $core_version"
else
	echo "failed to retrieve dhus-core version"
	exit 2
fi

dist_version=$(xmllint --xpath "/*[local-name()='project']/*[local-name()='properties']/*[local-name()='dhus.core']/text()" $dist_pom_path)
if [ $? -eq 0 ]
then
	echo "distribution version: $dist_version"
else
	echo "failed to retrieve distribution version"
	exit 2
fi

if [ $core_version == $dist_version ]
then
	echo "dhus-core and distribution version match"
	exit
else
	echo "dhus-core and distribution version mismatch"
	exit 1
fi