package utils;

import Dandelion.DandelionNode;
import Dandelion.Message;
import Dandelion.Network;
import Dandelion.Transaction;

import java.util.*;
import java.util.concurrent.atomic.LongAdder;

public class KnowledgeUtil {

    /** TODO: I need to make this an object, so that it can store current progress, and add one node after another.
     **  The current method is using to much time as a lot of the analysis is recomputed every time.
     **/

    private Network network;
    private ArrayList<DandelionNode> nodes;

    private HashMap<Integer, Transaction> toReduce;
    private HashSet<String> groundTruth;
    private HashMap<Integer, Transaction> knowledge;
    private HashSet<Integer> messageSeen;

    private int toReduceSizeLastUsed = 0;
    private int knowledgeSizeLastUSed = 0;

    public KnowledgeUtil(Network network){
        this.network = network;
    }

    public ArrayList<DandelionNode> getBestNodes(int number){
        ArrayList<DandelionNode> sortedNodes = network.getSortedNodes();
        return new ArrayList<>(sortedNodes.subList(0, number));
    }

    public void initialise(){
        this.nodes = new ArrayList<>();
        this.toReduce = new HashMap<>();
        this.groundTruth = new HashSet<>();
        this.knowledge = new HashMap<>();
        this.messageSeen = new HashSet<>();
        this.toReduceSizeLastUsed = 0;
        this.knowledgeSizeLastUSed = 0;
    }

    public void addNode(DandelionNode node){
        this.nodes.add(node);
        addKnowledge(node);
        addReceivedMessages(node);
        performReduction();
        System.out.println("Post-analysis(" + this.nodes.size() + "): toReduce size: " + this.toReduce.size() + ", knowledge size: " + this.knowledge.size() + ", the ground truth is size: " + this.groundTruth.size());
    }

    // TODO: Reevaluate, is this necessary, dont we get it all from addReceivedMessages?
    private void addKnowledge(DandelionNode node){
        HashMap<Integer, Message> nodeMessages = node.getMessagesCreated();
        for(Message msg : nodeMessages.values()){
            Transaction tx = msg.getMessage();
            this.knowledge.putIfAbsent(tx.hashCode(), tx);
        }
    }

    private void addToGroundTruth(Transaction tx){
        this.groundTruth.addAll(tx.getInputs());
    }

    private void addReceivedMessages(DandelionNode node){
        HashMap<Integer, Message> nodeMessages = node.getDeliveredMessages();
        for(Message msg : nodeMessages.values()){
            Transaction tx = msg.getMessage();
            if(!this.messageSeen.contains(tx.hashCode())){
                this.messageSeen.add(tx.hashCode());
                addToGroundTruth(tx);
                if(tx.isConclusive()){
                    this.knowledge.putIfAbsent(tx.hashCode(), tx);
                } else if (!tx.isEmpty()){
                    this.toReduce.putIfAbsent(tx.hashCode(), tx);
                }
            }
        }
    }

    /**
     * TODO: We should be able to make reductions smarter, taking into account the changes from the new node.
     * Instead of it all once more.
     */
    public void performReduction(){
        if(this.knowledgeSizeLastUSed == this.knowledge.size()) {
            reduceWithKnowledge(this.toReduce, this.knowledge);
        }
        if(this.toReduceSizeLastUsed == this.toReduce.size()) {
            reduceWithoutKnowledge(this.toReduce, this.knowledge);
            reduceWithKnowledge(this.toReduce, this.knowledge);
        }
    }





    private HashMap<Integer, Transaction> getReceivedMessages(ArrayList<DandelionNode> nodes){
        HashMap<Integer, Transaction> receivedMessages = new HashMap<>();
        for(DandelionNode node : nodes){
            HashMap<Integer, Message> nodeMessages = node.getDeliveredMessages();
            for(Message msg : nodeMessages.values()){
                Transaction tx = msg.getMessage();
                receivedMessages.putIfAbsent(tx.hashCode(), tx);
            }
        }
        return receivedMessages;
    }

    private HashMap<Integer, Transaction> getInitialKnowledge(ArrayList<DandelionNode> nodes){
        HashMap<Integer, Transaction> knowledge = new HashMap<>();
        for(DandelionNode node : nodes){
            HashMap<Integer, Message> nodeMessages = node.getMessagesCreated();
            for(Message msg : nodeMessages.values()){
                Transaction tx = msg.getMessage();
                knowledge.putIfAbsent(tx.hashCode(), tx);
            }
        }
        return knowledge;
    }



    public void analysis(ArrayList<DandelionNode> nodes, boolean print, boolean printFinalKnowledge, boolean printFinalToReduce){
        HashMap<Integer, Transaction> toReduce = getReceivedMessages(nodes);
        HashSet<String> groundTruth =  groundTruth(toReduce);
        HashMap<Integer, Transaction> knowledge = getInitialKnowledge(nodes);
        extendKnowledgeWithReceived(toReduce, knowledge);
        if(print) {
            System.out.println("--- full analysis start ---");
            for(DandelionNode node : nodes){
                System.out.println(node);
            }

            System.out.println("Pre-analysis: toReduce size: " + toReduce.size() + ", knowledge size: " + knowledge.size() + ", the ground truth is size: " + groundTruth.size());
            statistics(toReduce);
        }

        for(int i = 0 ; i < 2; i++){
            reduceWithKnowledge(toReduce, knowledge);
            reduceWithoutKnowledge(toReduce, knowledge);
        }

        reduceWithKnowledge(toReduce, knowledge);

        if(print) {
            System.out.println("Post-analysis: toReduce size: " + toReduce.size() + ", knowledge size: " + knowledge.size() + ", the ground truth is size: " + groundTruth.size());
            statistics(toReduce);
            if (printFinalKnowledge) {
                System.out.println("Printing full knowledge");
                for (Transaction tx : knowledge.values()) {
                    System.out.println(tx);
                }
                System.out.println("End of full knowledge");
            }
            if (printFinalToReduce) {
                System.out.println("Printing toReduce");
                for (Transaction tx : toReduce.values()) {
                    System.out.println(tx);
                }
                System.out.println("End of toReduce");
            }
            System.out.println("--- full analysis end ---");
        }
        else{
            System.out.println("Post-analysis(" + nodes.size() + "): toReduce size: " + toReduce.size() + ", knowledge size: " + knowledge.size() + ", the ground truth is size: " + groundTruth.size());
        }
    }


