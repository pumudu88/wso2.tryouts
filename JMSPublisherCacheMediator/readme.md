## JMS Publisher Cache Mediator for WSO2 ESB + WSO2 MB integration with JMS Topics/Queues

### Purpose

The default JMS transport in WSO2 ESB 4.8.1 does not support re-use of JMS sessions when publishing to the same destination multiple times. In the perspective of the message broker, this involves creation/disconnection of JMS sessions per each published message, introducing an unnecessary performance and memory overhead. 

There is a workaround to enable caching the session by configuring the ESB as per article [1]. However, this involves having a bulky configuration for each destination and requires the user to know all the destination names before using the application. 

This custom mediator enables re-use of JMS sessions specifically for JMS topics/queues that are not known beforehand (in a use case where the destination name is inferred based on user's input). Test results of the 3 approaches in comparison can be found at [2]. In summary, re-using the JMS sessions proved to have about a 3-fold performance improvement as per the tests.  

### Usage

1. Build the source using "mvn clean install". 
2. Copy the "target/JMSPublisherCacheMediator-1.0-SNAPSHOT.jar" into "ESB_HOME/repository/components/dropins" directory.
3. Restart the ESB server.

You can use the mediator within a proxy as follows : 

```xml
<?xml version="1.0" encoding="UTF-8"?>
<proxy xmlns="http://ws.apache.org/ns/synapse"
       name="JMSAPublisher"
       transports="https http"
       startOnLoad="true"
       trace="disable">
   <description/>
   <target>
      <inSequence>
         <property name="OUT_ONLY" value="true"/>
         <property name="FORCE_SC_ACCEPTED" value="true" scope="axis2"/>
         <iterate xmlns:xsd="http://services.samples/xsd"
                  xmlns:soapenv="http://www.w3.org/2003/05/soap-envelope"
                  xmlns:ser="http://services.samples"
                  id="msgIterator"
                  expression="//xsd:symbol">
            <target>
               <sequence>
                  
                  <property name="destinationName" expression="//xsd:symbol" description="GET STORE ID AKA TOPIC NAME"/>

                  <log level="custom" description="LOG STORE ID">
                     <property name="topicNameOfStore" expression="get-property('topicName')"/>
                  </log>
                  
                  <class name="org.wso2.mediators.custom.JMSPublisherCacheMediator">
                         <property name="connectionFactoryName" value="QueuePublisherFactory"/>
                         <property name="destinationType" value="queue"/>
		                 <property name="cacheExpirationInterval" value="3600"/>
		                 <property name="connectionPoolSize" value="10"/>
                  </class>
               </sequence>
            </target>
         </iterate>
      </inSequence>
      <outSequence>
         <send/>
      </outSequence>
   </target>
</proxy>
```

In the above configuration, the "connectionFactoryName" property points to the connectionFactory chosen to publish to topics/queues from the ESB_HOME/repository/conf/jndi.properties file. And the "cacheExpirationInterval" points to the interval at which the cached JMS sessions would expire (in seconds).

*CacheExpirationInterval is static across all proxies using the mediator, and cannot be changed once the server is started.

connectionPoolSize is the maximum number of active connections 1 ESB node can maintain with 1 broker node for a given destination+destinationType combination. This will allow multiple requests coming in parallelly to be handled in the same incoming speed instead of sharing a synchronous single JMS connection.

*If the cacheExpirationInterval is less than the time taken to publish a single message, there is a possibility of the JMS session being destroyed while publishing is in progress. To avoid this, always use a sensible interval for cache expiry. The wso2 carbon cache runs checks for expiration every 30 seconds, so it is ideal to maintain the cacheExpirationInterval as a value > 30).

[1] : http://waruapz.blogspot.com/2014/10/jms-performance-tuning-with-wso2-esb.html
[2] : https://github.com/hastef88/wso2.tryouts/blob/master/JMSPublisherCacheMediator/test_artifacts/Performance%20comparison%20-%20JMS%20Publisher%20Caching%20vs%20Non-caching.pdf

