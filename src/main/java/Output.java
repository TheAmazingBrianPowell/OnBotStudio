/*
 * Copyright (c) 2022 Brian Powell
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons
 * to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 * OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE
 * OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.io.IOException;
import java.util.Objects;

public class Output implements ToolWindowFactory {
    static JButton compile;
    static JButton compiling;
    static ConsoleView console;
    static String projectLoc;

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        JLayeredPane window = new JLayeredPane();
        Output.projectLoc = Objects.requireNonNull(project).getBasePath();
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        Content content = contentFactory.createContent(window, "", false);
        console = TextConsoleBuilderFactory.getInstance().createBuilder(project).getConsole();
        toolWindow.getContentManager().addContent(content);


        try {
            compile = new JButton();
            compile.setIcon(new ImageIcon(ImageIO.read(Objects.requireNonNull(getClass().getResource("icons/compile.png"))).getScaledInstance(40, 40, java.awt.Image.SCALE_SMOOTH)));
            compile.setFocusable(false);
            compile.setContentAreaFilled(false);
            compile.setBorder(BorderFactory.createEmptyBorder());
            compiling = new JButton();
            compiling.setFocusable(false);
            compiling.setContentAreaFilled(false);
            compiling.setBorder(BorderFactory.createEmptyBorder());
            compiling.setIcon(new ImageIcon(Objects.requireNonNull(getClass().getResource(("icons/loading.gif")))));
            compiling.setVisible(false);
        } catch (IOException ignored) {
            compile = new JButton("Compile");
        }
        SpringLayout springLayout = new SpringLayout();
        window.setLayout(springLayout);
        compile.addActionListener(e -> {
            FileDocumentManager.getInstance().saveAllDocuments();
            new Build().start();
        });
        springLayout.putConstraint(SpringLayout.EAST, compile, -15, SpringLayout.EAST, window);
        springLayout.putConstraint(SpringLayout.SOUTH, compile, -20, SpringLayout.SOUTH, window);

        springLayout.putConstraint(SpringLayout.EAST, compiling, -15, SpringLayout.EAST, window);
        springLayout.putConstraint(SpringLayout.SOUTH, compiling, -18, SpringLayout.SOUTH, window);

        springLayout.putConstraint(SpringLayout.NORTH, console.getComponent(), 0, SpringLayout.NORTH, window);
        springLayout.putConstraint(SpringLayout.SOUTH, console.getComponent(), 0, SpringLayout.SOUTH, window);
        springLayout.putConstraint(SpringLayout.WEST, console.getComponent(), 0, SpringLayout.WEST, window);
        springLayout.putConstraint(SpringLayout.EAST, console.getComponent(), 0, SpringLayout.EAST, window);
        window.add(compile, Integer.valueOf(2));
        window.add(compiling, Integer.valueOf(1));
        window.add(console.getComponent(), Integer.valueOf(0));
        Disposer.register(project, console);
    }
}