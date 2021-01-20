package client.gui;

import com.googlecode.lanterna.gui2.BorderLayout;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.TextBox;

public class StorePanel extends Panel {
    Label contentLabel;
    GUI gui;
    private String panelText = "";

    public StorePanel(GUI gui) {
        super(new BorderLayout());
        this.gui = gui;
        contentLabel = new Label(panelText);
        TextBox promptBox = new PromptBox(s -> gui.parse(s).run())
                .setLayoutData(BorderLayout.Location.BOTTOM);
        contentLabel.getSize();

        this.addComponent(contentLabel.setLayoutData(BorderLayout.Location.CENTER));
        this.addComponent(promptBox);

    }

    public void setPanelText(String text) {
        this.panelText += text;
        var textSize = panelText.lines().count();
        var to_crop = textSize - this.getSize().getRows() + 1;
        to_crop = (to_crop < 0) ? 0 : to_crop;
        String fittedText = this.panelText.replaceFirst("(.*\\n){" + to_crop + "}",
                "");
        contentLabel.setText(fittedText);
    }
}
