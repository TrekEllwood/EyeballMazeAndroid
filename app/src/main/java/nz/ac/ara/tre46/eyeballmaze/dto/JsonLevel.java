package nz.ac.ara.tre46.eyeballmaze.dto;

import java.util.List;

public class JsonLevel {
    public int id;
    public int startRow;
    public int startCol;
    public String startDir;
    public List<List<String>> grid;
    public List<GoalData> goals;

    public JsonLevel() {}
}
