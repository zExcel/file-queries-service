#!/usr/bin/bash

RUNNING_LOCALLY="true"

OPTIND=1
while getopts "l:" options; do
  case "${options}" in
    l)
      RUNNING_LOCALLY=$OPTARG
      ;;
    *)
      ;;
  esac
done

echo "Exporting environmental variables..."
export AWS_REGION=us-east-1
export AWS_DEFAULT_REGION=us-east-1
export AWS_ACCESS_KEY_ID=testAccessKey
export AWS_SECRET_ACCESS_KEY=testSecretKey
export LOCALSTACK_REGION=us-east-1
export ENVIRONMENT=local

if [[ $RUNNING_LOCALLY = "false" ]]
then
  echo "Using localstack"
  export LOCALSTACK_ENDPOINT=http://localstack:4566
  export LOCALSTACK_HOST=localstack
else
  echo "Using localhost"
  export LOCALSTACK_ENDPOINT=http://localhost:4566
  export LOCALSTACK_HOST=localhost
  fi