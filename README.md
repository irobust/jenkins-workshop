# Jenkins Administration
## Step to install jenkins
1. Install Docker for Desktop (https://www.docker.com/products/docker-desktop)
2. Build and run jenkins images
```
    docker pull jenkins/jenkins:lts-jdk11 
    docker run -p 8080:8080 -p 50000:50000 jenkins/jenkins:lts-jdk11
```
3. Get default credentials
```
    docker exec jenkins-server cat /var/jenkins_home/secrets/initialAdminPassword
```

4. Install suggested plug-ins
5. create admin user
6. Create Freestyle project

## Create First Project(Job)
### Hello Jenkins
Linux/Mac
Create a build step using “Execute Shell” and copy in the following commands:

```
#!/bin/bash
for i in {1..30}; do 
  echo $i; 
  sleep 1; 
done
```


Windows
Create a build step using “Execute Windows batch command” and copy in the following commands:

```
for /L %%i in (1,1,30) do (
  @echo %%i
)
```

### Use Environment Variable
Linux/Mac:
```
#!/bin/bash
echo "VERSION NUMBER: $JENKINS_VERSION"
```


Windows:
```
echo "VERSION NUMBER: %VERSION_NUMBER%"
```

### Using Jenkins Variable
Linux/Mac:
```
#!/bin/bash
echo "BUILD_NUMBER: $BUILD_NUMBER"
```


Windows:
```
echo "BUILD_NUMBER: %BUILD_NUMBER%"
```


### Parameterize Job

Linux/Mac:
```
#!/bin/bash
echo "VERSION NUMBER: $JENKINS_VERSION
echo "RUN_TESTS:      $RUN_TESTS"
echo
if [ "$RUN_TESTS" = "true" ];
then
    echo "Running Tests!";
else
    echo "Not Testing!";
fi
```

Windows:
```
echo "VERSION NUMBER: %VERSION_NUMBER%"
echo "RUN_TESTS:      %RUN_TESTS%"
echo
if "%RUN_TESTS%"=="true" (
    echo "Running Tests!"
) else (
    echo "Not Testing!"
)
```

## Build with Ant
1. Goto Global Configuration Tools
2. Add Ant Installer
3. Creaet free style project
4. Source Code Management(SCM)
   Set the following options
```
Repo URL = https://github.com/apache/ant-ivy
branch = */master
```
3. Build section using "Invoke Ant"
```
Ant version = Ant installer name that created in step 2
```
4. Save and Build


## Build with Maven
1. Install Maven Invoke plugins
2. Goto Global Configuration Tools
3. Add Maven Installer
4. Create Maven project
5. Source Code Management(SCM)
   Set the following options
```
Repo URL = https://github.com/jglick/simple-maven-project-with-tests.git
branch = */master
```
6. Save and Build