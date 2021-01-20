package client.gui;

import com.googlecode.lanterna.gui2.BorderLayout;
import com.googlecode.lanterna.gui2.TextBox;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;

import java.util.function.Consumer;

public class PromptBox extends TextBox {
    private final Consumer<String> behavior;

    public PromptBox(Consumer<String> behavior) {
        super();
        this.setLayoutData(BorderLayout.Location.CENTER);
        this.behavior = behavior;
    }

    @Override
    public synchronized Result handleKeyStroke(KeyStroke keyStroke) {
        if (keyStroke.getKeyType() == KeyType.Enter) {
            String d = this.getText();
            behavior.accept(d);
            this.setText("");
            return Result.HANDLED;
        }
        return super.handleKeyStroke(keyStroke);
    }
}
