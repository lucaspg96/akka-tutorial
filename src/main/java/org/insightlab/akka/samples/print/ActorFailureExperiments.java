package org.insightlab.akka.samples.print;

import java.io.IOException;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;

/*
This example shows how akka deals with actors that fails in runtime
*/

//This will be the parent actor, that will create the actor that should fail
class SupervisingActor extends AbstractActor {
	ActorRef child = getContext().actorOf(Props.create(SupervisedActor.class),"supervised-actor");
	
	//At this function we define, for each message pattern, the behavior of the actor
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				//When the message "faiLChild" is received, the actor will send
				//the message "fail" to its child
				.matchEquals("failChild", f -> 
					child.tell("fail", getSelf())
				).build();
	}
	
}

//This will be the child actor, the one that should fail in runtime
class SupervisedActor extends AbstractActor {
	
	//This function is the one that will be executed when the actor starts
	@Override
	public void preStart() {
		System.out.println("supervised actor started");
	}
	
	//This function is the one that will be executed when the actor stops
	@Override
	public void postStop() {
		System.out.println("supervised actor stopped");
	}
	
	//At this function we define, for each message pattern, the behavior of the actor
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				//When the message "fail" is received, the actor throws an Exception
				.matchEquals("fail", f -> {
					System.out.println("supervised actor fails now");
					throw new Exception("I failed!");
				}).build();
	}
}

//Here we have the the example's main code
//The main objective is to notice what happens when the
//child actor fails
public class ActorFailureExperiments {
	public static void main(String[] args) {
		//First, we must create a system where the actors are going to work
		ActorSystem system = ActorSystem.create("testSystem");
		
		//After, we create a new actor inside the system and name it "first-actor"
		ActorRef supervisingActor = system.actorOf(Props.create(SupervisingActor.class),"supervising-actor");
		
		//Then, we send the text message to the actor created.
		//On this case, isn't important to tell who is sending the message
		supervisingActor.tell("failChild", ActorRef.noSender());
		
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
