# dynatrace-dubbo-demo
the project is to provide a demo application to showcase distributed tracing across dubbo application by dynatrace appmon

## build
build project with maven
```sh
mvn clean install
```

## run
1. start Apache Zookeeper.
2. start dubbo-demo-consumer.
```java
cd dynatrace-dubbo-demo\dubbo-demo-consumer\target
java -jar dubbo-demo-consumer-*.RELEASE.jar
```
3. start dubbo-demo-provider.
```java
cd dynatrace-dubbo-demo\dubbo-demo-provider\target
java -jar dubbo-demo-provider-*.RELEASE.jar
```

## test
```
http://localhost:8080/dubbo/home
```

## monitor (optional)
to mintor the application with dynatrace appmon. 
add jvm argument to dubbo consumer.
```sh
-agentpath:"C:\Program Files\dynaTrace\agent\lib64\dtagent.dll"=name=dubbo-consumer,server=localhost
```
add jvm argument to dubbo provider.
```sh
-agentpath:"C:\Program Files\dynaTrace\agent\lib64\dtagent.dll"=name=dubbo-provider,server=localhost
```

## topology
automatical discovered topology by dynatrace appmon
![TransactionFlow](https://github.com/DeanWade/deanwade.github.io/blob/master/image/dynatrace/dubbo/TransactionFlow.png)

## method level tracing
![TransactionFlow](https://github.com/DeanWade/deanwade.github.io/blob/master/image/dynatrace/dubbo/PurePath.png)

