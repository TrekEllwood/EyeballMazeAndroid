package nz.ac.ara.tre46.eyeballmaze.viewmodel;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import nz.ac.ara.tre46.eyeballmaze.models.Game;

public class EyeballMazeViewModelFactory implements ViewModelProvider.Factory {
    private final Context context;

    public EyeballMazeViewModelFactory(Context context) {
        this.context = context.getApplicationContext();
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(EyeballMazeViewModel.class)) {
            Game game = new Game(context);

            try {
                game.loadLevelsFromAssets(context);
            } catch (RuntimeException e) {
                throw new RuntimeException("Failed to load levels from assets. Ensure levels.json exists in res/raw.", e);
            }

            return (T) new EyeballMazeViewModel(context, game);
        }

        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}
