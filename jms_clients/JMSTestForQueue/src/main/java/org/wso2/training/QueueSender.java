package org.wso2.training;/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
import org.wso2.andes.transport.MessageDeliveryPriority;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Properties;

public class QueueSender {
    public static final String QPID_ICF = "org.wso2.andes.jndi.PropertiesFileInitialContextFactory";
    private static final String CF_NAME_PREFIX = "connectionfactory.";
    private static final String QUEUE_NAME_PREFIX = "queue.";
    private static final String CF_NAME = "qpidConnectionfactory";
    String userName = "admin";
    String password = "admin";
    private static String CARBON_CLIENT_ID = "carbon";
    private static String CARBON_VIRTUAL_HOST_NAME = "carbon";
    private static String CARBON_DEFAULT_HOSTNAME = "localhost";
    private static String CARBON_DEFAULT_PORT = "5672";

    public static String queueName = "testQueue";
    public static int messageCount = 1000;
    public static int waitInterval = 100;
    public static long expirationTTL = 50l;

    public static void start(int _messageCount,String _queueName,int _waitInterval, long _expirationTTL) throws NamingException, JMSException {

        messageCount = _messageCount;
        queueName = _queueName;
        waitInterval = _waitInterval;
        expirationTTL = _expirationTTL;

        QueueSender queueSender = new QueueSender();
        queueSender.sendMessages();
    }
    public void sendMessages() throws NamingException, JMSException {
        Properties properties = new Properties();
        properties.put(Context.INITIAL_CONTEXT_FACTORY, QPID_ICF);
        properties.put(CF_NAME_PREFIX + CF_NAME, getTCPConnectionURL(userName, password));
        properties.put(QUEUE_NAME_PREFIX + queueName, queueName);
        System.out.println("getTCPConnectionURL(userName,password) = " + getTCPConnectionURL(userName, password));
        InitialContext ctx = new InitialContext(properties);
        // Lookup connection factory
        QueueConnectionFactory connFactory = (QueueConnectionFactory) ctx.lookup(CF_NAME);
        QueueConnection queueConnection = connFactory.createQueueConnection();
        queueConnection.start();
        QueueSession queueSession =
                queueConnection.createQueueSession(false, QueueSession.AUTO_ACKNOWLEDGE);
        // Send message
        Queue queue = (Queue)ctx.lookup(queueName);

        for (int i=0;i<messageCount;i++) {
            // create the message to send
            TextMessage textMessage = queueSession.createTextMessage("Test Message Content for "+queueName+" No : " + i);
            javax.jms.QueueSender queueSender = queueSession.createSender(queue);

            Long timeToLive = (i%2==1) ? 0l : expirationTTL;
            queueSender.setTimeToLive(timeToLive);
            queueSender.send(textMessage, DeliveryMode.PERSISTENT, 0, timeToLive);
            queueSender.close();

            try {
                Thread.sleep(waitInterval);
            } catch (InterruptedException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }


        queueSession.close();
        queueConnection.close();
    }
    public String getTCPConnectionURL(String username, String password) {
        // amqp://{username}:{password}@carbon/carbon?brokerlist='tcp://{hostname}:{port}'
        return new StringBuffer()
                .append("amqp://").append(username).append(":").append(password)
                .append("@").append(CARBON_CLIENT_ID)
                .append("/").append(CARBON_VIRTUAL_HOST_NAME)
                .append("?brokerlist='tcp://").append(CARBON_DEFAULT_HOSTNAME).append(":").append(CARBON_DEFAULT_PORT).append("'")
                .toString();
    }

}

