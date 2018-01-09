package org.insightlab.akka.samples.print;

import java.io.IOException;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;

class SupervisingActor extends AbstractActor {
	ActorRef child = getContext().actorOf(Props.create(SupervisedActor.class),"supervised-actor");
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.matchEquals("failChild", f -> {
					child.tell("fail", getSelf());
				}).build();
	}
	
}

class SupervisedActor extends AbstractActor {
	
  @Override
  public void preStart() {
    System.out.println("supervised actor started");
  }

  @Override
  public void postStop() {
    System.out.println("supervised actor stopped");
  }
  
  @Override
  public Receive createReceive() {
    return receiveBuilder()
        .matchEquals("fail", f -> {
          System.out.println("supervised actor fails now");
          throw new Exception("I failed!");
        }).build();
  }
}

public class ActorFailureExperiments {
	public static void main(String[] args) {
		ActorSystem system = ActorSystem.create("testSystem");
		
		ActorRef supervisingActor = system.actorOf(Props.create(SupervisingActor.class),"supervising-actor");
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
