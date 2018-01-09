package org.insightlab.akka.samples.iot.actors;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class IoTSupervisor extends AbstractActor {
	private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	
	public static Props props(){
		return Props.create(IoTSupervisor.class);
	}
	
	@Override
	public void preStart(){
		log.info("IoT Application started");
	}
	
	@Override
	public void postStop(){
		log.info("IoT Application stopped");
	}
	
	@Override
	public Receive createReceive(){
		return receiveBuilder().build();
	}
	
}
