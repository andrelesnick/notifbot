package notif.bot;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class ConfigHelper {
    private static String cwd; //current working directory
    private static String token; //bot token
    private static int server; //server ID
    private static HashMap<String , LocalTime> lastMessages; //channels and time of last message sent in each channel
    private static int cooldown; //minutes
    private static String pref; //command prefix
    private static File cfg; //the config file, settings.txt
    private static String sep; //line separator
    private static HashMap<String, ArrayList<String>> users; //the recipients of these notifications and the channels they're watching
    private static HashMap<String, String> help = initHelp(); //list of commands

    public static void initializeVars() {
        cfg = new File("settings.txt");
        System.out.println("ConfigHelper initialized!");
        sep = System.getProperty("line.separator");
        cwd = System.getProperty("user.dir");
        lastMessages = new HashMap<String, LocalTime>();
        users = new HashMap<String, ArrayList<String>>();
        cooldown = 30; //default value
        try {
            if (cfg.createNewFile()) {
                FileWriter writer = new FileWriter(cfg, true);
                writer.write("#Insert your bot's authentication token below https://i.imgur.com/DoJn80b.png" + sep);
                writer.write("token=" + sep);
                writer.write("#this is the prefix for your commands" + sep);
                writer.write("pref=." + sep);
                writer.write("#cooldown for new conversations (minutes)" + sep);
                writer.write("cooldown=30" + sep);
                writer.close();
            }
        } catch (Exception e) {
            System.out.println(e);
        }

        try {
            Scanner myReader = new Scanner(cfg);
            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();
                int start = data.indexOf("=") + 1;
                if (data.startsWith("token=")) {
                    token = data.replace(" ", "").substring(start);
                    System.out.println("token=" + token);
                } else if (data.startsWith("server=")) {
                    server = Integer.parseInt(data.replace(" ", "").substring(start));
                    System.out.println("server=" + server);
                } else if (data.startsWith("channel,user=")) { //contains channel followed by a user watching
                    String[] split = data.replace(" ", "").substring(start).split(",");
                    String channel = split[0];
                    String user = split[1];
                    updateChannel(channel);
                    if (!users.containsKey(user)) {
                        users.put(user, new ArrayList<String>());
                    }
                    users.get(user).add(channel); //adds channel to user

                } else if (data.startsWith("cooldown=")) {
                    cooldown = Integer.parseInt(data.replace(" ", "").substring(start));
                    System.out.println("cooldown=" + cooldown);
                } else if (data.startsWith("pref=")) {
                    pref = data.replace(" ", "").substring(start);
                    System.out.println("pref=" + pref);
                }
                //System.out.println(data);
            }
            myReader.close();
        } catch (Exception e) {
            System.out.println("Error reading\n" + e);
            e.printStackTrace();
        }
    }

    public static void addItem(String type, String id) {
        try {
            FileWriter writer = new FileWriter(cfg, true);
            writer.write(type + "=" + id + sep);
            writer.close();
        } catch (Exception e) {
            System.out.println("Error adding " + type + " | " + id);
            System.out.println(e);
        }
    }

    public static boolean removeItem(String type, String id) {
        String data;
        boolean removed = false;
        try {
            File temp = new File("temp.txt");
            temp.createNewFile();
            Scanner reader = new Scanner(cfg);
            FileWriter writer = new FileWriter(temp, false); //false -> should override the original file

            while (reader.hasNextLine()) {
                data = reader.nextLine();
                if (data.contains(type + "=" + id)) {
                    removed = true;
                    System.out.println("if: " + data);
                }
                else if (type.equals("channel,user") && data.contains(id)){ //special case for removeAll()
                    removed = true;
                    System.out.println("else if: " + data);
                } else {
                    writer.write(data + sep);
                    System.out.println("else: " + data);
                }
            }
            writer.close();
            reader.close();
            cfg.delete();
            System.out.println(temp.renameTo(cfg));


        } catch (Exception e) {
            System.out.println("Error writing | " + type + "|" + id + "\n" + e);
        }
        return removed;
    }

    public static boolean editItem(String type, String id) { //currently only works with variables with only one instance
        String data;
        boolean edited = false;
        try {
            File temp = new File("temp.txt");
            temp.createNewFile();
            Scanner reader = new Scanner(cfg);
            FileWriter writer = new FileWriter(temp, false); //false -> should override the original file

            while (reader.hasNextLine()) {
                data = reader.nextLine();
                if (data.startsWith(type)) {
                    writer.write(type + "=" + id + sep);
                    System.out.println("if: " + data);
                } else {
                    writer.write(data + sep);
                    System.out.println("else: " + data);
                }
            }
            writer.close();
            reader.close();
            cfg.delete();
            System.out.println(temp.renameTo(cfg));
        } catch (Exception e) {
            System.out.println("Error writing | " + type + "|" + id + "\n" + e);
        }
        return edited;
    }

    public static String getToken() {
        return token;
    }

    public static String getPref() {
        return pref;
    }

    public static int getCooldown() {
        return cooldown;
    }

    public static void setCooldown(int cd) {
        if (cd != cooldown) {
            cooldown = cd;
            editItem("cooldown", Integer.toString(cd));
        }
    }

    public static boolean updateChannel(String channel) {
        if (!lastMessages.containsKey(channel)) {
            lastMessages.put(channel, LocalTime.now());
            System.out.println(channel+" added to lastMessages");
        } else if (LocalTime.now().isAfter(lastMessages.get(channel).plusMinutes(cooldown))) {
            lastMessages.replace(channel, LocalTime.now());
            return true;
        } else {
            lastMessages.replace(channel, LocalTime.now());
            System.out.println("[not cooldown time yet]\nCurrent Time:" + LocalTime.now() + "|cdTime: " + lastMessages.get(channel).plusMinutes(cooldown));
        }
        return false;
    }

    public static ArrayList<String> notifyUsers(String channel) { //returns a list of users to send a notification to
        ArrayList<String> notify = new ArrayList<String>();
        for (String s: users.keySet()) {
            if (users.get(s).contains(channel)) {
                notify.add(s);
            }
        }
        return notify;
    }

    public static String addUser(String channel, String user) {
        String bool;
        String item = channel + "," + user;
        if (isWatching(channel, user)) {
            bool = "Error: You're already watching this channel.";
        }
        else {
            addItem("channel,user", item);
            if (users.containsKey(user)) {
                users.get(user).add(channel);
            }
            else {
                ArrayList<String> temp = new ArrayList<String>();
                temp.add(channel);
                users.put(user,temp);
            }
            bool = "You are now watching this channel!";
        }
        return bool;
    }

    public static String removeUser(String channel, String user) {
        String bool;
    String item = channel + "," + user;
    if (!isWatching(channel, user)) {
        bool = "Error: You're not watching this channel.";
    }
    else {
        removeItem("channel,user", item);
        users.get(user).remove(channel);
        bool = "You are no longer watching this channel!";
    }
    return bool;
    }

    public static String removeAll(String user) {
        if (users.containsKey(user)) {
            users.put(user, new ArrayList<String>()); //replaces existing channels with empty array
            removeItem("channel,user", user);
        }
        return "You are no longer watching any channels!";
    }

    public static HashMap<String,String> initHelp() { //order these such that there are no conflicts with 'contains', so remove goes before removeall
        HashMap<String, String> temp = new HashMap<String, String>();
        temp.put("help", "it helps you lol\nUsage: help [command]");
        temp.put("cooldown", "set length of time of inactivity in channels before a new notification is sent.\nDefault is 30 minutes.\nUsage: cooldown [minutes] or just cooldown to see current amount");
        temp.put("remove", "removes yourself from receiving notifications from the current channel.");
        temp.put("add", "adds yourself to the list of users receiving notifications from the current channel");
        temp.put("removeall", "removes yourself from receiving notifications from ALL channels.");
        temp.put("pref", "change the command prefix\nDefault is \".\"\nUsage:pref [prefix]");
        return temp;
    }
    //returns whether or not a user is watching the specified channel
    public static boolean isWatching(String channel, String user) {
        if (users.containsKey(user) && users.get(user).contains(channel)) {
            return true;
        }
        return false;
    }

}
