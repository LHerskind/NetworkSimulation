package utils;

import Dandelion.DandelionNode;
import Dandelion.Message;
import Dandelion.Network;
import Dandelion.Transaction;

import java.util.*;

public class KnowledgeUtil {

    /**
     * TODO: I need to make this an object, so that it can store current progress, and add one node after another.
     * *  The current method is using to much time as a lot of the analysis is recomputed every time.
     **/

    private Network network;
    private ArrayList<DandelionNode> nodes;

    private HashMap<Integer, Transaction> toReduce;
    private HashSet<String> groundTruth;
    private HashMap<Integer, Transaction> knowledge;
    private HashSet<Integer> messageSeen;
    private HashSet<Double> hasLookedAtReduceWithoutKnowledge = new HashSet<>();
    private HashSet<Double> hasLookedAtReduceWithIntersection = new HashSet<>();

    public KnowledgeUtil(Network network) {
        this.network = network;
    }

    public ArrayList<DandelionNode> getBestNodes(int number) {
        ArrayList<DandelionNode> sortedNodes = network.getSortedNodes();
        return new ArrayList<>(sortedNodes.subList(0, number));
    }

    public void initialise() {
        this.nodes = new ArrayList<>();
        this.toReduce = new HashMap<>();
        this.groundTruth = new HashSet<>();
        this.knowledge = new HashMap<>();
        this.messageSeen = new HashSet<>();
        this.hasLookedAtReduceWithoutKnowledge = new HashSet<>();
        this.hasLookedAtReduceWithIntersection = new HashSet<>();
    }

    public void addNode(DandelionNode node) {
        this.nodes.add(node);
        addKnowledge(node);
        addReceivedMessages(node);
        performReduction();
        System.out.println("Post-analysis(" + this.nodes.size() + "): toReduce size: " + this.toReduce.size() + ", knowledge size: " + this.knowledge.size() + ", the ground truth is size: " + this.groundTruth.size());
    }

    // TODO: Reevaluate, is this necessary, dont we get it all from addReceivedMessages?
    private void addKnowledge(DandelionNode node) {
        HashMap<Integer, Message> nodeMessages = node.getMessagesCreated();
        for (Message msg : nodeMessages.values()) {
            Transaction tx = msg.getMessage();
            this.knowledge.putIfAbsent(tx.hashCode(), tx);
        }
    }

    private void addToGroundTruth(Transaction tx) {
        this.groundTruth.addAll(tx.getInputs());
    }

    private void addReceivedMessages(DandelionNode node) {
        HashMap<Integer, Message> nodeMessages = node.getDeliveredMessages();
        for (Message msg : nodeMessages.values()) {
            Transaction tx = msg.getMessage();
            if (!this.messageSeen.contains(tx.hashCode())) {
                this.messageSeen.add(tx.hashCode());
                addToGroundTruth(tx);
                if (tx.isConclusive()) {
                    this.knowledge.putIfAbsent(tx.hashCode(), tx);
                } else if (!tx.isEmpty()) {
                    this.toReduce.putIfAbsent(tx.hashCode(), tx);
                }
            }
        }
    }

    /**
     * TODO: We should be able to make reductions smarter, taking into account the changes from the new node.
     * Instead of it all once more.
     */
    public void performReduction() {
        reduceWithKnowledge(this.toReduce, this.knowledge);
        reduceIntersectionOrDisjoint(this.toReduce, this.knowledge);
        reduceWithKnowledge(this.toReduce, this.knowledge);
        int toReduceSize = this.toReduce.size();
        reduceWithoutKnowledge(this.toReduce, this.knowledge);
        if(toReduceSize < this.toReduce.size()){
            reduceWithoutKnowledge(this.toReduce, this.knowledge);
        }
        reduceWithKnowledge(this.toReduce, this.knowledge);
    }


