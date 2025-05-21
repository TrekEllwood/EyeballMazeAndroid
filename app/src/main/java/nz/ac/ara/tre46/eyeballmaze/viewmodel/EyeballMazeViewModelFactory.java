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
            return (T) new EyeballMazeViewModel(game);
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}
