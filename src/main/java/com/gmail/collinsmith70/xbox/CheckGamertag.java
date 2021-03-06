package com.gmail.collinsmith70.xbox;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

public class CheckGamertag {
    
    private static final boolean DEBUG = false;
    
    private static final String ENDPOINT
            = "http://checkgamertag.com/CheckGamertag.php";
    
    private static final DateFormat TIMESTAMP
            = new SimpleDateFormat("HH:mm:ss.SSS");
    
    public static void main(String[] args) {
        if (args.length < 4) {
            System.out.println("Usage: <CheckGamertag>.jar "
                    + "<delay> <gmail> <app_password> <gamertag> ...");
            System.exit(0);
        }
        
        int delay = 1000;
        try {
            delay = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.out.println("Invalid delay specified: " + args[0]);
        }
        
        String gmail = args[1];
        String password = args[2];
        List<String> gamertags = new ArrayList<>(
                Arrays.asList(args).subList(3, args.length));
        while (!gamertags.isEmpty()) {
            for (Iterator<String> it = gamertags.iterator();
                    it.hasNext();) {
                String gamertag = it.next();
                System.out.printf(Locale.ROOT, "[%s] Checking %s... ",
                        TIMESTAMP.format(new Date()), gamertag);
                System.out.flush();
                String response = checkGamertag(gamertag);
                switch (response) {
                    case "taken":
                        System.out.println("Not available!");
                        break;
                    case "available":
                        System.out.println(
                                "Gamertag " + gamertag + " is available!");
                        System.out.print("Sending email... ");
                        System.out.flush();
                        sendEmailNotification(
                                gmail, password, EmailMethod.TLS, gamertag);
                        System.out.println("done");
                        it.remove();
                        break;
                    default:
                        if (DEBUG) {
                            System.out.println("Unexpected response: " + response);
                        }
                        
                        break;
                }
            }
            
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
            } finally {
                continue;
            }
        }        
    }
    
    public static String checkGamertag(String gamertag) {
        CloseableHttpClient client = HttpClients.createDefault();
        HttpPost post = new HttpPost(ENDPOINT);
        
        CloseableHttpResponse response = null;
        try {
            HttpEntity body
                = new StringEntity(
                        "tag=" + gamertag + "&t=0.8842985186650172",
                        ContentType.APPLICATION_FORM_URLENCODED);
            post.setEntity(body);
        
            response = client.execute(post);
            return EntityUtils.toString(response.getEntity());
        } catch (IOException e) {
            return e.getMessage();
        } finally {
            if (response != null) {
                try {
                    response.close();
                } catch (IOException e) {
                    return e.getMessage();
                }
            }
        }
    }
    
    public enum EmailMethod { SSL, TLS }
    
    public static void sendEmailNotification(
            String email, String password,
            EmailMethod emailMethod, String gamertag) {
        Properties props = new Properties();
        if (DEBUG) {
            props.put("mail.debug", "true");
        }
        
        switch (emailMethod) {
            case SSL:
                props.put("mail.smtp.host", "smtp.gmail.com");
                props.put("mail.smtp.socketFactory.port", "465");
                props.put("mail.smtp.socketFactory.class",
                        "javax.net.ssl.SSLSocketFactory");
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.port", "465");
                props.put("mail.smtp.ssl.enable", "true");
                break;
            case TLS:
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.starttls.required", "true");
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.host", "smtp.gmail.com");
                props.put("mail.smtp.port", "587");
                break;
            default:
                throw new IllegalArgumentException(
                        "Unsupported method given: " + emailMethod);
        }
        
        Session session = Session.getDefaultInstance(props, null);
        try {
            Message msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(
                    email));
            msg.addRecipient(Message.RecipientType.TO, new InternetAddress(
                    email));
            msg.setSubject("Xbox Live Gamertag \'" + gamertag + "\'Available");
            msg.setText(gamertag + " is now avaialble!!!\n"
                    + "https://account.xbox.com/en-US/changegamertag");
           
            Transport.send(msg, email, password);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
        
}
