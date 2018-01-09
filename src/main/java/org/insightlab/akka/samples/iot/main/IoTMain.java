package org.insightlab.akka.samples.iot.main;

import java.io.IOException;

import org.insightlab.akka.samples.iot.actors.IoTSupervisor;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

public class IoTMain {
	public static void main(String[] args) throws IOException {
		ActorSystem system = ActorSystem.create("iot-system");
		
		try{
			ActorRef supervisor = system.actorOf(IoTSupervisor.props(), "iot-supervisor");
			
			System.out.println("Press ENTER to exit the system");
			System.in.read();
		}
		finally{
			system.terminate();
		}
	}
}
