BOOTSTRAP="false"
COMPILE="false"
JUST_CODE_CHANGES="false"

while getopts "cbJ" options; do
  case "${options}" in
    c)
      COMPILE="true"
      ;;
    b)
      BOOTSTRAP="true"
      ;;
    J)
      JUST_CODE_CHANGES="true"
      ;;
    *)
      ;;
  esac
done

if [[ $JUST_CODE_CHANGES = "true" ]]
then
  source scripts/local-setup.sh
  INFRA_DIR=$(pwd)
  awslocal lambda update-function-code --function-name FunctionHandler --zip-file fileb://$INFRA_DIR/../software/build/libs/software-1.0-all.jar
  source scripts/local-setup.sh -l false
  echo "Uploaded Jars for all functions"
  docker kill "$(docker ps --filter "name=FunctionHandler" -q)" # In case docker-reuse is being used, don't want to use old containers.
  exit
  fi

if [[ $COMPILE = "true" ]]
then
  echo "Creating the Shadow Jar"
  cd ../software
  gradle shadowJar
  fi

if [[ $BOOTSTRAP = "true" ]]
then
  echo "Bootstrapping"
  cdklocal bootstrap
  fi

cdklocal deploy