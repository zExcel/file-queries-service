../gradlew bootjar
java -Dspring.profiles.active=local -jar "build/libs/infrastructure-1.0.jar"