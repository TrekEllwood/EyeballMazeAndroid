package nz.ac.ara.tre46.eyeballmaze.interfaces;

import java.util.Set;
import java.util.Collection;

public interface IGoalHolder {
    public void addGoal(int row, int column);

    public int getGoalCount();

    public boolean hasGoalAt(int targetRow, int targetColumn);

    public int getCompletedGoalCount();

    public boolean areAllGoalsCompleted();

    public void completedGoal(int row, int column);

    public void removeGoalAt(int row, int column);

    public Set<IGoal> getGoals();

    public void setGoals(Collection<IGoal> newGoals);
}
