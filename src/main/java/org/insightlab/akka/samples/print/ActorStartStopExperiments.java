package org.insightlab.akka.samples.print;

import java.io.IOException;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;

/*
 This example shows how the preStart and postStop functions
 works inside the actors
 */

//This will be the parent actor, that will be started first
class StartStopActor1 extends AbstractActor {
	
	//This function is the one that will be executed when the actor starts
	@Override
	public void preStart(){
		System.out.println("first started");
		getContext().actorOf(Props.create(StartStopActor2.class),"second");
	}
	
	//This function is the one that will be executed when the actor stops
	@Override
	public void postStop(){
		System.out.println("first stopped");
	}
	
	//At this function we define, for each message pattern, the behavior of the actor
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				//When the message "stop" is received, the actor will stop
				.matchEquals("stop", s -> {
					getContext().stop(getSelf());
				}).build();
	}

}

//This will be the child actor, that will be started after the parent
class StartStopActor2 extends AbstractActor {
	
	//This function is the one that will be executed when the actor starts
	@Override
	public void preStart(){
		System.out.println("second started");
	}
	
	//This function is the one that will be executed when the actor stops
	@Override
	public void postStop(){
		System.out.println("second stopped");
	}
	
	//At this function we define, for each message pattern, the behavior of the actor
	//On this case, the actor won't do nothing
	@Override
	public Receive createReceive() {
		return receiveBuilder().build();
	}

}

//Here we have the the example's main code
//The main objective is to notice the order that the actors
//are created
public class ActorStartStopExperiments {
	public static void main(String[] args) {
		//First, we must create a system where the actors are going to work
		ActorSystem system = ActorSystem.create("testSystem");
		
		//After, we create a new actor inside the system and name it "first-actor"
		ActorRef first = system.actorOf(Props.create(StartStopActor1.class),"first");
		
		//Then, we send the text message to the actor created.
		//On this case, isn't important to tell who is sending the message
		first.tell("stop", ActorRef.noSender());
		
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
