package main;

import java.util.List;

public class ScanOutput {
    public List<String> outputData;
    public String folderName;

    public ScanOutput(List<String> outputData, String folderName) {
        this.outputData = outputData;
        this.folderName = folderName;
    }
}
