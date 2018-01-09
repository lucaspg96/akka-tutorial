package org.insightlab.akka.samples.print;

import java.io.IOException;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;

class PrintMyActorRefActor extends AbstractActor {

	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.matchEquals("printit", p -> {
					ActorRef secondRef = getContext().actorOf(Props.empty(), "second-actor");
					System.out.println("Second: "+secondRef);
				}).build();
	}

}

public class ActorHierarchyExperiments{
	public static void main(String[] args) {
		ActorSystem system = ActorSystem.create("testSystem");
		
		ActorRef firstRef = system.actorOf(Props.create(PrintMyActorRefActor.class),"first-actor");
		System.out.println("First: " + firstRef);
		firstRef.tell("printit", ActorRef.noSender());
		
		System.out.println(">>> Press ENTER to exit <<<");
		
		try{
			System.in.read();
		} catch (IOException e) {
			e.printStackTrace();
		}
		finally{
			system.terminate();
		}
	}
}