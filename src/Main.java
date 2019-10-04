import Dandelion.DandelionNode;
import Dandelion.Network;
import utils.KnowledgeUtil;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;

public class Main {


    public static void main(String[] args) {
        String[] nodeTypes = {"Grin", "MyGrin", "Beam", "MyBeam"};
        for(String nodeType : nodeTypes) {
            //String nodeType = "MyGrin";
            boolean createResults = true;
            int time_in_seconds = 600;


            int numberOfRounds = time_in_seconds * 100;
            Network network = new Network(512, 8, numberOfRounds, 10, nodeType);
            KnowledgeUtil knowledgeUtil = new KnowledgeUtil(network);

            network.startNodes();
            network.waitForRun();
            network.printStats();

            if (createResults) {
                generateResults(network, knowledgeUtil, nodeType);
            } else {
                knowledgeUtil.analysis(knowledgeUtil.getBestNodes(8), true, true, true);
            }
        }
    }

    private static void generateResults(Network network, KnowledgeUtil knowledgeUtil, String nodeType){
        ArrayList<DandelionNode> observerNodes = network.getSortedNodes();
        ArrayList<DandelionNode> randomObserverNodes = network.getRandomizedNodes();

        ArrayList<DandelionNode> observerNodes1 = new ArrayList<>();
        ArrayList<DandelionNode> observerNodes2 = new ArrayList<>();
        ArrayList<DandelionNode> observerNodes3 = new ArrayList<>();

        for(int i = 0 ; i < 64; i++){
            observerNodes1.add(observerNodes.get(i));
            observerNodes2.add(observerNodes.get(observerNodes.size()- 1 - i));
            observerNodes3.add(randomObserverNodes.get(i));
        }

        try {
            PrintStream fileOut = new PrintStream("./" + nodeType + ".txt");
            System.setOut(fileOut);

            System.out.println("--- Results with most connected nodes ---");
            knowledgeUtil.initialise();
            for (DandelionNode node : observerNodes1) {
                knowledgeUtil.addNode(node);
            }

            System.out.println("--- Results with least connected nodes ---");
            knowledgeUtil.initialise();
            for (DandelionNode node : observerNodes2) {
                knowledgeUtil.addNode(node);
            }

            System.out.println("--- Results with random nodes ---");
            knowledgeUtil.initialise();
            for (DandelionNode node : observerNodes3) {
                knowledgeUtil.addNode(node);
            }

        } catch (FileNotFoundException e){
            e.printStackTrace();
        }
    }

}
