# yet-another-alarm

An Android alarm clock. To snooze, answer a trivia question from [Open Trivia Database](https://opentdb.com). A wrong answer means waiting for the next question.

- Multiple alarms with repeat days and a per-alarm snooze length
- Plays your own sound files; vibrates if the file is gone
- Material 3 Expressive, Material You colors
- No proprietary dependencies

Requires Android 12+.

## Install

With [Obtainium](https://github.com/ImranR98/Obtainium), add this repository's URL. Or download the APK from Releases.

On first launch, grant the permissions the app lists, or alarms may not ring.

## Build

```sh
devenv shell -- ./gradlew assembleDebug
```

## License

GPL-3.0. Trivia questions are from Open Trivia Database, licensed CC BY-SA 4.0.
