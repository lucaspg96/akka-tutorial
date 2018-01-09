package org.insightlab.akka.samples.remote;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import akka.actor.AbstractActor;

public class RemoteActor extends AbstractActor{
	
	public static void main(String[] args) {
		Config config = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + 2552);
	}
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.matchAny(msg -> {
					getContext().getSystem().log().info("Message received: "+msg);
					getSender().tell("Hello from remote!", getSelf());
				})
				.build();
	}

}