    // TODO: This could possibly reduced with new knowledge instead of looping through the entire knowledge-set
    private void reduceWithKnowledge(HashMap<Integer, Transaction> toReduce, HashMap<Integer, Transaction> knowledge) {
        boolean go_another_run = true;
        while (go_another_run) {
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
                                toRemove.add(tx_reduce);
                                break;
                            } else if (!reduced.isEmpty()) {
                                toAdd.add(reduced);
                                toRemove.add(tx_reduce);
                                break;
                            }
                        }
                    }
                }
            }
            for (Transaction tx : toRemove) {
                toReduce.remove(tx.hashCode());
            }
            for (Transaction tx : toAdd) {
                toReduce.putIfAbsent(tx.hashCode(), tx);
            }
        }
    }

    private void actualIntersection(HashMap<Integer, Transaction> knowledge, String inputString){
        Transaction txKnowledge = new Transaction();
        txKnowledge.getInputs().add(inputString);
        String outputString = inputString.replace('I', 'O');
        txKnowledge.getOutputs().add(outputString);
        knowledge.putIfAbsent(txKnowledge.hashCode(), txKnowledge);
        //System.out.println("Reduced through intersection? " + txKnowledge);
    }

    private void reduceIntersectionOrDisjoint(HashMap<Integer, Transaction> toReduce, HashMap<Integer, Transaction> knowledge) {
        // Initially, just take the intersections, where only a single transactions is left.
        Collection<Transaction> toReduceCollection = toReduce.values();

        for (Transaction tx_outer : toReduceCollection) {
            for (Transaction tx_inner : toReduceCollection) {
                double hasLooked = tx_outer.hashCode() * 13 + tx_inner.hashCode();
                if(hasLookedAtReduceWithIntersection.contains(hasLooked)){
                    continue;
                }
                hasLookedAtReduceWithIntersection.add(hasLooked);

                if (!tx_outer.equals(tx_inner)) {
                    HashSet<String> intersectionInputs = new HashSet<>(tx_outer.getInputs());
                    intersectionInputs.retainAll(tx_inner.getInputs());
                    HashSet<String> disjointInputs = new HashSet<>(tx_outer.getInputs());
                    disjointInputs.addAll(tx_inner.getInputs());
                    disjointInputs.removeAll(intersectionInputs);
                    if (intersectionInputs.size() == 1) {
                        String inputString = intersectionInputs.iterator().next();
                        actualIntersection(knowledge, inputString);
                    }
                    if(disjointInputs.size() == 1){
                        String inputString = disjointInputs.iterator().next();
                        actualIntersection(knowledge, inputString);
                    }
                }
            }
        }
    }

    // TODO: We need to actually remove stuff when it is possible, as we have some issues with heap elseway
    private void reduceWithoutKnowledge(HashMap<Integer, Transaction> toReduce, HashMap<Integer, Transaction> knowledge) {
        Collection<Transaction> toReduceCollection = toReduce.values();

        HashMap<Integer, Transaction> toRemove = new HashMap<>();
        HashMap<Integer, Transaction> toAdd = new HashMap<>();
        // TODO: Have troubles with when we should remove transactions from here. If this is fixed, analysis can be performed way faster.
        // We need to make sure, that we only remove, if no knowledge is lost, i.e.
        // to_reduce + tx_reduce_inner == tx_merge + to_reduce_inner
        // to_reduce = (i1,i2,i3,i4) -> (o1,o2,o3,o4)
        // tx_reduce_inner = (i1,i2,i5) -> (o1,o2,o5)
        // tx_merge = (i3, i4) -> (o3,o4)

        for (Transaction tx_reduce : toReduceCollection) {
            for (Transaction tx_reduce_inner : toReduceCollection) {
                double hasLooked = tx_reduce.hashCode() * 13 + tx_reduce_inner.hashCode();
                if(hasLookedAtReduceWithoutKnowledge.contains(hasLooked)){
                    continue;
                }
                hasLookedAtReduceWithoutKnowledge.add(hasLooked);
                if(toRemove.containsKey(tx_reduce.hashCode())){
                    break;
                }
                if(toRemove.containsKey(tx_reduce_inner.hashCode())){
                    continue;
                }

                if (!tx_reduce.equals(tx_reduce_inner) && isOverlapping(tx_reduce, tx_reduce_inner)) {
                    if (!isASubsetOfB(tx_reduce, tx_reduce_inner)) {
                        Transaction tx = tx_reduce.reduce(tx_reduce_inner);
                        //if(tx.add(tx_reduce_inner).equals(tx_reduce.add(tx_reduce_inner))){
                            // We have not removed any information, we can therefore remove the transactions.
                            // We are not sure that we remove the smaller of them, but what ever atm.
                            toRemove.putIfAbsent(tx_reduce.hashCode(), tx_reduce);
                        //}

                        if (tx.isConclusive()) {
                            knowledge.putIfAbsent(tx.hashCode(), tx);
                        } else if (!tx.isEmpty()) {
                            toAdd.putIfAbsent(tx.hashCode(), tx);
                        }
                    } else {
                        // If tx_reduce is a subset of tx_reduce_inner, reduce the largest and remove it
                        Transaction tx = tx_reduce_inner.reduce(tx_reduce);
                        toRemove.putIfAbsent(tx_reduce_inner.hashCode(), tx_reduce_inner);
                        if (tx.isConclusive()) {
                            knowledge.putIfAbsent(tx.hashCode(), tx);
                        } else if (!tx.isEmpty()) {
                            toAdd.putIfAbsent(tx.hashCode(), tx);
                        }
                    }
                }
            }
        }

        for (Transaction tx : toAdd.values()) {
            toReduce.putIfAbsent(tx.hashCode(), tx);
        }

        for(Transaction tx : toRemove.values()){
            toReduce.remove(tx.hashCode());
        }
    }

    public void printKnowledge(boolean all) {
        System.out.println("Post-analysis: toReduce size: " + toReduce.size() + ", knowledge size: " + knowledge.size() + ", the ground truth is size: " + groundTruth.size());
        statistics(this.toReduce);
        if(all) {
            System.out.println("Printing full knowledge");
            for (Transaction tx : this.knowledge.values()) {
                System.out.println(tx);
            }
            System.out.println("End of full knowledge");
            System.out.println("Printing toReduce");
            for (Transaction tx : this.toReduce.values()) {
                System.out.println(tx);
            }
            System.out.println("End of toReduce");
            System.out.println("--- full analysis end ---");
        }
    }

    // TODO: Redo this, we do not need to know the number of transactions, but instead the number of conclusive that is within sets smaller than *s*.
    private void statistics(HashMap<Integer, Transaction> toReduce) {

        HashSet<String> unReducable = new HashSet<>();

        int i = 2;
        int j = 0;
        while(j < toReduce.size()){
            for(Transaction tx : toReduce.values()){
                if(tx.getInputs().size() == i){
                    unReducable.addAll(tx.getInputs());
                    j++;
                }
            }
            System.out.println("Conclusive hidden in set of size " + i + ": " + unReducable.size());
            i++;
        }
    }









    private HashMap<Integer, Transaction> getReceivedMessages(ArrayList<DandelionNode> nodes) {
        HashMap<Integer, Transaction> receivedMessages = new HashMap<>();
        for (DandelionNode node : nodes) {
            HashMap<Integer, Message> nodeMessages = node.getDeliveredMessages();
            for (Message msg : nodeMessages.values()) {
                Transaction tx = msg.getMessage();
                receivedMessages.putIfAbsent(tx.hashCode(), tx);
            }
        }
        return receivedMessages;
    }

    private HashMap<Integer, Transaction> getInitialKnowledge(ArrayList<DandelionNode> nodes) {
        HashMap<Integer, Transaction> knowledge = new HashMap<>();
        for (DandelionNode node : nodes) {
            HashMap<Integer, Message> nodeMessages = node.getMessagesCreated();
            for (Message msg : nodeMessages.values()) {
                Transaction tx = msg.getMessage();
                knowledge.putIfAbsent(tx.hashCode(), tx);
            }
        }
        return knowledge;
    }


    public void analysis(ArrayList<DandelionNode> nodes, boolean print, boolean printFinalKnowledge, boolean printFinalToReduce) {
        HashMap<Integer, Transaction> toReduce = getReceivedMessages(nodes);
        HashSet<String> groundTruth = groundTruth(toReduce);
        HashMap<Integer, Transaction> knowledge = getInitialKnowledge(nodes);
        extendKnowledgeWithReceived(toReduce, knowledge);
        if (print) {
            System.out.println("--- full analysis start ---");
            for (DandelionNode node : nodes) {
                // System.out.println(node);
            }

            System.out.println("Pre-analysis: toReduce size: " + toReduce.size() + ", knowledge size: " + knowledge.size() + ", the ground truth is size: " + groundTruth.size());
            statistics(toReduce);
        }

        for (int i = 0; i < 2; i++) {
            reduceWithKnowledge(toReduce, knowledge);
            //reduceIntersection(toReduce, knowledge);
            //reduceWithKnowledge(toReduce, knowledge);
            //reduceWithoutKnowledge(toReduce, knowledge);
        }

        reduceWithKnowledge(toReduce, knowledge);

        if (print) {
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
        } else {
            System.out.println("Post-analysis(" + nodes.size() + "): toReduce size: " + toReduce.size() + ", knowledge size: " + knowledge.size() + ", the ground truth is size: " + groundTruth.size());
        }
    }

    public static boolean isASubsetOfB(Transaction a, Transaction b) {
        return a.reduce(b).isEmpty();
    }

    private boolean isOverlapping(Transaction a, Transaction b) {
        HashSet<String> a_inputs = a.getInputs();
        HashSet<String> b_inputs = b.getInputs();
        if (a_inputs.size() > b_inputs.size()) {
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

    private HashSet<String> getUniqueInputs(HashMap<Integer, Transaction> transactions) {
        HashSet<String> uniqueInputs = new HashSet<>();
        for (Transaction tx : transactions.values()) {
            uniqueInputs.addAll(tx.getInputs());
        }
        return uniqueInputs;
    }

    private HashSet<String> groundTruth(HashMap<Integer, Transaction> messages) {
        return getUniqueInputs(messages);
    }

    private void extendKnowledgeWithReceived(HashMap<Integer, Transaction> toReduce, HashMap<Integer, Transaction> knowledge) {
        for (Iterator<Transaction> iterator = toReduce.values().iterator(); iterator.hasNext(); ) {
            Transaction tx = iterator.next();
            if (tx.isConclusive()) {
                knowledge.putIfAbsent(tx.hashCode(), tx);
                iterator.remove();
            }
        }
    }





}