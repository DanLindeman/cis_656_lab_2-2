package server;

import compute.PresenceService;
import compute.RegistrationInfo;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class PresenceServiceImpl implements PresenceService {

    private Hashtable<String, RegistrationInfo> registry = new Hashtable<>();

    public static void main(String[] args) {
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }
        try {
            String name = "PresenceServer";
            PresenceServiceImpl server = new PresenceServiceImpl();
            PresenceService stub = (PresenceService) UnicastRemoteObject.exportObject(server, 0);
            Registry registry = LocateRegistry.getRegistry();
            registry.rebind(name, stub);
            System.out.println("Server bound");
        } catch (Exception e) {
            System.err.println("Server exception:");
            e.printStackTrace();
        }
    }

    public boolean register(RegistrationInfo reg) {
        if (!registry.containsKey(reg.getUserName())) {
            registry.put(reg.getUserName(), reg);
            System.out.println("Added " + reg.getUserName() + " to registry");
            return true;
        } else {
            System.out.println("User " + reg.getUserName() + " already in registry");
            return false;
        }
    }

    public boolean updateRegistrationInfo(RegistrationInfo reg) {
        if (registry.containsKey(reg.getUserName())) {
            registry.put(reg.getUserName(), reg);
            System.out.println("Updated User " + reg.getUserName());
            return true;
        } else {
            System.out.println("User " + reg.getUserName() + " not in registry");
            return false;
        }
    }

    public void unregister(String userName) {
        registry.remove(userName);
        System.out.println("Removed " + userName + " from registry");
    }

    public RegistrationInfo lookup(String name) {
        return registry.get(name);
    }

    public Vector<RegistrationInfo> listRegisteredUsers() {
        Vector<RegistrationInfo> users = new Vector<>();
        users.addAll(registry.values());
        return users;
    }
}
