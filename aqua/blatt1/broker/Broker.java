package aqua.blatt1.broker;

import aqua.blatt1.common.Direction;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.msgtypes.*;
import messaging.Endpoint;
import messaging.Message;

import javax.swing.*;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Broker {

    private ClientCollection<InetSocketAddress> clientList  = new ClientCollection<>();
    private ReadWriteLock lock = new ReentrantReadWriteLock();
    private Endpoint endpoint = new Endpoint(4711);
    private  volatile boolean stopRequest = false;

    private class StopRequest extends Thread{
        @Override
        public void run(){
            // Aufruf der statischen Methode showMessageDialog()
            JOptionPane.showMessageDialog(null, "Broker beenden?","Eine Nachricht",JOptionPane.WARNING_MESSAGE);
            stopRequest = true;
        }


    }
    private class BrokerTask implements Runnable {
        Message message;

        public BrokerTask (Message message){
            this.message = message;
        }

        @Override
        public void run() {
            System.out.println("Anfrage in Thread: " + Thread.currentThread().getName());
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
            else if( message.getPayload() instanceof PoisonPill)
            {
                stopRequest = true;
            }
        }

        private void register(Message message)
        {
            lock.readLock().lock();
            String id = "client" + clientList.size();
            lock.readLock().unlock();
            lock.writeLock().lock();
            clientList.add(id,message.getSender());
            lock.writeLock().unlock();
            endpoint.send(message.getSender(),new RegisterResponse(id));
        }

        private void deregister(Message message)
        {
            Serializable payload = message.getPayload();
            DeregisterRequest request = (DeregisterRequest)payload;
            lock.writeLock().lock();
            clientList.remove(clientList.indexOf(request.getId()));
            lock.writeLock().unlock();
        }

        private void handoffFish (Message message)
        {
            lock.readLock().lock();
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
            lock.readLock().unlock();
        }

    }

   public void broker()
   {
       StopRequest sr = new StopRequest();
       sr.start();
       ExecutorService executor = Executors.newFixedThreadPool(5);
       while(!stopRequest)
       {
           Message message =  endpoint.blockingReceive();
           Runnable worker = new BrokerTask(message);
           executor.execute(worker);
       }
       executor.shutdown();
   }


}
