package aqua.blatt1.broker;

import aqua.blatt1.common.msgtypes.DeregisterRequest;
import aqua.blatt1.common.msgtypes.RegisterResponse;
import messaging.Endpoint;
import messaging.Message;

import java.io.Serializable;

public class Broker {

    private ClientCollection clientList  = new ClientCollection<>();

    private Endpoint endpoint = new Endpoint(4711);

   public void broker()
   {
       while(true)
       {
           Message message =  endpoint.blockingReceive();
       }
   }

   private void register(Message message)
   {
       String id = "client" + clientList.size()+ 1;
       clientList.add(id,message.getSender());
       endpoint.send(message.getSender(),new RegisterResponse(id));
   }

   private void deregister(Message message)
   {
       Serializable payload = message.getPayload();
       DeregisterRequest request = (DeregisterRequest)payload;
       clientList.remove(clientList.indexOf(request.getId()));
   }
}
