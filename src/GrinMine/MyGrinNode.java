package GrinMine;

import Dandelion.DandelionNode;
import Dandelion.Message;
import Dandelion.Transaction;
import Grin.GrinNode;

public class MyGrinNode extends GrinNode {

    public MyGrinNode(String name, int id) {
        super(name, id);
    }

    private Transaction takeFee(Transaction tx){
        if(tx.hasFee()){
            String tx_id = this.id + "_" + this.message_counter++;
            Transaction my_tx = new Transaction(tx_id, false, false);
            my_tx.addFeeInput(tx.getFee());
            return tx.add(my_tx);
        } else {
            return tx;
        }
    }

    @Override
    protected void generateMessage(){
        if(Math.random() < this.generate_probability){
            String tx_id = this.id + "_" + this.message_counter++;
            Transaction tx = new Transaction(tx_id, false, true);
            Message message = new Message(this, tx, true);
            this.addMessageToSend(message);
        }
    }

    @Override
    protected void broadcast(Message message){
        if(message.isStem()){
            stemPool.add(message.getMessage());
            delivered_to_me_messages_map.putIfAbsent(message.hashCode(), message);
            if(lastMerge + mergeWait <= System.currentTimeMillis()){
                Transaction tx = new Transaction();
                for(Transaction stemTx : stemPool){
                    stemTx = takeFee(stemTx);
                    tx = tx.add(stemTx);
                }
                Message to_send = new Message(this, tx, false);
                for(DandelionNode peer : this.peers){
                    if(!peer.equals(message.getSender())){
                        peer.deliver_message(to_send);
                    }
                }
                delivered_to_me_messages_map.putIfAbsent(to_send.hashCode(), to_send);
                stemPool.clear();
                lastMerge = System.currentTimeMillis();
            }
        }else {
            Message to_send = message.copy();
            to_send.setSender(this);
            to_send.setStem(false);
            for (DandelionNode peer : this.peers) {
                if (!peer.equals(message.getSender())) {
                    peer.deliver_message(to_send);
                }
            }
            delivered_to_me_messages_map.putIfAbsent(message.hashCode(), message);
        }
    }







}
