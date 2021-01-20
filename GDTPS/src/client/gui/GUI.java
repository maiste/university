package client.gui;

import client.DataProvider;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.bundle.LanternaThemes;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import common.LetterBox;
import common.Logs;
import common.Message;
import common.PeerList;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Scanner;

public class GUI implements Runnable {

    private final DataProvider dataProvider;
    private final PeerList ipBook;
    private final LetterBox box;
    private StorePanel storePanel;
    private final Runnable missingArg = () -> println("Missing arguments");
    private InputStream in;
    private WindowBasedTextGUI gui;
    private Panel chatPanel;
    private String username;

    public GUI(DataProvider dataProvider) {
        this.dataProvider = dataProvider;
        this.ipBook = PeerList.get();
        this.box = LetterBox.get();
    }

    @Override
    public void run() {
        DefaultTerminalFactory defaultTerminalFactory = new DefaultTerminalFactory();
        Screen screen = null;
        try {
            screen = defaultTerminalFactory.createScreen();
            screen.startScreen();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
        }
        gui = new MultiWindowTextGUI(screen);
        TextGraphics textGraphics = screen.newTextGraphics();
        Window window = new BasicWindow("GDT");
        var hints = new HashSet<Window.Hint>();
        hints.add(Window.Hint.EXPANDED);
        window.setHints(hints);

        Panel mainPanel = new Panel(new BorderLayout());

        LayoutData layoutDataStore = BorderLayout.Location.LEFT;
        LayoutData layoutDataChat = BorderLayout.Location.CENTER;
        storePanel = new StorePanel(this);
        mainPanel.addComponent(storePanel.setLayoutData(layoutDataStore).withBorder(Borders.singleLine("Terminal")));
        chatPanel = new ChatPanel(this);
        mainPanel.addComponent(chatPanel.setLayoutData(layoutDataChat).withBorder(Borders.singleLine("Chat")));
        header();
        window.setComponent(mainPanel);
        window.setTheme(LanternaThemes.getDefaultTheme());
        gui.addWindowAndWait(window);

        try {
            if (screen != null) {
                screen.stopScreen();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        dataProvider.disconnect();
    }

    public void print(String s) {
        storePanel.setPanelText(s);
    }

    public void println(String s) {
        storePanel.setPanelText(s + "\n");
    }

    public void printf(String format, Object... args) {
        String s = String.format(format, args);
        storePanel.setPanelText(s);

    }

    private String read() {
        StringBuilder s = new StringBuilder();
        Scanner scanner = new Scanner(in);
        while (scanner.hasNextLine()) {
            s.append(scanner.next());
        }
        return s.toString();
    }

    private void header() {
        String head = "********************************\n" + "* Good Duck Transport Protocol *\n"
                + "*        Client Utility        *\n" + "*       By Marais - Viau       *\n"
                + "********************************\n";
        this.println(head);
    }

    public Runnable parse(String command) {
        String[] tokens = command.split("\\s+");
        if (tokens.length < 1)
            return () -> {
            };
        switch (tokens[0]) {
            case "own":
                return dataProvider::getMyAn;
            case "connect":
                return () -> this.username = dataProvider.genericConnect(tokens);
            case "delete":
                if (tokens.length < 2) {
                    return missingArg;
                } else {
                    return () -> {
                        dataProvider.deleteAnc(tokens[1]);
                    };
                }
            case "post":
                if (tokens.length < 5)
                    return () -> createAnnonceGUI(null);
                else {
                    return (() -> dataProvider.postAnc(Arrays.copyOfRange(tokens, 1, tokens.length)));
                }
            case "update":
                if (tokens.length == 2)
                    return () -> {
                        String[] l = new String[5];
                        l[0] = tokens[1];
                        createAnnonceGUI(tokens[1]);
                    };
                else if (tokens.length < 5)
                    return missingArg;
                else {
                    return (() -> dataProvider.updateAnc(Arrays.copyOfRange(tokens, 1, tokens.length)));
                }
            case "exit":
                return dataProvider::disconnect;
            case "echo":
                return () -> println("ECHO :" + command);
            case "domains":
                return dataProvider::getDomains;
            case "ancs":
                if(tokens.length <2) return missingArg;
                return () -> dataProvider.getProductByDomain(tokens[1]);
            case "ip":
                if (tokens.length == 2) {
                    return () -> {
                        Message ipMessage = dataProvider.requestIP(tokens[1]);
                        println(ipMessage.toNetFormat());
                    };
                }
            case "send_msg_to":
                return () -> sendMsg(false, tokens);
            case "talk":
                if (tokens.length < 2) return missingArg;
                var args = new String[]{tokens[0], tokens[1], "Bonjour! Je souhaiterais converser avec vous si vous le voulez bien."};
                return () -> sendMsg(true, args);
            case "":
                return () -> println("");
            case "help":
                return this::displayHelp;
            default:
                return () -> println("Command not found");
        }
    }

    public PeerList getIpBook() {
        return ipBook;
    }

    public LetterBox getBox() {
        return box;
    }

    private void displayHelp() {
        String st = "- connect              Reconnect to the server\n" +
                "- exit                 Quite GTDT Client\n" +
                "- domains              Display available domains\n" +
                "- ancs [DOMAIN]        Display all \"annonces\" from DOMAIN\n" +
                "- own                  Request your \"annonces\"\n" +
                "- post                 Post an \"annonces\" (enter interactive mode)\n" +
                "- update [ANC ID]      Update an \"annonces\"(enter interactive mode)\n" +
                "- talk [ANC ID]        Start a conversation with the \"annonce\" owner\n" +
                "- ip [ANC ID]          Request the ip of the \"annonces\" owner\n" +
                "- delete [ANC ID]      Delete an \"annonces\"";
        println(st);
    }

    private void createAnnonceGUI(String ancId) {
        boolean newEntry = ancId == null;
        BasicWindow window = new BasicWindow(newEntry ? "Create Annonce" : "Updating " + ancId);
        window.setHints(Collections.singletonList(Window.Hint.CENTERED));
        Panel panel = new Panel();
        panel.setLayoutManager(new GridLayout(2));

        final Label lblOutput = new Label("");

        panel.addComponent(new Label("Domain"));
        final TextBox domain = new TextBox().addTo(panel);

        panel.addComponent(new Label("Title"));
        final TextBox title = new TextBox().addTo(panel);

        panel.addComponent(new Label("Description"));
        final TextBox description = new TextBox().addTo(panel);

        panel.addComponent(new Label("Price"));
        final TextBox price = new TextBox().addTo(panel);

        panel.addComponent(new EmptySpace(new TerminalSize(0, 0)));
        panel.addComponent(new EmptySpace(new TerminalSize(0, 0)));

        new Button("Submit", () -> {
            String[] args;
            if (!newEntry) {
                args = new String[]{"null", "null", "null", "null"};
                if (!domain.getText().isEmpty()) args[0] = domain.getText();
                if (!title.getText().isEmpty()) args[1] = title.getText();
                if (!description.getText().isEmpty()) args[2] = description.getText();
                if (!price.getText().isEmpty()) args[3] = price.getText();
                String[] l = new String[5];
                l[0] = ancId;
                System.arraycopy(args, 0, l, 1, 4);
                dataProvider.updateAnc(l);
            } else {
                args = new String[4];
                args[0] = domain.getText();
                args[1] = title.getText();
                args[2] = description.getText();
                args[3] = price.getText();
                dataProvider.postAnc(args);
            }
            window.close();
        }).addTo(panel).setLayoutData(GridLayout.createLayoutData(GridLayout.Alignment.BEGINNING, GridLayout.Alignment.BEGINNING));
        new Button("Cancel", window::close).addTo(panel);
        panel.addComponent(lblOutput);
        window.setComponent(panel.withBorder(Borders.doubleLine()));
        gui.addWindowAndWait(window);
    }

    private String[] prepareMsg(String dest, String msg) {
        String[] msgTable = msg.split("\n");
        String[] args = new String[msgTable.length + 2];
        args[0] = dest;
        args[1] = Long.valueOf(System.currentTimeMillis()).toString();
        for (int i = 0; i < msgTable.length; i++) {
            args[i + 2] = msgTable[i];
        }
        return args;
    }

    void sendMsg(boolean anc, String[] tokens) {
        if (username != null && tokens != null && tokens.length == 3) {
            String dest = null;
            InetSocketAddress addr = null;
            if (anc) {
                Message m = dataProvider.requestIP(tokens[1]);
                if (m != null && m.getArgs() != null && m.getArgs().length == 2) {
                    dest = m.getArgs()[1];
                    addr = new InetSocketAddress(m.getArgs()[0], 7201);
                    ipBook.addOrUpdate(dest, addr);
                    Logs.log("New adress added with success");
                } else {
                    Logs.warning("The server didn't find the address -> drop");
                    return;
                }
            } else {
                dest = tokens[1];
                addr = ipBook.getIp(dest);
                if (addr == null) {
                    Logs.warning("Warning this person can't be found anymore. -> drop");
                    return;
                }
            }
            String[] args = prepareMsg(username, tokens[2]);
            Message request = new Message(Message.MessageType.MSG, args, addr);
            box.addToSendingList(request);
            Logs.log("Insert a new message to send");
        }
    }

}

