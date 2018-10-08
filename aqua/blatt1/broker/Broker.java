package aqua.blatt1.broker;

import aqua.blatt1.common.Direction;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.msgtypes.DeregisterRequest;
import aqua.blatt1.common.msgtypes.HandoffRequest;
import aqua.blatt1.common.msgtypes.RegisterRequest;
import aqua.blatt1.common.msgtypes.RegisterResponse;
import messaging.Endpoint;
import messaging.Message;

import java.io.Serializable;
import java.net.InetSocketAddress;

public class Broker {

    private ClientCollection<InetSocketAddress> clientList  = new ClientCollection<>();

    private Endpoint endpoint = new Endpoint(4711);

   public void broker()
   {
       while(true)
       {
           System.out.println("anfrage");
           Message message =  endpoint.blockingReceive();

            if(message.getPayload() instanceof RegisterRequest)
            {
                this.register(message);
            }
            else if ( message.getPayload() instanceof DeregisterRequest)
            {
                this.deregister(message);
            }
            else if( message.getPayload() instanceof HandoffRequest)
            {
                this.handoffFish(message);
            }
       }
   }

   private void register(Message message)
   {
       String id = "client" + clientList.size();
       clientList.add(id,message.getSender());
       endpoint.send(message.getSender(),new RegisterResponse(id));
   }

   private void deregister(Message message)
   {
       Serializable payload = message.getPayload();
       DeregisterRequest request = (DeregisterRequest)payload;
       clientList.remove(clientList.indexOf(request.getId()));
   }

    private void handoffFish (Message message)
    {
        int sourceindex = clientList.indexOf(message.getSender());
        Serializable payload = message.getPayload();
        HandoffRequest request = (HandoffRequest)payload;
        FishModel fish = request.getFish();
        if(fish.getDirection()== Direction.RIGHT)
        {
            if (sourceindex == clientList.size()- 1)
            {
                endpoint.send(clientList.getClient(0), message.getPayload());
            }
            else
            {
                endpoint.send(clientList.getClient(sourceindex + 1), message.getPayload());
            }
        }
        else if (fish.getDirection() == Direction.LEFT)
        {
            if(sourceindex == 0)
            {
                endpoint.send(clientList.getClient(clientList.size() - 1),message.getPayload());
            }
            else
            {
                endpoint.send(clientList.getClient(sourceindex - 1), message.getPayload());
            }
        }
    }
}
