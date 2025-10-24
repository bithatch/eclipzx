package uk.co.bithatch.zxbasic.ui.views;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.part.ViewPart;

public class MemoryMapView extends ViewPart {
    public static final String ID = "uk.co.bithatch.zxbasic.ui.views.MemoryMapView";

    private Composite parent;
    private List<MemoryBlock> memoryBlocks = new ArrayList<>();

    @Override
    public void createPartControl(Composite parent) {
        this.parent = parent;
        loadMemoryMap(new File("/home/SOUTHPARK/tanktarta/Desktop/Eclipsz/test-basic/bin/main.map")); // Load on start
        drawMemoryMap();
    }

    @Override
    public void setFocus() {
        parent.setFocus();
    }

    private void loadMemoryMap(File file) {
        memoryBlocks.clear();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Pattern pattern = Pattern.compile("^([0-9A-Fa-f]{4}):\\s+(.+)$");
                Matcher matcher = pattern.matcher(line);
                if (matcher.matches()) {
                    int address = Integer.parseInt(matcher.group(1), 16);
                    String label = matcher.group(2);
                    memoryBlocks.add(new MemoryBlock(address, label));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void drawMemoryMap() {
        Canvas canvas = new Canvas(parent, SWT.NONE);
        canvas.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        canvas.addPaintListener(e -> drawBlocks(e.gc, canvas));
    }

    private void drawBlocks(GC gc, Canvas canvas) {
        int height = canvas.getSize().y;
        int width = canvas.getSize().x;

        if (memoryBlocks.isEmpty()) return;

        int min = memoryBlocks.get(0).address;
        int max = memoryBlocks.get(memoryBlocks.size() - 1).address;
        int totalSpan = max - min;

        Map<String, Color> labelColors = new HashMap<>();
        Color[] palette = getColorPalette(canvas.getDisplay());

        String lastPrefix = "";
        int colorIndex = 0;

        for (int i = 0; i < memoryBlocks.size(); i++) {
            MemoryBlock block = memoryBlocks.get(i);
            String prefix = extractPrefix(block.label);
            if (!prefix.equals(lastPrefix)) {
                lastPrefix = prefix;
                colorIndex = (colorIndex + 1) % palette.length;
            }

            int nextAddr = (i + 1 < memoryBlocks.size()) ? memoryBlocks.get(i + 1).address : block.address + 1;
            int span = nextAddr - block.address;

//            System.out.println("p; " + prefix + " sp: " + span + " c: " + colorIndex);

            int y = (block.address - min) * height / totalSpan;
            int h = Math.max(1, span * height / totalSpan);

            gc.setBackground(palette[colorIndex]);
            gc.fillRectangle(10, y, width - 20, h);
        }
    }

    private String extractPrefix(String label) {
        int secondDot = label.indexOf('.', 1); // skip leading dot
        if (secondDot > 0) { 
        	label = label.substring(0, secondDot);
        	if(label.startsWith("__")) {
        		label = label.substring(2);
        	}
        	secondDot = label.indexOf('_');
        	if(secondDot == -1)
        		return label;
        	else 
            	return label.substring(0, secondDot);
        }
        else
        	return label;
    }

    private Color[] getColorPalette(Display display) {
        RGB[] materialRGBs = new RGB[]{
            new RGB(244, 67, 54), new RGB(233, 30, 99), new RGB(156, 39, 176),
            new RGB(103, 58, 183), new RGB(63, 81, 181), new RGB(33, 150, 243),
            new RGB(3, 169, 244), new RGB(0, 188, 212), new RGB(0, 150, 136),
            new RGB(76, 175, 80), new RGB(139, 195, 74), new RGB(205, 220, 57),
            new RGB(255, 235, 59), new RGB(255, 193, 7), new RGB(255, 152, 0),
            new RGB(255, 87, 34)
        };
        return Arrays.stream(materialRGBs)
                     .map(rgb -> new Color(display, rgb))
                     .toArray(Color[]::new);
    }

    static class MemoryBlock {
        int address;
        String label;
        MemoryBlock(int address, String label) {
            this.address = address;
            this.label = label;
        }
    }
}
