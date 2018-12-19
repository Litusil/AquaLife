package aqua.blatt1.broker;

import aqua.blatt1.common.SecureEndpoint;
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
    private SecureEndpoint endpoint = new SecureEndpoint(4711);
    private  volatile boolean stopRequest = false;
    private final int leaseTime = 10000;

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
            else if ( message.getPayload() instanceof NameResolutionRequest)
            {
                this.nameResolution(message);
            }
        }

        private void register(Message message)
        {
            lock.readLock().lock();
            String id = "client" + clientList.size();
            lock.readLock().unlock();
            lock.writeLock().lock();
            boolean clientIsUpToDate = clientList.updateClient(message.getSender());
            lock.writeLock().unlock();
            if(!clientIsUpToDate){
                if (clientList.size() == 0) {
                    endpoint.send(message.getSender(), new NeighborUpdate(message.getSender(), message.getSender()));
                    endpoint.send(message.getSender(), new Token());
                } else {
                    clientList.size();
                    endpoint.send(message.getSender(), new NeighborUpdate(clientList.getClient(0), clientList.getClient(clientList.size() - 1)));
                    endpoint.send(clientList.getClient(clientList.size() - 1), new NeighborUpdate(message.getSender(), null));
                    endpoint.send(clientList.getClient(0), new NeighborUpdate(null, message.getSender()));
                }
                lock.writeLock().lock();
                clientList.add(id, message.getSender());
                lock.writeLock().unlock();
                endpoint.send(message.getSender(), new RegisterResponse(id, leaseTime));
            }

        }

        private void deregister(Message message)
        {
            Serializable payload = message.getPayload();
            DeregisterRequest request = (DeregisterRequest)payload;
            lock.writeLock().lock();
            if(clientList.size() == 2){
                endpoint.send(clientList.getClient(0),new NeighborUpdate(clientList.getClient(0),clientList.getClient(0)));
            } else if (clientList.size() > 2) {
                endpoint.send(clientList.getLeftNeighorOf(clientList.indexOf(request.getId())),new NeighborUpdate(clientList.getRightNeighorOf(clientList.indexOf(request.getId())),null));
                endpoint.send(clientList.getRightNeighorOf(clientList.indexOf(request.getId())),new NeighborUpdate(null,clientList.getLeftNeighorOf(clientList.indexOf(request.getId()))));
            }
            clientList.remove(clientList.indexOf(request.getId()));
            lock.writeLock().unlock();
        }

        private void nameResolution(Message message)
        {
            Serializable payload = message.getPayload();
            NameResolutionRequest request = (NameResolutionRequest)payload;
            lock.readLock().lock();
            endpoint.send(message.getSender(),new NameResolutionResponse(clientList.getClient(clientList.indexOf(request.getTankId())),request.getRequestId()));
            lock.readLock().unlock();
        }

    }

   public void broker()
   {
       StopRequest sr = new StopRequest();
       sr.start();
       ExecutorService executor = Executors.newFixedThreadPool(5);

       new java.util.Timer().schedule(
               new java.util.TimerTask() {
                   @Override
                   public void run() {
                       System.out.println("start cleanup...");
                       lock.writeLock().lock();
                       long currentTime = System.currentTimeMillis();
                       for(int i = 0 ; i<clientList.size(); i++){
                           long clientTimeStamp = clientList.getTimeStamp(i);
                           if((currentTime - clientTimeStamp) > 10000 ){
                               System.out.println("clean: " + clientList.getID(i));
                               if(clientList.size() == 2){
                                   endpoint.send(clientList.getClient(0),new NeighborUpdate(clientList.getClient(0),clientList.getClient(0)));
                               } else if (clientList.size() > 2) {
                                   endpoint.send(clientList.getLeftNeighorOf(clientList.indexOf(clientList.getID(i))),new NeighborUpdate(clientList.getRightNeighorOf(clientList.indexOf(clientList.getID(i))),null));
                                   endpoint.send(clientList.getRightNeighorOf(clientList.indexOf(clientList.getID(i))),new NeighborUpdate(null,clientList.getLeftNeighorOf(clientList.indexOf(clientList.getID(i)))));
                               }
                               clientList.remove(clientList.indexOf(clientList.getID(i)));
                           }
                       }
                       lock.writeLock().unlock();
                   }
               }, 0, 2000);

       while(!stopRequest)
       {
           Message message =  endpoint.blockingReceive();
           if( message.getPayload() instanceof PoisonPill)
            {
                System.out.println("Goodbye!");
                JOptionPane.getRootFrame().dispose();
                break;
            }
           Runnable worker = new BrokerTask(message);
           executor.execute(worker);
       }
       executor.shutdown();

   }


}
