package com.jrak.gmailApiDemo.controller;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.util.Base64;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Message;
import com.jrak.gmailApiDemo.model.Letter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.*;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Properties;
import java.util.Set;
import java.util.List;

@Controller
public class EmailController {

    public static final String FROM_EMAIL_ADDERESS = "juliarak@knu.ua";

    @GetMapping("/")
    public String showMainPage() {
        return "index";
    }

    @PostMapping("/")
    public String sendEmail(HttpServletRequest request, Model model) {
        String to = request.getParameter("to");
        String subject = request.getParameter("subject");
        String body = request.getParameter("body");

        List<String> recipients = Arrays.asList(to.split("\\s*,\\s*"));
        for (String recipient : recipients) {
            Letter letter = new Letter(recipient, subject, body);
            try {
                sendMessage(letter);
                model.addAttribute("message", "Листи надіслано успішно!");
            } catch (Exception e) {
                e.printStackTrace();
                model.addAttribute("error",
                "Виникла помилка при відправленні листів: " + e.getMessage());
            }
        }
        return "index";
    }

    public static Credential getCredentials(final NetHttpTransport httpTransport) throws IOException {
        // Load client secrets.
        GsonFactory jsonFactory = GsonFactory.getDefaultInstance();
        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(jsonFactory,
                        new InputStreamReader(EmailController.class
                                .getResourceAsStream("/client_secret_851737896069-2j5b5vvfttklsojuc3kusnqn2r5ihas3." +
                                        "apps.googleusercontent.com.json")));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, jsonFactory, clientSecrets, Set.of(GmailScopes.GMAIL_SEND))
                .setDataStoreFactory(new FileDataStoreFactory(Paths.get("tokens").toFile()))
                .setAccessType("offline")
                .build();;

        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
        return credential;
    }

    public void sendMessage(Letter letter) throws GeneralSecurityException, IOException, MessagingException {
        NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        GsonFactory jsonFactory = GsonFactory.getDefaultInstance();
        Gmail service = new Gmail.Builder(httpTransport, jsonFactory, getCredentials(httpTransport))
                .setApplicationName("GmailApiDemo")
                .build();

        // Encode as MIME message
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);
        MimeMessage email = new MimeMessage(session);
        email.setFrom(new InternetAddress(FROM_EMAIL_ADDERESS));
        email.addRecipient(javax.mail.Message.RecipientType.TO,
                new InternetAddress(letter.getTo()));
        email.setSubject(letter.getSubject());
        email.setText(letter.getBody());

        // Encode and wrap the MIME message into a gmail message
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        email.writeTo(buffer);
        byte[] rawMessageBytes = buffer.toByteArray();
        String encodedEmail = Base64.encodeBase64URLSafeString(rawMessageBytes);
        Message message = new Message();
        message.setRaw(encodedEmail);

        try {
            // Create send message
            message = service.users().messages().send("me", message).execute();
            System.out.println("Message id: " + message.getId());
            System.out.println(message.toPrettyString());
        } catch (GoogleJsonResponseException e) {
            GoogleJsonError error = e.getDetails();
            if (error.getCode() == 403) {
                System.err.println("Unable to send message: " + e.getDetails());
            } else {
                throw e;
            }
        }
    }
}

