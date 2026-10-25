// Author: Macaraig
import java.util.ArrayList;

// Binary search tree of teams, keyed by team name (not case-sensitive).
public class TeamTree {
    private class Node {
        String key;        // team name in lowercase
        Team team;
        Node left, right;

        Node(Team team) {
            this.team = team;
            this.key = team.getName().toLowerCase();
        }
    }

    private Node root;
    private int size;

    public void addJobOrder(String teamName, String contactPerson, String contactNo, int joNumber) {
        root = insert(root, teamName, contactPerson, contactNo, joNumber);
    }

    private Node insert(Node node, String teamName, String contactPerson, String contactNo, int joNumber) {
        if (node == null) {
            Node newNode = new Node(new Team(teamName, contactPerson, contactNo));
            newNode.team.addJobOrder(joNumber, contactPerson, contactNo);
            size++;
            return newNode;
        }
        int cmp = teamName.toLowerCase().compareTo(node.key);
        if (cmp < 0) {
            node.left = insert(node.left, teamName, contactPerson, contactNo, joNumber);
        } else if (cmp > 0) {
            node.right = insert(node.right, teamName, contactPerson, contactNo, joNumber);
        } else {
            node.team.addJobOrder(joNumber, contactPerson, contactNo);   // returning team
        }
        return node;
    }

    public Team find(String teamName) {
        Node node = root;
        String key = teamName.toLowerCase();
        while (node != null) {
            int cmp = key.compareTo(node.key);
            if (cmp == 0) {
                return node.team;
            }
            node = (cmp < 0) ? node.left : node.right;
        }
        return null;
    }

    // all teams in A to Z order
    public ArrayList<Team> inOrder() {
        ArrayList<Team> out = new ArrayList<>();
        inOrder(root, out);
        return out;
    }

    private void inOrder(Node node, ArrayList<Team> out) {
        if (node == null) {
            return;
        }
        inOrder(node.left, out);
        out.add(node.team);
        inOrder(node.right, out);
    }

    // Teams whose name starts with the prefix, A to Z.
    // Only goes into the parts of the tree that can still have a match.
    public ArrayList<Team> startsWith(String prefix) {
        ArrayList<Team> out = new ArrayList<>();
        startsWith(root, prefix.toLowerCase(), out);
        return out;
    }

    private void startsWith(Node node, String prefix, ArrayList<Team> out) {
        if (node == null) {
            return;
        }
        boolean match = node.key.startsWith(prefix);
        if (node.key.compareTo(prefix) > 0) {
            startsWith(node.left, prefix, out);     // smaller names may still match
        }
        if (match) {
            out.add(node.team);
        }
        if (match || node.key.compareTo(prefix) < 0) {
            startsWith(node.right, prefix, out);    // larger names may still match
        }
    }

    public int size() {
        return size;
    }
}
