import java.util.*;
import javafx.scene.control.TextArea;

public class MessageLog {
    
    private TextArea outputTextArea;
    private Queue<String> buffer;
    private final int bufferSize;
    
    public MessageLog(int bufferSize) {
        
        this.bufferSize = bufferSize <= 0 ? 10 : bufferSize;

        outputTextArea = new TextArea();

        outputTextArea.setEditable(false);
        outputTextArea.setPrefRowCount(6);
        outputTextArea.setWrapText(true);

        buffer = new LinkedList<String>();
    }

    public TextArea getTextArea() {
        return outputTextArea;
    }

    public void println(String line) {
        
        if (buffer.size() == bufferSize) {
            buffer.remove();
        }

        buffer.add(line);

        StringBuilder sbr = new StringBuilder();
        ListIterator<String> it 
            = ((LinkedList<String>)buffer).listIterator(buffer.size()); 
        
        while (it.hasPrevious()) {
            sbr.append(it.previous());
            if (it.hasPrevious()) {
                sbr.append("\n");
            }
        }

        outputTextArea.setText(sbr.toString());
    }
}
