package Dandelion;

import Beam.BeamNode;
import Plain.PlainNode;

import java.util.ArrayList;
import java.util.Random;

public class Network {

    private int numberOfNodes, numberOfConnections, numberOfRounds, sleepBetweenRounds;
    private ArrayList<DandelionNode> nodes = new ArrayList<DandelionNode>();

    public Network(int numberOfNodes, int numberOfConnections, int numberOfRounds, int sleepBetweenRounds, String type){
        this.numberOfConnections = numberOfConnections;
        this.numberOfNodes = numberOfNodes;
        this.numberOfRounds = numberOfRounds;
        this.sleepBetweenRounds = sleepBetweenRounds;
        createNodes(type);
        makeConnections();
    }

    public int getSize(){
        return nodes.size();
    }

    public ArrayList<DandelionNode> getNodes(){
        return this.nodes;
    }

    private void createNodes(String type){
        for(int i = 0; i < this.numberOfNodes; i++){
            if(type.equals("Plain")){
                PlainNode n = new PlainNode("Node" + i, i);
                n.init(this.numberOfRounds, this.sleepBetweenRounds);
                nodes.add(n);
            } else if(type.equals("Beam")){
                BeamNode n = new BeamNode("Node" + i, i);
                n.init(this.numberOfRounds, this.sleepBetweenRounds);
                nodes.add(n);
            }
        }
    }

    private void makeConnections(){
        Random rand = new Random();
        for (int i = 0; i < this.numberOfNodes; i++){
            for(int j = 0; j < this.numberOfConnections; j++){
                int peer = rand.nextInt(nodes.size());
                while(peer == i || nodes.get(i).getPeers().contains(nodes.get(peer)) ){
                    peer = rand.nextInt(nodes.size());
                }
                nodes.get(i).addPeer(nodes.get(peer));
            }
        }
    }

    public void startNodes(){
        for(DandelionNode node : nodes){
            node.start();
        }
    }

    public void waitForRun() {
        for(DandelionNode node: nodes){
            try {
                node.join();
            } catch (InterruptedException e){
                e.printStackTrace();
            }
        }
    }

    public void printStats(){
        int tot = 0;
        int min = Integer.MAX_VALUE;
        int max = 0;
        DandelionNode n = null;

        for(DandelionNode node : nodes){
            int a = node.numberOfDelivedMessages();
            tot+= a;
            if (a < min){
                n = node;
                min = a;
            } else if(a > max){
                max = a;
            }
        }

        int connection_count = 0;
        for (DandelionNode node : nodes){
            if (node.getPeers().contains(n)){
                connection_count++;
            }
        }

        System.out.println("Minimum number of messages received: " + min + " : " + tot / nodes.size() + " : " + max);
        System.out.println(connection_count + " is connected to the worst: " + n);
    }

    public DandelionNode getNode(int index){
        return nodes.get(index);
    }

}
