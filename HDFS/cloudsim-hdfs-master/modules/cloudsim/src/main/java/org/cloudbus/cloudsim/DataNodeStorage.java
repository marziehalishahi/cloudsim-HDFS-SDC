package org.cloudbus.cloudsim;



public class DataNodeStorage {
    private StringBuilder originalData;

    // Constructor
    public DataNodeStorage() {
        this.originalData = new StringBuilder();
    }

    // Getter for originalData
    public StringBuilder getOriginalData() {
        return originalData;
    }

    // Setter for originalData
    public void setOriginalData(StringBuilder originalData) {
        this.originalData = originalData;
    }

    // Method to apply Level 1: Append 100 bits to original data
    public void applyLevelOne(int dataNode) {
        originalData.append(generateBits(100));
        System.out.println("Level 1 applied to Data Node " + dataNode + ": 100 bits added");
    }

    // Method to apply Level 2: Append 200 bits to original data
    public void applyLevelTwo(int dataNode) {
        originalData.append(generateBits(200));
        System.out.println("Level 2 applied to Data Node " + dataNode + ": 200 bits added");
    }

    // Method to apply Level 3: Append 400 bits to original data
    public void applyLevelThree(int dataNode) {
        originalData.append(generateBits(400));
        System.out.println("Level 3 applied to Data Node " + dataNode + ": 400 bits added");
    }

    // Method to generate a string of '0's with the specified length
    @org.jetbrains.annotations.NotNull
    @org.jetbrains.annotations.Contract(pure = true)
    private String generateBits(int length) {
        return "0".repeat(length);
    }
}

