package notif.bot;

import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.restaction.MemberAction;
import net.dv8tion.jda.internal.entities.UserById;

import java.util.HashMap;

public class Messager extends ListenerAdapter {
    HashMap<String, String> help;
    public Messager() {
        help = ConfigHelper.initHelp();
    }
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        User author = event.getAuthor();                //The user that sent the message
        Message message = event.getMessage();           //The message that was received.
        MessageChannel channel = event.getChannel();     //This is the MessageChannel that the message was sent to.
        String msg = message.getContentRaw();
        Guild guild = null;
        boolean isGuild = false;
        String pref = ConfigHelper.getPref();
        boolean bot = author.isBot();                    //This boolean is useful to determine if the User that
        // sent the Message is a BOT or not!
        if (channel.getType().equals(ChannelType.TEXT)) {
            isGuild = true;
            guild = event.getGuild();
        }

        if (isGuild && !bot) {
            //simple output for all messages
            System.out.println("[" + guild.getName() + "] " + "Name: " + author.getName() + " | " + channel.getName() + " (" + channel.getId() + ")\nID: " + author.getId() + "\n   " + message.getContentDisplay() + "\n");



            //resets the timer for the channel and checks to send notifications
            if (ConfigHelper.updateChannel(channel.getId())) {
                System.out.println("----------\n\nSending out notifications!\n\n----------");
                for (String id: ConfigHelper.notifyUsers(channel.getId())) {
                    String pm =
                            "There's a new message in #" + channel.getName()+"!";
                    Main.sendPM(id, pm);
                }
            }



            //checks for commands

            if (msg.startsWith(pref + "ping")) {
                channel.sendMessage("pong").queue();
            }
            if (msg.startsWith(pref + "pref")) {
                if (msg.length() != 7) {
                    channel.sendMessage("Error: prefix not specified or not of length 1").queue();
                }
                else if (msg.substring(msg.length()-1).equals(pref)) {
                    channel.sendMessage("Error: you already have this prefix").queue();
                }
                else {
                    ConfigHelper.editItem("pref", msg.substring(6));
                    ConfigHelper.initializeVars();
                    pref = ConfigHelper.getPref();
                    channel.sendMessage("Prefix successfully changed to '"+pref+"'").queue();
                }
            }
            if (msg.startsWith(pref+"cooldown ")) {
                ConfigHelper.removeItem("cooldown", msg.substring(10));
                channel.sendMessage("Cooldown successfully changed to " + msg.substring(10) + " minutes").queue();

            }
            if (msg.startsWith(pref+"help")) {
                if (msg.length()<=6) {
                    channel.sendMessage("Here is a list of commands.\nType \""+pref+"help [command] for more details.").queue();
                    String mssage = "```";
                    for (String s: help.keySet()) {
                        mssage += "\n"+s;
                    }
                    mssage += "\n```";
                    channel.sendMessage(mssage).queue();
                }
                else if (msg.length() > 6) {
                    boolean valid = false; //valid parameter
                    for (String s: help.keySet()) {
                        if (msg.substring(6).contains(s)) {
                            channel.sendMessage(help.get(msg.substring(6).split(" ")[0])).queue();
                            valid = true;
                            break;
                        }
                    }
                    if (!valid) {
                        channel.sendMessage("Error: command not found.").queue();
                    }
                }
            }

            if (msg.startsWith(pref+"cooldown")) {
                if (msg.length() <= 10) {
                    channel.sendMessage("The current cooldown is "+ConfigHelper.getCooldown()+" minutes.\nType \"cooldown [min]\" to set a new one.").queue();
                }
                else if (msg.length() > 10) {
                    try {
                        int cd = Integer.parseInt(msg.substring(10));
                        if (cd < 0) {
                            channel.sendMessage("Error: Please use a positive number.");
                        }
                        else {
                            ConfigHelper.setCooldown(cd);
                        }
                    }
                    catch (Exception e) {
                        channel.sendMessage("Error: please enter a valid number").queue();
                    }
                }
            }

            if (msg.equals(pref+"add")) {
                channel.sendMessage(ConfigHelper.addUser(channel.getId(), author.getId())).queue();
            }
            if (msg.equals(pref+"remove")) {
                channel.sendMessage(ConfigHelper.removeUser(channel.getId(), author.getId())).queue();
            }
            if (msg.equals(pref+"removeall")) {
                channel.sendMessage(ConfigHelper.removeAll(author.getId())).queue();
            }

        }
    }

    public static void sendPM(User user, String message) {
        try {
            user.openPrivateChannel().complete()
                    .sendMessage(message.substring(0, Math.min(message.length(), 1999))).queue();
        } catch (ErrorResponseException ignored) {
        }
    }
}
