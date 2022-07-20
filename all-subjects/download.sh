#!/bin/bash

# Use az to download all apps from candidates.csv file

CAN_FILE=../candidates.csv

sha256_list=""

while IFS="," read -r sha256 col_remains
do
    sha256_list+="$sha256,"
done < $CAN_FILE

# remove last ,
sha256_list=${sha256_list::-1}

echo $sha256_list

az --sha256 $sha256_list --threads 20
