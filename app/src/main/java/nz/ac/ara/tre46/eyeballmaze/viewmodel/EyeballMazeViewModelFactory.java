package nz.ac.ara.tre46.eyeballmaze.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import nz.ac.ara.tre46.eyeballmaze.interfaces.IGame;

public class EyeballMazeViewModelFactory implements ViewModelProvider.Factory {
    private final IGame gameInstance;

    public EyeballMazeViewModelFactory(IGame gameInstance) {
        this.gameInstance = gameInstance;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(EyeballMazeViewModel.class)) {
            return (T) new EyeballMazeViewModel(gameInstance);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}
