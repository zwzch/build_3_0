package com.company.util.Redis;

import com.company.frame.Config.ConfigInit;
import com.sun.org.apache.bcel.internal.generic.IF_ACMPEQ;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import java.util.HashMap;
import java.util.PropertyPermission;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;


/**
 * Created by zy on 17-8-23.
 */
public class HeartBeat {
    static {
        PropertyConfigurator.configure("./Logger/log4j.propertites");
    }
    private Logger heart = Logger.getLogger("thirdLogger");

    public static String HEARTBEAT = "";
    private static String MESSAGE = "HEARTBEAT|HEARTBEAT";
    private CountDownLatch latch = new CountDownLatch(1);

    private Jedis jedis;

    public void setJedis(Jedis jedis) {
        this.jedis = jedis;
    }

    public void countDown(){
        latch.countDown();
    }

    public void Heartbeat(String channel, JedisPubSub sub) throws InterruptedException {
        int time = 0;
        while (true){

            try {
                HEARTBEAT="";
                CountDownLatch latch = new CountDownLatch(1);
                Jedis jedis = new Jedis(ConfigInit.REDIS_IP,ConfigInit.REDIS_PORT);
                jedis.publish(channel,MESSAGE);
                latch.await(500, TimeUnit.MILLISECONDS);
                if (HEARTBEAT.equals("")){
                    if (sub != null) {
                        try {
                            sub.unsubscribe();
                        }catch (Exception e){

                        }
                    }
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Jedis jedis = new Jedis(ConfigInit.REDIS_IP,ConfigInit.REDIS_PORT);
                            try {
                                heart.debug("restart already");
                                jedis.subscribe(sub,channel);
                            }catch (Exception e){
                                heart.error("restart is error");
                            }
                        }
                    }).start();
                }
                time=0;
            }catch (Exception e){
                heart.error("reconnect");
                Thread.sleep(500);
                continue;
            }
            Thread.sleep(ConfigInit.REDIS_SUB_HEARTBEAT_TIME);
        }
    }
}
