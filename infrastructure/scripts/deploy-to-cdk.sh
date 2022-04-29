
BOOTSTRAP="false"
COMPILE="false"

while getopts "cb" options; do
  case "${options}" in
    c)
      COMPILE="true"
      ;;
    b)
      BOOTSTRAP="true"
      ;;
    *)
      ;;
  esac
done

if [[ $COMPILE = "true" ]]
then
  echo "Creating Jar for GetFileFunction"
  cd ../software/GetFileFunction
  gradle shadowJar
  echo "Creating Jar for ListFilesFunction"
  cd ../ListFilesFunction
  gradle shadowJar
  echo "Creating Jar for UploadFileFunction"
  cd ../UploadFileFunction
  gradle shadowJar
  cd ../../infrastructure
  fi

if [[ $BOOTSTRAP = "true" ]]
then
  cdklocal bootstrap
  fi

cdklocal deploy