package org.insightlab.akka.samples.remote;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;

public class Main {
	
	public static void main(String[] args) {
		final Config config = ConfigFactory.parseString("akka.remote.netty.tcp.port=2552").
                withFallback(ConfigFactory.parseString("akka.cluster.roles = [masterRole]")).
                withFallback(ConfigFactory.parseString("akka.remote.netty.tcp.hostname = \127.0.0.1\\")).
                withFallback(ConfigFactory.parseString("akka.cluster.role.workerRole.min-nr-of-members = 1")).
                withFallback(ConfigFactory.parseString("akka.cluster.seed-nodes = 127.0.0.1")).
                withFallback(ConfigFactory.load("bladyg"));

	}
}
