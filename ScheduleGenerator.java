import java.util.List;
import java.util.Set;
import java.util.Map;

import java.util.LinkedList;
import java.util.ArrayList;
import java.util.HashMap;

import java.util.Collections;

import java.util.Queue;
import java.util.ArrayDeque;

public class ScheduleGenerator {

    private Map<String, List<Tile>> tileMap;
    private List<String> roomTypes;
    private Grid grid;

    public static List<String> getStringList(List<List<Tile>> schedules) {
        
        List<String> list = new ArrayList<String>();
        
        for (List<Tile> schedule : schedules) {
            
            StringBuilder ss = new StringBuilder();
            ss.append("[ ");
            
            for (int i = 0; i < schedule.size(); i++) {
                
                Tile current = schedule.get(i);
                ss.append("(" + current.getRow() + ", " + current.getCol() + ") ");
                
                if (i != schedule.size() - 1) {
                    ss.append(" - ");
                }
            }

            ss.append("]");
            list.add(ss.toString());
        }

        return list;
    }

    public ScheduleGenerator(Grid grid) {
        
        this.grid = grid;

        Set<Tile> totalValidTiles = grid.getValidTiles();
        tileMap = new HashMap<String, List<Tile>>();
        
        for (Tile tile : totalValidTiles) {
            
            if ("floor".equals(tile.getAttribute())) {
                continue;
            }

            if (!tileMap.containsKey(tile.getAttribute())) {
                tileMap.put(tile.getAttribute(), (List<Tile>)new ArrayList<Tile>());
            }

            tileMap.get(tile.getAttribute()).add(tile);
        }

        roomTypes = new ArrayList<String>(tileMap.keySet());
    }

    public List<List<Tile>> generateSchedules(int numberOfSchedules) {
        
        List<List<Tile>> schedules = new ArrayList<List<Tile>>();
        
        for (int i = 0; i < numberOfSchedules; i++) {
            
            List<Tile> randomSchedule = new ArrayList<Tile>();
            Collections.shuffle(roomTypes);
            
            for (String roomType : roomTypes) {

                List<Tile> choices = tileMap.get(roomType);
                int targetIdx = (int)(Math.random() * choices.size());
                
                randomSchedule.add(choices.get(targetIdx));
            }

            schedules.add(randomSchedule);
        }

        return schedules;
    }

    // for ONE ACTOR's schedule
    // get paths for all periods
    public List<List<Tile>> getPathsForSchedule(List<Tile> schedule) {

        if (schedule.size() <= 1) {
            throw new RuntimeException("Cannot pathfind for a schedule of only one target");
        }
        
        List<List<Tile>> paths = new ArrayList<List<Tile>>();
        
        for (int i = 0; i < schedule.size() - 1; i++) {
            paths.add(getBFSPath(schedule.get(i), schedule.get(i + 1)));
        }
        
        return paths;
    }

    private List<Tile> getBFSPath(Tile from, Tile to) {
        
        Tile[][] parents = new Tile[grid.getHeight()][grid.getWidth()];
        Queue<Tile> bfs = new ArrayDeque<Tile>();
        
        bfs.add(from);
        parents[from.getRow()][from.getCol()] = from;

        while (bfs.size() != 0) {
            
            Tile current = bfs.poll();
            
            if (current.equals(to)) {
                break;
            }

            for (Tile neighbor : current.getNeighbors()) {
                
                if (parents[neighbor.getRow()][neighbor.getCol()] == null) {
                    parents[neighbor.getRow()][neighbor.getCol()] = current;
                    bfs.add(neighbor);
                }
            }
        }

        LinkedList<Tile> path = new LinkedList<Tile>();
        Tile current = to;
        
        do {
            path.addFirst(current);
            current = parents[current.getRow()][current.getCol()];
        }
        while (!parents[current.getRow()][current.getCol()].equals(current));

        ArrayList<Tile> convertedPath = new ArrayList<Tile>();
        
        for (Tile copy : path) {
            convertedPath.add(copy);
        }

        return convertedPath;
    }
}