    public static boolean isASubsetOfB(Transaction a, Transaction b){
        return a.reduce(b).isEmpty();
    }

    private boolean isOverlapping(Transaction a, Transaction b){
        HashSet<String> a_inputs = a.getInputs();
        HashSet<String> b_inputs = b.getInputs();
        if(a_inputs.size() > b_inputs.size()){
            for (String b_input : b_inputs) {
                if (a_inputs.contains(b_input)) {
                    return true;
                }
            }
        } else {
            for (String a_input : a_inputs) {
                if (b_inputs.contains(a_input)) {
                    return true;
                }
            }
        }
        return false;
    }

    private HashSet<String> getUniqueInputs(HashMap<Integer, Transaction> transactions){
        HashSet<String> uniqueInputs = new HashSet<>();
        for(Transaction tx : transactions.values()){
            uniqueInputs.addAll(tx.getInputs());
        }
        return uniqueInputs;
    }

    private HashSet<String> groundTruth(HashMap<Integer, Transaction> messages){
        return getUniqueInputs(messages);
    }

    // TODO: This could possibly reduced with new knowledge instead of looping through the entire knowledge-set
    private void reduceWithKnowledge(HashMap<Integer, Transaction> toReduce, HashMap<Integer, Transaction> knowledge){
        boolean go_another_run = true;
        while(go_another_run){
            go_another_run = false;
            Collection<Transaction> toReduceCollection = toReduce.values();
            Collection<Transaction> knowledgeCollection = knowledge.values();

            ArrayList<Transaction> toRemove = new ArrayList<>();
            ArrayList<Transaction> toAdd = new ArrayList<>();

            for (Transaction tx_reduce : toReduceCollection) {
                if (tx_reduce.isConclusive()) {
                    knowledge.putIfAbsent(tx_reduce.hashCode(), tx_reduce);
                    toRemove.add(tx_reduce);
                } else {
                    for (Transaction tx_knowledge : knowledgeCollection) {
                        if (isOverlapping(tx_reduce, tx_knowledge)) {// Let us find the first with overlap, and then do something about it
                            Transaction reduced = tx_reduce.reduce(tx_knowledge);
                            if (reduced.isConclusive()) {
                                knowledge.putIfAbsent(reduced.hashCode(), reduced);
                                go_another_run = true;
                            } else if (!reduced.isEmpty()) {
                                toAdd.add(reduced);
                            }
                            toRemove.add(tx_reduce);
                            break;
                        }
                    }
                }
            }
            for(Transaction tx : toRemove){
                toReduce.remove(tx.hashCode());
            }
            for(Transaction tx : toAdd){
                toReduce.putIfAbsent(tx.hashCode(), tx);
            }
        }
    }

    private void reduceWithoutKnowledge(HashMap<Integer, Transaction> toReduce, HashMap<Integer, Transaction> knowledge){
        Collection<Transaction> toReduceCollection = toReduce.values();

        ArrayList<Transaction> toAdd = new ArrayList<>();

        for(Iterator<Transaction> iterator = toReduceCollection.iterator(); iterator.hasNext();){
            Transaction tx_reduce = iterator.next();
            for (Transaction tx_reduce_inner : toReduceCollection) {
                if (tx_reduce.equals(tx_reduce_inner)) {
                    continue;
                }
                if (isOverlapping(tx_reduce, tx_reduce_inner)) {
                    // Transaction tx = (tx_reduce.add(tx_reduce_inner)).reduce(tx_reduce_inner);
                    Transaction tx = tx_reduce.reduce(tx_reduce_inner);
                    if (tx.isConclusive()) {
                        knowledge.putIfAbsent(tx.hashCode(), tx);
                    } else if (!tx.isEmpty()) {
                        toAdd.add(tx);
                    }
                    iterator.remove();
                    break;
                }
            }
        }

        for(Transaction tx : toAdd){
            toReduce.putIfAbsent(tx.hashCode(), tx);
        }

    }

    private void extendKnowledgeWithReceived(HashMap<Integer, Transaction> toReduce, HashMap<Integer, Transaction> knowledge){
        for(Iterator<Transaction> iterator = toReduce.values().iterator(); iterator.hasNext();){
            Transaction tx = iterator.next();
            if(tx.isConclusive()){
                knowledge.putIfAbsent(tx.hashCode(), tx);
                iterator.remove();
            }
        }
    }

    private void statistics(HashMap<Integer, Transaction> toReduce){
        HashMap<Integer, LongAdder> values = new HashMap<>();

        for(Transaction tx : toReduce.values()){
            int length = tx.getInputs().size();
            values.computeIfAbsent(length, key -> new LongAdder()).increment();
        }

        HashSet<String> unReduceable = getUniqueInputs(toReduce);
        System.out.println("Unable to deduce: " + unReduceable.size() + " inputs");

        int i = 0;
        int j = 0;
        while(j < values.size()){
            if(values.containsKey(i)){
                System.out.println(values.get(i) + " transactions of size: " + i);
                j++;
            }
            i++;
        }
    }



}