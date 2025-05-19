package nz.ac.ara.tre46.eyeballmaze.models;

import java.util.HashSet;
import java.util.Set;
import java.util.Collection;

import nz.ac.ara.tre46.eyeballmaze.interfaces.IGoal;
import nz.ac.ara.tre46.eyeballmaze.interfaces.IGoalHolder;

public class GoalHolder implements IGoalHolder {
    private final Set<IGoal> goals;
    private final Set<IGoal> completedGoals;

    public GoalHolder() {
	this.goals = new HashSet<>();
	this.completedGoals = new HashSet<>();
    }

    @Override
    public void addGoal(int row, int column) {
	goals.add(new Goal(row, column));
    }

    @Override
    public int getGoalCount() {
	return goals.size();
    }

    @Override
    public boolean hasGoalAt(int targetRow, int targetColumn) {
	return goals.contains(new Goal(targetRow, targetColumn));
    }

    @Override
    public int getCompletedGoalCount() {
	return completedGoals.size();
    }

    public boolean areAllGoalsCompleted() {
	return goals.isEmpty();
    }

    @Override
    public void completedGoal(int row, int column) {
	Goal goal = new Goal(row, column);
	if (goals.contains(goal)) {
	    goals.remove(goal);
	    completedGoals.add(goal);
	}
    }

    @Override
    public void removeGoalAt(int row, int column) {
	goals.remove(new Goal(row, column));
    }

    @Override
    public Set<IGoal> getGoals() {
	// Return a copy to avoid exposing internal state
	return new HashSet<>(goals);
    }

    @Override
    public void setGoals(Collection<IGoal> newGoals) {
	goals.clear();
	goals.addAll(newGoals);
	completedGoals.clear();
    }
}
