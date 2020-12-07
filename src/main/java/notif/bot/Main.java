package notif.bot;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.requests.RestAction;

import javax.security.auth.login.LoginException;
import java.io.File;

public class Main {
    static JDA jda;
    public static void main(String[] args) {
        ConfigHelper.initializeVars();
        try {
            jda = JDABuilder.createDefault(ConfigHelper.getToken()) // The token of the account that is logging in.
                    .addEventListeners(new Messager())   // An instance of a class that will handle events.
                    .build();
            jda.awaitReady(); // Blocking guarantees that JDA will be completely loaded.
            System.out.println("Finished Building JDA!");
        }
        catch (LoginException e)
        {
            //If anything goes wrong in terms of authentication, this is the exception that will represent it
            e.printStackTrace();
        }
        catch (InterruptedException e)
        {
            //Due to the fact that awaitReady is a blocking method, one which waits until JDA is fully loaded,
            // the waiting can be interrupted. This is the exception that would fire in that situation.
            //As a note: in this extremely simplified example this will never occur. In fact, this will never occur unless
            // you use awaitReady in a thread that has the possibility of being interrupted (async thread usage and interrupts)
            e.printStackTrace();
        }
        //catch () //this should be when it tries to access a text file
    }

    public static void sendPM(String id, String content) {
        // Retrieve the user by their id
        RestAction<User> action = jda.retrieveUserById(id);
        action.queue(
                // Handle success if the user exists
                (user) -> user.openPrivateChannel().queue(
                        (channel) -> channel.sendMessage(content).queue()),

                // Handle failure if the user does not exist (or another issue appeared)
                (error) -> error.printStackTrace()
        );
    }


}
