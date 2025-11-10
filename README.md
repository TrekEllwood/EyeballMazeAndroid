# EyeballMaze

EyeballMaze is an Android puzzle game / demo app where the player guides an eyeball through mazes and obstacles. This repository contains the Android Studio project and build files required to build and run the app locally.

## Table of contents

- About
- Features
- Requirements
- Building & running (Windows / PowerShell)
- Project structure
- Contributing
- License

## About

EyeballMaze is a small interactive Android game built with the Android SDK. It demonstrates custom view rendering, touch input handling, game loop basics, and simple level logic. Use this repository as a learning reference or as the starting point for adding new levels, graphics, and gameplay mechanics.

## Features

- Maze navigation with swipe/touch controls
- Physics-lite movement for the eyeball
- Multiple levels (placeholders)
- Simple collision detection and level completion

## Requirements

- Android Studio (Electric Eel or newer recommended)
- JDK 11+ (as required by your Android Gradle Plugin version)
- Android SDK (API levels used by the project are defined in `app/build.gradle`)
- Gradle wrapper included in the repo (`gradlew` / `gradlew.bat`)

## Project structure

Top-level important files and folders:

- `app/` — main Android application module
	- `src/main/java/` — application source code
	- `src/main/res/` — layouts, drawables, and resources
	- `AndroidManifest.xml` — app manifest
	- `build.gradle` — module build configuration
- `gradle/`, `gradlew`, `gradlew.bat` — Gradle wrapper and configuration
- `build.gradle`, `settings.gradle` — top-level Gradle files

If you want to add levels, look for the level/asset loader code in `app/src/main/java/...` (search for classes named with Level, Maze, or GameView).

## Contributing

Contributions are welcome. A simple suggested workflow:

1. Fork the repository.
2. Create a feature branch: `git checkout -b feature/my-feature`
3. Make changes and add tests where appropriate.
4. Run the app and tests locally.
5. Create a pull request with a clear description of changes.

Please follow these guidelines when contributing:
- Keep UI and gameplay changes incremental and testable.
- Add or update unit tests for logic changes when possible.
- Document new public APIs or resource expectations.

## Video tutorial

There is a short video tutorial / rules file included in the project resources:

<video controls width="640" poster="https://raw.githubusercontent.com/TrekEllwood/EyeballMazeAndroid/master/app/src/main/res/drawable/eyeball_preview.png">
  <source src="https://raw.githubusercontent.com/TrekEllwood/EyeballMazeAndroid/master/app/src/main/res/raw/eyeball_maze_rules.mp4" type="video/mp4">
  Your browser does not support the video tag. <a href="https://raw.githubusercontent.com/TrekEllwood/EyeballMazeAndroid/master/app/src/main/res/raw/eyeball_maze_rules.mp4">Download the video</a>.
</video>

- Local path: `app/src/main/res/raw/eyeball_maze_rules.mp4`

You can open or play this file directly in the GitHub web UI (click the path above), or on a local machine open it from the project folder. In the running app the resource is accessible as `R.raw.eyeball_maze_rules` and can be played with a `MediaPlayer` or `VideoView`.

## License

This project is licensed under the MIT License — see the `LICENSE` file in the repository root for details.

## Contact / Questions

If you have questions about running the project or want help adding features, include details in an issue or reach out to the project owner.

---
