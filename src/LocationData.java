import java.util.*;
import javafx.scene.paint.Color;

public class LocationData {

    private ScheduleGenerator generator;
    private List<List<Tile>> schedules;
    private Grid grid;
    
    // 1st idx : a'th actor
    // 2nd idx : p'th period
    // 3rd idx : n'th step
    private List<List<List<Tile>>> pathsAllActors;
    
    // 1st idx : p'th period
    // 2nd idx : n'th step
    // 3rd key : k'th tile
    private List<List<Map<Tile, Integer>>> tileUsage;

    private int period;
    private int lastTransitionPeriod;
    
    private int maxUsage;
    
    private int step;
    private int[] maxStepsAllPeriods;

    public LocationData(ScheduleGenerator generator, List<List<Tile>> schedules, Grid grid) {
        
        this.generator = generator;
        this.schedules = schedules;
        this.grid = grid;

        this.period = 0;
        this.lastTransitionPeriod = schedules.get(0).size() - 1;

        this.maxUsage = 0;

        this.step = 0;
        this.maxStepsAllPeriods = new int[lastTransitionPeriod];
        Arrays.fill(maxStepsAllPeriods, 0);

        initPaths();
        initTileUsage();
    }

    private void initPaths() {
        
        pathsAllActors = new ArrayList<List<List<Tile>>>();

        for (List<Tile> schedule : schedules) {
            List<List<Tile>> pathsOneActor = generator.getPathsForSchedule(schedule);
            pathsAllActors.add(pathsOneActor);
        }
    }

    private void initTileUsage() {
        
        tileUsage = new ArrayList<List<Map<Tile, Integer>>>();
        
        for (int p = 0; p < lastTransitionPeriod; p++) {
            tileUsage.add(new ArrayList<Map<Tile, Integer>>());
        }

        for (List<List<Tile>> pathsOneActorAllPeriods : pathsAllActors) {
            
            for (int p = 0; p < lastTransitionPeriod; p++) {
                
                List<Map<Tile, Integer>> usageOnePeriodAllSteps = tileUsage.get(p);
                List<Tile> pathOneActorOnePeriod = pathsOneActorAllPeriods.get(p);
                
                int numSteps = pathOneActorOnePeriod.size();
                maxStepsAllPeriods[p] = Math.max(maxStepsAllPeriods[p], numSteps);

                // pad out to number of steps (compute if absent)
                while (usageOnePeriodAllSteps.size() < numSteps) {
                    
                    Map<Tile, Integer> usageOnePeriodOneStep = new HashMap<Tile, Integer>();
                    
                    for (Tile validTile : grid.getValidTiles()) {
                        usageOnePeriodOneStep.put(validTile, 0);
                    }

                    usageOnePeriodAllSteps.add(usageOnePeriodOneStep);
                }

                for (int n = 0; n < pathOneActorOnePeriod.size(); n++) {

                    Map<Tile, Integer> usageOnePeriodOneStep = usageOnePeriodAllSteps.get(n);

                    Tile currentTile = pathOneActorOnePeriod.get(n);
                    
                    int oldUsage = usageOnePeriodOneStep.get(currentTile);
                    int newUsage = oldUsage + 1;

                    usageOnePeriodOneStep.put(currentTile, newUsage);
                    maxUsage = Math.max(newUsage, maxUsage);
                }
            }
        }
    }
    
    public int getPeriod() { return period; }

    public boolean increasePeriod() {
        if (!canIncreasePeriod()) {
            return false;
        }
        period++;
        step = 0;
        return true;
    }

    public boolean decreasePeriod() {
        if (!canDecreasePeriod()) {
            return false;
        }
        period--;
        step = 0;
        return true;
    }

    public boolean canIncreasePeriod() { return period + 1 < lastTransitionPeriod; }
    public boolean canDecreasePeriod() { return period > 0;                        }

    public int getStep()    { return step;                       }
    public int getMaxStep() { return maxStepsAllPeriods[period]; }

    public boolean increaseStep() {
        if (!canIncreaseStep()) {
            return false;
        }
        step++;
        return true;
    }

    public boolean decreaseStep() {
        if (!canDecreaseStep()) {
            return false;
        }
        step--;
        return true;
    }

    public boolean canIncreaseStep() { return step + 1 < maxStepsAllPeriods[period]; }
    public boolean canDecreaseStep() { return step > 0;                              }
    public boolean setStep(int updatedStep) {
        if (updatedStep >= 0 && updatedStep < maxStepsAllPeriods[period]) {
            this.step = updatedStep;
            return true;
        }
        return false;
    }

    public int getUsageAt(int period, int step, Tile tile) {

        return tileUsage.get(period).get(step).get(tile);
    }

    public Color getIntensityAt(int period, int step, Tile tile) {
        
        int usage = getUsageAt(period, step, tile);

        double scaleFactor = ((double) usage) / ((double) maxUsage);
        scaleFactor = (0.7 * Math.sqrt(Math.sqrt(scaleFactor))) + 0.3;

        float red = (float) Math.max(0.0, -1.0 + (2.0 * scaleFactor));
        float green = (float) Math.min(1.0, 2.0 - (2.0 * scaleFactor));

        Color intensity = Color.color(red, green, 0);
        return intensity;
    }
}

