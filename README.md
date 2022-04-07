
# GAMESDB

## Project Description

This was for the Project 1 for Revature's Big Data training. This project pulls data from an API with information on video games, and stores the data as json files on a local hdfs. Those files are then used to create external hive tables on the hdfs to perform analysis on. And then in the end the results of the analysis are exported back to the hdfs as json files once again.

## Technologies Used

* Spark 2.3.0
* lihaoyi requests 0.7.0
* lihaoyi ujson 0.7.1
* Hive 3.1.2
* Hadoop 3.3.0

## Features

* Pulls and formats data from an API
* Manages external Hive tables on the HDFS
* Exports analysis results as JSON to the HDFS

## Getting Started
   
* git clone https://github.com/calebreid2829/revature_project1.git
* If on Windows set up a WSL2 environment: https://docs.microsoft.com/en-us/windows/wsl/install
* In the WSL2 environment or Linux system create a Hadoop cluster: https://kontext.tech/column/hadoop/445/install-hadoop-330-on-windows-10-using-wsl
* In the newely created HDFS create the following directories: /user/hive/warehouse/gamesdb
* When you first run the program choose first time set up to download the data from the API
* You are now all set!

## Usage

* Either create a new account or log into an existing one, (as this was just a project for training the user info is shown at start up).
* Then choose which of the analysis options you would like to perform to see the results
