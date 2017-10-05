package client;

import compute.PresenceService;
import compute.RegistrationInfo;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class Client {

    public static void main(String args[]) {
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }

        Map<String, String> argsMap = parseArgs(args);
        String username = argsMap.get("username");
        String rmiHost = argsMap.get("host");
        Integer rmiPort = Integer.parseInt(argsMap.get("port"));
        String myAddress = "localhost";
        try {
            myAddress = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }


        BufferedReader is = new BufferedReader(new InputStreamReader(System.in));


        Integer listenPort = getListenPort(is);
        RegistrationInfo regInfo = new RegistrationInfo(username, myAddress, listenPort, true);
        Thread listenerThread = new Thread(new ClientListener(listenPort));
        listenerThread.start();

        try {
            String name = "PresenceServer";
            Registry registry = LocateRegistry.getRegistry(rmiHost, rmiPort);
            PresenceService server = (PresenceService) registry.lookup(name);
            if (server.lookup(username) != null) {
                System.out.println("Username is already in use, goodbye!");
                System.exit(1);
            }

            server.register(regInfo);
            Boolean running = true;
            while (running) {
                printGreeting();
                String input = is.readLine();
                String[] inputTokens = input.split(" ");
                if (inputTokens.length < 1) {
                    inputTokens = new String[1];
                    inputTokens[0] = "";
                }
                switch (inputTokens[0]) {
                    case "friends":
                        System.out.println("Users online right now:\n");
                        Vector<RegistrationInfo> users = server.listRegisteredUsers();
                        for (RegistrationInfo user : users) {
                            System.out.println(user.getUserName() + " -- online: " + user.getStatus() + "\n");
                        }
                        break;
                    case "talk":
                        if (inputTokens.length < 3) {
                            System.out.println("Must have a username and a message");
                            break;
                        }
                        String talkTarget = inputTokens[1];
                        String msg = buildMessage(inputTokens);
                        sendMessageToUser(username, msg, server, talkTarget);
                        break;
                    case "broadcast":
                        Vector<RegistrationInfo> allUsers = server.listRegisteredUsers();
                        if (inputTokens.length < 2) {
                            System.out.println("Broadcast must have a message");
                            break;
                        }
                        String bcastMessage = buildBroadcastMessage(inputTokens);
                        for (RegistrationInfo aUser : allUsers) {
                            if (!aUser.getUserName().equals(username)) {
                                sendMessageToUser(username, " (broadcast) " + bcastMessage, server, aUser.getUserName());
                            }
                        }
                        break;
                    case "busy":
                        RegistrationInfo myBusyInfo = server.lookup(username);
                        if (myBusyInfo != null) {
                            regInfo.setStatus(false);
                            server.updateRegistrationInfo(regInfo);
                            System.out.println("Set user <" + username + "> to 'busy'\n");
                        }
                        break;
                    case "available":
                        RegistrationInfo myAvailableInfo = server.lookup(username);
                        if (myAvailableInfo != null) {
                            regInfo.setStatus(true);
                            server.updateRegistrationInfo(regInfo);
                            System.out.println("Set user <" + username + "> to 'available'\n");
                        }
                        break;
                    case "exit":
                        System.out.println("See ya!");
                        server.unregister(username);
                        System.exit(0);
                    default:
                        System.out.println("Sorry, didn't understand that\n");
                }
            }
        } catch (Exception e) {
            System.err.println("Client exception:");
            e.printStackTrace();
        }
    }

    private static Integer getListenPort(BufferedReader is) {
        try {
            System.out.println("What port will you receive messages on?");
            String input = is.readLine();
            return Integer.parseInt(input);
        } catch (IOException e) {
            System.out.println("Didn't catch that, defaulting to listen port 9999");
            return 9999;
        }
    }

    private static void sendMessageToUser(String username, String trimmedMessage, PresenceService server, String talkTarget) {
        try {
            RegistrationInfo talkTargetInfo = server.lookup(talkTarget);
            if (talkTargetInfo != null) {
                Boolean talkTargetStatus = talkTargetInfo.getStatus();
                if (talkTargetStatus) {
                    String talkTargetHost = talkTargetInfo.getHost();
                    int talkTargetPort = talkTargetInfo.getPort();
                    Socket clientSocket = new Socket(talkTargetHost, talkTargetPort);
                    DataOutputStream os = new DataOutputStream(clientSocket.getOutputStream());
                    os.writeBytes(username + " : " + trimmedMessage);
                    os.close();
                    clientSocket.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static String buildBroadcastMessage(String[] inputTokens) {
        String message = "";
        for (int i = 1; i < inputTokens.length; i++) {
            message += inputTokens[i] + " ";
        }
        String trimmedMessage = message.trim();
        return trimmedMessage;
    }

    private static String buildMessage(String[] inputTokens) {
        String message = "";
        for (int i = 2; i < inputTokens.length; i++) {
            message += inputTokens[i] + " ";
        }
        String trimmedMessage = message.trim();
        return trimmedMessage;
    }

    private static Map parseArgs(String[] args) {
        HashMap<String, String> argsMap = new HashMap<>();
        String username = args[0];
        String host = "localhost";
        String port = "1099";
        if (args.length == 2) {
            String[] hostAndPort = args[1].split(":");
            int hpLength = hostAndPort.length;
            if (hpLength == 1) {
                host = hostAndPort[0];
            } else if (hpLength == 2) {
                host = hostAndPort[0];
                port = hostAndPort[1];
            }
        }
        argsMap.put("username", username);
        argsMap.put("host", host);
        argsMap.put("port", port);
        return argsMap;
    }

    private static void printGreeting() {
        System.out.println("What would you like to do?");
        System.out.println("friends - list all available users");
        System.out.println("talk {username} {message} - send a message to user");
        System.out.println("broadcast {message} - send a message to all users");
        System.out.println("busy - set your status to 'busy'");
        System.out.println("available - set your status to 'available'");
        System.out.println("exit - exit the chat\n\n");
    }

}
