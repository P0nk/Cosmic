package net.server;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisException;
import java.util.Timer;
import java.util.TimerTask;

public class RedisJobQueue {

    private final JedisPool jedisPool;
    private final String jobQueue;
    private final String processingQueue;
    private final String subChannel;

    public RedisJobQueue(JedisPool jedisPool, String jobName) {
        this.jedisPool = jedisPool;
        this.jobQueue = jobName + ":jobs";
        this.processingQueue = jobName + ":process";
        this.subChannel = jobName + ":channel";

        startJobProcessing();
    }

    private void startJobProcessing() {
        Timer timer = new Timer();

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    processJob();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 0, 1000);
    }

    private void processJob() {
        try (Jedis jedis = jedisPool.getResource()) {
            String jobKey = jedis.brpoplpush(jobQueue, processingQueue, 0);
            if (jobKey != null) {
                System.out.println("Processing job: " + jobKey);
                // TODO: Add handlers

                finishJob(jedis, jobKey, true);
            }
        } catch (JedisException e) {
            e.printStackTrace();
        }
    }

    private void finishJob(Jedis jedis, String jobKey, boolean success) {
        if (success) {
            jedis.lrem(processingQueue, 0, jobKey);
            jedis.del(jobKey);
        } else {
            // TODO: Handle errors
        }

        jedis.publish(subChannel, "job finished");
    }
}
