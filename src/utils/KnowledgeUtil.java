package utils;

import Dandelion.DandelionNode;
import Dandelion.Message;
import Dandelion.Transaction;

import java.util.*;
import java.util.concurrent.atomic.LongAdder;

public class KnowledgeUtil {

    public static boolean isASubsetOfB(Transaction a, Transaction b){
        return a.reduce(b).isEmpty();
    }

    private static boolean isOverlapping(Transaction a, Transaction b){
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

    private static HashMap<Integer, Transaction> getReceivedMessages(ArrayList<DandelionNode> nodes){
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

    private static HashMap<Integer, Transaction> getInitialKnowledge(ArrayList<DandelionNode> nodes){
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

    private static HashSet<String> getUniqueInputs(HashMap<Integer, Transaction> transactions){
        HashSet<String> uniqueInputs = new HashSet<>();
        for(Transaction tx : transactions.values()){
            uniqueInputs.addAll(tx.getInputs());
        }
        return uniqueInputs;

    }

    private static HashSet<String> groundTruth(HashMap<Integer, Transaction> messages){
        return getUniqueInputs(messages);
    }

    private static void reduceWithKnowledge(HashMap<Integer, Transaction> toReduce, HashMap<Integer, Transaction> knowledge){
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

    private static void reduceWithoutKnowledge(HashMap<Integer, Transaction> toReduce, HashMap<Integer, Transaction> knowledge){
        // TODO: Implement a sort on to the collection, so that execution will be similar each time. Or not.
        // TODO: Needs to be verified that this actually works and will not crash due to removal.
        Collection<Transaction> toReduceCollection = toReduce.values();

        ArrayList<Transaction> toAdd = new ArrayList<>();

        for(Iterator<Transaction> iterator = toReduceCollection.iterator(); iterator.hasNext();){
            Transaction tx_reduce = iterator.next();
            for (Transaction tx_reduce_inner : toReduceCollection) {
                if (tx_reduce.equals(tx_reduce_inner)) {
                    continue;
                }
                if (isOverlapping(tx_reduce, tx_reduce_inner)) {
                    // TODO: Think about this, it is necessary to do add and then reduce?
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

    private static void extendKnowledgeWithReceived(HashMap<Integer, Transaction> toReduce, HashMap<Integer, Transaction> knowledge){
        for(Iterator<Transaction> iterator = toReduce.values().iterator(); iterator.hasNext();){
            Transaction tx = iterator.next();
            if(tx.isConclusive()){
                knowledge.putIfAbsent(tx.hashCode(), tx);
                iterator.remove();
            }
        }
    }

    private static void statistics(HashMap<Integer, Transaction> toReduce){
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

    public static void analysis(ArrayList<DandelionNode> nodes, boolean printFinalKnowledge, boolean printFinalToReduce){
        HashMap<Integer, Transaction> toReduce = KnowledgeUtil.getReceivedMessages(nodes);
        HashSet<String> groundTruth =  groundTruth(toReduce);
        HashMap<Integer, Transaction> knowledge = getInitialKnowledge(nodes);
        extendKnowledgeWithReceived(toReduce, knowledge);
        System.out.println("Pre-analysis: toReduce size: " + toReduce.size() + ", knowledge size:" + knowledge.size() + ", the ground truth is size: " + groundTruth.size());
        statistics(toReduce);

        for(int i = 0 ; i < 3; i++){
            reduceWithKnowledge(toReduce, knowledge);
            reduceWithoutKnowledge(toReduce, knowledge);
        }
        reduceWithKnowledge(toReduce, knowledge);

        System.out.println("Post-analysis: toReduce size: " + toReduce.size() + ", knowledge size:" + knowledge.size() + ", the ground truth is size: " + groundTruth.size());
        statistics(toReduce);

        if(printFinalKnowledge){
            System.out.println("Printing full knowledge");
            for(Transaction tx : knowledge.values()){
                System.out.println(tx);
            }
            System.out.println("End of full knowledge");
        }

        if(printFinalToReduce){
            System.out.println("Printing toReduce");
            for(Transaction tx : toReduce.values()){
                System.out.println(tx);
            }
            System.out.println("End of toReduce");
        }
    }

}