package org.insightlab.akka.samples.print;

import java.io.IOException;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;

/*
 	This example shows how can we construct actors and send a simple text message
  */

class PrintMyActorRefActor extends AbstractActor {
	
	//At this function we define, for each message pattern, the behavior of the actor 
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				//When the message "printit" is received, the function is triggered
				//Since we are using Java 8, we can define this anonymous function
				.matchEquals("printit", p -> {
					//We create a new empty actor, just to illustrate the scope
					ActorRef secondRef = getContext().actorOf(Props.empty(), "second-actor");
					//Then we print the reference of the new actor
					System.out.println("Second: "+secondRef);
				}).build();
	}

}

//Here we have the the example's main code
//The main objective is to understand how the actors are created
public class ActorHierarchyExperiments{
	public static void main(String[] args) {
		//First, we must create a system where the actors are going to work
		ActorSystem system = ActorSystem.create("testSystem");
		
		//After, we create a new actor inside the system and name it "first-actor"
		ActorRef firstRef = system.actorOf(Props.create(PrintMyActorRefActor.class),"first-actor");
		System.out.println("First: " + firstRef);
		//Then, we send the text message to the actor created.
		//On this case, isn't important to tell who is sending the message
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