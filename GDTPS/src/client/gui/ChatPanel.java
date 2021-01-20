package client.gui;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.*;
import common.Logs;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;

public class ChatPanel extends Panel {
    private final ComboBox<String> comboBox;
    private final ConcurrentHashMap<String, String> messageHistory = new ConcurrentHashMap<>();
    private final Label text = new Label("Welcome to the GDT Chat!");
    private final GUI gui;
    private final Panel chatPanel = new Panel(new BorderLayout());
    private final Panel blankPanel = new Panel().addComponent(new EmptySpace(new TerminalSize(0, 0)));
    private final Panel currentPanel = chatPanel;
    private String panelText = "";
    private boolean isHidden = true;
    public ChatPanel(GUI gui) {
        super(new GridLayout(1));
        this.gui = gui;
        this.setPreferredSize(new TerminalSize(0,0));
        comboBox = new ComboBox<String>();
        Thread thread = new Thread(new LetterBoxThread());
        thread.start();

        PromptBox promptBox = new PromptBox(s -> {
            synchronized (messageHistory) {
                if (!s.isEmpty()) {
                    String user = comboBox.getSelectedItem();
                    String[] args = {"send_msg", user, s};
                    String hist = messageHistory.getOrDefault(user, "");
                    String newMessage = hist + "\n" + currentTimeToString() + "<me> " + s;
                    messageHistory.put(user, newMessage);
                    gui.sendMsg(false, args);
                    updatePanel();
                }
            }
        });
        chatPanel.addComponent(comboBox.setLayoutData(BorderLayout.Location.TOP).withBorder(Borders.doubleLineBevel()));
        chatPanel.addComponent(text.setLayoutData(BorderLayout.Location.CENTER));
        chatPanel.addComponent(promptBox.setLayoutData(BorderLayout.Location.BOTTOM));
        comboBox.addListener((i, i1) -> updatePanel());
    }

    public ComboBox<String> getUserList() {
        return comboBox;
    }

    private synchronized void updatePanel() {
        if (isHidden) {
            this.removeAllComponents();
            this.addComponent(currentPanel.setLayoutData(GridLayout.createLayoutData(
                    GridLayout.Alignment.FILL, GridLayout.Alignment.FILL, true, true
            )));
            isHidden = false;
        }
        String selectedUser = comboBox.getSelectedItem();
        if (selectedUser == null) {
            return;
        }
        panelText = messageHistory.getOrDefault(selectedUser, "");
        setText("");
    }

    private String currentTimeToString() {
        Date d = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("[HH:mm]");
        sdf.setTimeZone(TimeZone.getDefault());
        return sdf.format(d);
    }

    private synchronized void updatePeerList() {
        peerListToComboBox();
        if (gui.getIpBook().getPeerList().isEmpty()) {
            Logs.log("No contact yet");
        }
    }

    private void peerListToComboBox() {
        var peerList = gui.getIpBook().getPeerList();
        comboBox.clearItems();
        peerList.forEach(comboBox::addItem);
//        peerList.forEach(s ->
//        {
//            boolean isInComboBox = false;
//            for (int i = 0; i < comboBox.getItemCount(); i++) {
//
//                if (s.equals(comboBox.getItem(i))) {
//                    isInComboBox = true;
//                    break;
//                }
//            }
//            if (!isInComboBox) comboBox.addItem(s);
//        });
    }

    private void textAppend(String s) {
        text.setText(text.getText() + "\n" + s);
    }

    private void setText(String s) {
//        this.panelText += s;
        var textSize = panelText.lines().count();
        var to_crop = textSize - this.getSize().getRows() + 4;
        to_crop = (to_crop < 0) ? 0 : to_crop;
        String fittedText = this.panelText.replaceFirst("(.*\\n){" + to_crop + "}",
                "");
        text.setText(fittedText);
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

    /**
     * Thread which will update messages received
     */
    public class LetterBoxThread implements Runnable {
        final int updateFrequency = 300;

        @Override
        public synchronized void run() {
            //TODO Hide chat panel when there is nobody in the contact list
            var letterBox = ChatPanel.this.gui.getBox();
            Set<String> peerList = gui.getIpBook().getPeerList();
            while (true) {
                try {
                    Thread.sleep(updateFrequency);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                peerList.forEach(s -> {
                    var messages = letterBox.getNewMsgFor(s);
                    if (messages != null) {
                        var stringMessages = messages.stream().map(message -> {
                            if (message.getArgs().length >= 3) {
                                var name = message.getArgs()[0];
                                var date_s = message.getArgs()[1];
                                long milli = Long.parseLong(date_s);
                                Date date = new Date(milli);
                                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
                                sdf.setTimeZone(TimeZone.getDefault());
                                var final_date_s = sdf.format(date);
                                var msg_content = message.getArgs()[2];
                                return "[" + final_date_s + "]<" + name + "> " + msg_content;
                            }
                            return "Wrong format";
                        });
                        var stringMessage = stringMessages.reduce((s1, s2) -> s1 + "\n" + s2);
                        stringMessage.ifPresent(value -> {
                            var new_text = messageHistory.getOrDefault(s, "") + "\n" + value;
                            messageHistory.put(s, new_text);
                        });
                        updatePeerList();
                        updatePanel();
                    }
                });
            }
        }
    }


}
