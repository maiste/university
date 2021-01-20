package test;

import client.gui.ChatPanel;
import client.gui.PromptBox;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;

import java.io.IOException;
import java.util.HashSet;

public class PromptTest {

    public static void main(String[] args) {
       System.out.println("Hello World!");
       DefaultTerminalFactory defaultTerminalFactory = new DefaultTerminalFactory();
//        Screen mainScreen = null;
        Screen mainPrompt = null;
        try {
//             mainScreen= defaultTerminalFactory.createScreen();
            mainPrompt = defaultTerminalFactory.createScreen();
            mainPrompt.startScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
        WindowBasedTextGUI gui = new MultiWindowTextGUI(mainPrompt);
        TextGraphics textGraphics = mainPrompt.newTextGraphics();
        Window win1 = new BasicWindow("Board");
        var hints = new HashSet<Window.Hint>();
        hints.add(Window.Hint.EXPANDED);
        win1.setHints(hints);
        Panel contentPanel = new Panel(new BorderLayout());
        Panel mainPanel = new Panel(new GridLayout(2));
        Label title = new Label("This is a label that spans two columns");
        title.setLayoutData(GridLayout.createLayoutData(
                GridLayout.Alignment.BEGINNING, // Horizontal alignment in the grid cell if the cell is larger than the component's preferred size
                GridLayout.Alignment.BEGINNING, // Vertical alignment in the grid cell if the cell is larger than the component's preferred size
                true,       // Give the component extra horizontal space if available
                false,        // Give the component extra vertical space if available
                2,                  // Horizontal span
                1));                  // Vertical span
        contentPanel.addComponent(title);
        contentPanel.addComponent(new Label("ASLJDLSAJDLASJDKASDKASDJ").setLayoutData(BorderLayout.Location.CENTER));
        Panel promptPanel = new Panel(new BorderLayout());
        TextBox promptBox = new PromptBox(title::setText).setLayoutData(BorderLayout.Location.CENTER);
        promptPanel.addComponent(promptBox);

//        Button submitButton = new Button("Submit").setLayoutData(BorderLayout.Location.RIGHT);
//        submitButton.addListener(button -> System.out.println("LOL"));
//        submitButton.addListener(button -> {
//            if(button.isEnabled()){
//                title.setText(promptBox.getText());
//                promptBox.takeFocus().setText("");
//            }
//        });
//        promptPanel.addComponent(submitButton);
        contentPanel.addComponent(promptPanel.setLayoutData(BorderLayout.Location.BOTTOM));
        mainPanel.addComponent(contentPanel.setLayoutData(GridLayout.createHorizontallyFilledLayoutData()).withBorder(Borders.singleLine("Store")));
//        mainPanel.addComponent(new ChatPanel().setLayoutData(GridLayout.createHorizontallyFilledLayoutData()).withBorder(Borders.singleLine("Chat")));


        win1.setComponent(mainPanel);
        gui.addWindowAndWait(win1);
//        var textInputDialog = new TextInputDialogBuilder()
//                .setTitle("Prompt")
//                .setValidator(new TextInputDialogResultValidator() {
//                    @Override
//                    public String validate(String s) {
//                        return null;
//                    }
//                })
//                .build()
//                .showDialog(gui);
//        Window window1 = new BasicWindow("");
//        gui.addWindow(window1);

        char c ;
        String buffer = "";
        while(true) {
            try {
                assert mainPrompt != null;
                c = mainPrompt.readInput().getCharacter();
                buffer += c;
            } catch (IOException e) {
                e.printStackTrace();
            }
            textGraphics.putString(0,0,buffer);
            try {
                mainPrompt.refresh();
//                mainScreen.refresh();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
