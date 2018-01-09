package org.insightlab.akka.samples.print;

import java.io.IOException;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;

class StartStopActor1 extends AbstractActor {
	
	@Override
	public void preStart(){
		System.out.println("first started");
		getContext().actorOf(Props.create(StartStopActor2.class),"second");
	}
	
	@Override
	public void postStop(){
		System.out.println("first stopped");
	}
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.matchEquals("stop", s -> {
					getContext().stop(getSelf());
				}).build();
	}

}

class StartStopActor2 extends AbstractActor {
	
	@Override
	public void preStart(){
		System.out.println("second started");
	}
	
	@Override
	public void postStop(){
		System.out.println("second stopped");
	}
	
	@Override
	public Receive createReceive() {
		return receiveBuilder().build();
	}

}

public class ActorStartStopExperiments {
	public static void main(String[] args) {
		ActorSystem system = ActorSystem.create("testSystem");
		
		ActorRef first = system.actorOf(Props.create(StartStopActor1.class),"first");
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
