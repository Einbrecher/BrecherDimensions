@echo off
echo Running Brecher's Dimensions Development Server...
echo.
echo If you encounter module conflicts, try these solutions:
echo 1. Clean the build: gradlew clean
echo 2. Refresh dependencies: gradlew --refresh-dependencies
echo 3. Use the alternative run command below
echo.

REM Standard run command
gradlew runServer

REM If the above fails with module conflicts, uncomment and try this:
REM gradlew runServer --args="--fml.forgeGroup=net.minecraftforge"